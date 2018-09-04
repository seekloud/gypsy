package com.neo.sk.gypsy.front.scalajs


import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * Created by hongruying on 2018/8/29
  */
object FpsComponent{
  private var lastRenderTime = System.currentTimeMillis()
  private var lastRenderTimes = 0

  private var renderTimes = 0

  private val isRenderFps:Boolean = true

  private def addFps() ={
    val time = System.currentTimeMillis()
    renderTimes += 1
    if(time - lastRenderTime > 1000){
      lastRenderTime = time
      lastRenderTimes = renderTimes
      renderTimes = 0
    }
  }

   def renderFps(ctx:dom.CanvasRenderingContext2D) = {
    addFps()
    if(isRenderFps){
      ctx.font = "20px Helvetica"
      //ctx.textAlign = "start"
      ctx.fillStyle = Color.Black.toString()
      val fpsString = s"fps : $lastRenderTimes"
      ctx.fillText(fpsString,750,30)
      //      ctx.fillText(s"ping: ${networkLatency}ms",canvasBoundary.x * canvasUnit - ctx.measureText(),(canvasBoundary.y - LittleMap.h - 2) * canvasUnit,10 * canvasUnit)
    }
  }


}