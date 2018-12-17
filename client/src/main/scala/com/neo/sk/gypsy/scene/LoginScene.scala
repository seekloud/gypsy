package com.neo.sk.gypsy.scene

import java.io.ByteArrayInputStream

import com.neo.sk.gypsy.ClientBoot
import javafx.scene.control.{Button, Label, TextField}
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
    def onButtonEmailConnect(email:String,password:String)
    def onButtonBotConnect(botId:String,botKey:String)
    def onButtonReturn()
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
  val ReturnButton = new Button("返回")
  val BotConnectButton = new Button("开始训练")
//  val BotReturn = new Button("返回")

  val accountLabel = new Label("账号:")
  val accountInput = new TextField()
  val passwordLabel = new Label("密码:")
  val pwdInput = new TextField()

  val botIdLabel = new Label("botId:")
  val botIdInput = new TextField()
  val botKeyLabel = new Label("BotKey:")
  val botKeyInput = new TextField()

  val ErrorTip = new TextField()

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

  accountLabel.setLayoutX(120)
  accountLabel.setLayoutY(160)
  accountInput.setLayoutX(180)
  accountInput.setLayoutY(160)

  passwordLabel.setLayoutX(120)
  passwordLabel.setLayoutY(250)
  pwdInput.setLayoutX(180)
  pwdInput.setLayoutY(250)

  EmailConnect.setLayoutX(180)
  EmailConnect.setLayoutY(320)
  BotConnectButton.setLayoutX(180)
  BotConnectButton.setLayoutY(320)


  group.getChildren.add(canvas)
  group.getChildren.add(playerButton)
  group.getChildren.add(BotButton)
  val scene = new Scene(group)

  ReturnButton.setOnAction(_ => loginSceneListener.onButtonReturn())
  playerButton.setOnAction(_ => loginSceneListener.onButtonPlayerLogin())
  BotButton.setOnAction(_ => loginSceneListener.onButtonBotLogin())
  ScanButton.setOnAction(_ => loginSceneListener.onButtonScanLogin())
  EmailButton.setOnAction(_ => loginSceneListener.onButtonEmailLogin())
  BotConnectButton.setOnAction { _ =>
    val botId = botIdInput.getText()
    val botKey = botKeyInput.getText()
    if (botId == "") {
      ErrorTip.setText("botId不能为空")
    } else if (botKey == "") {
      ErrorTip.setText("botKey不能为空")
    } else {
      loginSceneListener.onButtonBotConnect(botId, botKey)
    }
  }

  EmailConnect.setOnAction{ _ =>
    val account = accountInput.getText()
    val pwd = pwdInput.getText()
    if (account == "") {
      ErrorTip.setText("email不能为空")
    } else if (pwd == "") {
      ErrorTip.setText("password不能为空")
    } else {
      loginSceneListener.onButtonEmailConnect(account, pwd)
    }
  }

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
      ctx.drawImage(image,100,100)
      ctx.fillText("请扫码登录",180,70)
      ReturnButton.setLayoutX(200)
      ReturnButton.setLayoutY(400)
      group.getChildren.add(ReturnButton)
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

  def drawEmailLogin():Unit={
    ClientBoot.addToPlatform{
      group.getChildren.remove(EmailButton)
      group.getChildren.remove(ScanButton)
      group.getChildren.add(accountLabel)
      group.getChildren.add(accountInput)
      group.getChildren.add(passwordLabel)
      group.getChildren.add(pwdInput)
      group.getChildren.add(EmailConnect)
      ReturnButton.setLayoutX(260)
      ReturnButton.setLayoutY(320)
      group.getChildren.add(ReturnButton)
    }
  }

  def drawBotLogin():Unit={
    ClientBoot.addToPlatform{
      group.getChildren.remove(EmailButton)
      group.getChildren.remove(ScanButton)
      group.getChildren.add(botIdLabel)
      group.getChildren.add(botIdInput)
      group.getChildren.add(botKeyLabel)
      group.getChildren.add(botKeyInput)
      group.getChildren.add(BotConnectButton)
      ReturnButton.setLayoutX(260)
      ReturnButton.setLayoutY(320)
      group.getChildren.add(ReturnButton)
    }
  }

  def drawReturn():Unit={
  ClientBoot.addToPlatform{
   // group.getChildren.removeAll(accountInput,accountLabel,passwordLabel,pwdInput,botKeyInput,botKeyLabel,botIdInput,botIdLabel)
    group.getChildren.remove(accountLabel)
    group.getChildren.remove(accountInput)
    group.getChildren.remove(passwordLabel)
    group.getChildren.remove(pwdInput)
    group.getChildren.remove(botIdLabel)
    group.getChildren.remove(botIdInput)
    group.getChildren.remove(botKeyLabel)
    group.getChildren.remove(botKeyInput)
    group.getChildren.remove(EmailConnect)
    group.getChildren.remove(BotConnectButton)
    group.getChildren.remove(ReturnButton)
    //group.getChildren.add(canvas)
    group.getChildren.add(playerButton)
    group.getChildren.add(BotButton)
  }
  }

}
