package com.neo.sk.gypsy.front.gypsyClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.shared.ptcl._

import scalatags.JsDom.all._
import scala.scalajs.js.JSApp
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.math._
import com.neo.sk.gypsy.front.utils.byteObject.MiddleBufferInJs
import com.neo.sk.gypsy.shared.util.utils.getZoomRate
import org.scalajs.dom.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.collection.mutable
/**
  * User: sky
  * Date: 2018/9/13
  * Time: 11:23
  *
  */
case class WebSocketClient() {
  private var wsSetup=false

  private var webSocketOpt: Option[WebSocket] =None

  def getWsState=wsSetup

  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(8192)
  def sendMsg(msg:WsMsgServer) = {
    import com.neo.sk.gypsy.front.utils.byteObject.ByteObject._
    webSocketOpt.foreach(_.send(msg.fillMiddleBuffer(sendBuffer).result()))
  }

  def setUp(gameStream:WebSocket)=
    if(wsSetup){
      println()
    }else{

    }

  def closeWs={
    sendMsg(UserLeft)
    webSocketOpt.foreach(_.close())
    webSocketOpt=None
    wsSetup=false
  }



}
