package com.neo.sk.gypsy

package object models {

  case class GypsyUserInfo(
                     userId:String,
                     userName:String,
                     isPlatUser:Boolean = true
                     )
}
