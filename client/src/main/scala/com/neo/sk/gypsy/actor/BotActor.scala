package com.neo.sk.gypsy.actor

import java.net.URLEncoder

import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.neo.sk.gypsy.botService.BotServer
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.util.{ByteString, ByteStringBuilder}
import com.google.protobuf.ByteString
import com.neo.sk.gypsy.ClientBoot
import org.seekloud.byteobject.ByteObject.{bytesDecode, _}
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.gypsy.common.{AppSettings, Constant, StageContext}

import scala.concurrent.Future
import com.neo.sk.gypsy.ClientBoot.{executor, materializer, scheduler, system, tokenActor}
import com.neo.sk.gypsy.actor.BotActor.LeaveRoom
import com.neo.sk.gypsy.common.Api4GameAgent.{botKey2Token, linkGameAgent}
import com.neo.sk.gypsy.common.Constant.{layeredCanvasHeight, layeredCanvasWidth}
import com.neo.sk.gypsy.holder.BotHolder
import com.neo.sk.gypsy.holder.BotHolder.Command
import com.neo.sk.gypsy.scene.LayeredScene
import org.seekloud.esheepapi.pb.actions.{Move, Swing}
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Protocol4Bot._
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.observations.{ImgData, LayeredObservation}
//import com.google.protobuf.ByteString


/**
  * Created by wym on 2018/12/3.
  **/

object BotActor {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class BotLogin(botId:Long, botKey:String) extends Command

  case class Work(stream: ActorRef[Protocol.WsSendMsg]) extends Command

  case class CreateRoom(sender:ActorRef[JoinRoomRsp]) extends Command

  case class JoinRoom(roomId: String,sender:ActorRef[JoinRoomRsp] ) extends Command

  case object LeaveRoom extends Command

  case object ActionSpace extends Command

  case class Action(key:Int, swing: Option[Swing],sender:ActorRef[ActionRsp]) extends Command

  case class Inform(sender:ActorRef[InformRsp]) extends Command

  case class ReturnObservation(sender:ActorRef[ObservationRsp]) extends Command

  case class MsgToService(sendMsg: WsSendMsg) extends Command

  case class GetByte(localByte:Array[Byte],noninteractByte:Array[Byte],interactByte:Array[Byte],allplayerByte:Array[Byte],playerByte:Array[Byte],infoByte:Array[Byte],humanByte:Array[Byte]) extends Command

  case object Stop extends Command

  var SDKReplyTo:ActorRef[JoinRoomRsp] = _

  var botHolder:BotHolder = _


  def create(
              gameClient: ActorRef[WsMsgSource],
              stageCtx: StageContext
            ): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers { implicit timer =>
        waitingGaming(gameClient,stageCtx)
      }
    }
  }

  def waitingGaming(gameClient: ActorRef[WsMsgSource],
                    stageCtx: StageContext)(implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case BotLogin(botId,botKey) =>
          botKey2Token(botId,botKey).map{
            case Right(value) =>
              val gameId = AppSettings.gameId
              val playerId = "bot" + botId
              linkGameAgent(gameId,playerId,value.token).map{
                case Right(res) =>
                  val accessCode = res.accessCode
                  val url = getWebSocketUri(playerId,value.botName,accessCode)
                  val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
                  val source = getSource(ctx.self)
                  val sink = getSink(gameClient)
                  val (stream, response) =
                    source
                        .viaMat(webSocketFlow)(Keep.both)
                        .toMat(sink)(Keep.left)
                        .run()
                  val connected = response.flatMap{ upgrade =>
                    if(upgrade.response.status == StatusCodes.SwitchingProtocols){
                      tokenActor ! TokenActor.InitToken(value.token,value.expireTime,playerId)
                      //暂时用普通玩家登陆流程
                      stream ! Protocol.JoinRoom(None)
                      val layeredScene = new LayeredScene
                      botHolder = new BotHolder(stageCtx,layeredScene,stream,ctx.self)
                      botHolder.connectToGameServer()
//                    ctx.self ! Work(stream)
                      Future.successful("BotActor webscoket connect success.")
                    }else{
                      throw new RuntimeException(s"BotActor webscoket connection failed: ${upgrade.response.status}")
                    }
                  }
                  connected.onComplete(i => log.info(i.toString))

                case Left(e) =>
              }
            case Left(e) =>
          }
          Behaviors.same
        case Work(stream) =>
          //启动BotService
          val port = 5321
          val server = BotServer.build(port, executor, ctx.self)
          server.start()
          log.debug(s"Server started at $port")
          sys.addShutdownHook {
            log.debug("JVM SHUT DOWN.")
            server.shutdown()
            log.debug("SHUT DOWN.")
          }
//          server.awaitTermination()
//          log.debug("DONE.")
          waitingGame(gameClient,stageCtx,stream)

        case unknown@_ =>
          log.debug(s"i receive an unknown msg:$unknown")
          Behaviors.unhandled
      }
    }
  }

  def waitingGame(gameClient: ActorRef[WsMsgSource],
                  stageCtx: StageContext,
                  stream: ActorRef[Protocol.WsSendMsg]
                 )(implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case CreateRoom(sender) =>
          SDKReplyTo = sender
          stream ! Protocol.CreateRoom
          val layeredScene = new LayeredScene
          botHolder = new BotHolder(stageCtx,layeredScene,stream,ctx.self)
          botHolder.connectToGameServer()
          gaming(stream,(Array.empty,Array.empty,Array.empty,Array.empty,Array.empty,Array.empty,Array.empty))

        case JoinRoom(roomId, sender) =>
          SDKReplyTo = sender
          stream ! Protocol.JoinRoom(Some(roomId.toLong))
          val layeredScene = new LayeredScene
          botHolder = new BotHolder(stageCtx,layeredScene,stream,ctx.self)
          botHolder.connectToGameServer()
          gaming(stream,(Array.empty,Array.empty,Array.empty,Array.empty,Array.empty,Array.empty,Array.empty))

        case Stop =>
          Behaviors.stopped

        case unknown@_ =>
          log.debug(s"i receive an unknown msg:$unknown")
          Behaviors.unhandled
      }
    }
  }

  def gaming(actor: ActorRef[Protocol.WsSendMsg],byteInfo: (Array[Byte], Array[Byte], Array[Byte], Array[Byte], Array[Byte], Array[Byte], Array[Byte])
            )(implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Action(key,swing,sender) =>
          botHolder.gameActionReceiver(key,swing)
          sender ! ActionRsp(frameIndex = botHolder.getFrameCount.toInt, msg = "ok")
          Behaviors.same


        case GetByte(localByte,noninteractByte,interactByte,allplayerByte,playerByte,infoByte,humanByte) =>
          gaming(actor,(localByte,noninteractByte,interactByte,allplayerByte,playerByte,infoByte,humanByte))

        case ReturnObservation(sender) =>
          //TODO
          val layerInfo = LayeredObservation(
            Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._1.length,ByteString.copyFrom(byteInfo._1))),
            Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._2.length,ByteString.copyFrom(byteInfo._2))),
            Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._3.length,ByteString.copyFrom(byteInfo._3))),
            Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._4.length,ByteString.copyFrom(byteInfo._4))),
            Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._5.length,ByteString.copyFrom(byteInfo._5))),
            Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._6.length,ByteString.copyFrom(byteInfo._6)))
          )
          val observation = ObservationRsp(Some(layerInfo),Some(ImgData(layeredCanvasWidth,layeredCanvasHeight,byteInfo._7.length,ByteString.copyFrom(byteInfo._7))))
          sender ! observation
          Behaviors.same

        case Inform(sender) =>
          sender ! InformRsp(score = botHolder.getInform._1.toInt, kills = botHolder.getInform._2,heath = botHolder.getInform._3)
          Behaviors.same

        case LeaveRoom =>
          log.info("BotActor now stop.")
          Behaviors.stopped

        case Stop =>
          Behaviors.stopped

        case unknown@_ =>
          log.debug(s"i receive an unknown msg:$unknown")
          Behaviors.unhandled
      }
    }
  }

  private[this] def getSink(gameClient: ActorRef[WsMsgSource]) =
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
    }.to(ActorSink.actorRef[WsMsgSource](gameClient, CompleteMsgServer, FailMsgServer))

  private[this] def getSource(botActor: ActorRef[Command]) = ActorSource.actorRef[WsSendMsg](
    completionMatcher = {
      case WsSendComplete =>
        log.info("webscoket complete")
        botActor ! Stop
    }, failureMatcher = {
      case WsSendFailed(ex) ⇒ ex
    },
    bufferSize = 64,
    overflowStrategy = OverflowStrategy.fail
  ).collect {
    case message: UserAction =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))
  }

  def getWebSocketUri(playerId: String, playerName:String, accessCode: String): String = {
    val wsProtocol = "ws"
//    val domain = "10.1.29.250:30371"
//    val domain = "localhost:30371"
    val domain = AppSettings.gameDomain  //部署到服务器上用这个
    val playerIdEncoder = URLEncoder.encode(playerId, "UTF-8")
    val playerNameEncoder = URLEncoder.encode(playerName, "UTF-8")
    s"$wsProtocol://$domain/gypsy/api/playGameBot?playerId=$playerIdEncoder&playerName=$playerNameEncoder&accessCode=$accessCode"
  }


}