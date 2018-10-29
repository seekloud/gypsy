package com.neo.sk.gypsy.holder

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.WsClient
import com.neo.sk.gypsy.common.StageContext
/**
  * @author zhaoyin
  * @date 2018/10/29  5:14 PM
  */
class LoginHolder(
                   wsClient: ActorRef[WsClient.WsCommand],
                   loginScene: LoginScene,
                   stageCtx: StageContext
                 ) {


  def showScene(): Unit ={
    ClientBoot.addToPlatform{
      stageCtx.showScene(,"Login")
    }
  }

}
