package com.neo.sk.gypsy.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document

object Routes {

  object UserRoute{
    private val baseUrl = "/gypsy/user"

    private def guestLogin(room:Long,name:String)=baseUrl+s"/guestLogin?room=$room&name=$name"
    private def userLoginWs(room:Long)=baseUrl+s"/userLoginWs?room=$room"
    private def watcherJoin(room:Long,name:String) = baseUrl + s"/watcherJoin?room=$room&name=$name"
    val userLogin: String =baseUrl+"/userLogin"
    val userRegister: String =baseUrl+"/userRegister"
    val updateUserScore:String=baseUrl+"/updateMaxScore"
    def checkName(name:String ,room:Long)=baseUrl+s"/checkName?name=$name&room=$room"

    def getWebSocketUri(document: Document,room:Long, nameOfChatParticipant: String,userType:Int): String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl=if (userType==0) guestLogin(room,nameOfChatParticipant) else if(userType == -1)
        watcherJoin(room,nameOfChatParticipant) else userLoginWs(room)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }
  }

  object ApiRoute{
    private val baseUrl = "/gypsy/api"

    private def playGame(playerId:String,
                         playerName:String,
                         roomId:Long,
                         accessCode:String
                        ) = if(roomId == 0l)
      baseUrl + s"/playGame?playerId=$playerId&playerName=$playerName&accessCode=$accessCode"
    else
      baseUrl + s"/playGame?playerId=$playerId&playerName=$playerName&accessCode=$accessCode&roomId=$roomId"

    val watchGame = baseUrl + "/watchGame"

    def getpgWebSocketUri(document: Document,
                        playerId:String,
                        playerName:String,
                        roomId:Long,
                        accessCode:String,
                        userType:Int):String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      //todo watchGame
      val wsUrl = if(userType == 0) playGame(playerId,playerName,roomId,accessCode)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }

    def getwrWebSocketUri(
                         recordId:Long,
                         playerId:String,
                         frame:Int,
                         accessCode:String
                         ):String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl = baseUrl + s"/watchRecord/?recordId=${recordId}&playerId=${playerId}&frame=${frame}&accessCode=${accessCode}"
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }

  }

}
