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
                               headImg:String,
                             )

}
