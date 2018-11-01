package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync


object GypsyGameEvent {

  sealed trait WsMsgSource

  case object CompleteMsgServe extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgFront extends WsMsgSource
  sealed trait WsMsgServer

  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent
  trait UserActionEvent extends UserEvent{
    val userId:String
    val serialNum:Int
  }

  /**
    * replay-frame-msg
    */
  final case class ReplayFrameData(ws:Array[Byte]) extends WsMsgSource
  final case class InitReplayError(msg:String) extends WsMsgServer
  final case class ReplayFinish() extends WsMsgServer

  /**
    * replay in front
    * */
  final case class DecodeError() extends WsMsgServer
  final case class EventData(list:List[WsMsgServer]) extends WsMsgServer
  final case class SyncGameAllState(gState:GypsyGameSnapInfo) extends WsMsgServer
  final case class UserJoinRoom(roomId:Long,playState:Player,override val frame:Long) extends UserEvent with WsMsgServer
  final case class UserLeftRoom(userId:String,userName:String,roomId:Long,override val frame:Long) extends UserEvent with WsMsgServer
  final case class MouseMove(userId:String,direct:(Double,Double),override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgServer
  final case class KeyPress(userId:String,keyCode: Int,override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgServer

  final case class GenerateApples(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class GenerateVirus(virus: List[Virus],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class GenerateMass(override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class GenerateCells(override val frame:Long) extends EnvironmentEvent with WsMsgServer

  //是否做消失事件？？？

  //  缩放放到
  final case class ShowScale(override val frame:Long,scale:Double) extends EnvironmentEvent with WsMsgServer

  sealed trait GameSnapshot
  final case class GypsyGameSnapshot(
                                    state:GypsyGameSnapInfo
                                  ) extends GameSnapshot

  final case class GypsyGameSnapInfo(
    frameCount: Long,
    playerDetails: List[Player],
    foodDetails: List[Food],
    massDetails: List[Mass],
    virusDetails: List[Virus],
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


  case class Id(id: String) extends WsMsgFront with WsMsgServer

}
