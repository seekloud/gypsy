package com.neo.sk.gypsy.scene

import java.util.concurrent.atomic.AtomicInteger
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.text.Font
import javafx.scene.layout._
import com.neo.sk.gypsy.holder.GameHolder._
import com.neo.sk.gypsy.shared.ptcl.{Point, Protocol, WsMsgProtocol}
import com.neo.sk.gypsy.utils.FpsComp
import com.neo.sk.gypsy.common.Constant._
class LayeredScene {
  import GameScene._
  var gameSceneListener: GameSceneListener = _
  val canvasWidth=1200
  val canvasHeight=600
  val window=Point(canvasWidth,canvasHeight)

  /**人类视图**/
  val humanWindow = Point(humanCanvasWidth,humanCanvasHeight)
  val gameCanvas = new Canvas(humanCanvasWidth,humanCanvasHeight)
  val gameCanvasCtx=gameCanvas.getGraphicsContext2D
  val middleCanvas = new Canvas(humanCanvasWidth,humanCanvasHeight)
  val middleCanvasCtx=middleCanvas.getGraphicsContext2D
  val topCanvas = new Canvas(humanCanvasWidth,humanCanvasHeight)
  val topCanvasCtx=topCanvas.getGraphicsContext2D
  val actionSerialNumGenerator = new AtomicInteger(0)
  val gameView=new GameCanvas(gameCanvas,gameCanvasCtx,humanWindow)
  val middleView=new GameCanvas(middleCanvas,middleCanvasCtx,humanWindow)
  val topView=new GameCanvas(topCanvas,topCanvasCtx,humanWindow)
  gameCanvas.setStyle("z-index: 1")
  middleCanvas.setStyle("z-index: 2")
  topCanvas.setStyle("z-index: 3")
  /**分层视图**/
  val layerWindow = Point(layeredCanvasWidth,layeredCanvasHeight)
  val locationCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //01视野在地图中的位置
  val locationCanvasCtx = locationCanvas.getGraphicsContext2D
  val locationView = new GameCanvas(locationCanvas,locationCanvasCtx,layerWindow)
  val nonInteracCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //02视野中不可交互的元素（背景）
  val nonInteracCanvasCtx = nonInteracCanvas.getGraphicsContext2D
  val nonInteracView = new GameCanvas(nonInteracCanvas,nonInteracCanvasCtx,layerWindow)
  val interactCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)//03视野内可交互的元素
  val interactCanvasCtx = interactCanvas.getGraphicsContext2D
  val interactView = new GameCanvas(interactCanvas,interactCanvasCtx,layerWindow)
  val allPlayerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //04视野内包括自己的所有玩家
  val allPlayerCanvasCtx = allPlayerCanvas.getGraphicsContext2D
  val allPlayerView = new GameCanvas(allPlayerCanvas,allPlayerCanvasCtx,layerWindow)
  val playerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)    //05玩家自己
  val playerCanvasCtx = playerCanvas.getGraphicsContext2D
  val playerView = new GameCanvas(playerCanvas,playerCanvasCtx,layerWindow)
  val informCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //06面板状态信息
  val informCanvasCtx = informCanvas.getGraphicsContext2D
  val informView = new GameCanvas(informCanvas,interactCanvasCtx,layerWindow)

  /**javafx的布局**/
  val flow = new FlowPane()
  flow.setPrefWrapLength(400) // 预设FlowPane的宽度，使其能够显示两列
  flow.setVgap(5)
  flow.setHgap(5)
  flow.getChildren.addAll(locationCanvas,nonInteracCanvas,interactCanvas,allPlayerCanvas,playerCanvas,informCanvas)
  val group = new Group()
  group.getChildren.addAll(gameCanvas,middleCanvas,topCanvas)
  val border = new BorderPane()
  border.setCenter(group)
  border.setRight(flow)
  val scene = new Scene(border)
//  group.getChildren.add(gameCanvas)
//  group.getChildren.add(middleCanvas)
//  group.getChildren.add(topCanvas)

  def resetScreen(viewWidth: Int,viewHeight: Int): Unit = {
    //TODO
    /**人类视图**/
    gameView.resetScreen(viewWidth/2,viewHeight * 2 / 3)
    middleView.resetScreen(viewWidth/2,viewHeight * 2 / 3)
    topView.resetScreen(viewWidth/2,viewHeight * 2 / 3)
    /**分层视图**/
    locationView.resetScreen(viewWidth/4,viewHeight/3)
    nonInteracView.resetScreen(viewWidth/4,viewHeight/3)
    interactView.resetScreen(viewWidth/4,viewHeight/3)
    allPlayerView.resetScreen(viewWidth/4,viewHeight/3)
    playerView.resetScreen(viewWidth/4,viewHeight/3)
    informView.resetScreen(viewWidth/4,viewHeight/3)
  }

  def draw(myId:String,offsetTime:Long)={
    var zoom = (30.0, 30.0)
    val data = grid.getGridData(myId,1200,600)
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
        gameView.drawGrid(myId,data,foods,offsetTime,firstCome,basePoint,zoom,grid)
        topView.drawRankMapData(myId,grid.currentRank,data.playerDetails,basePoint,data.playersPosition)
        gameCanvasCtx.save()
        gameCanvasCtx.setFont(Font.font(" Helvetica",24))
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
