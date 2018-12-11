package com.neo.sk.gypsy.utils

import com.neo.sk.gypsy.common.AppSettings.botSecure
/**
  * @author zhaoyin
  * 2018/12/11  3:02 PM
  */
object BotUtil {

  def checkBotToken(playerId: String, apiToken: String) = {
    if(playerId == botSecure._1 && apiToken == botSecure._2)
      true
    else
      false
  }

}
