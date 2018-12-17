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

    def onButtonPlayerLogin()
    def onButtonBotLogin()
    def onButtonScanLogin() //扫码
    def onButtonEmailLogin() //邮箱
    def onButtonEmailConnect()
    def onButtonBotConnect()
  }
}
class LoginScene {
  import LoginScene._

  val width = 500
  val height = 500
  val group = new Group
  val playerButton = new Button("娱乐模式")
  val BotButton = new Button("训练模式")
  val ScanButton = new Button("扫码登录")
  val EmailButton = new Button("邮箱登录")
  val EmailConnect = new Button("确定")
  val BotConnectButton = new Button("开始训练")
  val canvas = new Canvas(width,height)
  val ctx = canvas.getGraphicsContext2D
  var loginSceneListener: LoginSceneListener=_

  playerButton.setLayoutX(220)
  playerButton.setLayoutY(180)
  BotButton.setLayoutX(220)
  BotButton.setLayoutY(300)
  EmailButton.setLayoutX(220)
  EmailButton.setLayoutY(180)
  ScanButton.setLayoutX(220)
  ScanButton.setLayoutY(300)
  group.getChildren.add(canvas)
  group.getChildren.add(playerButton)
  group.getChildren.add(BotButton)
  val scene = new Scene(group)

  playerButton.setOnAction(_ => loginSceneListener.onButtonPlayerLogin())
  BotButton.setOnAction(_ => loginSceneListener.onButtonBotLogin())
  ScanButton.setOnAction(_ => loginSceneListener.onButtonScanLogin())
  EmailButton.setOnAction(_ => loginSceneListener.onButtonEmailLogin())
  EmailConnect.setOnAction(_ => loginSceneListener.onButtonEmailConnect())
  BotConnectButton.setOnAction(_ => loginSceneListener.onButtonBotConnect())

  def setLoginListener(listener: LoginSceneListener): Unit ={
    loginSceneListener = listener
  }

  def drawScanUrl(imageStream:ByteArrayInputStream)={
    ClientBoot.addToPlatform{
      group.getChildren.remove(EmailButton)
      group.getChildren.remove(ScanButton)
      val image = new Image(imageStream)
      ctx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,28))
      ctx.setFill(Color.BLACK)
      ctx.drawImage(image,100,80)
      ctx.fillText("请扫码登录",180,400)
    }
  }
  def drawLoginWay()={
    ClientBoot.addToPlatform{
      group.getChildren.remove(playerButton)
      group.getChildren.remove(BotButton)
      group.getChildren.add(EmailButton)
      group.getChildren.add(ScanButton)
    }
  }

}
