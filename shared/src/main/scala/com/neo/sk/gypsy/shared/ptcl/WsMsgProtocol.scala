package com.neo.sk.gypsy.shared.ptcl

/**
  * User: sky
  * Date: 2018/9/5
  * Time: 16:18
  * 在前后端解析
  */
object WsMsgProtocol {

  trait WsMsgSource

  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Throwable) extends WsMsgSource




  val frameRate = 150  //ms

  val advanceFrame = 0 //客户端提前的帧数

  val delayFrame = 1 //延时帧数，抵消网络延时

  val maxDelayFrame = 3

//  /**
//    * Websocket client
//    * */
//  sealed trait WsSendMsg
//  case object WsSendComplete extends WsSendMsg
//  case class WsSendFailed(ex:Throwable) extends WsSendMsg
//  sealed trait UserAction extends WsSendMsg
}
