package com.neo.sk.gypsy.actor

import akka.actor.ActorSystem
import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.stream.scaladsl.{Flow, Keep}
import akka.stream.typed.scaladsl.{ActorSink, _}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import org.seekloud.byteobject.{MiddleBufferForTest, MiddleBufferInJvm}
import org.slf4j.LoggerFactory
/**
  * @author zhaoyin
  * @date 2018/10/28  3:38 PM
  */
object WsClient {

  private val log = LoggerFactory.getLogger("WSClient")


  sealed trait WsCommand
  case class ConnectGame(id:String, name: String, accessCode: String) extends WsCommand

  def create():Behavior[WsCommand] = {
    Behaviors.setup[WsCommand]{ ctx=>
      Behaviors.withTimers{ timer =>
        working()(timer)
      }

    }
  }



  private def working()(
    implicit timer:TimerScheduler[WsCommand],
    executor: ExecutionContextExecutor
  ):Behavior[WsCommand] = {
    Behaviors.receive[WsCommand]{(ctx,msg)=>
      msg match {
        case ConnectGame(id,name,accessCode) =>
          val url = getWebSocketUri(id,name,accessCode)
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          val source = getSource
          val sink = getSink
          val ((stream,response, _)) =
            source
          .viaMat(webSocketFlow)(Keep.both)
          .toMat(sink)(Keep.both)
          .run()

          val connected = response.flatMap { upgrade =>


          }
          //链接建立时
          connected.onComplete(i=> log.info(i.toString))
          Behaviors.same
      }
    }
  }

  def getSource = ActorSource.actorRef[WsSendMsg](
    completionMatcher = {
      case WsSendComplete =>
    },
    failureMatcher = {
      case WsSendFailed(ex)  ⇒ ex
    },
    bufferSize = 8,
    overflowStrategy = OverflowStrategy.fail
  ).collect{
    case message: UserAction =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))
  }

  def getSink =
    Flow[Message].collect{
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from websocket: $msg")
        //todo 捋清protocol
        TextMsg(msg)

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[](buffer) match{
            case Right(v) => v
            case Left(e) =>
              println(s"decode error: ${e.message}")
              //todo
              TextMsg("decode error")
          }
        msg
    }.to(ActorSink.actorRef[]())

  def getWebSocketUri(playerId: String, playerName: String, accessCode: String):String = {
    val wsProtocol = "ws"
    //todo ???
    val host = "localhost:30372"
    s"$wsProtocol://$host/gypsy/api/playGameClient?playerId=$playerId&playerName=$playerName&accessCode=$accessCode"
  }


}
