package com.neo.sk.gypsy.ptcl

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.core.{GamePlayer, UserActor, UserManager}
import com.neo.sk.gypsy.shared.ptcl.CommonRsp

/**
  * User: sky
  * Date: 2018/10/18
  * Time: 14:54
  */
object ReplayProtocol {
  final case class EssfMapKey(
                               roomId:Long,
                               userId: String,
                               name:String,
                               ballId:Long
                             )
  final case class EssfMapJoinLeftInfo(
                                        joinF: Long,
                                        leftF: Long
                                      )
  final case class EssfMapInfo(m:List[(EssfMapKey,EssfMapJoinLeftInfo)])

  /**
    * GetUserInRecordMsg: 获取录像内用户列表
    * GetRecordFrameMsg: 获取录像总帧数 + 当前播放帧数
    */
  final case class GetUserInRecordMsg(recordId:Long, watchId:String, replyTo:ActorRef[CommonRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command
  final case class GetRecordFrameMsg(recordId:Long, watchId:String, replyTo:ActorRef[CommonRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command
}
