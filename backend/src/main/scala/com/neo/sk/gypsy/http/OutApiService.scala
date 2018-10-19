package com.neo.sk.gypsy.http

import org.slf4j.LoggerFactory
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, public}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.neo.sk.gypsy.core.RoomManager
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol.{RoomIdInfo, RoomIdRsp, RoomPlayerInfoRsp}
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.gypsy.Boot.{executor, roomManager, timeout}

import scala.concurrent.{ExecutionContextExecutor, Future}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._

trait OutApiService extends ServiceUtils with SessionBase{

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
    dealPostReq[String] { RoomId =>
      val msgFuture:Future[RoomPlayerInfoRsp] = roomManager ? (RoomManager.GetGamePlayerList(RoomId,_))
      msgFuture.map{
        msg => complete(msg)
      }
    }
  }

  val apiRoutes:Route=
    getRoomId~getGamePlayerList
}
