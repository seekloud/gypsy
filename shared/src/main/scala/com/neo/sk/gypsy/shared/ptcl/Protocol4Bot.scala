package com.neo.sk.gypsy.shared.ptcl

/**
  * @author zhaoyin
  * 2018/12/18  2:21 PM
  */
object Protocol4Bot {
  case class JoinRoomRsp(
                          roomId: Long,
                          errCode:Int =0,
                          msg:String="ok"
                        )

}
