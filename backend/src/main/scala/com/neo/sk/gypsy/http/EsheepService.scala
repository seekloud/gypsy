package com.neo.sk.gypsy.http

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer, Supervision}
import akka.util.{ByteString, Timeout}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.common.Constant.UserRolesType
import com.neo.sk.gypsy.http.SessionBase.GypsySession
import com.neo.sk.gypsy.ptcl.UserProtocol.BaseUserInfo
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.Boot.{esheepClient, executor, roomManager, timeout, userManager}
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.gypsy.core.{EsheepSyncClient, RoomManager,UserManager}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait EsheepService  extends ServiceUtils with SessionBase with AuthService{


  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  implicit val timeout: Timeout

  private[this] val log = LoggerFactory.getLogger(getClass)

  private def AuthUserErrorRsp(msg: String) = ErrorRsp(10001001, msg)

  private[this] val idGenerator = new AtomicInteger(1)

  private def playGame = (path("playGame") & get & pathEndOrSingleSlash) {
    parameter(
      'playerId.as[String],
      'playerName.as[String],
      'accessCode.as[String],
      'roomId.as[Long].?
    ){ case ( playerId, playerName, accessCode, roomIdOpt) =>
      if(AppSettings.gameTest){
        val session = GypsySession(BaseUserInfo(UserRolesType.player, playerId, playerName, ""), System.currentTimeMillis()).toSessionMap
        val flowFuture:Future[Flow[Message,Message,Any]]=userManager ? (UserManager.GetWebSocketFlow(Some(PlayerInfo(playerId,playerName)),roomIdOpt,_))
        dealFutureResult(
          flowFuture.map(r=>
            addSession(session) {
              handleWebSocketMessages(r)
            }
          )
        )
      }else{
        authPlatUser(accessCode){player =>
          val session = GypsySession(BaseUserInfo(UserRolesType.player, playerId, playerName, ""), System.currentTimeMillis()).toSessionMap
          val flowFuture:Future[Flow[Message,Message,Any]]=userManager ? (UserManager.GetWebSocketFlow(Some(PlayerInfo(player.playerId,playerName)),roomIdOpt,_))
          dealFutureResult(
            flowFuture.map(r=>
              addSession(session) {
                handleWebSocketMessages(r)
              }
            )
          )
        }
      }

    }
  }

  private def watchGame = (path("watchGame") & get) {
    parameter(
      'playerId.as[String].?,//观看视角
      'accessCode.as[String],
      'roomId.as[Long]
    ){ case ( playerIdOpt,  accessCode, roomId) =>
      if(AppSettings.gameTest){
        val watcherId = "watcher" + idGenerator.getAndIncrement()
        val session = GypsySession(BaseUserInfo(UserRolesType.watcher, watcherId, watcherId, ""), System.currentTimeMillis()).toSessionMap
        val flowFuture:Future[Flow[Message,Message,Any]]=userManager ? (UserManager.GetWatchWebSocketFlow(Some(PlayerInfo(watcherId,watcherId)),playerIdOpt,roomId,_))
        dealFutureResult(
          flowFuture.map(r=>
            addSession(session) {
              handleWebSocketMessages(r)
            }
          )
        )
      } else {
        authPlatUser(accessCode){ player =>
          val session = GypsySession(BaseUserInfo(UserRolesType.watcher, player.playerId, player.nickname, ""), System.currentTimeMillis()).toSessionMap
          val flowFuture:Future[Flow[Message,Message,Any]]=userManager ? (UserManager.GetWatchWebSocketFlow(Some(PlayerInfo(player.playerId,player.nickname)),playerIdOpt,roomId,_))
          dealFutureResult(
            flowFuture.map(r=>
              addSession(session) {
                handleWebSocketMessages(r)
              }
            )
          )
        }
      }
    }
  }

  private def watchRecord = (path("watchRecord") & get){
    parameter(
      'recordId.as[Long],
      'playerId.as[String], //回放视角
      'frame.as[Int],
      'accessCode.as[String]
    ){ (recordId, playerId, frame, accessCode) =>
      if(AppSettings.gameTest){
        val replayWatcherId = "replay" + idGenerator.getAndIncrement()
        val session = GypsySession(BaseUserInfo(UserRolesType.replayer, replayWatcherId, replayWatcherId, ""), System.currentTimeMillis()).toSessionMap
        val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetReplaySocketFlow(Some(PlayerInfo(replayWatcherId,replayWatcherId)),recordId,frame,playerId,_))
        dealFutureResult(
          flowFuture.map(t =>
            addSession(session){
              handleWebSocketMessages(t)
            }
          )
        )
      }else{
        authPlatUser(accessCode){player =>
          val session = GypsySession(BaseUserInfo(UserRolesType.replayer, player.playerId, player.nickname, ""), System.currentTimeMillis()).toSessionMap
          val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetReplaySocketFlow(Some(PlayerInfo(player.playerId,player.nickname)),recordId,frame,playerId,_))
          dealFutureResult(
            flowFuture.map(t =>
              addSession(session){
                handleWebSocketMessages(t)
              }
            )
          )
        }
      }
    }
  }
  //for bot
  private def playGameBot = (path("playGameBot") & get){
    parameter(
      'playerId.as[String],
      'playerName.as[String],
      'accessCode.as[String]
    ){ case (playerId, playerName, accessCode) =>
        authPlatUser(accessCode){player =>
          val session = GypsySession(BaseUserInfo(UserRolesType.player, playerId, playerName, ""), System.currentTimeMillis()).toSessionMap
          val flowFuture:Future[Flow[Message,Message,Any]]= userManager ? (UserManager.GetBotSocketFlow(Some(PlayerInfo(player.playerId,playerName)),_))
          dealFutureResult(
            flowFuture.map(r=>
              addSession(session) {
                handleWebSocketMessages(r)
              }
            )
          )
        }
    }
  }

  val esheepRoutes: Route = pathPrefix("api"){
      playGame ~ watchGame ~ watchRecord ~ playGameBot
    }
}
