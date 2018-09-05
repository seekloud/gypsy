package com.neo.sk.gypsy.shared.ptcl

import java.sql.Timestamp

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:40 PM
  */
object WsFrontProtocol {

  sealed trait GameMessage extends WsServerSourceProtocol.WsMsgSource

  trait GameAction{
    val serialNum:Int
    val frame:Long
  }


  case class GridDataSync(
    frameCount: Long,
    playerDetails: List[Player],
    foodDetails: List[Food],
    massDetails: List[Mass],
    virusDetails: List[Virus],
    scale:Double, //缩放比例
  ) extends GameMessage


  case class FeedApples(
    aLs: List[Food]
  ) extends GameMessage



  case class TextMsg(
    msg: String
  ) extends GameMessage

  case class Id(id: Long) extends GameMessage

  case class NewSnakeJoined(id: Long, name: String) extends GameMessage

  case class SnakeAction(id: Long, keyCode: KeyCode, frame: Long) extends GameMessage

  case class SnakeMouseAction(id: Long, mp:MousePosition, frame: Long) extends GameMessage

  case class PlayerLeft(id: Long, name: String) extends GameMessage

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends GameMessage

  case class SnakeRestart(id:Long) extends GameMessage

  case class MousePosition(clientX:Double,clientY:Double,override val frame:Long,override val serialNum:Int) extends GameAction with GameMessage

  case class KeyCode(keyCode: Int,override val frame:Long,override val serialNum:Int)extends GameAction with GameMessage

  case object UserLeft extends GameMessage

  case object ErrorGameMessage extends GameMessage

  case class UserDeadMessage(id:Long,killerId:Long,killerName:String,killNum:Int,score:Int,lifeTime:Long)extends GameMessage

  case class KillMessage(killerId:Long,deadId:Long)extends GameMessage

  case class Ping(timestamp: Long)extends GameMessage
  case class Pong(timestamp: Long)extends GameMessage

  val frameRate = 150

  val advanceFrame = 2 //客户端提前的帧数

  val maxDelayFrame = 3
}
