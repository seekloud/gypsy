package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync


object GypsyGameEvent {

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
  final case class InitReplayError(msg:String) extends WsMsgFront
  final case class ReplayFinish() extends WsMsgFront

  /**
    * replay in front
    * */
  final case class DecodeError() extends WsMsgFront
  final case class EventData(list:List[WsMsgFront]) extends WsMsgFront
  final case class SyncGameAllState(gState:GypsyGameSnapInfo) extends WsMsgFront
  final case class UserJoinRoom(roomId:Long,playState:Player,override val frame:Long) extends UserEvent with WsMsgFront
  final case class UserLeftRoom(userId:String,userName:String,roomId:Long,override val frame:Long) extends UserEvent with WsMsgFront
  final case class MouseMove(userId:String,direct:(Double,Double),override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgFront
  final case class KeyPress(userId:String,keyCode: Int,override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgFront

  final case class GenerateApples(apples:Map[Point, Int],override val frame:Long) extends EnvironmentEvent with WsMsgFront with WsMsgServer
  final case class RemoveApples(apples:Map[Point, Int],override val frame:Long) extends EnvironmentEvent with WsMsgFront with WsMsgServer
//  final case class GenerateVirus(virus: List[Virus],override val frame:Long) extends EnvironmentEvent with WsMsgFront
  // TODO mass改变是否要算进生成病毒
  final case class GenerateVirus(virus: Map[Long,Virus],override val frame:Long) extends EnvironmentEvent with WsMsgFront with WsMsgServer
  final case class RemoveVirus(virus: Map[Long,Virus],override val frame:Long) extends EnvironmentEvent with WsMsgFront with WsMsgServer
  final case class GenerateMass(massList:List[Mass],override val frame:Long) extends EnvironmentEvent with WsMsgFront with WsMsgServer
  final case class RemoveMass(massList:List[Mass],override val frame:Long) extends EnvironmentEvent with WsMsgFront with WsMsgServer




  //  Cell变化直接做到用户信息改变那了
//  final case class GenerateCells(override val frame:Long) extends EnvironmentEvent with WsMsgFront

  //是否做消失事件？？？

  final case class ReduceApples(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgFront
  final case class ReduceVirus(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgFront

  final case class PlayerInfoChange(player: Map[String,Player],override val frame:Long) extends InfoChange with WsMsgFront with WsMsgServer

  //  缩放放到
  final case class ShowScale(override val frame:Long,scale:Double) extends EnvironmentEvent with WsMsgFront

  sealed trait GameSnapshot
  final case class GypsyGameSnapshot(
                                    state:GypsyGameSnapInfo
                                  ) extends GameSnapshot

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

}
