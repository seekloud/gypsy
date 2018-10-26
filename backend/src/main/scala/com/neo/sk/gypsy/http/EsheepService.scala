package com.neo.sk.gypsy.http

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer, Supervision}
import akka.util.{ByteString, Timeout}
import com.neo.sk.gypsy.common.Constant.UserRolesType
import com.neo.sk.gypsy.http.SessionBase.GypsySession
import com.neo.sk.gypsy.models.Dao.UserDao
import com.neo.sk.gypsy.ptcl.UserProtocol.BaseUserInfo
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, KeyCode, WsMsgServer}
import com.neo.sk.gypsy.shared.ptcl.{ErrorRsp, SuccessRsp}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol._
import com.neo.sk.gypsy.utils.SecureUtil
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.utils.byteObject.ByteObject._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.Boot.{esheepClient, executor, roomManager, timeout}
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.gypsy.core.{EsheepSyncClient, RoomManager}
import com.neo.sk.gypsy.http.ServiceUtils.CommonRsp
import com.neo.sk.gypsy.ptcl.EsheepProtocol

import scala.concurrent.{ExecutionContextExecutor, Future}

trait EsheepService  extends ServiceUtils with SessionBase{
  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe._

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  implicit val timeout: Timeout


  private[this] val log = LoggerFactory.getLogger(getClass)

  private def AuthUserErrorRsp(msg: String) = ErrorRsp(10001001, msg)

  private def playGame = (path("playGame") & get) {
    parameter(
      'playerId.as[Long],
      'playerName.as[String],
      'accessCode.as[String],
      'roomId.as[Long].?
    ){ case ( userId, nickName, accessCode, roomIdOpt) =>
      val verifyAccessCodeFutureRst: Future[EsheepProtocol.VerifyAccessCodeRsp] = esheepClient ? (e => EsheepSyncClient.VerifyAccessCode(accessCode, e))
      dealFutureResult{
        verifyAccessCodeFutureRst.map{ rsp =>
          if(rsp.errCode == 0 && rsp.data.nonEmpty){
            val session = GypsySession(BaseUserInfo(UserRolesType.guest, userId, nickName, ""), System.currentTimeMillis()).toSessionMap
            val flowFuture:Future[Flow[Message,Message,Any]]=roomManager ? (RoomManager.JoinGame(roomIdOpt.getOrElse(1000001),nickName,userId,false,_))
            dealFutureResult(
              flowFuture.map(r=>
                addSession(session) {
                  handleWebSocketMessages(r)
                }
              )
            )
          } else{
            complete(AuthUserErrorRsp(rsp.msg))
          }
        }.recover{
          case e:Exception =>
            log.warn(s"verifyAccess code failed, code=${accessCode}, error:${e.getMessage}")
            complete(AuthUserErrorRsp(e.getMessage))
        }
      }
    }
  }
  private def watchGame = (path("watchGame") & get) {
    parameter(
      'userId.as[Long],
      'accessCode.as[String],
      'roomId.as[Long]
    ){ case ( userId,  accessCode, roomId) =>
      val verifyAccessCodeFutureRst: Future[EsheepProtocol.VerifyAccessCodeRsp] = esheepClient ? (e => EsheepSyncClient.VerifyAccessCode(accessCode, e))
      dealFutureResult{
        verifyAccessCodeFutureRst.map{ rsp =>
          if(rsp.errCode == 0 && rsp.data.nonEmpty){
            val session = GypsySession(BaseUserInfo(UserRolesType.guest, userId, userId.toString, ""), System.currentTimeMillis()).toSessionMap
            val flowFuture:Future[Flow[Message,Message,Any]]=roomManager ? (RoomManager.JoinGame(roomId,userId.toString,userId,true,_))
            dealFutureResult(
              flowFuture.map(r=>
                addSession(session) {
                  handleWebSocketMessages(r)
                }
              )
            )
          } else{
            complete(AuthUserErrorRsp(rsp.msg))
          }
        }.recover{
          case e:Exception =>
            log.warn(s"verifyAccess code failed, code=${accessCode}, error:${e.getMessage}")
            complete(AuthUserErrorRsp(e.getMessage))
        }
      }
    }
  }

  val esheepRoutes: Route =pathPrefix("api"){
    playGame ~ watchGame
  }
}
