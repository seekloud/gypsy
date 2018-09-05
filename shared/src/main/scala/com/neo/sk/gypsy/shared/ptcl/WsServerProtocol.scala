package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsFrontProtocol.WsMsgFront

/**
  * User: sky
  * Date: 2018/9/5
  * Time: 16:18
  * 在后台解析
  */
object WsServerProtocol {

  trait WsMsgServer

  trait GameAction{
    val serialNum:Int
    val frame:Long
  }

  case class MousePosition(id: Long,clientX:Double,clientY:Double,override val frame:Long,override val serialNum:Int) extends GameAction with WsMsgFront with WsMsgServer

  case class KeyCode(id: Long,keyCode: Int,override val frame:Long,override val serialNum:Int)extends GameAction with WsMsgServer with WsMsgFront

  case object UserLeft extends WsMsgServer

  case object ErrorWsMsgServer extends WsMsgServer

  case class Ping(timestamp: Long)extends WsMsgServer

  val frameRate = 150

  val advanceFrame = 2 //客户端提前的帧数

  val maxDelayFrame = 3
}
