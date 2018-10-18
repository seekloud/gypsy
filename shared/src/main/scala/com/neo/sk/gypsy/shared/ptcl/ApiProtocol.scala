package com.neo.sk.gypsy.shared.ptcl

object ApiProtocol {

  case class RoomIdInfo(
                         roomId:Long
                       )

  case class PlayerInfo(
                       playerId:String,
                       nickname:String
                       )

  case class RoomPlayerInfoRsp(
                           data:List[PlayerInfo],
                           errCode: Int= 0,
                           msg:String = "ok"
                           )

  case class RoomIdRsp(
                        data:RoomIdInfo,
                        errCode: Int= 0,
                        msg:String = "ok"
                      )
}
