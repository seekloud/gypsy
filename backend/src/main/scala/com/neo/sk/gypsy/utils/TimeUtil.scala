package com.neo.sk.gypsy.utils

import java.text.SimpleDateFormat

import com.github.nscala_time.time.Imports.DateTime
/**
  * Created by TangYaruo on 2017/11/5.
  */
object TimeUtil {

  def date2TimeStamp(date: String): Long = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date).getTime
  }

  def timeStamp2Date(timestamp: Long) = {
    new SimpleDateFormat("yyyyMMdd").format(timestamp)
  }

  def timeStamp2yyyyMMdd(timestamp: Long) = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)
  }

  def timestamp2DateOnly(timestamp: Long): String = {
    new SimpleDateFormat("yyyy/MM/dd").format(timestamp)
  }

  def date2Timestamp(date: String): Long = {
    new SimpleDateFormat("yyyy/MM/dd").parse(date).getTime
  }

  def getTodayZeroStamp: Long = {
    val curTime = System.currentTimeMillis()
    val date = TimeUtil.timestamp2DateOnly(curTime).split("/").take(3).mkString("-")
    val zeroStamp = TimeUtil.date2TimeStamp(date + " 00:00:00")
    zeroStamp
  }

  def todayBegin():Long ={
    new DateTime().withTimeAtStartOfDay().getMillis
  }


  def MTime2HMS(time:Long)={
    var ts = (time/1000)
    var result = ""
    if(ts/3600>0){
      result += s"${ts/3600}小时"
    }
    ts = ts % 3600
    if(ts/60>0){
      result += s"${ts/60}分"
    }
    ts = ts % 60
    result += s"${ts}秒"
    result
  }

}
