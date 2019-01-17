package com.neo.sk.gypsy.scene

import java.util.concurrent.atomic.AtomicInteger

import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.scene.text.Font
import com.neo.sk.gypsy.holder.GameHolder._
import com.neo.sk.gypsy.utils.FpsComp
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import scalafx.scene.text.Text

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
  var canvasWidth=1200
  var canvasHeight=600
  var bigPlayerMass = 500.0
  val window=Point(canvasWidth,canvasHeight)
  val group = new Group()
  val gameCanvas = new Canvas(canvasWidth,canvasHeight)
  val gameCanvasCtx=gameCanvas.getGraphicsContext2D
  val middleCanvas = new Canvas(canvasWidth,canvasHeight)
  val middleCanvasCtx=middleCanvas.getGraphicsContext2D
  val topCanvas = new Canvas(canvasWidth,canvasHeight)
  val topCanvasCtx=topCanvas.getGraphicsContext2D
  val actionSerialNumGenerator = new AtomicInteger(0)

//  offCanvas.setStyle("z-index: 1")
  gameCanvas.setStyle("z-index: 1")
  middleCanvas.setStyle("z-index: 2")
  topCanvas.setStyle("z-index: 3")


  val scene = new Scene(group)
  group.getChildren.add(gameCanvas)
  group.getChildren.add(middleCanvas)
  group.getChildren.add(topCanvas)

  val gameView=new GameCanvas(gameCanvas,gameCanvasCtx,window)
  val middleView=new GameCanvas(middleCanvas,middleCanvasCtx,window)
  val topView=new GameCanvas(topCanvas,topCanvasCtx,window)

  def resetScreen(viewWidth: Int,viewHeight: Int): Unit = {
    gameView.resetScreen(viewWidth,viewHeight)
    middleView.resetScreen(viewWidth,viewHeight)
    topView.resetScreen(viewWidth,viewHeight)
    canvasWidth = viewWidth
    canvasHeight = viewHeight
  }

  def draw(myId:String,offsetTime:Long)={
    var zoom = (30.0, 30.0)
    val data = grid.getGridData(myId,canvasWidth,canvasHeight)
    val bigPlayerPosition=grid.playerMap.values.toList.filter(i=>i.cells.map(_.newmass).sum>bigPlayerMass).map(i=>PlayerPosition(i.id,i.x.toShort,i.y.toShort))
    data.playerDetails.find(_.id == myId) match {
      case Some(p) =>
        firstCome=false
        var sumX = 0.0
        var sumY = 0.0
        var xMax = 0.0
        var xMin = 10000.0
        var yMin = 10000.0
        var yMax = 0.0
        var kill = ""
        var Score = ""
        //zoom = (p.cells.map(a => a.x+a.radius).max - p.cells.map(a => a.x-a.radius).min, p.cells.map(a => a.y+a.radius).max - p.cells.map(a => a.y-a.radius).min)
        p.cells.foreach { cell =>
          val offx = cell.speedX * offsetTime.toDouble / frameRate
          val offy = cell.speedY * offsetTime.toDouble / frameRate
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
//        val foods=grid.food
        gameView.drawGrid(myId,data,offsetTime,basePoint,zoom,grid)
        topView.drawRankMapData(myId,grid.currentRank,data.playerDetails,basePoint,bigPlayerPosition)
        gameCanvasCtx.save()
        gameCanvasCtx.setFont(Font.font(" Helvetica",24))
        kill=s"KILL: ${p.kill}"
        Score=s"SCORE: ${p.cells.map(_.newmass).sum.toInt}"
        val txt=new Text(kill)
        val killWidth=txt.getLayoutBounds.getWidth
        gameCanvasCtx.fillText(kill, 250, 10)
        gameCanvasCtx.fillText(Score, 300+killWidth, 10)
        gameCanvasCtx.restore()
 //       renderFps(topCanvas,NetDelay.latency)
        //todo 解决返回值问题
        val paraBack = gameView.drawKill(myId,grid,isDead,killList)
        killList=paraBack._1
        isDead=paraBack._2
      case None =>
        gameView.drawGameWait(firstCome)
    }
    FpsComp.renderFps(gameCanvasCtx, 550, 10)
  }

  def drawWhenDead(msg:Protocol.UserDeadMessage) = {
    topView.drawWhenDead(msg)
  }
  def drawWhenFinish(msg:String) = {
    topView.drawWhenFinish(msg)
  }

  topCanvas.requestFocus()
  topCanvas.setOnKeyPressed(event => gameSceneListener.onKeyPressed(event.getCode))
  topCanvas.setOnMouseMoved(event => gameSceneListener.OnMouseMoved(event))


  def setGameSceneListener(listener: GameSceneListener) {
    gameSceneListener = listener
  }

}
