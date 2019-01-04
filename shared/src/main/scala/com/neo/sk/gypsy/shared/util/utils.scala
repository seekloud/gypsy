package com.neo.sk.gypsy.shared.util

import com.neo.sk.gypsy.shared.ptcl.Game._

import scala.math._
object utils {
//计算速度衰减
  def logSlowDown(n:Double,base:Double) = {
    val a = if (base>1) log(base) else 1
    log(n)/a
  }
//碰撞检测
  def checkCollision(p1:Point,p2:Point,r1:Double,r2:Double,coverRate:Float):Boolean = {
    if(r1>r2 && sqrt(pow(p1.x-p2.x,2) + pow(p1.y - p2.y,2))< r1-r2*coverRate){
      true
    }else{
      false
    }
  }
  //归一化
  def normalization(x:Double,y:Double):(Double,Double)={
    val base = sqrt(pow(x,2)+pow(y,2))
    if(base!=0)
    (x/base,y/base)
    else(0,0)
  }
//计算缩放比例

  def getZoomRate(width:Double,height:Double,newCWidth:Int,newCHeight:Int):Double = {
    var scale = 1.0
    if (width * height >= newCHeight * newCWidth / 4) {
      scale = (newCHeight * newCWidth) / 4 / (width * height)
    } else if (width >= newCWidth / 2 || height >= newCHeight / 2) {
      scale = if (newCWidth / 2 / width > newCHeight / 2 / height) {
        newCHeight / 2 / height
      } else {
        newCWidth / 2 / width
      }
      //      scale = List(300.0/height,600.0/width).min
    }
    scale
  }


//  def getZoomRate(width:Double,height:Double,newCWidth:Int,newCHeight:Int):Double = {
//    var scale = 1.0
//    if(width < newCWidth/2 && height < newCHeight/2){
//
//    }else{
//      scale = if(newCWidth/2/width > newCHeight/2/height){newCHeight/2/height}else{newCWidth/2/width}
////      scale = List(300.0/height,600.0/width).min
//    }
//    scale
//  }


  //检查物体是否在屏幕内
  def checkScreenRange(center:Point, obj:Point, radius:Double, width:Double, height:Double):Boolean ={
    if(obj.x - radius > center.x + width ||
      obj.x + radius < center.x - width ||
      obj.y - radius > center.y + height ||
      obj.y + radius < center.y - height ||
      !checkCollision(center,obj,sqrt(pow(height,2.0)+pow(width,2.0)),radius,-1)
    )
      {false}
    else{
      true
    }
  }


  def MTime2HMS(time:Long)={
    var ts = (time/1000)
    //    println(s"一共有 $ts 秒！")
    var result = ""
    if(ts/3600>0){
      result += s"${ts/3600}小时"
    }
    ts = ts % 3600
    //    println(s"第一次 $ts 秒！")
    if(ts/60>0){
      result += s"${ts/60}分"
    }
    ts = ts % 60
    //    println(s"第二次 $ts 秒！")
    result += s"${ts}秒"
    result
  }

}
