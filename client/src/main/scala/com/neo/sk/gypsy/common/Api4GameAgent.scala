package com.neo.sk.gypsy.common

import com.neo.sk.gypsy.utils.HttpUtil
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.common.AppSettings._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author zhaoyin
  * 2018/11/19  11:07 PM
  */
object Api4GameAgent extends HttpUtil{

  private[this] val log = LoggerFactory.getLogger("Api4GameAgent")

  def getLoginResponseFromEs() ={
    val methodName = "GET"
    val url = esheepProtocol + "://" + esheepDomain + "/esheep/api/gameAgent/login"
    getRequestSend(methodName, url, Nil,"UTF-8").map{
      case Right(r) =>
        decode[LoginResponse](r) match {
          case Right(rin) =>
            Right(rin)
          case Left(lout) =>
            Left(s"error:${lout}")
        }
      case Left(e) =>
        log.info(s"${e}")
        Left("error")
    }
  }

  def linkGameAgent(gameId:Long,playerId:String,token:String) = {
    val data = LinkGameData(gameId,playerId).asJson.noSpaces
    val url  = esheepProtocol + "://" + esheepHost + "/esheep/api/gameAgent/joinGame?token="+token
    postJsonRequestSend("post",url,Nil,data).map{
      case Right(jsonStr) =>
        decode[LinkGameRes](jsonStr) match {
          case Right(res) =>
            Right(LinkResElement(res.data.accessCode,res.data.gsPrimaryInfo))
          case Left(le) =>
            Left("decode error: "+le)
        }
      case Left(erStr) =>
        Left("get return error:"+erStr)
    }
  }

}
