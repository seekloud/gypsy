package com.neo.sk.gypsy.http


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server
import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.{ExecutionContextExecutor, Future}
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.ptcl.EsheepProtocol
import com.neo.sk.gypsy.Boot.{esheepClient, executor, scheduler, timeout}
import com.neo.sk.gypsy.core.EsheepSyncClient

import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._

/**
  * @author zhaoyin
  * @date 2018/10/25  下午9:39
  */
trait AuthService extends ServiceUtils{

  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def AuthUserErrorRsp(msg: String) = ErrorRsp(10001001, msg)

  protected def authPlatUser(accessCode:String)(f: EsheepProtocol.PlayerInfo => server.Route) :server.Route = {
    if(true){
      val verifyAccessCodeFutureRst: Future[EsheepProtocol.VerifyAccessCodeRsp] = esheepClient ? (e => EsheepSyncClient.VerifyAccessCode(accessCode, e))
      dealFutureResult{
        verifyAccessCodeFutureRst.map{ rsp=>
          if(rsp.errCode == 0 && rsp.data.nonEmpty){
            f(rsp.data.get)
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
      f(EsheepProtocol.PlayerInfo("","test"))
    }
  }

}
