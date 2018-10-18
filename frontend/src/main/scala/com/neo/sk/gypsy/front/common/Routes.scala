package com.neo.sk.gypsy.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document

object Routes {

  object UserRoute{
    private val baseUrl = "/gypsy/user"

    private def guestLogin(room:String,name:String,watcher:Boolean)=baseUrl+s"/guestLogin?room=$room&name=$name&watcher=$watcher"
    private def userLoginWs(room:String,watcher:Boolean)=baseUrl+s"/userLoginWs?room=$room&watcher=$watcher"
    private def watcherJoin(room:String,name:String,watcher:Boolean) = baseUrl + s"/watcherJoin?room=$room&name=$name&watcher=$watcher"
    val userLogin: String =baseUrl+"/userLogin"
    val userRegister: String =baseUrl+"/userRegister"
    val updateUserScore:String=baseUrl+"/updateMaxScore"
    def checkName(name:String ,room:String)=baseUrl+s"/checkName?name=$name&room=$room"

    def getWebSocketUri(document: Document,room:String, nameOfChatParticipant: String,userType:Int,watcher:Boolean): String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl=if (userType==0) guestLogin(room,nameOfChatParticipant,watcher) else if(userType == -1)
        watcherJoin(room,nameOfChatParticipant,watcher) else userLoginWs(room,watcher)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }
  }

}
