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
}
