package com.neo.sk.gypsy.shared.ptcl

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:40 PM
  */
object Protocol {

  sealed trait GameMessage

  case class GridDataSync(
    frameCount: Long,
    playerDetails: List[Player],
    foodDetails: List[Food],
    massDetails: List[Mass],
    virusDetails: List[Virus]
  ) extends GameMessage


  case class FeedApples(
    aLs: List[Food]
  ) extends GameMessage



  case class TextMsg(
    msg: String
  ) extends GameMessage

  case class Id(id: Long) extends GameMessage

  case class NewSnakeJoined(id: Long, name: String) extends GameMessage

  case class SnakeAction(id: Long, keyCode: Int, frame: Long) extends GameMessage

  case class SnakeMouseAction(id: Long, x:Double, y:Double,  frame: Long) extends GameMessage

  case class PlayerLeft(id: Long, name: String) extends GameMessage

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends GameMessage

  case class NetDelayTest(createTime: Long) extends GameMessage

  case class MousePosition(clientX:Double,clientY:Double)

  val frameRate = 100

}
