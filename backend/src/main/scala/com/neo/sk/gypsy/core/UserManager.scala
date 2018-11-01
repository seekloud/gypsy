package com.neo.sk.gypsy.core


import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorAttributes
import akka.util.ByteString
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, WsMsgServer}
import com.neo.sk.gypsy.utils.byteObject.ByteObject.bytesDecode
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl.GypsyGameEvent
import akka.stream.{ActorAttributes, Supervision}
import com.neo.sk.gypsy.models.GypsyUserInfo
import com.neo.sk.gypsy.ptcl.EsheepProtocol

/**
  * @author zhaoyin
  * 2018/10/25  下午9:10
  */
object UserManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class GetReplaySocketFlow(playerName: String,playerId: String, recordId:Long, frame:Int,replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  final case class GetWebSocketFlow(name:String,replyTo:ActorRef[Flow[Message,Message,Any]], userInfoOpt:Option[GypsyUserInfo], roomId:Option[Long] = None) extends Command
  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val uidGenerator = new AtomicLong(1L)
            //todo 自增计时器
            idle(uidGenerator)
        }
    }
  }

  private def idle(uidGenerator: AtomicLong)(
                  implicit timer: TimerScheduler[Command]
  ):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case GetReplaySocketFlow(playerName,playerId,recordId,frame,replyTo) =>
          //TODO getUserActorOpt

          getUserActorOpt(ctx,playerId) match{
            case Some(userActor)=>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx, playerId,GypsyUserInfo(playerId,playerName,true))
          //开始创建flow
          replyTo ! getWebSocketFlow(userActor)
          userActor ! UserActor.StartReply(recordId,playerId,frame)
          Behaviors.same


        case GetWebSocketFlow(name,replyTo, userInfoOpt, roomIdOpt) =>

          val userInfo = userInfoOpt.getOrElse(GypsyUserInfo("gypsyGuest" + s"-${uidGenerator.getAndIncrement()}",name,false))
          getUserActorOpt(ctx, userInfo.userId) match {
            case Some(userActor) =>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx, userInfo.userId,userInfo)
          replyTo ! getWebSocketFlow(userActor)
          userActor ! UserActor.StartGame(roomIdOpt)
          Behaviors.same


        case unknow =>
          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
          Behaviors.same
      }

    }
  }

  private def getWebSocketFlow(userActor: ActorRef[UserActor.Command]):Flow[Message,Message,Any] = {
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._

    implicit def parseJsonString2WsMsgFront(s:String): Option[GypsyGameEvent.WsMsgServer] = {
      import io.circe.generic.auto._
      import io.circe.parser._
      try {
        val wsMsg = decode[GypsyGameEvent.WsMsgServer](s).right.get
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
          bytesDecode[GypsyGameEvent.WsMsgServer](buffer) match {
            case Right(req) => UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              UserActor.WebSocketMsg(None)
          }
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          UserActor.WebSocketMsg(msg)

        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }.via(UserActor.flow(userActor))
      .map{
        case t: GypsyGameEvent.ReplayFrameData =>
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


  private def getUserActor(ctx: ActorContext[Command], id: String,userInfo:GypsyUserInfo):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(id,userInfo),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }
  private def getUserActorOpt(ctx: ActorContext[Command],id:String):Option[ActorRef[UserActor.Command]] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).map(_.upcast[UserActor.Command])
  }


}
