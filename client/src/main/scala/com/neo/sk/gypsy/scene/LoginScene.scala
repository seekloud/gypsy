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
import scalafx.scene.control.Alert
import javafx.scene.image.ImageView
import scalafx.scene.text.Text
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
    def onButtonBotConnect(botId:Long,botKey:String)
    def onButtonReturn()
  }
}
class LoginScene {
  import LoginScene._

  val width = 500
  val height = 500
  val peopleHeader = new Image(ClientBoot.getClass.getResourceAsStream("/img/people.jpg"))
  val BotHeader = new Image(ClientBoot.getClass.getResourceAsStream("/img/bot.jpg"))
  val group = new Group
  val playerButton = new Button("娱乐模式")
  val BotButton = new Button("训练模式")
  val ScanButton = new Button("扫码登录")
  val EmailButton = new Button("邮箱登录")
  val EmailConnect = new Button("确定")
  val ReturnButton = new Button("返回")
  val BotConnectButton = new Button("开始训练")

  val accountLabel = new Label("账号:")
  val accountInput = new TextField()
  val passwordLabel = new Label("密码:")
  val pwdInput = new TextField()

  val botIdLabel = new Label("botId:")
  val botIdInput = new TextField()
  val botKeyLabel = new Label("BotKey:")
  val botKeyInput = new TextField()

//  val ErrorTip = new Text()
//  ErrorTip.setLayoutX(190)
//  ErrorTip.setLayoutY(310)
//  ErrorTip.setFill(Color.RED)

  val LoginBack = new Image(ClientBoot.getClass.getResourceAsStream("/img/LoginB1.JPG"))
  val canvas = new Canvas(width,height)
  val ctx = canvas.getGraphicsContext2D

  var loginSceneListener: LoginSceneListener=_

  ReturnButton.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0);-fx-font: 16 arial; -fx-base: #ee2211;")
  playerButton.setLayoutX(210)
  playerButton.setLayoutY(180)
  playerButton.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); -fx-font:16 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic; -fx-base:#2486d6")
  BotButton.setLayoutX(210)
  BotButton.setLayoutY(300)
  BotButton.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); -fx-font:16 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic; -fx-base:#2486d6")
  EmailButton.setLayoutX(210)
  EmailButton.setLayoutY(180)
  EmailButton.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); -fx-font:16 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic; -fx-base:#2486d6")
  ScanButton.setLayoutX(210)
  ScanButton.setLayoutY(300)
  ScanButton.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0); -fx-font:16 Helvetica; -fx-font-weight: bold; -fx-font-posture:italic; -fx-base:#2486d6")

  accountLabel.setLayoutX(120)
  accountLabel.setLayoutY(165)
  accountLabel.setStyle("-fx-font: 16 arial;-fx-base:#000000")
  accountInput.setLayoutX(180)
  accountInput.setLayoutY(160)

  passwordLabel.setLayoutX(120)
  passwordLabel.setLayoutY(255)
  passwordLabel.setStyle("-fx-font: 16 arial;-fx-base:#000000")
  pwdInput.setLayoutX(180)
  pwdInput.setLayoutY(250)

  botIdLabel.setLayoutX(120)
  botIdLabel.setLayoutY(165)
  botIdLabel.setStyle("-fx-font: 16 arial;-fx-base:#000000")
  botIdInput.setLayoutX(180)
  botIdInput.setLayoutY(160)

  botKeyLabel.setLayoutX(120)
  botKeyLabel.setLayoutY(255)
  botKeyLabel.setStyle("-fx-font: 16 arial;-fx-base:#000000")
  botKeyInput.setLayoutX(180)
  botKeyInput.setLayoutY(250)

  EmailConnect.setLayoutX(190)
  EmailConnect.setLayoutY(320)
  EmailConnect.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0);-fx-font: 16 arial; -fx-base: #76EE00;")
  BotConnectButton.setLayoutX(190)
  BotConnectButton.setLayoutY(320)
  BotConnectButton.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropShadow(three-pass-box, #528B8B, 10.0, 0, 0, 0);-fx-font: 16 arial; -fx-base: #76EE00;")

  val alert = new Alert(Alert.AlertType.Error)
  alert.setTitle("错误警告")
  alert.setHeaderText("登录信息错误")
  ctx.drawImage(LoginBack,0,0,width,height)
  ctx.setFill(Color.web("#eb5514"))
  ctx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,34))
  ctx.fillText("Gypsy",195,100)
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
    println(s"Id ：$botId  Key:$botKey ")
    if (botId == ""||botKey=="") {
      ClientBoot.addToPlatform{
        alert.setContentText("BotId或BotKey不能为空")
        alert.showAndWait()
      }
    } else {
      loginSceneListener.onButtonBotConnect(botId.trim.toLong, botKey.trim)
    }
  }

  EmailConnect.setOnAction{ _ =>
    val account = accountInput.getText()
    val pwd = pwdInput.getText()
    if (account == ""||pwd=="") {
      ClientBoot.addToPlatform{
        alert.setContentText("邮箱或密码不能为空")
        alert.showAndWait()
      }
    } else {
      loginSceneListener.onButtonEmailConnect(account, pwd)
    }
  }

  def setLoginListener(listener: LoginSceneListener): Unit ={
    loginSceneListener = listener
  }

  def clearCanvas() = {
    ctx.setFill(Color.web("rgb(250, 250, 250)"))
    ctx.fillRect(0, 0, width, height)
  }

  def drawScanUrl(imageStream:ByteArrayInputStream)={
    ClientBoot.addToPlatform{
      group.getChildren.remove(EmailButton)
      group.getChildren.remove(ScanButton)
      val image = new Image(imageStream)
      clearCanvas()
      ctx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,26))
      ctx.setFill(Color.BLACK)
      ctx.drawImage(image,100,120)
      ctx.fillText("请扫码登录",180,70)
      ReturnButton.setLayoutX(225)
      ReturnButton.setLayoutY(400)
      //group.getChildren.add(ReturnButton)
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
      ReturnButton.setLayoutX(280)
      ReturnButton.setLayoutY(320)
      group.getChildren.add(ReturnButton)
    }
  }

  def drawBotLogin():Unit={
    ClientBoot.addToPlatform{
      group.getChildren.remove(playerButton)
      group.getChildren.remove(BotButton)
      group.getChildren.add(botIdLabel)
      group.getChildren.add(botIdInput)
      group.getChildren.add(botKeyLabel)
      group.getChildren.add(botKeyInput)
      group.getChildren.add(BotConnectButton)
      ReturnButton.setLayoutX(280)
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
    clearCanvas()
    ctx.drawImage(LoginBack,0,0,width,height)
    ctx.setFill(Color.web("#eb5514"))
    ctx.setFont(Font.font("Helvetica", FontWeight.BOLD ,FontPosture.ITALIC,34))
    ctx.fillText("Gypsy",195,100)
    group.getChildren.add(playerButton)
    group.getChildren.add(BotButton)
  }
  }

}
