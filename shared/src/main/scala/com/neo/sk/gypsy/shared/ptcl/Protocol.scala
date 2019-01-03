package com.neo.sk.gypsy.shared.ptcl

import Game._

import scala.collection.mutable
object Protocol {

  /**
    * 后端发送的数据
    * */
  trait WsMsgSource

  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Throwable) extends WsMsgSource

  sealed trait GameMessage extends WsMsgSource

  trait GameBeginning extends WsMsgSource

  case class ErrorWsMsgFront(msg:String) extends GameMessage
  final case object RebuildWebSocket extends GameMessage
  case class DecodeEvent(data:SyncGameAllState) extends GameMessage
  case class DecodeEvents(data:EventData) extends GameMessage
  case class DecodeEventError(data:DecodeError) extends GameMessage
  case class ReplayFrameData(ws:Array[Byte]) extends GameMessage

  case class GridDataSync(
                           frameCount: Long,
                           playerDetails: List[Player],
                           massDetails: List[Mass],
                           virusDetails: Map[Long,Virus],
                           scale: Double, //缩放比例
                           playersPosition: List[PlayerPosition],
                           var newFoodDetails:List[Food]=Nil, //增量数据传输
                           var eatenFoodDetails:List[Food]=Nil
                         ) extends GameMessage

  case class FeedApples(
                         aLs: List[Food]
                       ) extends GameMessage

  case class Id(id: String) extends GameMessage

//  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends GameMessage
  case class Ranks(currentRank: List[RankInfo]) extends GameMessage

  case class MyRank(rank:RankInfo) extends GameMessage

  case class PlayerRestart(id:String) extends GameMessage

  case class UserDeadMessage(id:String,killerId:String,killerName:String,killNum:Int,score:Int,lifeTime:Long) extends GameMessage

  case class Wrap(ws:Array[Byte],isKillMsg:Boolean = false) extends WsMsgSource

  case class KillMessage(killerId:String,deadPlayer:Player) extends GameMessage

  case class GameOverMessage(id:String,killNum:Int,score:Int,lifeTime:Long) extends GameMessage

  case class MatchRoomError() extends GameMessage

  case class UserMerge(id:String,player: Player)extends GameMessage

  case class UserCrash(crashMap:Map[String,List[Cell]]) extends GameMessage

  case class Pong(timestamp: Long)extends GameMessage

  case class AddVirus(virus:Map[Long,Virus]) extends GameMessage

  //只有用户离开房间时候发送
  case class PlayerLeft(id: String, name: String) extends GameMessage

  case class PlayerSpilt(player: Map[String,Player]) extends GameMessage

  case class PlayerJoin(id:String,player:Player) extends GameMessage

  case class JoinRoomSuccess(playerId:String, roomId:Long) extends GameMessage

  case class JoinRoomFailure(playerId:String,roomId:Long,errorCode:Int,msg:String) extends GameMessage

  /**
    * 前端发送的数据
    * */
  sealed trait WsSendMsg{
        val serialNum:Int = -1 //类似每一帧的动作顺序
        val frame:Long = -1l
      }

  case object WsSendComplete extends WsSendMsg

  case class WsSendFailed(ex:Throwable) extends WsSendMsg

  sealed trait UserAction extends WsSendMsg

//  case class TextInfo(msg:String) extends UserAction

  case class MousePosition(id: String,clientX:Double,clientY:Double, override val frame:Long, override val serialNum:Int) extends UserAction with GameMessage

  case class KeyCode(id: String,keyCode: Int, override val frame:Long,override val serialNum:Int) extends UserAction with GameMessage

  case object PressSpace extends UserAction

  case class ReLiveMsg(id: String,override val frame:Long) extends UserAction with GameMessage

  case class WatchChange(id:String, watchId: String) extends UserAction

  case class Ping(timestamp: Long) extends UserAction

//  case class ReLiveAck(id:String) extends UserAction

  case object CreateRoom extends UserAction

  case class JoinRoom(roomId:Option[Long]) extends UserAction


  /**
    * 事件记录
    * */
  sealed trait GameEvent {
    val frame:Long = -1l
    val serialNum:Int = -1
  }

  case class DecodeError() extends GameEvent
  case class InitReplayError(msg:String) extends GameEvent
  case class ReplayFinish() extends GameEvent
  case class EventData(list:List[GameEvent]) extends GameEvent
  case class SyncGameAllState(gState:GypsyGameSnapInfo) extends GameEvent

  case class UserJoinRoom(roomId:Long,playState:Player, override val frame:Long) extends GameEvent
  case class UserLeftRoom(userId:String,userName:String,ballId:Long,roomId:Long, override val frame:Long) extends GameEvent
  case class UserWsJoin(roomId:Long,userId:String,userName:String,ballId:Long, override val frame:Long, override val serialNum:Int) extends GameEvent //每次webSocket加入时候记，不记Play的具体状态
  case class MouseMove(userId:String,direct:(Double,Double), override val frame:Long, override val serialNum:Int) extends GameEvent
  case class KeyPress(userId:String,keyCode: Int, override val frame:Long, override val serialNum:Int) extends GameEvent
  case class GenerateApples(apples:Map[Point, Int], override val frame:Long) extends GameEvent
  case class GenerateVirus(virus: Map[Long,Virus], override val frame:Long) extends GameEvent with WsMsgSource
  case class KillMsg(killerId:String,deadPlayer:Player,score:Int,lifeTime:Long, override val frame: Long) extends GameEvent
  case class CurrentRanks(currentRank: List[RankInfo]) extends GameEvent
  case class PongEvent(timestamp: Long)extends GameEvent


  case class PlayerInfoChange(player: Map[String,Player], override val frame:Long) extends GameEvent
  //  缩放放到
  case class ShowScale( override val frame:Long,scale:Double) extends GameEvent

  sealed trait GameSnapshot

  case class GypsyGameSnapshot(
                                      state:GypsyGameSnapInfo

                                    ) extends GameSnapshot

  case class GypsyGameSnapInfo(
                                      frameCount: Long,
                                      playerDetails: List[Player],
                                      foodDetails: List[Food],
                                      massDetails: List[Mass],
                                      virusDetails: Map[Long,Virus],
                                      currentRank:List[RankInfo]
                                    )

  final case class GameInformation(
                                    gameStartTime:Long
//                                    gypsyConfig: GypsyGameConfigImpl
                                  )

  //配置数据以后补上
  final case class GypsyGameConfigImpl(
                                  x:Int = 100,
                                  y:Int = 200
                                  //     boundary: ,
                                  //     window:
                                )



}
