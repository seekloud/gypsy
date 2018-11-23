package com.neo.sk.gypsy.core

import java.io.FileReader

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import akka.stream.testkit.TestPublisher.Subscribe
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.shared.ptcl.Protocol.GameEvent
//import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.models.Dao.RecordDao
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey, GetRecordFrameMsg, GetUserInRecordMsg}
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl._
import org.seekloud.essf.io.{EpisodeInfo, FrameData, FrameInputStream}

import scala.concurrent.duration.FiniteDuration
import com.neo.sk.gypsy.utils.ESSFSupport._
import org.seekloud.essf.io.FrameInputStream
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl
import com.neo.sk.gypsy.shared.ptcl.Protocol.{GameInformation, GameMessage, ReplayFrameData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.concurrent.duration._
import org.seekloud.byteobject._


/**
  * @author zhaoyin
  * 2018/10/25  下午12:54
  */
object GamePlayer {

  private final val log = LoggerFactory.getLogger(this.getClass)

  private val waitTime = 10.minutes
  trait Command

  case class TimeOut(msg:String) extends Command
  case object GameLoop extends Command
  case class StopReplay(recordId:Long) extends Command

  final case class SwitchBehavior(
                                 name: String,
                                 behavior: Behavior[Command],
                                 duration: Option[FiniteDuration] = None,
                                 timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  private final case object BehaviorChangeKey
  private final case object BehaviorWaitKey
  private final case object GameLoopKey

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  /**来自UserActor的消息**/
  case class InitReplay(userActor: ActorRef[WsMsgSource], userId: String,frame:Int) extends Command

  def create(recordId: Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"GamePlayer is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        //操作数据库
        RecordDao.getRecordById(recordId).map {
          case Some(r)=>
            val replay=initFileReader(r.filePath)
            val info=replay.init()
            try{
              ctx.self ! SwitchBehavior("work",
                work(
                  replay,
                  metaDataDecode(info.simulatorMetadata).right.get,
                  initStateDecode(info.simulatorInitState).right.get.asInstanceOf[Protocol.GypsyGameSnapshot],
                  info.frameCount,
                  userMapDecode(replay.getMutableInfo(AppSettings.essfMapKeyName).getOrElse(Array[Byte]())).right.get.m,
                ))
            }catch {
              case e:Throwable=>
                log.error("error---"+e.getMessage)
                ctx.self ! SwitchBehavior("initError", initError)
            }
          case None=>
            log.debug(s"record--$recordId didn't exist!!")
            ctx.self ! SwitchBehavior("initError", initError)
        }
        switchBehavior(ctx,"busy",busy())
      }
    }
  }

  def work(fileReader: FrameInputStream,
           metaData:Protocol.GameInformation,
           initState:Protocol.GypsyGameSnapshot,
           frameCount:Int,
           userMap:List[(EssfMapKey,EssfMapJoinLeftInfo)],
           userOpt:Option[ActorRef[WsMsgSource]]= None
          )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer: MiddleBufferInJvm
  ):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay =>
          log.info("start new replay !")
          //停止之前的重放
//          timer.cancelAll()
          timer.cancel(GameLoopKey)

//          fileReader.mutableInfoIterable
//          log.info(s"UserMap-------$msg | $userMap---------")
//          log.info(s"metaData------- | $metaData********")
//          log.info(s"initState------- | $initState========")
          userMap.filter(t=>t._1.userId == msg.userId && t._2.leftF >= msg.frame).sortBy(_._2.joinF).headOption match {
            case Some(u)=>
              log.info(s"total FrameCount :${frameCount}")
              log.info(s"set replay from frame=${msg.frame}")
              fileReader.gotoSnapshot(msg.frame)
//              log.info(s"replay from frame=${fileReader.getFramePosition}")
              if(fileReader.hasMoreFrame){
                timer.startPeriodicTimer(GameLoopKey, GameLoop, 150.millis)
                work(fileReader,metaData,initState,frameCount,userMap,Some(msg.userActor))
              }else{
//                timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"),waitTime)
                Behaviors.stopped
              }
            case None =>
              dispatchTo(msg.userActor,Protocol.InitReplayError("本局游戏中不存在该用户"))
//              timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"), waitTime)
              Behaviors.stopped
          }
        case GameLoop=>
//          println(s"Loop ${fileReader.getFramePosition}========")
          if(fileReader.hasMoreFrame){
            userOpt.foreach(u=>
              fileReader.readFrame().foreach{ f=>
//                println(s" f: ${f.eventsData}  ********** ")
                dispatchByteTo(u,f)
              }
            )
            Behaviors.same
          }else{
            println(s"replay finish!")
            userOpt.foreach { u =>
              dispatchTo(u, Protocol.ReplayFinish())
            }
            timer.cancel(GameLoopKey)
            Behaviors.stopped
          }

        case msg:GetRecordFrameMsg=>
          msg.replyTo ! GetRecordFrameRsp(RecordFrameInfo(fileReader.getFramePosition,frameCount.toLong))
          Behaviors.same

        case msg:GetUserInRecordMsg=>
          println(s"GamePlayer Receive !! ")
          val data=userMap.groupBy(r=>(r._1.userId,r._1.name)).map{r=>
            val fList=r._2.map(f=>ExistTimeInfo(f._2.joinF-initState.state.frameCount,f._2.leftF-initState.state.frameCount))
            PlayerInRecordInfo(r._1._1,r._1._2,fList)
          }.toList
          println(s"GetUserInRecordMsg: $data ################$frameCount ")
          msg.replyTo ! userInRecordRsp(PlayerList(frameCount,data))
          Behaviors.same

//        case msg:TimeOut=>
//          Behaviors.stopped

        case msg:StopReplay =>
          log.info(s"Stop Replay! ${msg.recordId}")
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same

      }
    }
  }

  private def initError(
                       implicit sendBuffer: MiddleBufferInJvm
                       ):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay =>
          dispatchTo(msg.userActor,Protocol.InitReplayError("游戏文件不存在或者已损坏！！"))
          Behaviors.stopped

        case msg:GetRecordFrameMsg=>
          msg.replyTo ! ErrorRsp(10001,"init error")
          Behaviors.stopped

        case msg:GetUserInRecordMsg=>
          msg.replyTo ! ErrorRsp(10001,"init error")
          Behaviors.stopped
      }
    }
  }

  import org.seekloud.byteobject.ByteObject._

  def dispatchTo(subscribe: ActorRef[WsMsgSource], msg:GameEvent)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribe ! ReplayFrameData(List(msg).fillMiddleBuffer(sendBuffer).result())
  }

  def dispatchByteTo(subscribe:ActorRef[WsMsgSource], msg:FrameData)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribe ! ReplayFrameData(msg.eventsData)
    // foreach和map都可以去掉Option
    msg.stateData.foreach(s => subscribe ! ReplayFrameData(s))
  }

  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
          switchBehavior(ctx, name, behavior,durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behaviors.same
      }
    }



}
