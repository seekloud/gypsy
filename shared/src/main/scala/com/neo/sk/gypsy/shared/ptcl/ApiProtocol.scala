package com.neo.sk.gypsy.shared.ptcl

/**
  * Created by wangchengyuan
  */
object ApiProtocol {

  case class roomInfo(
                     roomId:Long
                   )

  case class PlayerInfo(
                       playerId:String,
                       nickname:String
                       )

  case class players(
                       playerList:List[PlayerInfo]
                       )

  case class roomListInfo(
                     roomList:List[Long]
                     )

  case class RecordInfo(
                       recordId:Long,
                       roomId:Long,
                       startTime:Long,
                       endTime:Long,
                       userCounts:Int,
                       userList:Seq[String]
                       )

  case class RecordsInfo(
                       recordList:List[RecordInfo]
                       )

  case class RoomPlayerInfoRsp(
                           data:players,
                           errCode: Int =0,
                           msg:String = "ok"
                           )

  case class RoomIdRsp(
                        data:roomInfo,
                        errCode: Int= 0,
                        msg:String = "ok"
                      )

  case class RoomListRsp(
                        data:roomListInfo,
                        errCode: Int=0,
                        msg:String ="ok"
                        )

  case class AllVideoRecordReq(
                         lastRecordId:Long,
                         count:Int
                         )

  case class TimeVideoRecordReq(
                          startTime:Long,
                          endTime:Long,
                          lastRecordId:Long,
                          count:Int
                          )

  case class PlayerVideoRecordReq(
                                 playerId:String,
                                 lastRecordId:Long,
                                 count:Int
                                 )

  case class RecordListRsp(
                          data:RecordsInfo,
                          errCode:Int=0,
                          msg:String="ok"
                          )
}
