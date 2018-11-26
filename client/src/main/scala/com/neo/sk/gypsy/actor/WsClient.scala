package com.neo.sk.gypsy.actor

import java.net.URLEncoder

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.typed.scaladsl.{ActorSink, _}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.{ByteString, ByteStringBuilder}
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.holder.GameHolder
import com.neo.sk.gypsy.scene.GameScene
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._
import com.neo.sk.gypsy.common.AppSettings

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import io.circe.parser.decode
import io.circe.generic.auto._
import com.neo.sk.gypsy.common.Api4GameAgent._
import com.neo.sk.gypsy.ClientBoot.tokenActor


import scala.concurrent.ExecutionContext.Implicits.global
/**
  * @author zhaoyin
  * 2018/10/28  3:38 PM
  */
object WsClient {
  private val log = LoggerFactory.getLogger("WSClient")
  private val logPrefix = "WSClient"


  sealed trait WsCommand
  case class ConnectGame(id:String, name: String, accessCode: String) extends WsCommand
  case class ConnectEsheep(ws:String) extends WsCommand
  case object Stop extends WsCommand

  def create(gameClient: ActorRef[WsMsgSource],
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



  private def working(gameClient: ActorRef[WsMsgSource],
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
              val gameHolder = new GameHolder(stageCtx,gameScene,stream)
              gameHolder.connectToGameServer(gameHolder)
              Future.successful(s"$logPrefix connect success. EstablishConnectionEs!")
            } else {
              throw new RuntimeException(s"WSClient connection failed: ${upgrade.response.status}")
            }
          }
          //链接建立时
          connected.onComplete(i=> log.info(i.toString))
          Behaviors.same

        case ConnectEsheep(wsUrl) =>
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(wsUrl))
          val source = getSource(ctx.self)
          val sink = getSinkDup(ctx.self)
          val response =
            source
                .viaMat(webSocketFlow)(Keep.right)
                .toMat(sink)(Keep.left)
                .run()
          val connected = response.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
              Future.successful(s"$logPrefix connect success. ConnectEsheep!")
            } else {
              throw new RuntimeException(s"WSClient connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.onComplete(i => log.info(i.toString))
          Behaviors.same
        case Stop =>
          log.info("WsClient now stop")
          Behavior.stopped

        case _ =>
          Behaviors.same
      }
    }
  }
  //客户端发消息给后台
  def getSource(wsClient: ActorRef[WsCommand]) = ActorSource.actorRef[WsSendMsg](
    completionMatcher = {
      case WsSendComplete =>
        log.info("Websocket Complete")
        wsClient ! Stop
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

  //收到gypsy后台发给前端的消息
  def getSink(actor: ActorRef[WsMsgSource]) =
    Flow[Message].collect{
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from websocket: $msg")
        ErrorWsMsgFront(msg)

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[GameMessage](buffer) match {
            case Right(v) => v
            case Left(e) =>
              println(s"decode error: ${e.message}")
              ErrorWsMsgFront(e.message)
          }
        msg


    }.to(ActorSink.actorRef[WsMsgSource](actor, CompleteMsgServer, FailMsgServer))

  //收到esheep后台发给前端的消息
  def getSinkDup(self: ActorRef[WsCommand]):Sink[Message,Future[Done]]={
    Sink.foreach{
      case TextMessage.Strict(msg) =>
        val gameId = AppSettings.gameId
        import io.circe.generic.auto._
        import scala.concurrent.ExecutionContext.Implicits.global
        if(msg.length > 50) {
          decode[Ws4AgentResponse](msg) match {
            case Right(res) =>
              if(res.Ws4AgentRsp.errCode == 0){
                val data=res.Ws4AgentRsp.data
                val playerId = "user" + data.userId
                val nickName = data.nickname
                linkGameAgent(gameId,playerId,data.token).map{
                  case Right(resl) =>
                    tokenActor ! TokenActor.InitToken(data.token,data.tokenExpireTime,s"user${data.userId}")
                    self ! ConnectGame(playerId,nickName,resl.accessCode)
                  case Left(l) =>
                    log.error("link error!res:  "+ l)
                }
              }else{
                log.error("get token error!")
              }
            case Left(le) =>
              log.error(s"decode esheep webmsg error! Error information:${le}")

          }
        }
      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        bytesDecode[WsResponce](buffer) match {
          case Right(v) =>
          case Left(e) =>
            println(s"decode error: ${e.message}")
        }
    }
  }

  def getWebSocketUri(playerId: String, playerName: String, accessCode: String):String = {
    val wsProtocol = "ws"
    val domain = AppSettings.gameDomain  //部署到服务器上用这个
//    val domain = "localhost:30371"
    val playerIdEncoder = URLEncoder.encode(playerId, "UTF-8")
    val playerNameEncoder = URLEncoder.encode(playerName, "UTF-8")
    s"$wsProtocol://$domain/gypsy/api/playGame?playerId=$playerIdEncoder&playerName=$playerNameEncoder&accessCode=$accessCode"
  }

}
