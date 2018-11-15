package com.neo.sk.gypsy.holder

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.WsClient
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.LoginScene
/**
  * @author zhaoyin
  * @date 2018/10/29  5:14 PM
  */
class LoginHolder(
                   wsClient: ActorRef[WsClient.WsCommand],
                   loginScene: LoginScene,
                   stageCtx: StageContext
                 ) {
  loginScene.setLoginSceneListener(new LoginScene.LoginSceneListener {
    override def onButtonConnect(): Unit = {
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
