package com.neo.sk.gypsy

import javafx.stage.Stage
import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.actor.typed.scaladsl.adapter._
import javafx.application.{Application, Platform}
import akka.actor.typed.ActorRef
import akka.util.Timeout
import com.neo.sk.gypsy.actor.{BotActor, GameClient, TokenActor, WsClient}
import com.neo.sk.gypsy.common.AppSettings._
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.holder.LoginHolder
import com.neo.sk.gypsy.scene.LoginScene
import com.neo.sk.gypsy.actor.SdkServer
import concurrent.duration._

/**
  * @author zhaoyin
  * 2018/10/28  2:45 PM
  */

object ClientBoot{

  implicit val system = ActorSystem("gypsy",config)
  implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20.seconds) // for actor ask
  val tokenActor:ActorRef[TokenActor.Command] = system.spawn(TokenActor.create(),"esheepSyncClient")
  val gameClient= system.spawn(GameClient.create(),"gameHolder")
  val sdkServer: ActorRef[SdkServer.Command] = system.spawn(SdkServer.create(),"sdkServer")
  /**保证线程安全**/
  def addToPlatform(fun: => Unit) = {
    Platform.runLater(() => fun)
  }
}

class ClientBoot extends javafx.application.Application{

  import ClientBoot._

  override def start(mainStage: Stage): Unit = {
    val context = new StageContext(mainStage)
    val wsClient = system.spawn(WsClient.create(gameClient,context,system,materializer,executor),"WsClient")
    val botActor = system.spawn(BotActor.create(gameClient,context),"botActor")
    val loginScene = new LoginScene()
    val loginHolder = new LoginHolder(wsClient,botActor,loginScene,context)
    loginHolder.showScene()
  }
}


