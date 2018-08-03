package com.neo.sk.gypsy.core

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import io.circe.{Decoder, Encoder}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

/**
  * User: sky
  * Date: 2018/7/23
  * Time: 11:09
  */
object RoomManager {
  //todo 增加多房间模式
  private val log=LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  case object TimeKey
  case object TimeOut extends Command
  val idGenerator = new AtomicInteger(1000000)
  case class JoinGame(room:String,sender:String, id:Long)

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

            Behaviors.same
          case x=>
            log.debug("")
            Behaviors.unhandled
        }
    }

  def webSocketChatFlow(room:String,sender: String, id: Long): Flow[Message, Message, Any] =
    Flow[Message]
      .collect {
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          msg
        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }
      .via(playGround.getOrElse(room,playGround("11")).joinGame(id, sender)) // ... and route them through the chatFlow ...
      .map { msg => TextMessage.Strict(msg.asJson.noSpaces) // ... pack outgoing messages into WS JSON messages ...
      //.map { msg => TextMessage.Strict(write(msg)) // ... pack outgoing messages into WS JSON messages ...
    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin


  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }
}
