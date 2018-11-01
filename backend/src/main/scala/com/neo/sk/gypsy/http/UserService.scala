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
//import com.neo.sk.gypsy.models.Dao.UserDao
import com.neo.sk.gypsy.ptcl.UserProtocol.BaseUserInfo
//import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, KeyCode}
import com.neo.sk.gypsy.shared.ptcl.GypsyGameEvent.{ErrorWsMsgServer, KeyCode}
import com.neo.sk.gypsy.shared.ptcl.{ErrorRsp, SuccessRsp}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol._
import com.neo.sk.gypsy.utils.SecureUtil
import org.seekloud.byteobject._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.Boot.{executor, roomManager, timeout}
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.gypsy.core.RoomManager
import com.neo.sk.gypsy.http.ServiceUtils.CommonRsp

import scala.concurrent.{ExecutionContextExecutor, Future}

trait UserService extends ServiceUtils with SessionBase {



  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  implicit val timeout: Timeout

  val idGenerator = new AtomicInteger(1000000)
  val secretKey = "dsacsodaux84fsdcs4wc32xm"

  private[this] val log = LoggerFactory.getLogger(getClass)

//  private val updateMaxScore=(path("updateMaxScore") & pathEndOrSingleSlash & post){
//    memberAuth{
//      user=>
//        entity(as[Either[Error,UserMaxScore]]){
//          case Left(error)=>
//            log.warn(s"some error: $error")
//            complete(ErrorRsp(1002003, "Pattern error."))
//          case Right(score)=>
//            dealFutureResult(
//              UserDao.updateScoreById(score.id,score.score).map{
//                a=>
//                  complete(SuccessRsp())
//              }
//            )
//        }
//    }
//  }

  private val checkName = (path("checkName") & pathEndOrSingleSlash & get) {
    import io.circe.generic.auto._
    import io.circe.syntax._
    import io.circe._
    loggingAction {
      _ =>
        parameter('name.as[String],'room.as[Long]){
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
        //todo 这里要改
        parameter(
          'room.as[Long],
          'name.as[String]) {
          (room,name) =>
            val watcherId = "watcher" + idGenerator.getAndIncrement()
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
       checkName ~ watcherJoin
    }


}
