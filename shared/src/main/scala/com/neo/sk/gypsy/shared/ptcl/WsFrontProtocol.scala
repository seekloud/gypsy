package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsSourceProtocol.WsMsgSource

/**
  * User: sky
  * Date: 2018/9/5
  * Time: 16:18
  * 在前端解析
  */
object WsFrontProtocol {

  trait WsMsgFront extends WsMsgSource

  case class GridDataSync(
                           frameCount: Long,
                           playerDetails: List[Player],
                           foodDetails: List[Food],
                           massDetails: List[Mass],
                           virusDetails: List[Virus],
                           scale:Double //缩放比例
                         ) extends WsMsgFront

  case class FeedApples(
                         aLs: List[Food]
                       ) extends WsMsgFront

  case class Id(id: Long) extends WsMsgFront

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgFront

  case class SnakeRestart(id:Long) extends WsMsgFront

  case class UserDeadMessage(id:Long,killerId:Long,killerName:String,killNum:Int,score:Int,lifeTime:Long) extends WsMsgFront

  case class KillMessage(killerId:Long,deadId:Long) extends WsMsgFront

  case class Pong(timestamp: Long)extends WsMsgFront

}
