package com.neo.sk.gypsy.actor

import akka.actor.ActorSystem
import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.typed.scaladsl.{ActorSink, _}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.holder.GameHolder
import com.neo.sk.gypsy.scene.GameScene
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import io.circe.parser.decode
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext.Implicits.global
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
             stageCtx: StageContext,
             _system: ActorSystem,
             _materializer: Materializer,
             _executor: ExecutionContextExecutor):Behavior[WsCommand] = {
    Behaviors.setup[WsCommand]{ ctx=>
      Behaviors.withTimers{ timer =>
        working(gameClient, stageCtx)(timer,_system,_materializer,_executor)
      }

    }
  }



  private def working(gameClient: ActorRef[ptcl.WsMsgSource],
                      stageCtx: StageContext
                     )(
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
              val gameScene = new GameScene()
              val gameHolder = new GameHolder(stageCtx,gameScene)
              gameHolder.connectToGameServer(gameHolder)
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
  //客户端发消息给后台
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

  //收到后台发给前端的消息
  def getSink(actor: ActorRef[ptcl.WsMsgSource]) =
    Flow[Message].collect{
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from websocket: $msg")
        ErrorWsMsgFront

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[ptcl.WsMsgSource](buffer) match {
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
    s"$wsProtocol://$host/gypsy/api/playGame?playerId=$playerId&playerName=$playerName&accessCode=$accessCode"
  }


}
