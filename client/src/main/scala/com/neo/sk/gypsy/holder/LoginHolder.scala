package com.neo.sk.gypsy.holder

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

}
