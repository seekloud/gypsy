package com.neo.sk.gypsy

import javafx.application.{Application, Platform}
import javafx.stage.Stage
import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.actor.typed.scaladsl.adapter._
import javafx.application.{Application, Platform}
import com.neo.sk.gypsy.actor.WsClient
import com.neo.sk.gypsy.actor.GameClient
import com.neo.sk.gypsy.common.AppSettings._
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.holder.LoginHolder
import com.neo.sk.gypsy.scene.LoginScene

/**
  * @author zhaoyin
  * @date 2018/10/28  2:45 PM
  */

object ClientBoot{
  implicit val system = ActorSystem("gypsy",config)
  implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  val gameHolderClient = system.spawn(GameClient.create(),"gameHolder")

  def addToPlatform(fun: => Unit) = {
    Platform.runLater(() => fun)
  }
}


class ClientBoot extends javafx.application.Application{

  import ClientBoot._
  override def start(mainStage: Stage): Unit = {
    val context = new StageContext(mainStage)
    val wsClient = system.spawn(WsClient.create(gameHolderClient,context,system,materializer,executor),"WsClient")
    val loginScene = new LoginScene()
    val loginHolder = new LoginHolder(wsClient,loginScene,context)
    loginHolder.showScene()
  }


}


