package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsSourceProtocol.WsMsgSource

/**
  * User: sky
  * Date: 2018/9/5
  * Time: 16:18
  * 在前后端解析
  */
object WsMsgProtocol {

  sealed trait WsMsgServer

  sealed trait WsMsgFront extends WsMsgSource

  trait GameAction{
    val serialNum:Int
    val frame:Long
  }

  /**
    * 后端解析*/
  case class MousePosition(id: Long,clientX:Double,clientY:Double,override val frame:Long,override val serialNum:Int) extends GameAction with WsMsgFront with WsMsgServer

  case class KeyCode(id: Long,keyCode: Int,override val frame:Long,override val serialNum:Int)extends GameAction with WsMsgServer with WsMsgFront

  case object UserLeft extends WsMsgServer

  case object ErrorWsMsgServer extends WsMsgServer

  case class Ping(timestamp: Long) extends WsMsgServer

  /**
    * 前端解析*/
  case class GridDataSync(
                           frameCount: Long,
                           playerDetails: List[Player],
                           foodDetails: List[Food],
                           massDetails: List[Mass],
                           virusDetails: List[Virus],
                           scale:Double, //缩放比例
                           var newFoodDetails:List[Food]=Nil,
                           var eatenFoodDetails:List[Food]=Nil
                         ) extends WsMsgFront

  case class FeedApples(
                         aLs: List[Food]
                       ) extends WsMsgFront

  case class Id(id: Long) extends WsMsgFront

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgFront

  case class SnakeRestart(id:Long) extends WsMsgFront

  case class UserDeadMessage(id:Long,killerId:Long,killerName:String,killNum:Int,score:Int,lifeTime:Long) extends WsMsgFront

  case class KillMessage(killerId:Long,deadPlayer:Player) extends WsMsgFront

  case class GameOverMessage(id:Long,killNum:Int,score:Int,lifeTime:Long) extends WsMsgFront

  case class Pong(timestamp: Long)extends WsMsgFront

  val frameRate = 150

  val advanceFrame = 0 //客户端提前的帧数

  val delayFrame = 1 //延时帧数，抵消网络延时

  val maxDelayFrame = 3
}
