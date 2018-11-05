package com.neo.sk.gypsy.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import com.neo.sk.gypsy.shared.ptcl.{Protocol, WsMsgProtocol}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.gypsy.core.RoomActor.{CompleteMsgFront, FailMsgFront}
import com.neo.sk.gypsy.models.GypsyUserInfo
import com.neo.sk.gypsy.Boot.roomManager
import com.neo.sk.gypsy.shared.ptcl.Protocol.{KeyCode, MousePosition, Ping, WatchChange}

import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * @author zhaoyin
  *  2018/10/25  下午10:27
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final case object BehaviorChangeKey
  private final val InitTime = Some(5.minutes)

  trait Command

  case class WebSocketMsg(reqOpt: Option[Protocol.GameMessage]) extends Command

  case class DispatchMsg(msg:WsMsgProtocol.WsMsgSource) extends Command

  case class JoinRoom(uid:String,gameStateOpt:Option[Int],name:String,startTime:Long,userActor:ActorRef[UserActor.Command], roomIdOpt:Option[Long] = None,watch:Boolean) extends Command with RoomManager.Command


  case class JoinRoomSuccess(tank:TankServerImpl,uId:String,roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  /**
    * 此处的actor是前端虚拟acotr，GameReplayer actor直接与前端acotr通信
    * */
  case class UserFrontActor(actor: ActorRef[WsMsgProtocol.WsMsgSource]) extends Command

  case class TimeOut(msg: String) extends Command
  case class StartReply(recordId:Long, playerId:String, frame:Int) extends Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case object ChangeBehaviorToInit extends Command

  case class StartGame(roomId:Option[Long]) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut  = TimeOut("busy time error")
                                   )(
                                  implicit stashBuffer: StashBuffer[Command],
                                  timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path}  becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg, WsMsgProtocol.WsMsgSource,Any] = {
        val in = Flow[Protocol.UserAction]
          .map {
            case KeyCode(i,keyCode,f,n)=>
              log.debug(s"键盘事件$keyCode")
              Key(id,keyCode,f,n)
            case MousePosition(i,clientX,clientY,f,n)=>
              Mouse(id,clientX,clientY,f,n)
            case UserLeft()=>
              Left(id,name)
            case Ping(timestamp)=>
              NetTest(id,timestamp)
            case WatchChange(id, watchId) =>
              log.debug(s"切换观察者: $watchId")
              ChangeWatch(id, watchId)
            case _=>
              UnKnowAction
          }
          .to(sink(actor))
    val out =
      ActorSource.actorRef[WsMsgProtocol.WsMsgSource](
        completionMatcher = {
          case WsMsgProtocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case WsMsgProtocol.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(uId:String,userInfo:GypsyUserInfo):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        init(uId,userInfo)
      }
    }
  }

  private def init(uId: String,userInfo:GypsyUserInfo)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle", idle(uId,userInfo,System.currentTimeMillis(),frontActor))
      }

    }

  private def idle(
                    uId: String,
                    userInfo:GypsyUserInfo,
                    startTime:Long,
                    frontActor: ActorRef[WsMsgProtocol.WsMsgSource]
                  )(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx,msg) =>
      msg match {
        case StartReply(recordId,playerId,frame) =>
          getGameReply(ctx,recordId) ! GamePlayer.InitReplay(frontActor,playerId,frame)
          Behaviors.same

        case StartGame(roomIdOp) =>

          roomManager ! UserActor.JoinRoom(uId,None,userInfo.userName,startTime,ctx.self,roomIdOp,false)
          Behaviors.same

        case JoinRoomSuccess(player,uid,roomActor)
          switchBehavior(ctx,"play",play(uId, userInfo,tank,startTime,frontActor,roomActor))


      }
    }


  private def play(
                    uId:String,
                    userInfo:GypsyUserInfo,
                    startTime:Long,
                    frontActor:ActorRef[WsMsgProtocol.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    sendBuffer:MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:Protocol.UserAction) =>
              //分发数据给roomActor
              roomActor ! RoomActor.WebSocketMsg(uId,tank.tankId,t)
            case Some(t:TankGameEvent.PingPackage) =>

              frontActor !TankGameEvent.Wrap(t.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())

            case _ =>

          }
          Behaviors.same

        case DispatchMsg(m) =>
          if(m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            frontActor ! m
            println(s"${ctx.self.path} tank 当前生命值${tank.getTankState().lives}")
            if (tank.lives > 1){
              //玩家进入复活状态
              roomManager ! RoomActor.LeftRoomByKilled(uId,tank.tankId,tank.getTankState().lives,userInfo.name)
              switchBehavior(ctx,"waitRestartWhenPlay",waitRestartWhenPlay(uId,userInfo,startTime,frontActor, tank))
            } else {
              roomManager ! RoomActor.LeftRoomByKilled(uId,tank.tankId,tank.getTankState().lives,userInfo.name)
              switchBehavior(ctx,"idle",idle(uId,userInfo,startTime,frontActor))
            }
          }else{
            frontActor ! m
            Behaviors.same
          }

        case ChangeBehaviorToInit=>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.RebuildWebSocket.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom(uId,tank.tankId,userInfo.name,Some(uId))
          ctx.unwatch(frontActor)
          switchBehavior(ctx,"init",init(uId, userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(uId,tank.tankId,userInfo.name,Some(uId))
          Behaviors.stopped

        case k:InputRecordByDead =>
          log.debug(s"input record by dead msg")
          if(tank.lives -1 <= 0 && !uId.contains(Constants.TankGameUserIdPrefix)){
            val endTime = System.currentTimeMillis()
            log.debug(s"input record ${EsheepSyncClient.InputRecord(uId,userInfo.nickName,k.killTankNum,tank.config.getTankLivesLimit,k.damageStatistics, startTime, endTime)}")
            esheepSyncClient ! EsheepSyncClient.InputRecord(uId,userInfo.nickName,k.killTankNum,tank.config.getTankLivesLimit,k.damageStatistics, startTime, endTime)
          }
          Behaviors.same

        case unknowMsg =>
          //          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }


  /**
    * replay-actor
    */
  private def getGameReply(ctx: ActorContext[Command], recordId:Long): ActorRef[GamePlayer.Command] = {
    val childName = s"gameReply-$recordId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(GamePlayer.create(recordId), childName)
      actor
    }
  }.upcast[GamePlayer.Command]


}
