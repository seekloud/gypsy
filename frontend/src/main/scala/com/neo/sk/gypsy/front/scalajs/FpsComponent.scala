package com.neo.sk.gypsy.front.scalajs


import org.scalajs.dom
import org.scalajs.dom.ext.Color
import com.neo.sk.gypsy.shared.ptcl._

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

   def renderFps(ctx:dom.CanvasRenderingContext2D,ping:Long,newWindowX:Int) = {
    addFps()
    if(isRenderFps){
      ctx.font = s"${ 20 * newWindowX / Window.w }px Helvetica"
      ctx.fillStyle = Color.White.toString()
      val fpsString = "fps : "
      val pingString = "ping: "
      ctx.fillText(fpsString, newWindowX * 0.6, 30)
      ctx.fillText(pingString, newWindowX * 0.6 + ctx.measureText(fpsString).width + 50, 30)
      //fps
      ctx.strokeStyle = "black"
      ctx.strokeText(lastRenderTimes.toString, newWindowX * 0.6 + ctx.measureText(fpsString).width, 30)
      ctx.fillStyle = if (lastRenderTime < 50) Color.Red.toString() else Color.Green.toString()
      ctx.fillText(lastRenderTimes.toString, newWindowX * 0.6 + ctx.measureText(fpsString).width, 30)
      //ping
      ctx.strokeStyle = "black"
      ctx.strokeText(s"${ping}ms", newWindowX * 0.6 + ctx.measureText(fpsString).width + ctx.measureText(pingString).width + 60, 30)
      ctx.fillStyle = if (ping <= 100) Color.Green.toString() else if (ping > 100 && ping <= 200) Color.Yellow.toString() else Color.Red.toString()
      ctx.fillText(s"${ping}ms", newWindowX * 0.6 + ctx.measureText(fpsString).width + ctx.measureText(pingString).width + 60, 30)
    }
  }


}