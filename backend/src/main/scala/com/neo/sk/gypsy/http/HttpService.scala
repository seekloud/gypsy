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
import java.net.URLEncoder
import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends ResourceService with OutApiService with UserService with EsheepService
{


  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler



  val routes: server.Route =
    ignoreTrailingSlash {
      pathPrefix("gypsy") {
        pathEndOrSingleSlash {
          //todo 不需要session了吧
          optionalGypsySession{
            case Some(_)=>
              getFromResource("html/gypsy.html")
            case None=>
              log.info("guest comeIn withOut session")
              addSession( GypsySession(BaseUserInfo(UserRolesType.player,"","",""),System.currentTimeMillis()).toSessionMap){
                ctx=>
                  ctx.redirect("/gypsy",StatusCodes.SeeOther)
              }
          }
        }~ path("playGame") {
          parameter(
            'playerId.as[String],
            'playerName.as[String],
            'accessCode.as[String],
            'roomId.as[Long].?
          ){
            case (playerId, playerName, accessCode,roomIdOpt) =>
              redirect(s"/gypsy#/playGame/${playerId}/${URLEncoder.encode(playerName,"utf-8")}/${roomIdOpt.getOrElse(0l)}/${accessCode}",
                StatusCodes.SeeOther
              )
          }
        } ~ path("watchRecord"){
          parameter(
            'recordId.as[Long],
            'playerId.as[String],
            'frame.as[Int],
            'accessCode.as[String]
          ){
            case (recordId, playerId,frame,accessCode) =>
              redirect(s"/gypsy#/watchRecord/${recordId}/${playerId}/${frame}/${accessCode}",
                StatusCodes.SeeOther)
          }
        } ~ path("watchGame"){
          parameter(
            'roomId.as[Long],
            'playerId.as[String].?,
            'accessCode.as[String]
          ){
            case (roomId,playerIdOpt,accessCode) =>
              redirect(s"/gypsy#/watchGame/${roomId}/${playerIdOpt.getOrElse("")}/${accessCode}",
                StatusCodes.SeeOther
              )
          }
        } ~ resourceRoutes ~ userRoutes ~ esheepRoutes ~ apiRoutes

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
