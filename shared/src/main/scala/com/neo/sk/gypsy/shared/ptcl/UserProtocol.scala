package com.neo.sk.gypsy.shared.ptcl

object UserProtocol {

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
                         id:Long,
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
                         id:Long,
                         score:Int
                         )
  case class CheckNameRsp(
                           roomId:String,
                           errCode:Int=0,
                           msg:String="ok"
                         )
  case class JoinGame(
                     playerId:Long,
                     playerName:String,
                     roomId:Long,
                     accessCode:String
                     )

}
