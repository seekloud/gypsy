package com.neo.sk.gypsy

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.actor.typed.scaladsl.adapter._
import javafx.application.{Application, Platform}
import com.neo.sk.gypsy.common.AppSettings._

/**
  * @author zhaoyin
  * @date 2018/10/28  2:45 PM
  */

object ClientBoot{
  implicit val system = ActorSystem("gypsy",config)
}
class ClientBoot {

}


