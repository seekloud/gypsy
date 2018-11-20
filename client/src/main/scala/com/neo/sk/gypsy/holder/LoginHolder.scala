package com.neo.sk.gypsy.holder

import java.io.ByteArrayInputStream

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.WsClient
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.LoginScene
import com.neo.sk.gypsy.common.Api4GameAgent._

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
  var wsUrl = ""
  var scanUrl = ""
  loginScene.setLoginSceneListener(new LoginScene.LoginSceneListener {
    override def onButtonConnect(): Unit = {
      getLoginResponseFromEs().map {
        case Right(r) =>
          wsUrl = r.data.wsUrl
          scanUrl = r.data.scanUrl
          //TODO
          loginScene.drawScanUrl(imageFromBase64(scanUrl))
          wsClient ! ConnectEsheep(wsUrl)
        case Left(l) =>
      }

      val id = System.currentTimeMillis().toString
      val name = "name" + System.currentTimeMillis().toString
      val accessCode = "jgfkldpwer"
      wsClient ! WsClient.ConnectGame(id,name,accessCode)
    }
  })

  def showScene(): Unit ={
    ClientBoot.addToPlatform{
      stageCtx.showScene(loginScene.scene,"Login")
    }
  }

  def imageFromBase64(base64Str:String)={
    if(base64Str == null) null

    import sun.misc.BASE64Decoder
    val decoder = new BASE64Decoder
    val bytes:Array[Byte] = decoder.decodeBuffer(base64Str)
    for(i <- 0 until bytes.length){
      if(bytes(i) < 0) bytes(i) = (bytes(i).+(256)).toByte
    }
    val b = new ByteArrayInputStream(bytes)
    b
  }

}
