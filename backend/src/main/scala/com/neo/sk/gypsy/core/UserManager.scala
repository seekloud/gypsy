package com.neo.sk.gypsy.core


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorAttributes
import com.neo.sk.gypsy.core.RoomActor.ChildDead
import com.neo.sk.gypsy.core.RoomManager.{decider, log}
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, WsMsgServer}
import com.neo.sk.gypsy.utils.byteObject.ByteObject.bytesDecode
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

/**
  * @author zhaoyin
  * @date 2018/10/25  下午9:10
  */
object UserManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class GetReplaySocketFlow(replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            idle()
        }
    }
  }


  private def idle()(
                  implicit timer: TimerScheduler[Command]
  ):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {(ctx, msg) =>
        case GetReplaySocketFlow(replyTo) =>
          //TODO getUserActorOpt
          val userActor = getUserActor(ctx, uid)
          replyTo ! getWebSocketFlow(userActor)
          userActor ! UserActor.StartReply()
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

    Flow[Message]
      .collect {
        case BinaryMessage.Strict(msg)=>
          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
          bytesDecode[WsMsgServer](buffer) match {
            case Right(req) => req
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              ErrorWsMsgServer
          }
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          ErrorWsMsgServer

        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }.via(UserActor.flow(userActor))
      .map{
        //TODO
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))
  }


  private def getUserActor(ctx: ActorContext[Command], id: Long):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }


}
