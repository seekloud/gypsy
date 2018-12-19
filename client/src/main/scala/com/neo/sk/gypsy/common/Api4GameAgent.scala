package com.neo.sk.gypsy.common

import com.neo.sk.gypsy.utils.HttpUtil
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.common.AppSettings._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  def linkGameAgent(gameId:Long, playerId:String, token:String) = {
    val data = LinkGameData(gameId,playerId).asJson.noSpaces
    val url  = esheepProtocol + "://" + esheepDomain + "/esheep/api/gameAgent/joinGame?token="+token
    postJsonRequestSend("linkGameAgentPost",url,Nil,data).map{
      case Right(jsonStr) =>
        decode[LinkGameRes](jsonStr) match {
          case Right(res) =>
            Right(LinkResElement(res.data.accessCode,res.data.gsPrimaryInfo))
          case Left(le) =>
            log.info("what error????",le)
            Left("decode error: "+le)
        }
      case Left(erStr) =>
        log.info("what error?",erStr)
        Left("get return error:"+erStr)
    }
  }

  def refreshToken(playerId:String, token:String): Future[Either[ErrorRsp,TokenInfo]] = {
    val methodName = s"gaRefreshToken"
    val url = esheepProtocol + "://" + esheepDomain +s"/esheep/api/gameAgent/gaRefreshToken?token=$token"

    val data = GaRefreshTokenReq(playerId).asJson.noSpaces

    postJsonRequestSend(methodName,url,Nil,data).map{
      case Right(jsonStr) =>
        decode[GaRefreshTokenRsp](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0){
              Right(rsp.data)
            }else{
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            println(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        println(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1,error.getMessage))
    }
  }

  def botKey2Token(botId:Long, botKey:String) = {
    val methodName = "botkey2token"
    val url = esheepProtocol + "://" + esheepDomain + "/esheep/api/sdk/botKey2Token"
    val data = BotKey2Token(botId,botKey).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data).map{
      case Right(jsonStr) =>
        decode[BotTokenRsp](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0){
              Right(rsp.data)
            }else{
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        Left(ErrorRsp(-1,error.getMessage))
    }
  }

  def emailLogin(email:String,password:String)={
    val methodName="GET"
    val url = esheepProtocol + "://" + esheepDomain + "/esheep/rambler/login"
    val data=LoginReq(email,password).asJson.noSpaces
    postJsonRequestSend(methodName,url,Nil,data).map{
      case Right(data) =>
        decode[ESheepUserInfoRsp](data) match {
          case Right(rsp) =>
            if(rsp.errCode==0){
              Right(rsp)
            }
            else{
              Left(ErrorRsp(rsp.errCode,rsp.msg))
            }
          case Left(error)=>
            Left(ErrorRsp(-1,error.getMessage))
        }
      case Left(error) =>
        Left(ErrorRsp(-1,error.getMessage))
    }
  }
}
