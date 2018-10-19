//package com.neo.sk.gypsy.core
//
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
//import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
//import akka.stream.{ActorAttributes, Supervision}
//import akka.stream.scaladsl.Flow
//import akka.util.ByteString
//import com.neo.sk.gypsy.core.RoomManager.{decider, log}
//import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
//import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, WsMsgServer}
//import com.neo.sk.tank.shared.protocol.TankGameEvent
//import io.circe.{Decoder, Encoder}
//import org.slf4j.LoggerFactory
//
///**
//  * Created by hongruying on 2018/7/9
//  */
//object UserManager {
//
//  import io.circe.generic.auto._
//  import io.circe.syntax._
//  import org.seekloud.byteobject.ByteObject._
//  import org.seekloud.byteobject.MiddleBufferInJvm
//
//  sealed trait Command
//
//  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command
//
//  final case class GetWebSocketFlow(id:Long,name:String,replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command
//
//  //  final case class GetWebSocketFlow(name:String, userId:Long ,roomIdOpt:Option[Long], replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command
//
//  private val log = LoggerFactory.getLogger(this.getClass)
//
//  def create():Behavior[Command] ={
//    log.debug(s"UserManager start...")
//    Behaviors.setup[Command]{
//      ctx =>
//        Behaviors.withTimers[Command]{
//          implicit timer =>
//            idle()
//        }
//    }
//  }
//
//  private def idle()
//                  (
//                    implicit timer:TimerScheduler[Command]
//                  ):Behavior[Command] = {
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case GetWebSocketFlow(id,name,replyTo) =>
//          replyTo ! getWebSocketFlow(getUserActor(ctx,id,name))
//          Behaviors.same
//
//
//        case ChildDead(child,childRef) =>
//          ctx.unwatch(childRef)
//          Behaviors.same
//
//        case unknow =>
//          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
//          Behaviors.same
//      }
//    }
//  }
//
//  private def getWebSocketFlow(userActor: ActorRef[UserActor.Command]):Flow[Message,Message,Any] = {
//    import scala.language.implicitConversions
//    import org.seekloud.byteobject.ByteObject._
//
//
//    implicit def parseJsonString2WsMsgFront(s:String):Option[TankGameEvent.WsMsgFront] = {
//      import io.circe.generic.auto._
//      import io.circe.parser._
//
//      try {
//        val wsMsg = decode[TankGameEvent.WsMsgFront](s).right.get
//        Some(wsMsg)
//      }catch {
//        case e:Exception =>
//          log.warn(s"parse front msg failed when json parse,s=${s}")
//          None
//      }
//    }
//
//
//    Flow[Message]
//      .collect {
//        case BinaryMessage.Strict(msg)=>
//          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
//          bytesDecode[WsMsgServer](buffer) match {
//            case Right(req) => req
//            case Left(e) =>
//              log.error(s"decode binaryMessage failed,error:${e.message}")
//              ErrorWsMsgServer
//          }
//        case TextMessage.Strict(msg) =>
//          log.debug(s"msg from webSocket: $msg")
//          ErrorWsMsgServer
//
//        // unpack incoming WS text messages...
//        // This will lose (ignore) messages not received in one chunk (which is
//        // unlikely because chat messages are small) but absolutely possible
//        // FIXME: We need to handle TextMessage.Streamed as well.
//      }
//      .via(UserActor.flow(userActor)) // ... and route them through the chatFlow ...
//      .map {
//      case t:WsMsgProtocol.WsMsgFront =>
//        val sendBuffer = new MiddleBufferInJvm(409600)
//        BinaryMessage.Strict(ByteString(t.fillMiddleBuffer(sendBuffer).result()))
//      case x =>
//        TextMessage.apply("")
//    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin
//
//  }
//
//  private val decider: Supervision.Decider = {
//    e: Throwable =>
//      e.printStackTrace()
//      log.error(s"WS stream failed with $e")
//      Supervision.Resume
//  }
//
//
//
//
//
//  private def getUserActor(ctx: ActorContext[Command],id:Long,name:String):ActorRef[UserActor.Command] = {
//    val childName = s"UserActor-${id}"
//    ctx.child(childName).getOrElse{
//      val actor = ctx.spawn(UserActor.create(id,name),childName)
//      ctx.watchWith(actor,ChildDead(childName,actor))
//      actor
//    }.upcast[UserActor.Command]
//  }
//
//}
