package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._


object Protocol {

  /**
    * 后端解析的数据
    * */

  sealed trait GameMessage extends WsMsgSource

  case class GridDataSync(
                           frameCount: Long,
                           playerDetails: List[Player],
                           //                           foodDetails: List[Food],
                           massDetails: List[Mass],
                           //                           virusDetails: List[Virus],
                           virusDetails: Map[Long,Virus],
                           scale:Double, //缩放比例
                           var newFoodDetails:List[Food]=Nil, //增量数据传输
                           var eatenFoodDetails:List[Food]=Nil
                         ) extends GameMessage

  case class FeedApples(
                         aLs: List[Food]
                         //                         aLs: Map[Point, Int]
                       ) extends GameMessage

  case class Id(id: String) extends GameMessage

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends GameMessage

  case class SnakeRestart(id:String) extends GameMessage

  case class UserDeadMessage(id:String,killerId:String,killerName:String,killNum:Int,score:Int,lifeTime:Long) extends GameMessage

  case class Wrap(ws:Array[Byte],isKillMsg:Boolean = false) extends WsMsgSource

  case class KillMessage(killerId:String,deadPlayer:Player) extends GameMessage

  case class GameOverMessage(id:String,killNum:Int,score:Int,lifeTime:Long) extends GameMessage

  case class MatchRoomError() extends GameMessage

  case class UserMerge(id:String,player: Player)extends GameMessage

  case class Pong(timestamp: Long)extends GameMessage

  /**
    * 前端发送的数据
    * */
  sealed trait WsSendMsg{
        val serialNum:Int = -1 //类似每一帧的动作顺序
        val frame:Long = -1l
      }

  case object WsSendComplete extends WsSendMsg

  case class WsSendFailed(ex:Throwable) extends WsSendMsg

//  sealed trait GameAction{
//    val serialNum:Int //类似每一帧的动作顺序
//    val frame:Long
//  }

  sealed trait UserAction extends WsSendMsg

  case class TextInfo(msg:String) extends UserAction

  case class MousePosition(id: String,clientX:Double,clientY:Double, override val frame:Long, override val serialNum:Int) extends UserAction with GameMessage

  case class KeyCode(id: String,keyCode: Int, override val frame:Long,override val serialNum:Int) extends UserAction with GameMessage

  case class WatchChange(id:String, watchId: String) extends UserAction

  case class UserLeft() extends UserAction

//  case object ErrorWsMsgServer extends UserAction

  case class Ping(timestamp: Long) extends UserAction


  /**
    * event
    * */
  sealed trait GameEvent {
    val frame:Long = -1l
    val serialNum:Int = -1
  }

//  trait UserEvent extends GameEvent
//  trait EnvironmentEvent extends GameEvent
//  trait InfoChange extends GameEvent
//  trait UserActionEvent extends UserEvent{
//    val userId:String
//    val serialNum:Int
//  }
  /**异地登录消息
    * WebSocket连接重新建立*/
  final case object RebuildWebSocket extends GameMessage

  /**
    * replay-frame-msg
    */
   case class ReplayFrameData(ws:Array[Byte]) extends GameMessage
   case class InitReplayError(msg:String) extends GameMessage
   case class ReplayFinish() extends GameMessage

  /**
    * replay in front
    * */
   case class DecodeError() extends GameEvent
   case class EventData(list:List[GameEvent]) extends GameEvent
   case class SyncGameAllState(gState:GypsyGameSnapInfo) extends GameEvent

   case class UserJoinRoom(roomId:Long,playState:Player, override val frame:Long) extends GameEvent
   case class UserLeftRoom(userId:String,userName:String,roomId:Long, override val frame:Long) extends GameEvent
   case class MouseMove(userId:String,direct:(Double,Double), override val frame:Long, override val serialNum:Int) extends GameEvent
   case class KeyPress(userId:String,keyCode: Int, override val frame:Long, override val serialNum:Int) extends GameEvent

   case class GenerateApples(apples:Map[Point, Int], override val frame:Long) extends GameEvent
   case class RemoveApples(apples:Map[Point, Int], override val frame:Long) extends GameEvent
   case class GenerateVirus(virus: Map[Long,Virus], override val frame:Long) extends GameEvent
   case class RemoveVirus(virus: Map[Long,Virus], override val frame:Long) extends GameEvent
   case class GenerateMass(massList:List[Mass], override val frame:Long) extends GameEvent
   case class RemoveMass(massList:List[Mass], override val frame:Long) extends GameEvent
   case class ReduceApples(apples:List[Food], override val frame:Long) extends GameEvent
  case class ReduceVirus(apples:List[Food], override val frame:Long) extends GameEvent
  case class PlayerInfoChange(player: Map[String,Player], override val frame:Long) extends GameEvent
  //  缩放放到
  case class ShowScale( override val frame:Long,scale:Double) extends GameEvent
  case class GypsyGameSnapshot(
                                      state:GypsyGameSnapInfo
                                    ) extends GameEvent

  case class GypsyGameSnapInfo(
                                      frameCount: Long,
                                      playerDetails: List[Player],
                                      foodDetails: List[Food],
                                      massDetails: List[Mass],
                                      //    virusDetails: List[Virus],
                                      virusDetails: Map[Long,Virus]
                                    )

  case class GameInformation(
                                    gameStartTime:Long,
                                    gypsyConfig: GypsyGameConfigImpl
                                  )

  //配置数据以后补上
  case class GypsyGameConfigImpl(
                                  x:Int = 100,
                                  y:Int = 200
                                  //     boundary: ,
                                  //     window:
                                )

  // TODO mass改变是否要算进生成病毒

  //  final case class GenerateVirus(virus: List[Virus],override val frame:Long) extends EnvironmentEvent with WsMsgFront

  //  Cell变化直接做到用户信息改变那了
//  final case class GenerateCells(override val frame:Long) extends EnvironmentEvent with WsMsgFront

  //是否做消失事件？？？


//  sealed trait GameSnapshot
//  final case class GypsyGameSnapshot(
//                                    state:GypsyGameSnapInfo
//                                  ) extends GameSnapshot with GameEvent



}
