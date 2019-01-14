package com.neo.sk.gypsy.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.gypsy.core.RoomActor.{ReStart, UserReLive}
import com.neo.sk.gypsy.Boot.roomManager
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{GetRecordFrameMsg, GetUserInRecordMsg}

import scala.concurrent.duration._
import scala.language.implicitConversions
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
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

  case class DispatchMsg(msg:Protocol.WsMsgSource) extends Command

  case class JoinRoom(playerInfo: PlayerInfo,roomIdOpt:Option[Long] = None,userActor:ActorRef[UserActor.Command]) extends Command with RoomManager.Command

  case class JoinRoomByCreate(playerInfo: PlayerInfo,userActor:ActorRef[UserActor.Command]) extends Command with RoomManager.Command

  case class JoinRoom4Watch(playerInfo: PlayerInfo,roomId:Long,watchId:Option[String],userActor:ActorRef[UserActor.Command]) extends Command with RoomManager.Command

  case class JoinRoomSuccess(roomId:Long, roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case class JoinRoomFailure(roomId: Long, errorCode: Int, msg: String) extends Command with RoomManager.Command

  case class JoinRoomSuccess4Watch(roomActor: ActorRef[RoomActor.Command],roomId:Long) extends Command with RoomManager.Command

  case class Left(playerInfo: PlayerInfo) extends Command with RoomActor.Command

  case class Left4Watch(playerInfo: PlayerInfo) extends Command with RoomActor.Command

  case class Key(keyCode: Int,frame:Int,n:Int) extends Command with RoomActor.Command

  case class Mouse(clientX:Short,clientY:Short,frame:Int,n:Int) extends Command with RoomActor.Command

  case class NetTest(id: String, createTime: Long) extends Command with RoomActor.Command with GamePlayer.Command

//  case class UserReLiveAck(id: String) extends Command with RoomActor.Command

  case class UserReLiveMsg(frame: Int) extends Command with RoomActor.Command

  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command with RoomActor.Command

  private case object UnKnowAction extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  /**
    * 此处的actor是前端虚拟acotr，GameReplayer actor直接与前端acotr通信
    * */
  case class UserFrontActor(actor: ActorRef[Protocol.WsMsgSource]) extends Command

  case class TimeOut(msg: String) extends Command

  case class StartGame(roomId:Option[Long]) extends Command

  case object CreateRoom extends Command

  case class StartWatch(roomId:Long, watchId:Option[String]) extends Command

  case class StartReply(recordId:Long, watchId:String, frame:Int) extends Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case object ChangeBehaviorToInit extends Command


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

  def flow(id:String,name:String,recordId:Long,actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg, Protocol.WsMsgSource,Any] = {
    val in = Flow[UserActor.WebSocketMsg]
          .map {a=>
            val req = a.reqOpt.get
                 req match{
                   case KC(_,keyCode,f,n)=>
                     log.debug(s"键盘事件$keyCode")
                     Key(keyCode,f,n)

                   case MP(_,clientX,clientY,f,n)=>
                     Mouse(clientX,clientY,f,n)

                   case Ping(timestamp)=>
                     NetTest(id,timestamp)

                   case ReLiveMsg(frame) =>
                      UserReLiveMsg(frame)

//                   case ReLiveAck(id) =>
//                     UserReLiveAck(id)

                   case Protocol.CreateRoom =>
                     CreateRoom

                   case Protocol.JoinRoom(roomIdOp) =>
                     log.info("JoinRoom!!!!!!")
                     StartGame(roomIdOp)

                   case _=>
                     UnKnowAction
                 }
          }
          .to(sink(actor,recordId))

    val out =
      ActorSource.actorRef[Protocol.WsMsgSource](
        completionMatcher = {
          case Protocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case Protocol.FailMsgServer(e)  ⇒ e
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

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }

    }

  private def idle(
                    userInfo:PlayerInfo,
                    startTime:Long,
                    frontActor: ActorRef[Protocol.WsMsgSource]
                  )(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx,msg) =>
      msg match {
        case StartGame(roomIdOp) =>
          roomManager ! UserActor.JoinRoom(userInfo,roomIdOp,ctx.self)
          Behaviors.same

        case StartWatch(roomId,watchId) =>
          roomManager ! UserActor.JoinRoom4Watch(userInfo,roomId,watchId,ctx.self)
          Behaviors.same

        case StartReply(recordId,watchId,frame) =>
          getGameReply(ctx,recordId) ! GamePlayer.InitReplay(frontActor,watchId,frame)
          val gamePlayer = getGameReply(ctx,recordId)
          switchBehavior(ctx,"replay",replay(recordId,userInfo,frontActor,gamePlayer))

        case CreateRoom =>
          roomManager ! UserActor.JoinRoomByCreate(userInfo,ctx.self)
          Behaviors.same

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case JoinRoomSuccess(roomId,roomActor)=>
          frontActor ! Protocol.Wrap(Protocol.JoinRoomSuccess(userInfo.playerId,roomId).asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
//          frontActor ! Protocol.JoinRoomSuccess(userInfo.playerId,roomId)
          switchBehavior(ctx,"play",play(userInfo,frontActor,roomActor))

        case JoinRoomFailure(roomId,errorCode,msg) =>
          frontActor ! Protocol.Wrap(Protocol.JoinRoomFailure(userInfo.playerId,roomId,errorCode,msg).asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
//          frontActor ! Protocol.JoinRoomFailure(userInfo.playerId,roomId,errorCode,msg)
          Behaviors.same

        case JoinRoomSuccess4Watch(roomActor,roomId) =>
          switchBehavior(ctx,"watch",watch(userInfo,roomId,frontActor,roomActor))

        case UnKnowAction =>
          Behavior.same

        case ChangeBehaviorToInit =>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          ctx.unwatch(frontActor)
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

//        case msg:GetUserInRecordMsg=>
//          getGameReply(ctx,msg.recordId) ! msg
//          Behaviors.same
//
//        case msg:GetRecordFrameMsg=>
//          getGameReply(ctx,msg.recordId) ! msg
//          Behaviors.same

        case unknowMsg=>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


  /**
    * 玩游戏
    */
  private def play(
                    userInfo:PlayerInfo,
                    frontActor:ActorRef[Protocol.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    sendBuffer:MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Key(keyCode,frame,n) =>
          log.debug(s"got $msg")
          roomActor ! RoomActor.KeyR(userInfo.playerId, keyCode,frame,n)
          Behaviors.same

        case Mouse(x,y,frame,n) =>
          log.debug(s"gor $msg")
          roomActor ! RoomActor.MouseR(userInfo.playerId,x,y,frame,n)
          Behaviors.same

//        case UserReLiveAck(id) =>
//          println(s"UserActor got $id relive Ack ")
//          roomActor ! ReStartAck(id)
//          Behaviors.same

        case UserReLiveMsg(frame) =>
//          println(s"UserActor got $id relive Msg")
          roomActor ! UserReLive(userInfo.playerId,frame)
          Behaviors.same

        case DispatchMsg(m)=>
//          log.info(s"bot:    $m")
          frontActor ! m
          Behaviors.same

        case ChangeBehaviorToInit=>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom(userInfo)
          ctx.unwatch(frontActor) //这句是必须的，将不会受到UserLeft消息
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(userInfo)
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
    * 观战
    */
  private def watch(
                     userInfo:PlayerInfo,
                     roomId:Long,
                     frontActor:ActorRef[Protocol.WsMsgSource],
                     roomActor: ActorRef[RoomActor.Command]
                   )(
                     implicit stashBuffer:StashBuffer[Command],
                     timer:TimerScheduler[Command],
                     sendBuffer:MiddleBufferInJvm
                   ):Behavior[Command]=
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom4Watch(userInfo,roomId)
          Behaviors.stopped

        case DispatchMsg(m)=>
          frontActor ! m
          Behaviors.same

        case ChangeBehaviorToInit=>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom4Watch(userInfo,roomId)
          ctx.unwatch(frontActor) //这句是必须的，将不会受到UserLeft消息
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case e: NetTest=>
          roomActor ! e
          Behaviors.same

        case unknowMsg =>
          log.warn(s"${ctx.self.path} recv an unknown msg=${msg}")
          Behavior.same
      }
    }
  /**
    * 回放
    */
  private def replay(
                      recordId: Long,
                      userInfo:PlayerInfo,
                      frontActor: ActorRef[Protocol.WsMsgSource],
                      gamePlayer: ActorRef[GamePlayer.Command]
                    )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ):Behavior[Command] =
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case ChangeBehaviorToInit =>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          gamePlayer ! GamePlayer.StopReplay(recordId)
          ctx.unwatch(frontActor) //这句是必须的，将不会受到UserLeft消息
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          gamePlayer ! GamePlayer.StopReplay(recordId)
          ctx.unwatch(actor)
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case msg:GetUserInRecordMsg=>
          gamePlayer ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          gamePlayer ! msg
          Behaviors.same

        case e: NetTest=>
          gamePlayer ! e
          Behaviors.same

        case unknowMsg =>
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
