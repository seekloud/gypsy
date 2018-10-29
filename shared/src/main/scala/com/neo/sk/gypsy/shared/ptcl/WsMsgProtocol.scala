package com.neo.sk.gypsy.shared.ptcl

/**
  * User: sky
  * Date: 2018/9/5
  * Time: 16:18
  * 在前后端解析
  */
object WsMsgProtocol {

  /**
    * WsMsgServer、WsMsgFront、WsMsgSource
    * */

  trait GameAction{
    val serialNum:Int //类似每一帧的动作顺序
    val frame:Long
  }

  /**
    * 后端解析
    * */
  case class MousePosition(id: String,clientX:Double,clientY:Double,override val frame:Long,override val serialNum:Int) extends GameAction with WsMsgFront with WsMsgServer

  case class KeyCode(id: String,keyCode: Int,override val frame:Long,override val serialNum:Int)extends GameAction with WsMsgFront with WsMsgServer

  case class WatchChange(id:String, watchId: String) extends WsMsgServer

  case object UserLeft extends WsMsgServer

  case object ErrorWsMsgServer extends WsMsgServer

  case class Ping(timestamp: Long) extends WsMsgServer

  /**
    * 前端解析
    * */
  case class GridDataSync(
                           frameCount: Long,
                           playerDetails: List[Player],
                           foodDetails: List[Food],
                           massDetails: List[Mass],
                           virusDetails: List[Virus],
                           scale:Double, //缩放比例
                           var newFoodDetails:List[Food]=Nil, //增量数据传输
                           var eatenFoodDetails:List[Food]=Nil
                         ) extends WsMsgFront

  case class FeedApples(
                         aLs: List[Food]
                       ) extends WsMsgFront

  case class Id(id: String) extends WsMsgFront

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgFront

  case class SnakeRestart(id:String) extends WsMsgFront

  case class UserDeadMessage(id:String,killerId:String,killerName:String,killNum:Int,score:Int,lifeTime:Long) extends WsMsgFront

  case class KillMessage(killerId:String,deadPlayer:Player) extends WsMsgFront

  case class GameOverMessage(id:String,killNum:Int,score:Int,lifeTime:Long) extends WsMsgFront

  case class MatchRoomError() extends WsMsgFront

  case class UserMerge(id:String,player: Player)extends WsMsgFront

  case class Pong(timestamp: Long)extends WsMsgFront

  val frameRate = 150

  val advanceFrame = 0 //客户端提前的帧数

  val delayFrame = 1 //延时帧数，抵消网络延时

  val maxDelayFrame = 3

  /**
    * Websocket client
    * */
  sealed trait WsSendMsg
  case object WsSendComplete extends WsSendMsg
  case class WsSendFailed(ex:Throwable) extends WsSendMsg
  sealed trait UserAction extends WsSendMsg
}
