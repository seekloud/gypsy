package com.neo.sk.gypsy.shared.ptcl

//import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync


object GypsyGameEvent {

  /**
    * WsMsgServer、WsMsgFront、WsMsgSource
    * */


  sealed trait WsMsgSource
  case class CompleteMsgServer() extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  trait WsMsgFront extends WsMsgSource
  trait WsMsgServer extends WsMsgSource


  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent
  trait InfoChange extends GameEvent
  trait UserActionEvent extends UserEvent{
    val userId:String
    val serialNum:Int
  }

  /**
    * replay-frame-msg
    */
  final case class ReplayFrameData(ws:Array[Byte]) extends WsMsgSource
  final case class InitReplayError(msg:String) extends WsMsgSource
  final case class ReplayFinish() extends WsMsgSource

  /**
    * replay in front
    * */
  final case class DecodeError() extends WsMsgSource
  final case class EventData(list:List[WsMsgSource]) extends WsMsgSource
  final case class SyncGameAllState(gState:GypsyGameSnapInfo) extends WsMsgSource
  final case class UserJoinRoom(roomId:Long,playState:Player,override val frame:Long) extends UserEvent with WsMsgSource
  final case class UserLeftRoom(userId:String,userName:String,roomId:Long,override val frame:Long) extends UserEvent with WsMsgSource
  final case class MouseMove(userId:String,direct:(Double,Double),override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgSource
  final case class KeyPress(userId:String,keyCode: Int,override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgSource

  final case class GenerateApples(apples:Map[Point, Int],override val frame:Long) extends EnvironmentEvent with WsMsgSource
  final case class RemoveApples(apples:Map[Point, Int],override val frame:Long) extends EnvironmentEvent with WsMsgSource
//  final case class GenerateVirus(virus: List[Virus],override val frame:Long) extends EnvironmentEvent with WsMsgFront
  // TODO mass改变是否要算进生成病毒
  final case class GenerateVirus(virus: Map[Long,Virus],override val frame:Long) extends EnvironmentEvent with WsMsgSource
  final case class RemoveVirus(virus: Map[Long,Virus],override val frame:Long) extends EnvironmentEvent with WsMsgSource
  final case class GenerateMass(massList:List[Mass],override val frame:Long) extends EnvironmentEvent with WsMsgSource
  final case class RemoveMass(massList:List[Mass],override val frame:Long) extends EnvironmentEvent with WsMsgSource




  //  Cell变化直接做到用户信息改变那了
//  final case class GenerateCells(override val frame:Long) extends EnvironmentEvent with WsMsgFront

  //是否做消失事件？？？

  final case class ReduceApples(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgSource
  final case class ReduceVirus(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgSource

  final case class PlayerInfoChange(player: Map[String,Player],override val frame:Long) extends InfoChange with WsMsgSource

  //  缩放放到
  final case class ShowScale(override val frame:Long,scale:Double) extends EnvironmentEvent with WsMsgSource

  sealed trait GameSnapshot
  final case class GypsyGameSnapshot(
                                    state:GypsyGameSnapInfo
                                  ) extends GameSnapshot with WsMsgSource

  final case class GypsyGameSnapInfo(
    frameCount: Long,
    playerDetails: List[Player],
    foodDetails: List[Food],
    massDetails: List[Mass],
//    virusDetails: List[Virus],
    virusDetails: Map[Long,Virus]
  )

  final case class GameInformation(
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


  /*
   *WsMsgProtocol中的
   *
   */



  trait GameAction{
    val serialNum:Int //类似每一帧的动作顺序
    val frame:Long
  }

  /**
    * 后端解析
    * */
  case class MousePosition(id: String,clientX:Double,clientY:Double,override val frame:Long,override val serialNum:Int) extends GameAction with WsMsgSource

  case class KeyCode(id: String,keyCode: Int,override val frame:Long,override val serialNum:Int)extends GameAction with WsMsgSource

  case class WatchChange(id:String, watchId: String) extends WsMsgSource

  case object UserLeft extends WsMsgSource

  case object ErrorWsMsgServer extends WsMsgSource

  case class Ping(timestamp: Long) extends WsMsgSource

  /**
    * 前端解析
    * */
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
  ) extends WsMsgSource

  case class FeedApples(
    aLs: List[Food]
    //                         aLs: Map[Point, Int]
  ) extends WsMsgSource

  case class Id(id: String) extends WsMsgSource

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgSource

  case class SnakeRestart(id:String) extends WsMsgSource

  case class UserDeadMessage(id:String,killerId:String,killerName:String,killNum:Int,score:Int,lifeTime:Long) extends WsMsgSource

  case class KillMessage(killerId:String,deadPlayer:Player) extends WsMsgSource

  case class GameOverMessage(id:String,killNum:Int,score:Int,lifeTime:Long) extends WsMsgSource

  case class MatchRoomError() extends WsMsgSource

  case class UserMerge(id:String,player: Player)extends WsMsgSource

  case class Pong(timestamp: Long)extends WsMsgSource


  /**
    * Websocket client
    * */
  sealed trait WsSendMsg
  case object WsSendComplete extends WsSendMsg
  case class WsSendFailed(ex:Throwable) extends WsSendMsg
  sealed trait UserAction extends WsSendMsg


}
