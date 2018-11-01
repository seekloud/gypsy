package com.neo.sk.gypsy.ptcl

/**
  * User: sky
  * Date: 2018/7/9
  * Time: 11:41
  */
object UserProtocol {
  case class BaseUserInfo(
                           userType: String,
                           userId: String,
                           name: String,
                           headImg:String
                         )
}
