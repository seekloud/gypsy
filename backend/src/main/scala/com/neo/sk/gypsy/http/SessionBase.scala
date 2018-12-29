package com.neo.sk.gypsy.http

import akka.actor.Status
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive1, RequestContext}
import akka.http.scaladsl.server.directives.BasicDirectives
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.common.Constant.UserRolesType
import com.neo.sk.gypsy.ptcl.UserProtocol.BaseUserInfo
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.utils.{CirceSupport, SessionSupport}
import org.slf4j.LoggerFactory
import io.circe.generic.auto._
/**
  * User: Taoz
  * Date: 12/4/2016
  * Time: 7:57 PM
  */

object SessionBase{
  private val log = LoggerFactory.getLogger(this.getClass)

  val SessionTypeKey = "STKey"

  object SessionKeys {
    val sessionType = "gypsy_session"
    val userType = "gypsy_userType"
    val userId = "gypsy_userId"
    val name = "gypsy_name"
    val headImg = "head_img"
    val timestamp = "gypsy_timestamp"
  }

  case class GypsySession(
                             userInfo: BaseUserInfo,
                             time: Long
                           ) {
    def toSessionMap: Map[String, String] = {
      Map(
        SessionTypeKey -> SessionKeys.sessionType,
        SessionKeys.userType -> userInfo.userType,
        SessionKeys.userId -> userInfo.userId.toString,
        SessionKeys.name -> userInfo.name,
        SessionKeys.headImg -> userInfo.headImg,
        SessionKeys.timestamp -> time.toString
      )
    }
  }
}

trait SessionBase extends SessionSupport with ServiceUtils with CirceSupport{

  import SessionBase._

  override val sessionEncoder = SessionSupport.PlaySessionEncoder
  override val sessionConfig = AppSettings.sessionConfig
  private val timeout = AppSettings.sessionTime * 24 * 60 * 60 * 1000
  implicit class SessionTransformer(sessionMap: Map[String, String]) {
    def toGypsySession:Option[GypsySession] = {
      try {
        if (sessionMap.get(SessionTypeKey).exists(_.equals(SessionKeys.sessionType))) {
          if(System.currentTimeMillis()-sessionMap(SessionKeys.timestamp).toLong> timeout){
            None
          }else {
            Some(GypsySession(
              BaseUserInfo(
                sessionMap(SessionKeys.userType),
                sessionMap(SessionKeys.userId),
                sessionMap(SessionKeys.name),
                sessionMap(SessionKeys.headImg)
              ),
              sessionMap(SessionKeys.timestamp).toLong
            ))
          }
        } else {
          log.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.warn(s"toAdminSession: ${e.getMessage}")
          None
      }
    }
  }
  def loggingAction: Directive[Tuple1[RequestContext]] = extractRequestContext.map { ctx =>
    ctx
  }
  protected val optionalGypsySession: Directive1[Option[GypsySession]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toGypsySession)
    case Left(error) =>
      log.debug(error)
      BasicDirectives.provide(None)
  }

  def noSessionError(message:String = "no session") = ErrorRsp(1000102,s"$message")

  //管理员
  def adminAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
    optionalGypsySession {
      case Some(session) =>
        if(session.userInfo.userType == UserRolesType.devManager){
          f(session.userInfo)
        } else{
          complete(noSessionError("you don't have right."))
        }

      case None =>
        complete(noSessionError())
    }
  }


  //会员
  def memberAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
    optionalGypsySession {
      case Some(session) =>
        if(session.userInfo.userType == UserRolesType.comMember){
          f(session.userInfo)
        } else{
          redirect("/gypsy/cell",StatusCodes.SeeOther)
        }

      case None =>
        complete(noSessionError())
    }
  }



  //管理员和会员
  def customerAuth(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
    optionalGypsySession {
      case Some(session) =>
        if(session.userInfo.userType == UserRolesType.devManager || session.userInfo.userType == UserRolesType.comMember){
          f(session.userInfo)
        } else{
          complete(noSessionError("您没有此项权限，请先登录。"))
        }

      case None =>
        complete(noSessionError())
    }
  }

  def parseGypsySession(f: BaseUserInfo => server.Route) = loggingAction { ctx =>
    optionalGypsySession {
      case Some(session) =>
        f(session.userInfo)

      case None =>
        complete(noSessionError())
    }
  }
}
