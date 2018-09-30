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

   def renderFps(ctx:dom.CanvasRenderingContext2D,ping:Long) = {
    addFps()
    if(isRenderFps){
      ctx.font = "20px Helvetica"
      ctx.fillStyle = Color.White.toString()
      val fpsString = "fps : "
      val pingString = "ping: "
      ctx.fillText(fpsString, 750, 30)
      ctx.fillText(pingString, 750 + ctx.measureText(fpsString).width + 50, 30)
      ctx.strokeStyle = "black"
      ctx.strokeText(lastRenderTimes.toString, 750 + ctx.measureText(fpsString).width, 30)
      ctx.fillStyle = if (lastRenderTime < 50) Color.Red.toString() else Color.Green.toString()
      ctx.fillText(lastRenderTimes.toString, 750 + ctx.measureText(fpsString).width, 30)
      ctx.strokeStyle = "black"
      ctx.strokeText(s"${ping}ms", 750 + ctx.measureText(fpsString).width + ctx.measureText(pingString).width + 60, 30)
      ctx.fillStyle = if (ping <= 100) Color.Green.toString() else if (ping > 100 && ping <= 200) Color.Yellow.toString() else Color.Red.toString()
      ctx.fillText(s"${ping}ms", 750 + ctx.measureText(fpsString).width + ctx.measureText(pingString).width + 60, 30)
      //      ctx.fillText(s"ping: ${networkLatency}ms",canvasBoundary.x * canvasUnit - ctx.measureText(),(canvasBoundary.y - LittleMap.h - 2) * canvasUnit,10 * canvasUnit)
    }
  }


}