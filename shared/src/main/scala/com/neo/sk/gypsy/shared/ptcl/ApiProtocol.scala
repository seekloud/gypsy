package com.neo.sk.gypsy.shared.ptcl

import Game._
/**
  * Created by wangchengyuan
  */
object ApiProtocol {

  trait CommonRsp {
    val errCode: Int
    val msg: String
  }


  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp


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
                       userList:Seq[(String,String)]
                       )


  case class RecordListRsp(
                            data:List[RecordInfo],
                            errCode:Int=0,
                            msg:String="ok"
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

  case class getRoomReq(
                       playerId:String
                       )

  case class getPlayerReq(
                         roomId:Long
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

  case class DownloadRecordReq(
                                recordId:Long
                              )

  case class GetUserInRecordReq(
                                 recordId:Long,
                                 playerId:String //观看玩家id
                               )

  case class ExistTimeInfo(
                            startFrame:Long,
                            endFrame:Long
                          )

  case class PlayerInRecordInfo(
                                 playerId:String,
                                 nickname:String,
                                 existTime:List[ExistTimeInfo]
                               )

  case class PlayerList(
                         totalFrame:Int,
                         playerList:List[PlayerInRecordInfo]
                        )

  case class userInRecordRsp(
                              data:PlayerList,
                              errCode: Int = 0,
                              msg: String = "ok"
                            ) extends CommonRsp

  case class GetRecordFrameReq(
                                recordId:Long,
                                playerId:String  //观看者
                              )

  case class GetRecordFrameRsp(
                                data:RecordFrameInfo,
                                errCode: Int = 0,
                                msg: String = "ok"
                              ) extends CommonRsp

  case class RecordFrameInfo(
                              frame:Int,
                              frameNum:Long,
                              frameDuration:Long
                            )

  case class LoginData(
                      wsUrl:String,
                      scanUrl:String
                    )

  final case class LoginReq(
                           email:String,
                           password:String,
                           )

  final case class ESheepUserInfoRsp(
                                      userName: String ,
                                      userId: Long ,
                                      headImg: String ,
                                      token: String ,
                                      gender: Int ,
                                      errCode: Int = 0,
                                      msg: String = "ok"
                                    )

  case class LoginResponse(
                            data:LoginData,
                            errCode:Int = 0,
                            msg:String = "ok"
                          )
  case class WsData(
                     userId:Long,
                     nickname:String,
                     token:String,
                     tokenExpireTime:Long,
                     headImg:Option[String],
                     gender:Option[Int]
                   )
  case class WsResponse(
                       data:WsData,
                       errCode:Int = 0,
                       msg:String = "ok"
                       )
  case class Ws4AgentResponse(
                               Ws4AgentRsp:WsResponse
                             )

  case class LinkGameData(
                           gameId:Long,
                           playerId:String
                         )

  case class GameServerInfo(
                             ip:String,
                             port:Int,
                             domain:String
                           )

  case class LinkResElement(
                             accessCode:String,
                             gsPrimaryInfo:GameServerInfo
                           )

  case class LinkGameRes(
                          data:LinkResElement,
                          errCode:Int = 0,
                          msg:String = "ok"
                        )

  case class GaRefreshTokenReq(
                                playerId: String
                              )

  case class TokenInfo(
                        token: String,
                        expireTime: Long
                      )

  case class TokenAndAcessCode(
                                token: String,
                                expireTime: Long,
                                accessCode: String
                              )

  case class GaRefreshTokenRsp(
                                data: TokenInfo,
                                errCode: Int = 0,
                                msg: String = "ok"
                              )extends CommonRsp

  case class BotTokenInfo(
                            botName:String,
                            token: String,
                            expireTime: Long
                          )


  case class BotTokenRsp(
                        data: BotTokenInfo,
                        errCode: Int = 0,
                        msg: String = "ok"
                      )extends CommonRsp


  case class BotKey2Token(
                         botId: Long,
                         botKey: String
                         )


  case class UserRegisterInfo(
                               name:String,
                               password:String,
                               headImg:String,
                             )
  case class UserLoginInfo(
                            name:String,
                            password:String,
                          )
  case class UserLoginRsqJson(
                               id:String,
                               name:String,
                               headImg:String,
                               score: Int
                             )

  case class UserLoginRsq(
                           data:Option[UserLoginRsqJson],
                           errCode: Int = 0,
                           msg: String = "ok"
                         )

  case class UserMaxScore(
                           id:String,
                           score:Int
                         )
  case class CheckNameRsp(
                           roomId:Long,
                           errCode:Int=0,
                           msg:String="ok"
                         )
  case class JoinGameWithRoomId(
                                 playerId:String,
                                 playerName:String,
                                 roomId:Long,
                                 accessCode:String
                               )
  case class JoinGame(
                       playerId: String,
                       playerName:String,
                       accessCode:String
                     )



}
