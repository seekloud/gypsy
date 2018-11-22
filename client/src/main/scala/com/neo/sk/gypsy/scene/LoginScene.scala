package com.neo.sk.gypsy.scene

import java.io.ByteArrayInputStream

import com.neo.sk.gypsy.ClientBoot
import javafx.scene.control.Button
import javafx.scene.canvas.Canvas
import javafx.scene.paint.{Color, Paint}
import javafx.scene.{Group, Scene}
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.FontPosture
/**
  * @author zhaoyin
  * 2018/10/29  5:21 PM
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
  val button = new Button("登录")
  val canvas = new Canvas(width,height)
  val ctx = canvas.getGraphicsContext2D
  var loginSceneListener: LoginSceneListener=_

  button.setLayoutX(230)
  button.setLayoutY(240)
  ctx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,28))
  ctx.setFill(Color.BLACK)
  group.getChildren.add(canvas)
  group.getChildren.add(button)
  val scene = new Scene(group)

  button.setOnAction(_ => loginSceneListener.onButtonConnect())

  def setLoginSceneListener(listener: LoginSceneListener): Unit ={
    loginSceneListener = listener
  }

  def drawScanUrl(imageStream:ByteArrayInputStream)={
    ClientBoot.addToPlatform{
      group.getChildren.remove(button)
      val image = new Image(imageStream)
      ctx.drawImage(image,100,80)
      ctx.fillText("请扫码登录",180,400)
    }
  }

}
