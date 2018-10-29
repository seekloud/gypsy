package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync


object GypsyGameEvent {

  sealed trait WsMsgFront
  sealed trait WsMsgServer

  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent
  trait InfoChange extends GameEvent
  trait UserActionEvent extends UserEvent{
    val userId:Long
    val serialNum:Int
  }


  final case class EventData(list:List[WsMsgServer]) extends WsMsgServer
  final case class DecodeError() extends WsMsgServer


  final case class UserJoinRoom(roomId:Long,playState:Player,override val frame:Long) extends UserEvent with WsMsgServer
  final case class UserLeftRoom(userId:Long,userName:String,roomId:Long,override val frame:Long) extends UserEvent with WsMsgServer

  final case class GenerateApples(apples:Map[Point, Int],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class RemoveApples(apples:Map[Point, Int],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class GenerateVirus(virus: List[Virus],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class RemoveVirus(virus: List[Virus],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class GenerateMass(massList:List[Mass],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class RemoveMass(massList:List[Mass],override val frame:Long) extends EnvironmentEvent with WsMsgServer




  //  Cell变化直接做到用户信息改变那了
//  final case class GenerateCells(override val frame:Long) extends EnvironmentEvent with WsMsgServer

  //是否做消失事件？？？

  final case class ReduceApples(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class ReduceVirus(apples:List[Food],override val frame:Long) extends EnvironmentEvent with WsMsgServer

  final case class PlayerInfoChange(player: Map[Long,Player],override val frame:Long) extends InfoChange with WsMsgServer

  //  缩放放到
  final case class ShowScale(override val frame:Long,scale:Double) extends EnvironmentEvent with WsMsgServer



  final case class MouseMove(userId:Long,direct:(Double,Double),override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgServer
  final case class KeyPress(userId:Long,keyCode: Int,override val frame:Long,override val serialNum:Int) extends UserActionEvent with WsMsgServer


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

}
