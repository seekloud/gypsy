package com.neo.sk.gypsy

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.gypsy.http.HttpService

import scala.language.postfixOps
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.gypsy.core.RoomManager

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:25 PM
  */
object Boot extends HttpService {

  import concurrent.duration._
  import com.neo.sk.gypsy.common.AppSettings._


  override implicit val system = ActorSystem("arges", config)
  // the executor should not be the default dispatcher.
  override implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  override implicit val materializer = ActorMaterializer()

  override val timeout = Timeout(20 seconds) // for actor asks

  override implicit val scheduler = system.scheduler

  val log: LoggingAdapter = Logging(system, getClass)

  val roomManager: ActorRef[RoomManager.Command] =system.spawn(RoomManager.behaviors,"roomManager")

  def main(args: Array[String]) {
    log.info("Starting.")
    Http().bindAndHandle(routes, httpInterface, httpPort)
    log.info(s"Listen to the $httpInterface:$httpPort")
    log.info("Done.")
    println(s"Server is listening on http://localhost:${httpPort}/gypsy/cell")
  }

}
