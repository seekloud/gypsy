package com.neo.sk.gypsy.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document

object Routes {

  object UserRoute{
    private val baseUrl = "/gypsy/user"

    private def guestLogin(name:String)=baseUrl+s"/guestLogin?name=$name"
    private val userLoginWs=baseUrl+"/userLoginWs"
    val  userLogin: String =baseUrl+"/userLogin"
    val userRegister: String =baseUrl+"/userRegister"

    def getWebSocketUri(document: Document, nameOfChatParticipant: String,userType:Int): String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl=if (userType==0) guestLogin(nameOfChatParticipant) else userLoginWs
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }


  }

}
