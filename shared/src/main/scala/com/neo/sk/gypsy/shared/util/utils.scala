package com.neo.sk.gypsy.shared.util

import com.neo.sk.gypsy.shared.ptcl.Point

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
  def getZoomRate(width:Double,height:Double):Double = {
    var scale = 1.0
    if(width < 600 && height < 300){
    }else{
      scale = List(300.0/height,600.0/width).min
    }
    scale
  }
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
}
