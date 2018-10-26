package com.neo.sk.gypsy.core

import java.io.FileReader

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.models.Dao.RecordDao
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey}
import org.seekloud.essf.io.{EpisodeInfo, FrameData, FrameInputStream}

import scala.concurrent.duration.FiniteDuration
import com.neo.sk.gypsy.utils.ESSFSupport._
import org.seekloud.essf.io.FrameInputStream
import com.neo.sk.gypsy.shared.ptcl.GypsyGameEvent
import com.neo.sk.gypsy.shared.ptcl.GypsyGameEvent.{GameInformation, ReplayFrameData}

import scala.language.implicitConversions
import scala.concurrent.duration._


/**
  * @author zhaoyin
  * @date 2018/10/25  下午12:54
  */
object GamePlayer {

  private final val log = LoggerFactory.getLogger(this.getClass)

  private val waitTime = 10.minutes
  trait Command

  case class TimeOut(msg:String) extends Command
  case object GameLoop extends Command

  final case class SwitchBehavior(
                                 name: String,
                                 behavior: Behavior[Command],
                                 duration: Option[FiniteDuration],
                                 timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  private final case object BehaviorChangeKey
  private final case object BehaviorWaitKey
  private final case object GameLoopKey

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName:String,
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
  case class InitReplay(userActor: ActorRef[GypsyGameEvent.WsMsgServer], userId: Long,frame:Int) extends Command

  def create(recordId: Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        //操作数据库
        RecordDao.getRecordById(recordId).map{
          case Some(r) =>
            val replay = initFileReader(r.filePath)
            val info = replay.init()
            try{
              ctx.self ! SwitchBehavior("work",
                work(
                  replay,
                  metaDataDecode(info.simulatorMetadata).right.get,
                  userMapDecode(replay.getMutableInfo(AppSettings.essfMapKeyName).getOrElse(Array[Byte]())).right.get.m
                ))
            }catch {
              case e:Throwable=>
                log.error("error--"+ e.getMessage)
            }
          case None =>
            log.debug(s"record--$recordId didn't exist!!")
        }
        switchBehavior(ctx,"busy",busy())
      }
    }
  }

  def work(fileReader: FrameInputStream,
           metaData:GameInformation,
           userMap:List[(EssfMapKey,EssfMapJoinLeftInfo)],
           userOpt:Option[ActorRef[GypsyGameEvent.WsMsgSource]]= None
          )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer: MiddleBufferInJvm
  ):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay =>
          log.info("start new replay !")
          //只要看回放，就不能再玩儿游戏了
          timer.cancel(GameLoopKey)
          userMap.find(_._1.userId == msg.userId) match {
            case Some(u) =>
              log.info(s"set replay from frame=${msg.frame}")
              fileReader.gotoSnapshot(msg.frame)
              if(fileReader.hasMoreFrame){
                timer.startPeriodicTimer(GameLoopKey, GameLoop, 100.millis)
                work(fileReader,metaData,userMap,Some(msg.userActor))
              }else{
                timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"),waitTime)
                Behaviors.same
              }
            case None=>
              timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"), waitTime)
              Behaviors.same
          }
        case GameLoop=>
          if(fileReader.hasMoreFrame){
            userOpt.foreach(u=>
              fileReader.readFrame().foreach{ f=>
                //TODO
                dispatchByteTo(u,f)
              }
            )
            Behaviors.same
          }else{
            timer.cancel(GameLoopKey)
            timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"),waitTime)
            Behaviors.same
          }
        case msg:TimeOut=>
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same

      }
    }
  }
  import org.seekloud.byteobject.ByteObject._
  def dispatchByteTo(subscribe:ActorRef[GypsyGameEvent.WsMsgSource], msg:FrameData)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribe ! ReplayFrameData(msg.eventsData)
    msg.stateData.foreach(s=>subscribe ! ReplayFrameData(s))
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
