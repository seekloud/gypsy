package com.neo.sk.gypsy.http

import akka.actor.{ActorRef, ActorSystem, Scheduler}
import akka.http.javadsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import com.neo.sk.gypsy.common.Constant.UserRolesType
import com.neo.sk.gypsy.http.SessionBase.GypsySession
import com.neo.sk.gypsy.ptcl.UserProtocol.BaseUserInfo

import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends ResourceService
  with UserService
{


  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler



  val routes: server.Route =
    ignoreTrailingSlash {
      pathPrefix("gypsy") {
        (path("game") & get) {
          pathEndOrSingleSlash {
          optionalGypsySession{
            case Some(_)=>
            getFromResource("html/gypsy.html")
            case None=>
              log.info("guest comeIn withOut session")
              addSession( GypsySession(BaseUserInfo(UserRolesType.guest,0,"",""),System.currentTimeMillis()).toSessionMap){
                ctx=>
                  ctx.redirect("/gypsy/game",StatusCodes.SeeOther)
              }
           }
          }
        }~userRoutes~resourceRoutes~esheepRoutes

      }
    }


  def tmp = {
    val out = Source.empty
    val in = Sink.ignore
    Flow.fromSinkAndSource(in, out)
  }


  def tmp2 = {

    val sink = Sink.ignore
    def chatFlow(sender: String): Flow[String, String, Any] = {
      val in =
        Flow[String]
          .to(sink)

      // The counter-part which is a source that will create a target ActorRef per
      // materialization where the chatActor will send its messages to.
      // This source will only buffer one element and will fail if the client doesn't read
      // messages fast enough.
      val chatActor: ActorRef = null
      val out =
        Source.actorRef[String](1, OverflowStrategy.fail)
          .mapMaterializedValue(actor => chatActor ! "NewParticipant(sender, _)")

      Flow.fromSinkAndSource(in, out)
    }
  }


}
