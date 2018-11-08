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
import com.neo.sk.gypsy.models.Dao.RecordDao
import com.neo.sk.gypsy.shared.ptcl.ErrorRsp
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

  private val getVideoList = (path("getRecordList") & post & pathEndOrSingleSlash) {
    dealPostReq[AllVideoRecordReq] { j =>
      RecordDao.getAllRecord(j.lastRecordId,j.count).map{
        i =>
          val userListMap=i._2.groupBy(_.recordId)
          val record=i._1.map(i=>
            ( i.recordId,
              i.roomId,
              i.startTime,
              i.endTime,
              userListMap(i.recordId).length,
              userListMap(i.recordId).map(_.userId)
            )
          ).toList
          val data=RecordsInfo(record.map{i=>
            RecordInfo(i._1,i._2,i._3,i._4,i._5,i._6)
          })
        complete(RecordListRsp(data))
      }.recover {
        case e: Exception =>
          log.info(s"getAllVideoRecord exception.." + e.getMessage)
          complete(ErrorRsp(100000, "error occured."))
      }
    }
  }

  private val getVideoByTime = (path("getRecordListByTime")&post&pathEndOrSingleSlash) {
    dealPostReq[TimeVideoRecordReq] { j=>
      RecordDao.getRecordByTime(j.lastRecordId,j.count,j.startTime,j.endTime).map{
        i =>
          val userListMap=i._2.groupBy(_.recordId)
          val record=i._1.map(i=>
            ( i.recordId,
              i.roomId,
              i.startTime,
              i.endTime,
              userListMap(i.recordId).length,
              userListMap(i.recordId).map(_.userId)
            )
          ).toList
          val data=RecordsInfo(record.map{i=>
            RecordInfo(i._1,i._2,i._3,i._4,i._5,i._6)
          })
          complete(RecordListRsp(data))
      }.recover {
        case e: Exception =>
          log.info(s"getVideoRecordByTime exception.." + e.getMessage)
          complete(ErrorRsp(100001, "error occured."))
      }
    }
  }

  private val getVideoByPlayer = (path("getRecordListByPlayer")&post&pathEndOrSingleSlash){
    dealPostReq[PlayerVideoRecordReq] { j=>
      RecordDao.getRecordByPlayer(j.lastRecordId,j.count,j.playerId).map{
        i=>
          val userListMap=i._2.groupBy(_.recordId)
          val record=i._1.map(i=>
            ( i.recordId,
              i.roomId,
              i.startTime,
              i.endTime,
              userListMap(i.recordId).length,
              userListMap(i.recordId).map(_.userId)
            )
          ).toList
          val data=RecordsInfo(record.map{i=>
            RecordInfo(i._1,i._2,i._3,i._4,i._5,i._6)
          })
          complete(RecordListRsp(data))
      }.recover {
        case e: Exception =>
          log.info(s"getVideoRecordByPlayer exception.." + e.getMessage)
          complete(ErrorRsp(100002, "error occured."))
      }
    }
  }


  val apiRoutes:Route=
    getRoomId~getGamePlayerList~getGameRoomList~getVideoList~getVideoByTime~getVideoByPlayer
}
