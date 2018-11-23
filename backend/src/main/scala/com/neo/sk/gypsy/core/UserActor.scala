package com.neo.sk.gypsy.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import com.neo.sk.gypsy.shared.ptcl.{Protocol, UserState, WsMsgProtocol}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.gypsy.core.RoomActor.{CompleteMsgFront, FailMsgFront, ReStartAck}
import com.neo.sk.gypsy.models.GypsyUserInfo
import com.neo.sk.gypsy.Boot.roomManager
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{GetRecordFrameMsg, GetUserInRecordMsg}
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol.userInRecordRsp

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

  case class WebSocketMsg(reqOpt: Option[Protocol.UserAction]) extends Command

  case class DispatchMsg(msg:WsMsgProtocol.WsMsgSource) extends Command

  case class JoinRoom(uid:String,gameStateOpt:Option[Int],name:String,startTime:Long,userActor:ActorRef[UserActor.Command], roomIdOpt:Option[Long] = None,watch:Boolean,watchId:Option[String]) extends Command with RoomManager.Command

  case class JoinRoomSuccess(uId:String,roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case class Left(id: String, name: String) extends Command with RoomActor.Command

  case class Key(id: String, keyCode: Int,frame:Long,n:Int) extends Command with RoomActor.Command

  case class Mouse(id: String, clientX:Double,clientY:Double,frame:Long,n:Int) extends Command with RoomActor.Command

  case class NetTest(id: String, createTime: Long) extends Command with RoomActor.Command

  case class UserReLiveAck(id: String) extends Command with RoomActor.Command

  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command with RoomActor.Command

  private case object UnKnowAction extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  /**
    * 此处的actor是前端虚拟acotr，GameReplayer actor直接与前端acotr通信
    * */
  case class UserFrontActor(actor: ActorRef[WsMsgProtocol.WsMsgSource]) extends Command

  case class TimeOut(msg: String) extends Command

  case class StartReply(recordId:Long, watchId:String, frame:Int) extends Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case object ChangeBehaviorToInit extends Command

  case class StartGame(roomId:Option[Long],watchId:Option[String],watch:Boolean) extends Command


  private var userState = UserState.waiting
  private[this] var watchRecordId  = 0l

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

  private def sink(actor: ActorRef[Command],recordId:Long) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(id:String,name:String,recordId:Long,actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg, WsMsgProtocol.WsMsgSource,Any] = {
    val in = Flow[UserActor.WebSocketMsg]
          .map {a=>
            val req = a.reqOpt.get
                 req match{
                   case KeyCode(id,keyCode,f,n)=>
                     log.debug(s"键盘事件$keyCode")
                     Key(id,keyCode,f,n)
                   case MousePosition(id,clientX,clientY,f,n)=>
                     Mouse(id,clientX,clientY,f,n)
//                   case Protocol.UserLeft()=>
//                     Left(id,name)
                   case Ping(timestamp)=>
                     NetTest(id,timestamp)
//                   case WatchChange(id, watchId) =>
//                     log.debug(s"切换观察者: $watchId")
//                     ChangeWatch(id, watchId)

//                   case ReLive(id) =>
//                     UserReLive(id)

                   case ReLiveAck(id) =>
                     UserReLiveAck(id)

                   case _=>
                     UnKnowAction
                 }
          }
          .to(sink(actor,recordId))

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

  def create(userInfo:PlayerInfo):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))
      }
    }
  }

  private def init(userInfo:PlayerInfo)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle", idle(userInfo,System.currentTimeMillis(),frontActor))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case UnKnowAction =>
          Behavior.same

        case ChangeBehaviorToInit=>
          Behaviors.same

        case msg:GetUserInRecordMsg=>
          getGameReply(ctx,msg.recordId) ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          getGameReply(ctx,msg.recordId) ! msg
          Behaviors.same

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }

    }

  private def idle(
                    userInfo:PlayerInfo,
                    startTime:Long,
                    frontActor: ActorRef[WsMsgProtocol.WsMsgSource]
                  )(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx,msg) =>
      msg match {
        case StartReply(recordId,watchId,frame) =>
          userState = UserState.replay
          watchRecordId = recordId
          getGameReply(ctx,recordId) ! GamePlayer.InitReplay(frontActor,watchId,frame)
          Behaviors.same

        case StartGame(roomIdOp,watchId,watch) =>
          userState = UserState.play
          roomManager ! UserActor.JoinRoom(userInfo.playerId,None,userInfo.nickname,startTime,ctx.self,roomIdOp,watch,watchId)
          Behaviors.same

        case UserLeft(actor) =>
//          log.info(s"${actor.path} @@@@@@@@@@@@@@UserLeft")
          if(userState == UserState.replay){
            getGameReply(ctx,watchRecordId) ! GamePlayer.StopReplay(watchRecordId)
            watchRecordId = 0l
            userState = UserState.waiting
          }
          ctx.unwatch(actor)
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case JoinRoomSuccess(uid,roomActor)=>
          switchBehavior(ctx,"play",play(uid, userInfo,startTime,frontActor,roomActor))

        case UnKnowAction =>
          //          stashBuffer.stash(unknowMsg)
          //          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same

          //for 回放
        case ChangeBehaviorToInit =>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          ctx.unwatch(frontActor)
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case msg:GetUserInRecordMsg=>
          getGameReply(ctx,msg.recordId) ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          getGameReply(ctx,msg.recordId) ! msg
          Behaviors.same

        case unknowMsg=>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


  //玩游戏+观战
  private def play(
                    uId:String,
                    userInfo:PlayerInfo,
                    startTime:Long,
                    frontActor:ActorRef[WsMsgProtocol.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    sendBuffer:MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Key(id, keyCode,frame,n) =>
          log.debug(s"got $msg")
          roomActor ! Key(id, keyCode,frame,n)
          Behaviors.same

        case Mouse(id,x,y,frame,n) =>
          log.debug(s"gor $msg")
          roomActor !  Mouse(id,x,y,frame,n)
          Behaviors.same

        case UserReLiveAck(id) =>
          println(s"UserActor got $id relive Ack ")
          roomActor ! ReStartAck(id)
          Behavior.same

        case DispatchMsg(m)=>
          frontActor ! m
          Behaviors.same

          //for 玩游戏+观战
        case ChangeBehaviorToInit=>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom(uId,userInfo.nickname)
          ctx.unwatch(frontActor)
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(uId,userInfo.nickname)
          Behaviors.stopped

        case e: NetTest=>
          roomActor ! e
          Behaviors.same

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
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
