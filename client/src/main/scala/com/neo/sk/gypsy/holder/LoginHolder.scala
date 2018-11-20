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

  loginScene.setLoginSceneListener(new LoginScene.LoginSceneListener {
    override def onButtonConnect(): Unit = {
      getLoginResponseFromEs().map {
        case Right(r) =>
          println("lalalla:     "+r.data.scanUrl)
          val wsUrl = r.data.wsUrl
          val scanUrl = r.data.scanUrl
          loginScene.drawScanUrl(imageFromBase64(scanUrl))
          wsClient ! ConnectEsheep(wsUrl)
        case Left(l) =>
      }
    }
  })

  def showScene(): Unit ={
    ClientBoot.addToPlatform{
      stageCtx.showScene(loginScene.scene,"Login",false)
    }
  }

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
