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
import com.neo.sk.gypsy.shared.ptcl
/**
  * @author zhaoyin
  * @date 2018/10/28  3:38 PM
  */
object WsClient {

  private val log = LoggerFactory.getLogger("WSClient")
  private val logPrefix = "WSClient"


  sealed trait WsCommand
  case class ConnectGame(id:String, name: String, accessCode: String) extends WsCommand
  case object Stop extends WsCommand

  def create(gameClient: ActorRef[ptcl.WsMsgSource],
             _system: ActorSystem,
             _materializer: Materializer,
             _executor: ExecutionContextExecutor):Behavior[WsCommand] = {
    Behaviors.setup[WsCommand]{ ctx=>
      Behaviors.withTimers{ timer =>
        working(gameClient)(timer,_system,_materializer,_executor)
      }

    }
  }



  private def working(gameClient: ActorRef[ptcl.WsMsgSource])(
    implicit timer:TimerScheduler[WsCommand],
    system: ActorSystem,
    materializer: Materializer,
    executor: ExecutionContextExecutor
  ):Behavior[WsCommand] = {
    Behaviors.receive[WsCommand]{(ctx,msg)=>
      msg match {
        case ConnectGame(id,name,accessCode) =>
          val url = getWebSocketUri(id,name,accessCode)
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          val source = getSource(ctx.self)
          val sink = getSink(gameClient)
          val ((stream,response), _) =
            source
              .viaMat(webSocketFlow)(Keep.both)
              .toMat(sink)(Keep.both)
              .run()

          val connected = response.flatMap { upgrade =>
            if(upgrade.response.status == StatusCodes.SwitchingProtocols){
              Future.successful(s"$logPrefix connect success. EstablishConnectionEs!")
            } else {
              throw new RuntimeException(s"WSClient connection failed: ${upgrade.response.status}")
            }
          }
          //链接建立时
          connected.onComplete(i=> log.info(i.toString))
          Behaviors.same

        case Stop =>
          log.info("WsClient now stop")
          Behavior.stopped
      }
    }
  }

  def getSource(wsClient: ActorRef[WsCommand]) = ActorSource.actorRef[ptcl.WsSendMsg](
    completionMatcher = {
      case ptcl.WsSendComplete =>
        log.info("Websocket Complete")
        wsClient ! Stop
    },
    failureMatcher = {
      case ptcl.WsSendFailed(ex)  ⇒ ex
    },
    bufferSize = 8,
    overflowStrategy = OverflowStrategy.fail
  ).collect{
    case message: ptcl.UserAction =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))
  }

  def getSink(actor: ActorRef[ptcl.WsMsgSource]) =
    Flow[Message].collect{
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from websocket: $msg")
        ErrorWsMsgFront

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[ptcl.WsMsgFront](buffer) match{
            case Right(v) => v
            case Left(e) =>
              println(s"decode error: ${e.message}")
              ErrorWsMsgFront
          }
        msg
    }.to(ActorSink.actorRef[ptcl.WsMsgSource](actor,ptcl.CompleteMsgServer(), ptcl.FailMsgServer))

  def getWebSocketUri(playerId: String, playerName: String, accessCode: String):String = {
    val wsProtocol = "ws"
    val host = "localhost:30372"
    s"$wsProtocol://$host/gypsy/api/playGameClient?playerId=$playerId&playerName=$playerName&accessCode=$accessCode"
  }


}
