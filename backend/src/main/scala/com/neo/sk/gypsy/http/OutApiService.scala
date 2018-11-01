package com.neo.sk.gypsy.http

import org.slf4j.LoggerFactory
import akka.actor.{ActorSystem, Scheduler}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, public}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.neo.sk.gypsy.core.RoomManager
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.neo.sk.gypsy.Boot.{executor, roomManager, timeout}
import org.seekloud.byteobject.encoder.BytesEncoder

import scala.concurrent.{ExecutionContextExecutor, Future}


trait OutApiService extends ServiceUtils with SessionBase{

  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe._

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  private val log = LoggerFactory.getLogger(this.getClass)

  private val getRoomId = (path("getRoomId") & post & pathEndOrSingleSlash){
    dealPostReq[String]{ playerId =>
      val msgFuture:Future[RoomIdRsp]= roomManager ? (RoomManager.GetRoomId(playerId , _))
      msgFuture.map{
          msg => complete(msg)
        }
     }
  }

  private val getGamePlayerList = (path("getGamePlayerList") & post & pathEndOrSingleSlash){
    dealPostReq[Long] { RoomId =>
      val msgFuture:Future[RoomPlayerInfoRsp] = roomManager ? (RoomManager.GetGamePlayerList(RoomId,_))
      msgFuture.map{
        msg => complete(msg)
      }
    }
  }

  private val getGameRoomList = (path("getGameRoomList") & post & pathEndOrSingleSlash){
    dealPostReq[String] { _ =>
      val msgFuture:Future[RoomListRsp] = roomManager ? (RoomManager.GetRoomList(_))
      msgFuture.map{
        msg => complete(msg)
      }
    }
  }

  val apiRoutes :Route=
    getRoomId~getGamePlayerList~getGameRoomList
}
