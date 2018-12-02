package com.neo.sk.gypsy.core


import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorAttributes
import akka.util.ByteString
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import akka.stream.{ActorAttributes, Supervision}
import com.neo.sk.gypsy.models.GypsyUserInfo
import com.neo.sk.gypsy.ptcl.EsheepProtocol.PlayerInfo
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{GetRecordFrameMsg, GetUserInRecordMsg}
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol.userInRecordRsp

/**
  * @author zhaoyin
  * 2018/10/25  下午9:10
  */
object UserManager {

  import org.seekloud.byteobject.MiddleBufferInJvm


  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command
  //玩游戏
  final case class GetWebSocketFlow(playerInfo: Option[ApiProtocol.PlayerInfo] = None,roomId:Option[Long] = None,replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command
  //观战
  final case class GetWatchWebSocketFlow(playerInfo: Option[ApiProtocol.PlayerInfo] = None, watchId:Option[String],roomId:Long, replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command
  //回放
  final case class GetReplaySocketFlow(playerInfo: Option[ApiProtocol.PlayerInfo] = None, recordId:Long,frame:Int,watchId:String,replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val uidGenerator = new AtomicLong(1L)
            idle(uidGenerator)
        }
    }
  }

  private def idle(uidGenerator: AtomicLong)(
                  implicit timer: TimerScheduler[Command]
  ):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case GetWebSocketFlow(playerInfoOpt,roomIdOpt,replyTo) =>
          val playerInfo = playerInfoOpt.get
          getUserActorOpt(ctx, playerInfo.playerId) match {
            case Some(userActor) =>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx,playerInfo)
          replyTo ! getWebSocketFlow(playerInfo,0L,userActor)
          userActor ! UserActor.StartGame(roomIdOpt)
          Behaviors.same

        case GetWatchWebSocketFlow(playerInfoOpt,watchIdOpt,roomId,replyTo) =>
          val playerInfo = playerInfoOpt.get
          getUserActorOpt(ctx, playerInfo.playerId) match {
            case Some(userActor) =>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx,playerInfo)
          replyTo ! getWebSocketFlow(playerInfo,0L,userActor)
          userActor ! UserActor.StartWatch(roomId,watchIdOpt)
          Behaviors.same


        case GetReplaySocketFlow(playerInfoOpt,recordId,frame,watchId,replyTo) =>
          log.info("Replay WS connecting =========")
          val playerInfo = playerInfoOpt.get
          getUserActorOpt(ctx,playerInfo.playerId) match{
            case Some(userActor)=>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx,playerInfo)
          replyTo ! getWebSocketFlow(playerInfo,recordId,userActor)
          userActor ! UserActor.StartReply(recordId,watchId,frame)
          Behaviors.same

        case msg:GetUserInRecordMsg=>
          getUserActor(ctx,ApiProtocol.PlayerInfo(msg.watchId,msg.watchId)) ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          getUserActor(ctx,ApiProtocol.PlayerInfo(msg.watchId,msg.watchId)) ! msg
          Behaviors.same

        case ChildDead(child, childRef) =>
          ctx.unwatch(childRef)
          Behaviors.same

        case unknow =>
          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
          Behaviors.same
      }

    }
  }

  //三种共用
  private def getWebSocketFlow(playerInfo: ApiProtocol.PlayerInfo,recordId:Long,userActor: ActorRef[UserActor.Command]):Flow[Message,Message,Any] = {
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._

    implicit def parseJsonString2WsMsgFront(s:String): Option[Protocol.UserAction] = {

      try {
        import io.circe.generic.auto._
              import io.circe.parser._
        val wsMsg = decode[Protocol.UserAction](s).right.get
        Some(wsMsg)
      }catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          None
      }
    }

    Flow[Message]
      .collect {
        case BinaryMessage.Strict(msg)=>
          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
          bytesDecode[Protocol.UserAction](buffer) match {
            case Right(req) =>  UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              UserActor.WebSocketMsg(None)
          }
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          UserActor.WebSocketMsg(None)

        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }.via(UserActor.flow(playerInfo.playerId,playerInfo.nickname,recordId,userActor))
      .map{
        case t:Protocol.Wrap =>
          BinaryMessage.Strict(ByteString(t.ws))
        case t: Protocol.ReplayFrameData =>
          BinaryMessage.Strict(ByteString(t.ws))
        case x =>
          log.debug(s"akka stream receive unknown msg=${x}")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))
  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"ws stream failed with $e")
      Supervision.Resume
  }


  private def getUserActor(ctx: ActorContext[Command],userInfo:ApiProtocol.PlayerInfo):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${userInfo.playerId}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(userInfo),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }
  private def getUserActorOpt(ctx: ActorContext[Command],id:String):Option[ActorRef[UserActor.Command]] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).map(_.upcast[UserActor.Command])
  }


}
