package com.neo.sk.gypsy.scene

import javafx.scene.control.Button
import javafx.scene.canvas.Canvas
import javafx.scene.paint.{Color, Paint}
import javafx.scene.{Group, Scene}

/**
  * @author zhaoyin
  * @date 2018/10/29  5:21 PM
  */

object LoginScene {
  trait LoginSceneListener {
    def onButtonConnect()
  }
}
class LoginScene {
  import LoginScene._

  val width = 500
  val height = 500
  val group = new Group
  val button = new Button("start game")
  val canvas = new Canvas(width,height)
  val ctx = canvas.getGraphicsContext2D
  var loginSceneListener: LoginSceneListener=_

  button.setLayoutX(230)
  button.setLayoutY(240)
  ctx.setFill(Color.rgb(255,255,255))
  ctx.fillRect(0,0,width,height)
  group.getChildren.add(canvas)
  group.getChildren.add(button)
  val scene = new Scene(group)

  button.setOnAction(_ => loginSceneListener.onButtonConnect())

  def setLoginSceneListener(listener: LoginSceneListener): Unit ={
    loginSceneListener = listener
  }
}
