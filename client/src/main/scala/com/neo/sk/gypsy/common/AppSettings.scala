package com.neo.sk.gypsy.common

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory


/**
  * @author zhaoyin
  * @date 2018/10/28  3:29 PM
  */
object AppSettings {
  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())
  val appConfig = config.getConfig("app")
  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")
}
