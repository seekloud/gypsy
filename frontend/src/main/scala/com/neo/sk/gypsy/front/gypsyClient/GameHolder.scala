package com.neo.sk.gypsy.front.gypsyClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.front.scalajs.LoginPage
import com.neo.sk.gypsy.shared.ptcl._
import scalatags.JsDom.all._

import scala.scalajs.js.JSApp
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
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
  * Time: 13:26
  */
class GameHolder {
  private[this] val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas2 = dom.document.getElementById("MiddleView").asInstanceOf[Canvas]
  private[this] val ctx2 = canvas2.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas3 = dom.document.getElementById("TopView").asInstanceOf[Canvas]
  private[this] val ctx3 = canvas3.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val img = dom.document.getElementById("virus").asInstanceOf[HTMLElement]

  var isDead=true

  private[this] val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.Space,
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down,
    KeyCode.Escape
  )

  private[this] val webSocketClient =WebSocketClient()

  def addActionListenEvent={
    canvas3.focus()
    //在画布上监听键盘事件
    canvas3.onkeydown = {
      (e: dom.KeyboardEvent) => {
        println(s"keydown: ${e.keyCode}")
        if (watchKeys.contains(e.keyCode)) {
          println(s"key down: [${e.keyCode}]")
          if (e.keyCode == KeyCode.Escape && !isDead) {
            LoginPage.homePage()
            webSocketClient.closeWs
            isDead = true
          } else if (e.keyCode == KeyCode.Space) {
            println(s"down+${e.keyCode.toString}")
          } else {
            println(s"down+${e.keyCode.toString}")
            val keyCode=WsMsgProtocol.KeyCode(myId,e.keyCode,grid.frameCount+advanceFrame,getActionSerialNum)
            grid.addActionWithFrame(myId, keyCode, grid.frameCount+advanceFrame)
            addUncheckActionWithFrame(myId,keyCode,grid.frameCount+advanceFrame)
            webSocketClient.sendMsg(keyCode)
          }
          e.preventDefault()
        }
      }
    }
    //在画布上监听鼠标事件
    canvas3.onmousemove = { (e: dom.MouseEvent) => {
      val mp=MousePosition(myId,e.pageX - window.x / 2, e.pageY - 48 - window.y.toDouble / 2,grid.frameCount+advanceFrame,getActionSerialNum)
      grid.addMouseActionWithFrame(myId,mp,mp.frame)
      addUncheckActionWithFrame(myId,mp,mp.frame)
      //gameStream.send(MousePosition(e.pageX-windWidth/2, e.pageY-48-window.y.toDouble/2).asJson.noSpaces)
      webSocketClient.sendMsg(mp)
      //println(s"pageX${e.pageX},pageY${e.pageY},X${e.pageX - windWidth / 2},Y${e.pageY - 48 - window.y.toDouble / 2}")

    }
  }
}
