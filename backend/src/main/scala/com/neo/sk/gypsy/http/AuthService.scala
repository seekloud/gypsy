package com.neo.sk.gypsy.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.{ExecutionContextExecutor, Future}
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.ptcl.EsheepProtocol
import com.neo.sk.gypsy.Boot.{esheepClient, executor, roomManager, timeout}
import com.neo.sk.gypsy.core.EsheepSyncClient

/**
  * @author zhaoyin
  * @date 2018/10/25  下午9:39
  */
trait AuthService extends ServiceUtils{

  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def AuthUserErrorRsp(msg: String) = ErrorRsp(10001001, msg)

  protected def authPlatUser(accessCode:String)(f: EsheepProtocol.PlayerInfo => server.Route) :server.Route = {
    if(false){
      val verifyAccessCodeFutureRst: Future[EsheepProtocol.VerifyAccessCodeRsp] = esheepClient ? (e => EsheepSyncClient.VerifyAccessCode(accessCode.toLong, e))
      dealFutureResult{
        verifyAccessCodeFutureRst.map{ rsp=>
          if(rsp.errCode == 0 && rsp.data.nonEmpty){
            f(rsp.data.get.playerInfo)
          } else{
            complete(AuthUserErrorRsp(rsp.msg))
          }
        }.recover{
          case e:Exception =>
            log.warn(s"verifyAccess code failed, code=${accessCode}, error:${e.getMessage}")
            complete(AuthUserErrorRsp(e.getMessage))
        }
      }
    }else{
      f(EsheepProtocol.PlayerInfo(12,"test"))
    }
  }

}
