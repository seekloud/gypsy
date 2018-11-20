package com.neo.sk.gypsy.scene

import java.util.concurrent.atomic.AtomicInteger
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.scene.text.Font

import com.neo.sk.gypsy.shared.ptcl.{Point, WsMsgProtocol}
import com.neo.sk.gypsy.holder.GameHolder._
import com.neo.sk.gypsy.shared.ptcl.Protocol._

import javafx.scene.image.Image


/**
  * @author zhaoyin
  * 2018/10/30  2:25 PM
  */
object GameScene {
  trait GameSceneListener {
    def onKeyPressed(e: KeyCode): Unit
    def OnMouseMoved(e: MouseEvent):Unit
  }

}
class GameScene {
  import GameScene._
  var gameSceneListener: GameSceneListener = _
  val canvasWidth=1200
  val canvasHeight=600
//  val boundaryWidth=4800
//  val boundaryHeight=2400
  val window=Point(canvasWidth,canvasHeight)
//  val bounds=Point(boundaryWidth,boundaryHeight)
  val group = new Group()
  val gameCanvas = new Canvas(canvasWidth,canvasHeight)
  val gameCanvasCtx=gameCanvas.getGraphicsContext2D
  val middleCanvas = new Canvas(canvasWidth,canvasHeight)
  val middleCanvasCtx=middleCanvas.getGraphicsContext2D
  val topCanvas = new Canvas(canvasWidth,canvasHeight)
  val topCanvasCtx=topCanvas.getGraphicsContext2D
//  val clockCanvas = new Canvas(canvasWidth,canvasHeight)
//  val clockCanvasCtx=clockCanvas.getGraphicsContext2D
  val offCanvas = new Canvas(canvasWidth,canvasHeight)
  val offCanvasCtx= offCanvas.getGraphicsContext2D
  val actionSerialNumGenerator = new AtomicInteger(0)

//  offCanvas.setStyle("z-index: 1")
  gameCanvas.setStyle("z-index: 2")
  middleCanvas.setStyle("z-index: 3")
  topCanvas.setStyle("z-index: 4")


  val scene = new Scene(group)
  group.getChildren.add(gameCanvas)
  group.getChildren.add(middleCanvas)
  group.getChildren.add(topCanvas)
//  group.getChildren.add(clockCanvas)
//  group.getChildren.add(offCanvas)

  val gameView=new GameCanvas(gameCanvas,gameCanvasCtx,window)
  val middleView=new GameCanvas(middleCanvas,middleCanvasCtx,window)
  val topView=new GameCanvas(topCanvas,topCanvasCtx,window)
//  val clockView=new GameCanvas(clockCanvas,clockCanvasCtx,window)
  val offView=new GameCanvas(offCanvas,offCanvasCtx,window)

  def draw(myId:String,offsetTime:Long,offCanvasImgae:Image)={
    var zoom = (30.0, 30.0)
    val data = grid.getGridData(myId,window.x,window.y)
    data.playerDetails.find(_.id == myId) match {
      case Some(p) =>
        firstCome=false
        var sumX = 0.0
        var sumY = 0.0
        var xMax = 0.0
        var xMin = 10000.0
        var yMin = 10000.0
        var yMax = 0.0
        //zoom = (p.cells.map(a => a.x+a.radius).max - p.cells.map(a => a.x-a.radius).min, p.cells.map(a => a.y+a.radius).max - p.cells.map(a => a.y-a.radius).min)
        p.cells.foreach { cell =>
          val offx = cell.speedX * offsetTime.toDouble / WsMsgProtocol.frameRate
          val offy = cell.speedY * offsetTime.toDouble / WsMsgProtocol.frameRate
          val newX = if ((cell.x + offx) > bounds.x-15) bounds.x-15 else if ((cell.x + offx) <= 15) 15 else cell.x + offx
          val newY = if ((cell.y + offy) > bounds.y-15) bounds.y-15 else if ((cell.y + offy) <= 15) 15 else cell.y + offy
          if (newX>xMax) xMax=newX
          if (newX<xMin) xMin=newX
          if (newY>yMax) yMax=newY
          if (newY<yMin) yMin=newY
          zoom=(xMax-xMin+2*cell.radius,yMax-yMin+2*cell.radius)
          sumX += newX
          sumY += newY
        }
        val offx = sumX /p.cells.length
        val offy = sumY /p.cells.length
        val basePoint = (offx, offy)
        val foods=grid.food
        gameView.drawGrid(myId,data,foods,offsetTime,firstCome,basePoint,zoom,offCanvasImgae)
        topView.drawRankMapData(myId,grid.currentRank,data.playerDetails,basePoint)
        gameCanvasCtx.save()
        gameCanvasCtx.setFont(Font.font("34px Helvetica"))
        gameCanvasCtx.fillText(s"KILL: ${p.kill}", 250, 10)
        gameCanvasCtx.fillText(s"SCORE: ${p.cells.map(_.mass).sum.toInt}", 400, 10)
        gameCanvasCtx.restore()
        //TODO 绘制fps值
 //       renderFps(topCanvas,NetDelay.latency)
        //todo 解决返回值问题
        val paraBack = gameView.drawKill(myId,grid,isDead,killList)
        killList=paraBack._1
        isDead=paraBack._2
      case None =>
        gameView.drawGameWait(firstCome)
    }
  }

  topCanvas.requestFocus()
  topCanvas.setOnKeyPressed(event => gameSceneListener.onKeyPressed(event.getCode))
  topCanvas.setOnMouseMoved(event => gameSceneListener.OnMouseMoved(event))


  def setGameSceneListener(listener: GameSceneListener) {
    gameSceneListener = listener
  }

}
