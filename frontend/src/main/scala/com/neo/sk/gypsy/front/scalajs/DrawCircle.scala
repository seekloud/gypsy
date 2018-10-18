package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import org.scalajs
import org.scalajs.dom

object DrawCircle {

  case class Point(x: Double, y: Double)
  var pointList: List[Point] =setLinePoints(9)
  var  phase =Math.random()* Math.PI * 2
  dom.window.setInterval(() => phase= Math.random()* Math.PI * 2, frameRate)

  def drawCircle(ctx: dom.CanvasRenderingContext2D, centerX:Double, centerY:Double, radius:Double): Unit ={
    //val  phase =Math.random()* Math.PI * 2
    var theta=phase
    var x0=centerX+radius*Math.cos(theta)
    var y0=centerY+radius*Math.sin(theta)
    ctx.lineTo(x0,y0)
    pointList.tail.foreach{
      point=>
        theta=2*Math.PI *point.x+phase
        val rad=radius+point.y*3
        x0=centerX+rad*Math.cos(theta)
        y0=centerY+rad*Math.sin(theta)
        ctx.lineTo(x0,y0)
    }
    ctx.lineJoin="round"
    ctx.lineCap="round"
    ctx.stroke()
  }

   def setLinePoints(iterations:Int): List[Point] ={
    var pointList = List[Point]()
    pointList ::= Point(0, 1)
    var point = pointList.head
    var lastPoint = Point(1, 1)
    var minY = 1d
    var maxY = 1d
    pointList :+= lastPoint
    for (i <- 0 until iterations) {
      point = pointList.head
      for (j <- 0 until (pointList.length - 1)) {
        val nextPoint = pointList(pointList.indexOf(point) + 1)
        val dx = nextPoint.x - point.x
        val newX = 0.5 * (point.x + nextPoint.x)
        var newY = 0.5 * (point.y + nextPoint.y)
        newY += dx * (Math.random() * 2 - 1)
        val newPoint = Point(newX, newY)
        if (newY < minY) {
          minY = newY
        } else if (newY > maxY) {
          maxY = newY
        }
        pointList = (pointList.take(pointList.indexOf(point)+ 1) :+ newPoint) ::: pointList.takeRight(pointList.length - 1 - pointList.indexOf(point))
        point = pointList(pointList.indexOf(point) + 2)
      }
    }
    if(maxY!=minY){
      val normalizeRate = 1 / (maxY - minY)
      pointList=pointList.map{
        p=>
          Point(p.x,normalizeRate*(p.y-minY))
      }
      println("pointList"+pointList.size)
    }else{
      pointList=pointList.map{
        p=>
          Point(p.x,1)
      }
    }
    pointList
  }



}
