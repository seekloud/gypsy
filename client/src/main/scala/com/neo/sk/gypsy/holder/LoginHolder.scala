package com.neo.sk.gypsy.holder

import java.io.ByteArrayInputStream

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.WsClient
import com.neo.sk.gypsy.common.{AppSettings, StageContext}
import com.neo.sk.gypsy.scene.LoginScene
import com.neo.sk.gypsy.common.Api4GameAgent._
import com.neo.sk.gypsy.actor.WsClient.{ConnectEsheep, ConnectGame}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author zhaoyin
  * 2018/10/29  5:14 PM
  */
class LoginHolder(
                   wsClient: ActorRef[WsClient.WsCommand],
                   loginScene: LoginScene,
                   stageCtx: StageContext
                 ) {

  private[this] val log = LoggerFactory.getLogger("LoginMessages")

  loginScene.setLoginListener(new LoginScene.LoginSceneListener {
    override def onButtonScanLogin: Unit = {
      getLoginResponseFromEs().map {
        case Right(r) =>
          val wsUrl = r.data.wsUrl
          val scanUrl = r.data.scanUrl.replaceFirst("data:image/png;base64,","")
          loginScene.drawScanUrl(imageFromBase64(scanUrl))
          wsClient ! ConnectEsheep(wsUrl)
        case Left(l) =>
      }
    }

    override def onButtonBotConnect(botId:String,botKey:String): Unit = {

    }

    override def onButtonEmailConnect(email:String,password:String): Unit = {
      emailLogin(email,password).map{
        case Right(userInfo)=>
            val playerId=s"user+${userInfo.userId}"
            linkGameAgent(AppSettings.gameId,playerId,userInfo.token).map{
              case Right(res) =>
                wsClient ! ConnectGame(playerId,userInfo.userName,res.accessCode)
              case Left(error)=>
                log.info(s"$error occured")
            }

        case Left(error)=>
          log.info(s"$error occured")
          ClientBoot.addToPlatform{
//              loginScene.ErrorTip.setText(error.msg)
//              loginScene.group.getChildren.add(loginScene.ErrorTip)
            loginScene.alert.setContentText(error.msg)
            loginScene.alert.showAndWait()

            }
      }

    }

    override def onButtonBotLogin(): Unit = {
      loginScene.drawBotLogin()

    }

    override def onButtonEmailLogin(): Unit = {
      loginScene.drawEmailLogin()

    }

    override def onButtonPlayerLogin(): Unit = {
      loginScene.drawLoginWay()
    }

    override def onButtonReturn(): Unit = {
      loginScene.drawReturn()
    }
  })



  def showScene(): Unit ={
    ClientBoot.addToPlatform{
      stageCtx.showScene(loginScene.scene,"Login",false)
    }
  }

  stageCtx.setStageListener(new StageContext.StageListener {
    override def onCloseRequest(): Unit = {
      stageCtx.closeStage()
    }
  })


  def imageFromBase64(base64Str:String)={
    if(base64Str == null) null

    import sun.misc.BASE64Decoder
    val decoder = new BASE64Decoder
    val bytes:Array[Byte] = decoder.decodeBuffer(base64Str)
    bytes.indices.foreach{ i =>
      if(bytes(i) < 0) bytes(i) = (bytes(i) + 256).toByte
    }
    val b = new ByteArrayInputStream(bytes)
    b
  }

}
