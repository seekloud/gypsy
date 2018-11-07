package com.neo.sk.gypsy.http

import java.io.File

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
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import com.neo.sk.gypsy.Boot.{executor, roomManager, timeout, userManager}
import com.neo.sk.gypsy.models.Dao.RecordDao
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{GetRecordFrameMsg, GetUserInRecordMsg}
import com.neo.sk.gypsy.shared.ptcl.{CommonRsp, ErrorRsp}
import com.neo.sk.gypsy.utils.byteObject.encoder.BytesEncoder

import scala.concurrent.{ExecutionContextExecutor, Future}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._

trait OutApiService extends ServiceUtils with SessionBase {

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  private val log = LoggerFactory.getLogger(this.getClass)

  private def getGameRecErrorRsp(msg: String) = ErrorRsp(1000, msg)

  private val getRoomId = (path("getRoomId") & post & pathEndOrSingleSlash) {
    dealPostReq[getRoomReq] { req =>
      val msgFuture: Future[RoomIdRsp] = roomManager ? (RoomManager.GetRoomId(req.playerId, _))
      msgFuture.map {
        msg => complete(msg)
      }
    }
  }

  private val getGamePlayerList = (path("getGamePlayerList") & post & pathEndOrSingleSlash) {
    dealPostReq[getPlayerReq] { req =>
      val msgFuture: Future[RoomPlayerInfoRsp] = roomManager ? (RoomManager.GetGamePlayerList(req.roomId, _))
      msgFuture.map {
        msg => complete(msg)
      }
    }
  }

  private val getGameRoomList = (path("getGameRoomList") & post & pathEndOrSingleSlash) {
    dealGetReq {
      val msgFuture: Future[RoomListRsp] = roomManager ? (RoomManager.GetRoomList(_))
      msgFuture.map {
        msg => complete(msg)
      }
    }
  }

  private val getVideoList = (path("getRecordList") & post & pathEndOrSingleSlash) {
    dealPostReq[AllVideoRecordReq] { j =>
      RecordDao.getAllRecord(j.lastRecordId, j.count).map {
        i =>
          val userListMap = i._2.groupBy(_.recordId)
          val record = i._1.map(i =>
            (i.recordId,
              i.roomId,
              i.startTime,
              i.endTime,
              userListMap(i.recordId).length,
              userListMap(i.recordId).map(_.userId)
            )
          ).toList
          val data = RecordsInfo(record.map { i =>
            RecordInfo(i._1, i._2, i._3, i._4, i._5, i._6)
          })
          complete(RecordListRsp(data))
      }.recover {
        case e: Exception =>
          log.info(s"getAllVideoRecord exception.." + e.getMessage)
          complete(ErrorRsp(100000, "error occured."))
      }
    }
  }

  private val getVideoByTime = (path("getRecordListByTime") & post & pathEndOrSingleSlash) {
    dealPostReq[TimeVideoRecordReq] { j =>
      RecordDao.getRecordByTime(j.lastRecordId, j.count, j.startTime, j.endTime).map {
        i =>
          val userListMap = i._2.groupBy(_.recordId)
          val record = i._1.map(i =>
            (i.recordId,
              i.roomId,
              i.startTime,
              i.endTime,
              userListMap(i.recordId).length,
              userListMap(i.recordId).map(_.userId)
            )
          ).toList
          val data = RecordsInfo(record.map { i =>
            RecordInfo(i._1, i._2, i._3, i._4, i._5, i._6)
          })
          complete(RecordListRsp(data))
      }.recover {
        case e: Exception =>
          log.info(s"getVideoRecordByTime exception.." + e.getMessage)
          complete(ErrorRsp(100001, "error occured."))
      }
    }
  }

  private val getVideoByPlayer = (path("getRecordListByPlayer") & post & pathEndOrSingleSlash) {
    dealPostReq[PlayerVideoRecordReq] { j =>
      RecordDao.getRecordByPlayer(j.lastRecordId, j.count, j.playerId).map {
        i =>
          val userListMap = i._2.groupBy(_.recordId)
          val record = i._1.map(i =>
            (i.recordId,
              i.roomId,
              i.startTime,
              i.endTime,
              userListMap(i.recordId).length,
              userListMap(i.recordId).map(_.userId)
            )
          ).toList
          val data = RecordsInfo(record.map { i =>
            RecordInfo(i._1, i._2, i._3, i._4, i._5, i._6)
          })
          complete(RecordListRsp(data))
      }.recover {
        case e: Exception =>
          log.info(s"getVideoRecordByPlayer exception.." + e.getMessage)
          complete(ErrorRsp(100002, "error occured."))
      }
    }
  }

  private val downloadRecord = (path("downloadRecord") & post) {
    dealPostReq[DownloadRecordReq] { req =>
      RecordDao.getFilePath(req.recordId).map { r =>
        val fileName = r.head
        val f = new File(fileName)
        if (f.exists()) {
          val responseEntity = HttpEntity(
            ContentTypes.`application/octet-stream`,
            f.length,
            FileIO.fromPath(f.toPath, chunkSize = 262144))
          complete(responseEntity)
        } else complete(getGameRecErrorRsp("file not exist"))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordPlayerList=(path("getRecordPlayerList") & post){
    dealPostReq[GetUserInRecordReq]{req=>
      val flowFuture:Future[CommonRsp]= userManager ? (GetUserInRecordMsg(req.recordId,req.playerId,_))
      flowFuture.map {
        case r: userInRecordRsp =>
          complete(r)
        case _=>
          complete(ErrorRsp(10001,"init error"))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(ErrorRsp(10001,"init error"))
      }
    }
  }

  private val getRecordFrame=(path("getRecordFrame") & post) {
    dealPostReq[GetRecordFrameReq] { req =>
      val flowFuture: Future[CommonRsp] = userManager ? (GetRecordFrameMsg(req.recordId, req.playerId, _))
      flowFuture.map {
        case r: GetRecordFrameRsp =>
          complete(r)
        case _ =>
          complete(ErrorRsp(10001, "init error"))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(ErrorRsp(10001, "init error"))
      }
    }
  }




  val apiRoutes:Route=
    getRoomId~getGamePlayerList~getGameRoomList~getVideoList~getVideoByTime~getVideoByPlayer~
      downloadRecord~getRecordPlayerList~getRecordFrame
}
