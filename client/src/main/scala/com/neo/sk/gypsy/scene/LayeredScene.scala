package com.neo.sk.gypsy.scene

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.common.AppSettings
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.text.Font
import com.neo.sk.gypsy.holder.GameHolder._
import com.neo.sk.gypsy.shared.ptcl.{Point, Protocol, WsMsgProtocol}
import com.neo.sk.gypsy.utils.FpsComp


class LayeredScene {
  import GameScene._
  var gameSceneListener: GameSceneListener = _

  val group = new Group()
  //TODO 为什么是1300*900 不是1600 * 900
  val scene = new Scene(group, 1300,900)

//  400*200
  val layerWidth = AppSettings.layerCanvasW
  val layerHeight = AppSettings.layerCanvasH

//  800*400
  val viewWidth = AppSettings.viewCanvasW
  val viewHeight = AppSettings.viewCanvasH

  val layerMapCanvas = new Canvas(layerWidth,layerHeight)
  val layerInfoCanvas = new Canvas(layerWidth,layerHeight)
  val layerBgCanvas = new Canvas(layerWidth,layerHeight)
  val layerAppleCanvas = new Canvas(layerWidth,layerHeight)
  val layerAllSnakesCanvas = new Canvas(layerWidth,layerHeight)
  val layerMySnakeCanvas = new Canvas(layerWidth,layerHeight)

//  人类视图
  val viewCanvas = new Canvas(viewWidth,viewHeight)


  /*
   *  这里几个是设置布局位置
   */
//  TODO 调一调布局
  layerBgCanvas.setLayoutX(800)
  layerBgCanvas.setLayoutY(0)
  layerInfoCanvas.setLayoutX(1200)
  layerInfoCanvas.setLayoutY(0)
  layerMySnakeCanvas.setLayoutX(800)
  layerMySnakeCanvas.setLayoutY(400)
  layerAllSnakesCanvas.setLayoutX(1200)
  layerAllSnakesCanvas.setLayoutY(400)
  layerMapCanvas.setLayoutX(800)
  layerMapCanvas.setLayoutY(600)
  layerAppleCanvas.setLayoutX(1200)
  layerAppleCanvas.setLayoutY(600)

  viewCanvas.setLayoutX(0)
  viewCanvas.setLayoutY(100)

//  把Canvas加进group
  group.getChildren.add(viewCanvas)

  group.getChildren.add(layerMapCanvas)
  group.getChildren.add(layerInfoCanvas)
  group.getChildren.add(layerBgCanvas)
  group.getChildren.add(layerAllSnakesCanvas)
  group.getChildren.add(layerAppleCanvas)
  group.getChildren.add(layerMySnakeCanvas)





  val canvasWidth=1200
  val canvasHeight=600
  val window=Point(canvasWidth,canvasHeight)
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


  group.getChildren.add(gameCanvas)
  group.getChildren.add(middleCanvas)
  group.getChildren.add(topCanvas)

  val gameView=new GameCanvas(gameCanvas,gameCanvasCtx,window)
  val middleView=new GameCanvas(middleCanvas,middleCanvasCtx,window)
  val topView=new GameCanvas(topCanvas,topCanvasCtx,window)

  def resetScreen(viewWidth: Int,viewHeight: Int): Unit = {
    //    //    val viewWidth = 1200//1800
    //    //    val viewHeight = 750//900
    //    //    val rankWidth = 1200//1800
    //    //    val rankHeight = 250//300
    //
    //    rank.resetRankView(rankWidth, rankHeight)
    //    view.resetScreen(viewWidth, viewHeight, rankWidth, rankHeight)

    gameView.resetScreen(viewWidth,viewHeight)
    middleView.resetScreen(viewWidth,viewHeight)
    topView.resetScreen(viewWidth,viewHeight)
    //
    //    rankCanvas.setWidth(rankWidth)
    //    rankCanvas.setHeight(rankHeight)
  }

  def draw(myId:String,offsetTime:Long)={
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
