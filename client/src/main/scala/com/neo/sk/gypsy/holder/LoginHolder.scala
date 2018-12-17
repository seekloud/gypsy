package com.neo.sk.gypsy.holder

import java.io.ByteArrayInputStream

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.BotActor._
import com.neo.sk.gypsy.actor.{BotActor, WsClient}
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.LoginScene
import com.neo.sk.gypsy.common.Api4GameAgent._
import com.neo.sk.gypsy.actor.WsClient.ConnectEsheep
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author zhaoyin
  * 2018/10/29  5:14 PM
  */
class LoginHolder(
                   wsClient: ActorRef[WsClient.WsCommand],
                   botActor: ActorRef[BotActor.Command],
                   loginScene: LoginScene,
                   stageCtx: StageContext
                 ) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

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

    override def onButtonBotConnect(): Unit = {

    }

    override def onButtonEmailConnect(): Unit = {

    }

    override def onButtonBotLogin(botId: String, botKey: String): Unit = {
      log.info(s"bot join")
      botActor ! BotLogin(botId,botKey)
    }

    override def onButtonEmailLogin(): Unit = {

    }

    override def onButtonPlayerLogin(): Unit = {
      loginScene.drawLoginWay()
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
