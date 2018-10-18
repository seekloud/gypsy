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
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, WsMsgServer, KeyCode}
import com.neo.sk.gypsy.shared.ptcl.{ErrorRsp, SuccessRsp}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol._
import com.neo.sk.gypsy.utils.SecureUtil
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.utils.byteObject.ByteObject._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.Boot.{executor, roomManager, timeout}
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.gypsy.core.RoomManager
import com.neo.sk.gypsy.http.ServiceUtils.CommonRsp

import scala.concurrent.{ExecutionContextExecutor, Future}

trait UserService extends ServiceUtils with SessionBase {

  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe._

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  implicit val timeout: Timeout

  val idGenerator = new AtomicInteger(1000000)
  val secretKey = "dsacsodaux84fsdcs4wc32xm"

  private[this] val log = LoggerFactory.getLogger(getClass)

  private val guestLogin = (path("guestLogin") & get) {
    loggingAction {
      _ =>
        parameter(
          'room.as[String],
          'name.as[String]) {
          (room,name) =>
            val guestId = idGenerator.getAndIncrement()
            val session = GypsySession(BaseUserInfo(UserRolesType.guest, guestId, name, ""), System.currentTimeMillis()).toSessionMap
            val flowFuture:Future[Flow[Message,Message,Any]]=roomManager ? (RoomManager.JoinGame(room,name,guestId,false,_))
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

  private val userRegister = (path("userRegister") & post) {
    loggingAction {
      _ =>
        entity(as[Either[Error, UserRegisterInfo]]) {
          case Left(error) =>
            log.warn(s"some error: $error")
            complete(ErrorRsp(1002003, "Pattern error."))
          case Right(userInfo) =>
            dealFutureResult(
              UserDao.getUserByName(userInfo.name).map {
                userNameOption =>
                  if (userNameOption.isDefined) {
                    complete(ErrorRsp(1002006, "name has existed!"))
                  } else {
                    val pwdMd5 = SecureUtil.generateSignature(List(userInfo.name, userInfo.password), secretKey)
                    dealFutureResult(
                      UserDao.addUser(userInfo.name, pwdMd5, userInfo.headImg, System.currentTimeMillis()).map {
                        id =>
                          val session = GypsySession(BaseUserInfo(UserRolesType.comMember, id, userInfo.name, userInfo.headImg), System.currentTimeMillis()).toSessionMap
                          addSession(session) {
                            complete(SuccessRsp())
                          }
                      }
                    )
                  }
              }
            )

        }
    }

  }

  private val userLogin = (path("userLogin") & pathEndOrSingleSlash & post) {
    loggingAction {
      _ =>
        entity(as[Either[Error, UserLoginInfo]]) {
          case Left(error) =>
            log.warn(s"some error: $error")
            complete(ErrorRsp(1002003, "Pattern error."))

          case Right(userInfo) =>
            dealFutureResult(
              UserDao.getUserByName(userInfo.name).map {
                userOption =>
                  if (userOption.isEmpty) {
                    complete(ErrorRsp(1002004, "User not exist."))
                  } else {
                    val user = userOption.get
                    val pwdMd5 = SecureUtil.generateSignature(List(userInfo.name, userInfo.password), secretKey)
                    if (pwdMd5 != user.password) {
                      complete(ErrorRsp(1002005, "Password wrong."))
                    } else {
                      if (user.isBan) {
                        complete(ErrorRsp(1002006, "您的账号已被禁用。"))
                      } else {
                        val session = GypsySession(BaseUserInfo(UserRolesType.comMember, user.id, userInfo.name, user.headImg.getOrElse("")), System.currentTimeMillis()).toSessionMap
                        addSession(session) {
                          //handleWebSocketMessages(webSocketChatFlow(userInfo.name,user.get.id))
                          complete(UserLoginRsq(Some(UserLoginRsqJson(user.id, user.name, user.headImg.getOrElse(""), user.score))))
                        }

                      }
                    }
                  }
              }
            )

        }
    }
  }

  private val userLoginWs= path("userLoginWs") {
    memberAuth{
      user=>
        parameter(
          'room.as[String]){room=>
          val flowFuture:Future[Flow[Message,Message,Any]]=roomManager ? (RoomManager.JoinGame(room,user.name,user.userId,false,_))
          dealFutureResult(
            flowFuture.map(r=>
              handleWebSocketMessages(r)
            )
          )
        }
    }
  }

/*  private val getUserScore=(path("getUserScore") & pathEndOrSingleSlash & get){
    memberAuth{
      user=>
        parameter('userId.as[Long]){
          userId=>
            dealFutureResult(
              UserDao.getScoreById(userId).map{
                score=>
                  if(score.isEmpty){
                    complete(UserScoreRsq(Some(0)))
                  }else{
                    complete(UserScoreRsq(Some(score.get)))
                  }
              }
            )
        }
    }

  }*/
  private val updateMaxScore=(path("updateMaxScore") & pathEndOrSingleSlash & post){
    memberAuth{
      user=>
        entity(as[Either[Error,UserMaxScore]]){
          case Left(error)=>
            log.warn(s"some error: $error")
            complete(ErrorRsp(1002003, "Pattern error."))
          case Right(score)=>
            dealFutureResult(
              UserDao.updateScoreById(score.id,score.score).map{
                a=>
                  complete(SuccessRsp())
              }
            )
        }
    }
  }

  private val checkName = (path("checkName") & pathEndOrSingleSlash & get) {
    loggingAction {
      _ =>
        parameter('name.as[String],'room.as[String]){
          (name,room)=>
            val flowFuture:Future[CheckNameRsp]=roomManager ? (RoomManager.CheckName(name,room,_))
            dealFutureResult(
              flowFuture.map(r=>
                  complete(r)
              )
            )
        }
    }
  }

  private val watcherJoin = (path("watcherJoin") & get){
    loggingAction {
      _ =>
        parameter(
          'room.as[String],
          'name.as[String]) {
          (room,name) =>
            val watcherId = idGenerator.getAndIncrement()
            val session = GypsySession(BaseUserInfo(UserRolesType.watcher, watcherId, name, ""), System.currentTimeMillis()).toSessionMap
            //随机分配一个视角给前端
            val flowFuture:Future[Flow[Message,Message,Any]]=roomManager ? (RoomManager.JoinGame(room,name,watcherId,true,_))
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


  val userRoutes: Route =
    pathPrefix("user") {
      guestLogin ~ userRegister ~ userLogin ~ userLoginWs ~ updateMaxScore~checkName ~ watcherJoin
    }


}
