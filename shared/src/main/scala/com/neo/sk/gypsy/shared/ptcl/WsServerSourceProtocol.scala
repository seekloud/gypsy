package com.neo.sk.gypsy.shared.ptcl

/**
  * Created by hongruying on 2018/7/11
  */
object WsServerSourceProtocol {
  trait WsMsgSource

  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

}
