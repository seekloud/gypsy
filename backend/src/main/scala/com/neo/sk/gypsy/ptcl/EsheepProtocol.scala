package com.neo.sk.gypsy.ptcl

import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._

object EsheepProtocol {

  case class VerifyAccCode(
                          gameId:Long,
                          accessCode:String
                          )
  case class GameServerKey2TokenReq(
                                gameId:Long,
                                gsKey:String
                                )

  case class GameServerKey2TokenInfo(
                                      token:String,
                                      expireTime:Long
                                    )

  case class GameServerKey2TokenRsp(
                                     data: GameServerKey2TokenInfo,
                                     errCode: Int = 0,
                                     msg: String = "ok"
                                   ) extends CommonRsp


  case class PlayerInfo(
                         playerId:String,
                         nickname:String
                       )

  case class VerifyAccessCodeInfo(
                                   playerInfo:PlayerInfo
                                 )

  case class VerifyAccessCodeRsp(
                                  data: Option[PlayerInfo],
                                  errCode: Int = 0,
                                  msg: String = "ok"
                                ) extends CommonRsp

  case class RecordInfo(
                         playerId: String,
                         gameId: Long,
                         nickname: String,
                         killing: Int,
                         killed: Int,
                         score: Int,
                         gameExtent: String,
                         startTime: Long,
                         endTime: Long
                       )
  case class PlayerRecordInfo(
                               playerRecord:RecordInfo
                             )

}
