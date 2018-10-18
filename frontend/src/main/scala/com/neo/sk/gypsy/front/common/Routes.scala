package com.neo.sk.gypsy.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document

object Routes {

  object UserRoute{
    private val baseUrl = "/gypsy/user"

    private def guestLogin(room:String,name:String)=baseUrl+s"/guestLogin?room=$room&name=$name"
    private def userLoginWs(room:String)=baseUrl+s"/userLoginWs?room=$room"
    private def watcherJoin(room:String,name:String) = baseUrl + s"/watcherJoin?room=$room&name=$name"
    val userLogin: String =baseUrl+"/userLogin"
    val userRegister: String =baseUrl+"/userRegister"
    val updateUserScore:String=baseUrl+"/updateMaxScore"
    def checkName(name:String ,room:String)=baseUrl+s"/checkName?name=$name&room=$room"

    def getWebSocketUri(document: Document,room:String, nameOfChatParticipant: String,userType:Int): String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl=if (userType==0) guestLogin(room,nameOfChatParticipant) else if(userType == -1)
        watcherJoin(room,nameOfChatParticipant) else userLoginWs(room)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }
  }

}
