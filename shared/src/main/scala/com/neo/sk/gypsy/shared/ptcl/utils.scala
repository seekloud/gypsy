package com.neo.sk.gypsy.shared.ptcl

import scala.math._
object utils {

  def logSlowDown(n:Double,base:Double) = {
    val a = if (base>1) log(base) else 1
    log(n)/a
  }

  def checkCollision(p1:Point,p2:Point,r1:Double,r2:Double,coverRate:Float):Boolean = {
    if(r1>r2 && sqrt(pow(p1.x-p2.x,2) + pow(p1.y - p2.y,2))< r1-r2*coverRate){
      true
    }else{
      false
    }
  }
}
