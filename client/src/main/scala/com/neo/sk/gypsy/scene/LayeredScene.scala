package com.neo.sk.gypsy.scene

import java.util.concurrent.atomic.AtomicInteger
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.text.Font
import javafx.scene.layout._
import com.neo.sk.gypsy.holder.BotHolder._
import com.neo.sk.gypsy.utils.{BotUtil, FpsComp}
import com.neo.sk.gypsy.common.Constant._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.util.Utils.getZoomRate
import javafx.scene.input.{KeyCode, MouseEvent}

object LayeredScene {
  trait LayeredSceneListener {
    def onKeyPressed(e: KeyCode): Unit
    def OnMouseMoved(e: MouseEvent):Unit
  }

}
class LayeredScene(
                    is2Byte:Boolean
                  ) {
  import LayeredScene._
  var layeredSceneListener: LayeredSceneListener = _

  /**人类视图**/
  val humanWindow = Point(humanCanvasWidth,humanCanvasHeight)
  val humanCanvas = new Canvas(humanCanvasWidth,humanCanvasHeight)
  val humanCtx = humanCanvas.getGraphicsContext2D
  val humanView = new LayeredCanvas(humanCanvas,humanCtx,humanWindow,is2Byte)

  val actionSerialNumGenerator = new AtomicInteger(0)

  /**分层视图：8个, 状态显示log: 1个**/
  val layerWindow = Point(layeredCanvasWidth,layeredCanvasHeight)
  val locationCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //01视野在地图中的位置
  val locationCanvasCtx = locationCanvas.getGraphicsContext2D
  val locationView = new LayeredCanvas(locationCanvas,locationCanvasCtx,layerWindow,is2Byte)
  val nonInteractCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //02视野中不可交互的元素（背景）
  val nonInteractCanvasCtx = nonInteractCanvas.getGraphicsContext2D
  val nonInteractView = new LayeredCanvas(nonInteractCanvas,nonInteractCanvasCtx,layerWindow,is2Byte)
  val interactCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)//03视野内可交互的元素
  val interactCanvasCtx = interactCanvas.getGraphicsContext2D
  val interactView = new LayeredCanvas(interactCanvas,interactCanvasCtx,layerWindow,is2Byte)
  val kernelCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //04视野内的玩家实体
  val kernelCanvasCtx = kernelCanvas.getGraphicsContext2D
  val kernelView = new LayeredCanvas(kernelCanvas,kernelCanvasCtx,layerWindow,is2Byte)
  val allPlayerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //05视野内包括自己的所有玩家
  val allPlayerCanvasCtx = allPlayerCanvas.getGraphicsContext2D
  val allPlayerView = new LayeredCanvas(allPlayerCanvas,allPlayerCanvasCtx,layerWindow,is2Byte)
  val playerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)    //06玩家自己
  val playerCanvasCtx = playerCanvas.getGraphicsContext2D
  val playerView = new LayeredCanvas(playerCanvas,playerCanvasCtx,layerWindow,is2Byte)
  val pointerCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight)  //07鼠标指针位置
  val pointerCanvasCtx = pointerCanvas.getGraphicsContext2D
  val pointerView = new LayeredCanvas(pointerCanvas,pointerCanvasCtx,layerWindow,is2Byte)
  val informCanvas = new Canvas(layeredCanvasWidth,layeredCanvasHeight) //08当前用户状态视图
  val informCanvasCtx = informCanvas.getGraphicsContext2D
  val informView = new LayeredCanvas(informCanvas,informCanvasCtx,layerWindow,is2Byte)

  val actionLogWindow = Point(actionLogCanvasWidth,actionLogCanvasHeight)
  val actionLogCanvas = new Canvas(actionLogCanvasWidth,actionLogCanvasHeight) //09状态log
  val actionLogCanvasCtx = actionLogCanvas.getGraphicsContext2D
  val actionLogView = new ActionLogCanvas(actionLogCanvas,actionLogCanvasCtx,actionLogWindow)


  /**javafx的布局**/

  val group = new Group()
  val scene = new Scene(group)
  humanCanvas.setLayoutX(0)
  humanCanvas.setLayoutY(0)
  group.getChildren.add(humanCanvas)


  val canvasSpace = 3

  //设置分层视图
  locationCanvas.setLayoutX(humanCanvasWidth + 2 * canvasSpace) //01视野在整个地图中的位置
  locationCanvas.setLayoutY(0)

  nonInteractCanvas.setLayoutX(humanCanvasWidth + 3 * canvasSpace + layeredCanvasWidth) //02视野内的不可变元素
  nonInteractCanvas.setLayoutY(0)

  interactCanvas.setLayoutX(humanCanvasWidth + 2 * canvasSpace)  //03视野内的可变元素
  interactCanvas.setLayoutY(canvasSpace + layeredCanvasHeight)

  kernelCanvas.setLayoutX(humanCanvasWidth + 3 * canvasSpace + layeredCanvasWidth) //04视野内的玩家实体
  kernelCanvas.setLayoutY(canvasSpace + layeredCanvasHeight)

  allPlayerCanvas.setLayoutX(humanCanvasWidth + 2 * canvasSpace)  //05视野内的所有权视图
  allPlayerCanvas.setLayoutY(2 * canvasSpace + 2 * layeredCanvasHeight)

  playerCanvas.setLayoutX(humanCanvasWidth + 3 * canvasSpace + layeredCanvasWidth) //06视野内的当前玩家资产视图
  playerCanvas.setLayoutY(2 * canvasSpace + 2 * layeredCanvasHeight)

  pointerCanvas.setLayoutX(humanCanvasWidth + 2 * canvasSpace)//07鼠标指针位置
  pointerCanvas.setLayoutY(3 * canvasSpace + 3 * layeredCanvasHeight)

  informCanvas.setLayoutX(humanCanvasWidth + 3 * canvasSpace + layeredCanvasWidth)   //08当前用户状态视图
  informCanvas.setLayoutY(3 * canvasSpace + 3 * layeredCanvasHeight)

  actionLogCanvas.setLayoutX(0)   //09 actionLog
  actionLogCanvas.setLayoutY(humanCanvasHeight + 1 * canvasSpace)


  group.getChildren.addAll(locationCanvas,nonInteractCanvas,interactCanvas,kernelCanvas,
    allPlayerCanvas,playerCanvas,pointerCanvas,informCanvas,actionLogCanvas)

  def drawLayered() = {
    var zoom = (30.0, 30.0)
    var humanReturn = (new Array[Byte](0),1.toDouble)
    var localByte = new Array[Byte](0)
    var noninteractByte = new Array[Byte](0)
    var interactByte = new Array[Byte](0)
    var kernelByte = new Array[Byte](0)
    var allplayerByte = new Array[Byte](0)
    var playerByte = new Array[Byte](0)
    var pointerByte = new Array[Byte](0)
    var infoByte = new Array[Byte](0)
    //TODO 这里其他数据设置为空
    gameState match {
      case GameState.play if grid.myId != ""=>

        actionLogView.addFrameCountActionLog(grid.actionMap.get(grid.frameCount).flatMap(t => t.get(grid.myId)))
        actionLogView.addFrameCountActionLog(grid.mouseActionMap.get(grid.frameCount).flatMap(t => t.get(grid.myId)))
        actionLogView.drawLog()

        //screenScale为0.75，即从(800,400)->(1200,600)
        val data = grid.getGridData(grid.myId,humanCanvasWidth,humanCanvasHeight,0.75)
        data.playerDetails.find(_.id == grid.myId) match {
          case Some(p) =>
            firstCome=false
            //TODO zoom是否正确
            zoom = (p.cells.map(a => a.x+a.radius).max - p.cells.map(a => a.x-a.radius).min, p.cells.map(a => a.y+a.radius).max - p.cells.map(a => a.y-a.radius).min)
            val basePoint = (p.x.toDouble, p.y.toDouble)
            humanReturn = humanView.drawPlayState(data,basePoint,zoom)
            localByte = locationView.drawLocation(basePoint)
            noninteractByte = nonInteractView.drawNonInteract(basePoint,humanReturn._2)
            interactByte = interactView.drawInteract(data,basePoint,humanReturn._2)
            kernelByte = kernelView.drawKernel(data,basePoint,humanReturn._2)
            allplayerByte = allPlayerView.drawAllPlayer(data,basePoint,humanReturn._2)
            playerByte = playerView.drawPlayer(data,basePoint,humanReturn._2)
            pointerByte = pointerView.drawPointer(grid.mouseActionMap,basePoint,humanReturn._2)
            infoByte = informView.drawInform(Some(data))
          //TODO 显示击杀弹幕
          case None =>
            humanView.drawGameWait(firstCome)
        }
      case GameState.dead if deadInfo.isDefined =>
        humanReturn = (humanView.drawDeadState(deadInfo.get),1.toDouble)
        infoByte = informView.drawInform(None)

      case GameState.victory if victoryInfo.isDefined=>
        humanReturn = (humanView.drawVictory(victoryInfo.get),1.toDouble)

      case GameState.allopatry =>
        humanReturn = (humanView.drawFinishState("存在异地登录"),1.toDouble)

      case GameState.passwordError =>
        humanReturn = (humanView.drawFinishState("房间密码错误"),1.toDouble)

      case _ =>
        (BotUtil.emptyArray,1.toDouble)
    }


    (localByte,noninteractByte,interactByte,kernelByte,allplayerByte,playerByte,pointerByte,infoByte,humanReturn._1)
  }


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

  humanCanvas.requestFocus()
  humanCanvas.setOnKeyPressed(event => layeredSceneListener.onKeyPressed(event.getCode))
  humanCanvas.setOnMouseMoved(event => layeredSceneListener.OnMouseMoved(event))

  def setLayeredSceneListener(listener: LayeredSceneListener) {
    layeredSceneListener = listener
  }
}
