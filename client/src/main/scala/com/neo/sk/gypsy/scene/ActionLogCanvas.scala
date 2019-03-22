package com.neo.sk.gypsy.scene

import com.neo.sk.gypsy.shared.ptcl.Game.Point
import com.neo.sk.gypsy.shared.ptcl.Protocol.{KC, MP, UserAction}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment

/**
  * User: XuSiRan
  * Date: 2019/3/17
  * Time: 13:15
  */
class ActionLogCanvas(
  canvas: Canvas,
  ctx:GraphicsContext,
  size:Point
){

  var actionLogList: List[String] = List.empty[String]

  def addFrameCountActionLog(action: Option[UserAction]): Unit ={
    action match {
      case Some(MP(_, cX, cY, f, _)) =>
        actionLogList = s"frame：$f, mouseMove: Point($cX, $cY)" :: actionLogList
      case Some(KC(_, kC, f, _)) =>
        actionLogList = s"frame：$f, keyPress: code($kC) " :: actionLogList
      case _ =>
    }
    if(actionLogList.length > 10) actionLogList = actionLogList.take(10)
  }

  def drawLog(): Unit ={
    var textHeight = 20
    ctx.save()
    ctx.clearRect(0, 0, size.x, size.y)
    ctx.setFill(Color.BLACK)
    ctx.setTextAlign(TextAlignment.LEFT)
    actionLogList.reverse.foreach{ log =>
      ctx.fillText(log, 15, textHeight)
      textHeight += 18
    }
    ctx.restore()
  }
}
