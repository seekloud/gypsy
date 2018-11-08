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


  object GameState{
    val firstCome = 1
    val play = 2
    val stop = 3
    val loadingPlay = 4
    val relive = 5
  }

}
