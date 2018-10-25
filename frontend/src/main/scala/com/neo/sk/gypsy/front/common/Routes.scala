package com.neo.sk.gypsy.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document

object Routes {

  object UserRoute{
    private val baseUrl = "/gypsy/user"

    private def guestLogin(room:Long,name:String)=baseUrl+s"/guestLogin?room=$room&name=$name"
    private def userLoginWs(room:Long)=baseUrl+s"/userLoginWs?room=$room"
    val userLogin: String =baseUrl+"/userLogin"
    val userRegister: String =baseUrl+"/userRegister"
    val updateUserScore:String=baseUrl+"/updateMaxScore"
    def checkName(name:String ,room:Long)=baseUrl+s"/checkName?name=$name&room=$room"

    def getWebSocketUri(document: Document,room:Long, nameOfChatParticipant: String,userType:Int): String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl=if (userType==0) guestLogin(room,nameOfChatParticipant) else userLoginWs(room)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }


  }

}
