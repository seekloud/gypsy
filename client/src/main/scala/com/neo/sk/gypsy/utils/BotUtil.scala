package com.neo.sk.gypsy.utils

import com.neo.sk.gypsy.common.AppSettings.botSecure
/**
  * @author zhaoyin
  * 2018/12/11  3:02 PM
  */
object BotUtil {

  def checkBotToken(apiToken: String) = {
    if(apiToken == botSecure)
      true
    else
      false
  }

}
