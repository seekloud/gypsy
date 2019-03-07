package com.neo.sk.gypsy.scene

import java.util.concurrent.atomic.AtomicInteger
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.text.Font
import javafx.scene.layout._
import com.neo.sk.gypsy.holder.GameHolder._
import com.neo.sk.gypsy.utils.FpsComp
import com.neo.sk.gypsy.common.Constant._

import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._

class LayeredScene {
  import GameScene._
  var gameSceneListener: GameSceneListener = _


  /**人类视图**/
  val humanWindow = Point(humanCanvasWidth,humanCanvasHeight)
  val humanCanvas = new Canvas(humanCanvasWidth,humanCanvasHeight)
  val humanCtx = humanCanvas.getGraphicsContext2D
  val humanView = new GameCanvas(humanCanvas,humanCtx,humanWindow)

  val actionSerialNumGenerator = new AtomicInteger(0)

  /**分层视图：8个**/
  val layerWindow = Point(layeredCanvasWidth,layeredCanvasHeight)
  val locationCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //01视野在地图中的位置
  val locationCanvasCtx = locationCanvas.getGraphicsContext2D
  val locationView = new GameCanvas(locationCanvas,locationCanvasCtx,layerWindow)
  val nonInteractCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //02视野中不可交互的元素（背景）
  val nonInteractCanvasCtx = nonInteractCanvas.getGraphicsContext2D
  val nonInteractView = new GameCanvas(nonInteractCanvas,nonInteractCanvasCtx,layerWindow)
  val interactCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)//03视野内可交互的元素
  val interactCanvasCtx = interactCanvas.getGraphicsContext2D
  val interactView = new GameCanvas(interactCanvas,interactCanvasCtx,layerWindow)
  val kernelCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //04视野内的玩家实体
  val kernelCanvasCtx = kernelCanvas.getGraphicsContext2D
  val kernelView = new GameCanvas(kernelCanvas,kernelCanvasCtx,layerWindow)
  val allPlayerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //05视野内包括自己的所有玩家
  val allPlayerCanvasCtx = allPlayerCanvas.getGraphicsContext2D
  val allPlayerView = new GameCanvas(allPlayerCanvas,allPlayerCanvasCtx,layerWindow)
  val playerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)    //06玩家自己
  val playerCanvasCtx = playerCanvas.getGraphicsContext2D
  val playerView = new GameCanvas(playerCanvas,playerCanvasCtx,layerWindow)
  val pointerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //07鼠标指针位置
  val pointerCanvasCtx = pointerCanvas.getGraphicsContext2D
  val pointerView = new GameCanvas(pointerCanvas,pointerCanvasCtx,layerWindow)
  val informCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //08当前用户状态视图
  val informCanvasCtx = informCanvas.getGraphicsContext2D
  val informView = new GameCanvas(informCanvas,informCanvasCtx,layerWindow)





  /**javafx的布局**/
//  val flow = new FlowPane()
//  flow.setPrefWrapLength(800) // 预设FlowPane的宽度，使其能够显示两列

  //设置行列之间的间隙大小
//  flow.setVgap(5)
//  flow.setHgap(5)
//  flow.getChildren.addAll(locationCanvas,nonInteractCanvas,interactCanvas,allPlayerCanvas,playerCanvas,informCanvas)

  //  val group = new Group()
//  group.getChildren.addAll(humanCanvas)
//  val border = new BorderPane()
//  border.setCenter(group)
//  border.setRight(flow)
//  val scene = new Scene(border)

  val group = new Group()
//  val scene = new Scene(group,2000,800)
  val scene = new Scene(group)
  humanCanvas.setLayoutX(0)
//  humanCanvas.setLayoutY(100)
  humanCanvas.setLayoutY(0)
  group.getChildren.add(humanCanvas)


  val canvasSpace = 5

//  locationCanvas.setLayoutX(805)
//  locationCanvas.setLayoutY(0)
//  nonInteractCanvas.setLayoutX(1210)
//  nonInteractCanvas.setLayoutY(0)
//  interactCanvas.setLayoutX(805)
//  interactCanvas.setLayoutY(205)
//  allPlayerCanvas.setLayoutX(1210)
//  allPlayerCanvas.setLayoutY(205)
//  playerCanvas.setLayoutX(805)
//  playerCanvas.setLayoutY(410)
//  informCanvas.setLayoutX(1210)
//  informCanvas.setLayoutY(410)


  //设置分层视图
  locationCanvas.setLayoutX(humanCanvasWidth+canvasSpace) //01视野在整个地图中的位置
  locationCanvas.setLayoutY(0)
  nonInteractCanvas.setLayoutX(humanCanvasWidth+layeredCanvasWidth+2*canvasSpace) //02视野内的不可变元素
  nonInteractCanvas.setLayoutY(0)
  interactCanvas.setLayoutX(humanCanvasWidth+canvasSpace)  //03视野内的可变元素
  interactCanvas.setLayoutY(layeredCanvasHeight+canvasSpace)
  kernelCanvas.setLayoutX(humanCanvasWidth+layeredCanvasWidth+2*canvasSpace) //04视野内的玩家实体
  kernelCanvas.setLayoutY(layeredCanvasHeight+canvasSpace)
  allPlayerCanvas.setLayoutX(humanCanvasWidth+canvasSpace)  //05视野内的所有权视图
  allPlayerCanvas.setLayoutY(layeredCanvasHeight*2+canvasSpace*2)
  playerCanvas.setLayoutX(humanCanvasWidth+layeredCanvasWidth+canvasSpace*2) //06视野内的当前玩家资产视图
  playerCanvas.setLayoutY(layeredCanvasHeight*2+canvasSpace*2)
  pointerCanvas.setLayoutX(0)                               //07鼠标指针位置
  pointerCanvas.setLayoutY(humanCanvasHeight+canvasSpace)
  informCanvas.setLayoutX(layeredCanvasWidth+canvasSpace)   //08当前用户状态视图
  informCanvas.setLayoutY(layeredCanvasHeight+canvasSpace)


  group.getChildren.addAll(locationCanvas,nonInteractCanvas,interactCanvas,kernelCanvas,
    allPlayerCanvas,playerCanvas,pointerCanvas,informCanvas)

  def resetScreen(viewWidth: Int,viewHeight: Int): Unit = {
    //TODO
    /**人类视图**/
//    gameView.resetScreen(viewWidth/2,viewHeight * 2 / 3)
//    middleView.resetScreen(viewWidth/2,viewHeight * 2 / 3)
//    topView.resetScreen(viewWidth/2,viewHeight * 2 / 3)
    /**分层视图**/
    locationView.resetScreen(viewWidth/4,viewHeight/3)
    nonInteractView.resetScreen(viewWidth/4,viewHeight/3)
    interactView.resetScreen(viewWidth/4,viewHeight/3)
    kernelView.resetScreen(viewWidth/4,viewHeight/3)
    allPlayerView.resetScreen(viewWidth/4,viewHeight/3)
    playerView.resetScreen(viewWidth/4,viewHeight/3)
    pointerView.resetScreen(viewWidth/4,viewHeight/3)
    informView.resetScreen(viewWidth/4,viewHeight/3)
  }

/*  def draw(myId:String,offsetTime:Long)={
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
  }*/

/*  def drawWhenDead(msg:Protocol.UserDeadMessage) = {
    topView.drawWhenDead(msg)
  }
  def drawWhenFinish(msg:String) = {
    topView.drawWhenFinish(msg)
  }

  topCanvas.requestFocus()
  topCanvas.setOnKeyPressed(event => gameSceneListener.onKeyPressed(event.getCode))
  topCanvas.setOnMouseMoved(event => gameSceneListener.OnMouseMoved(event))*/


  def setGameSceneListener(listener: GameSceneListener) {
    gameSceneListener = listener
  }
}
