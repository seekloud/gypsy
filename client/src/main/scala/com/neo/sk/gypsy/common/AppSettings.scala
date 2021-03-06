package com.neo.sk.gypsy.common

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory


/**
  * @author zhaoyin
  * 2018/10/28  3:29 PM
  */
object AppSettings {
  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("productClient.conf").withFallback(ConfigFactory.load())
  val appConfig = config.getConfig("app")
  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")

  val esheepProtocol = appConfig.getString("esheep.protocol")
  val esheepHost = appConfig.getString("esheep.host")
  val esheepDomain = appConfig.getString("esheep.domain")
  val gameId = appConfig.getLong("esheep.gameId")

  val isView = appConfig.getBoolean("isView")
  var isLayer = appConfig.getBoolean("isLayer")

  val botInfo = if(!isView) {
    (appConfig.getString("botInfo.botId"), appConfig.getString("botInfo.botKey"))
  } else("","")

  val gameProtocol = appConfig.getString("server.protocol")
  val gameHost = appConfig.getString("server.host")
  val gameDomain = appConfig.getString("server.domain")
  val botSecure =  appConfig.getString("botSecure.apiToken")
  val isBot = appConfig.getBoolean("isBot")
  val botServerPort = appConfig.getInt("botServerPort")
  val framePeriod = appConfig.getInt("framePeriod")
  val isGray = appConfig.getBoolean("isGray")
  val botTest = appConfig.getBoolean("botTest")
}
