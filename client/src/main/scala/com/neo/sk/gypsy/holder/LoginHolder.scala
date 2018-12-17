package com.neo.sk.gypsy.holder

import java.io.ByteArrayInputStream

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.WsClient
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.LoginScene
import com.neo.sk.gypsy.common.Api4GameAgent._
import com.neo.sk.gypsy.actor.WsClient.ConnectEsheep

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

    }

    override def onButtonBotLogin(): Unit = {

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
