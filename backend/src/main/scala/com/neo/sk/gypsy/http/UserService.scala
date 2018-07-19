package com.neo.sk.gypsy.http

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer, Supervision}
import akka.util.Timeout
import com.neo.sk.gypsy.common.Constant.UserRolesType
import com.neo.sk.gypsy.http.SessionBase.GypsySession
import com.neo.sk.gypsy.models.Dao.UserDao
import com.neo.sk.gypsy.ptcl.UserProtocol.BaseUserInfo
import com.neo.sk.gypsy.shared.ptcl.{ErrorRsp, SuccessRsp}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.{UserLoginInfo, UserLoginRsq, UserLoginRsqJson, UserRegisterInfo}
import com.neo.sk.gypsy.snake.PlayGround
import com.neo.sk.gypsy.utils.SecureUtil
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

trait UserService extends ServiceUtils with SessionBase {

  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe._

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  implicit val timeout: Timeout

  lazy val playGround = Map(
    "11"->PlayGround.create("11",system),
    "12"->PlayGround.create("12",system),
    "21"->PlayGround.create("21",system),
    "22"->PlayGround.create("22",system))

  val idGenerator = new AtomicInteger(1000000)
  val secretKey = "dsacsodaux84fsdcs4wc32xm"

  private[this] val log = LoggerFactory.getLogger(getClass)

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

  private val guestLogin = (path("guestLogin") & get) {
    loggingAction {
      _ =>
        parameter(
          'room.as[String],
          'name.as[String]) {
          (room,name) =>
            val guestId = idGenerator.getAndIncrement()
            val session = GypsySession(BaseUserInfo(UserRolesType.guest, guestId, name, ""), System.currentTimeMillis()).toSessionMap
            addSession(session) {
              handleWebSocketMessages(webSocketChatFlow(room,name, guestId))
            }
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
        parameter('room.as[String]){room=>
          handleWebSocketMessages(webSocketChatFlow(room,user.name, user.userId))
        }
    }
  }


  val userRoutes: Route =
    pathPrefix("user") {
      guestLogin ~ userRegister ~ userLogin~userLoginWs

    }


}
