package com.neo.sk.gypsy.core

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Materializer, Supervision}
import akka.stream.scaladsl.Flow
import org.slf4j.LoggerFactory
import akka.util.ByteString

import scala.concurrent.duration._
import com.neo.sk.gypsy.Boot.executor
import com.neo.sk.gypsy.shared.ptcl.{WsFrontProtocol, WsServerProtocol}
import com.neo.sk.gypsy.shared.ptcl.WsServerProtocol.ErrorWsMsgServer
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.CheckNameRsp
import com.neo.sk.gypsy.shared.ptcl.WsFrontProtocol.WsMsgFront
import com.neo.sk.gypsy.utils.CirceSupport
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.utils.byteObject.ByteObject._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * User: sky
  * Date: 2018/7/23
  * Time: 11:09
  */
object RoomManager {

  private val log=LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  case object TimeKey
  case object TimeOut extends Command
  val idGenerator = new AtomicInteger(1000000)
  case class JoinGame(room:String,sender:String,id:Long, replyTo:ActorRef[Flow[Message,Message,Any]])extends Command
  case class CheckName(name:String,room:String,replyTo:ActorRef[CheckNameRsp])extends Command

  val behaviors:Behavior[Command] ={
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            idle
        }
    }
  }

  def idle(implicit timer:TimerScheduler[Command])=
    Behaviors.receive[Command]{
      (ctx,msg)=>
        msg match {
          case msg:JoinGame=>
            msg.replyTo ! webSocketChatFlow(getRoomActor(ctx,msg.room),msg.sender,msg.id)
            Behaviors.same

          case msg:CheckName=>
            getRoomActor(ctx,msg.room) ! RoomActor.CheckName(msg.name,msg.replyTo)
            Behavior.same
          case x=>
            log.debug("")
            Behaviors.unhandled
        }
    }

  import com.neo.sk.gypsy.utils.byteObject.ByteObject._
  import scala.language.implicitConversions
  def webSocketChatFlow(actor:ActorRef[RoomActor.Command],sender: String, id: Long): Flow[Message, Message, Any] =
    Flow[Message]
      .collect {
        case BinaryMessage.Strict(msg)=>
          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
          bytesDecode[WsServerProtocol.WsMsgServer](buffer) match {
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
      }
      .via(RoomActor.joinGame(actor,id, sender)) // ... and route them through the chatFlow ...
      .map {
      case t:WsFrontProtocol.WsMsgFront =>
        val sendBuffer = new MiddleBufferInJvm(409600)
        BinaryMessage.Strict(ByteString(t.fillMiddleBuffer(sendBuffer).result()))
      case x =>
        TextMessage.apply("")
    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin


  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }

  private def getRoomActor(ctx: ActorContext[Command],name:String):ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$name"
    ctx.child(childName).getOrElse{
      ctx.spawn(RoomActor.create(name),childName)
    }.upcast[RoomActor.Command]
  }
}
