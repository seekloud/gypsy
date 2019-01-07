package com.neo.sk.gypsy.common

import java.util.concurrent.TimeUnit

import com.neo.sk.gypsy.utils.SessionSupport.SessionConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import collection.JavaConverters._

/**
  * User: Taoz
  * Date: 9/4/2015
  * Time: 4:29 PM
  */
object AppSettings {

  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }

  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")

  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")

  val botMap = {
    import collection.JavaConverters._
    val botIdList = appConfig.getStringList("bot.botId").asScala
    val botNames = appConfig.getStringList("bot.botName").asScala
    require(botIdList.length == botNames.length, "botIdList.length and botNames.length not equel.")
    botIdList.zip(botNames).toMap
  }

  val botConfig = appConfig.getConfig("bot")
  val starNames = botConfig.getStringList("starName")
  val botNum = botConfig.getInt("botNum")
  val addBotPlayer = botConfig.getBoolean("addBotPlayer")


  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")

  val sConf = config.getConfig("session")
  val sessionConfig = {
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }
  val sessionTime=sConf.getInt("sessionTime")

  val hestiaConfig = config.getConfig("hestia")
  val hestiaProtocol = hestiaConfig.getString("protocol")
  val hestiaHost = hestiaConfig.getString("host")
  val hestiaPort = hestiaConfig.getString("port")
  val hestiaDomain = hestiaConfig.getString("domain")
  val hestiaAppId = hestiaConfig.getString("appId")
  val hestiaSecureKey = hestiaConfig.getString("secureKey")
  val hestiaAddress = hestiaConfig.getString("address")

  val gameConfig=config.getConfig("game")
  val waitTime=gameConfig.getInt("waitTime")
  val matchTime=gameConfig.getInt("matchTime")
  val gameTime=gameConfig.getInt("gameTime")
  val limitCount=gameConfig.getInt("limitCount")
  val reliveTime = gameConfig.getInt("reliveTime")
  val SyncCount = gameConfig.getInt("SyncCount")

  val esheepConfig = appConfig.getConfig("esheep")
  val esheepAppId = esheepConfig.getString("appId")
  val esheepSecureKey = esheepConfig.getString("secureKey")
  val esheepProtocol = esheepConfig.getString("protocol")
  val esheepHost = esheepConfig.getString("host")
  val esheepPort = esheepConfig.getInt("port")
  val esheepDomain = esheepConfig.getString("domain")
  val esheepGameId = esheepConfig.getLong("gameId")
  val esheepGameKey = esheepConfig.getString("gsKey")
  val esheepAuthToken = esheepConfig.getBoolean("authToken")
  val appSecureMap = {
    val appIds = appConfig.getStringList("client.appIds").asScala
    val secureKeys = appConfig.getStringList("client.secureKeys").asScala
    require(appIds.length == secureKeys.length, "appIdList.length and secureKeys.length not equel.")
    appIds.zip(secureKeys).toMap
  }

  val gameDataDirectoryPath = appConfig.getString("gameDataDirectoryPath")
  val gameRecordIsWork = appConfig.getBoolean("gameRecordIsWork")
  val gameTest = appConfig.getBoolean("gameTest")
//  val addBotPlayer = appConfig.getBoolean("addBotPlayer")

  val essfMapKeyName = "essfMap"

}
