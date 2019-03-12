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
import javafx.scene.input.{KeyCode, MouseEvent}

object LayeredScene {
  trait LayeredSceneListener {
    def onKeyPressed(e: KeyCode): Unit
    def OnMouseMoved(e: MouseEvent):Unit
  }

}
class LayeredScene {
  import LayeredScene._
  var layeredSceneListener: LayeredSceneListener = _


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

  val group = new Group()
  val scene = new Scene(group)
  humanCanvas.setLayoutX(0)
  humanCanvas.setLayoutY(0)
  group.getChildren.add(humanCanvas)


  val canvasSpace = 5

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
  informCanvas.setLayoutY(humanCanvasHeight+canvasSpace)


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


  def setLayeredSceneListener(listener: LayeredSceneListener) {
    layeredSceneListener = listener
  }
}
