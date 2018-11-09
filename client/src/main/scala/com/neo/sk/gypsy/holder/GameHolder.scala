package com.neo.sk.gypsy.holder

import com.neo.sk.gypsy.ClientBoot
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}

import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.model.GridOnClient
import javafx.scene.input.KeyCode
import javafx.util.Duration

import com.neo.sk.gypsy.scene.GameScene
import com.neo.sk.gypsy.shared.ptcl.Protocol.{GridDataSync, MousePosition}
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.GameScene
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.GameClient.myId
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode

import scala.math.atan2

/**
  * @author zhaoyin
  * @date 2018/10/29  5:13 PM
  */
object GameHolder {

  val bounds = Point(Boundary.w,Boundary.h)
  val grid = new GridOnClient(bounds)
  var justSynced = false
  var isDead = false
  var firstCome=true
  var logicFrameTime = System.currentTimeMillis()
  var syncGridData: scala.Option[GridDataSync] = None
  var killList = List.empty[(Int,String,Player)]

  private[this] val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.SPACE,
    KeyCode.LEFT,
    KeyCode.UP,
    KeyCode.RIGHT,
    KeyCode.DOWN,
    KeyCode.ESCAPE
  )

}
class GameHolder(
                  stageCtx: StageContext,
                 gameScene: GameScene,
                ) {
  import GameHolder._

  def connectToGameServer(gameHolder:GameHolder) = {
    ClientBoot.addToPlatform{
      stageCtx.showScene(gameScene.scene,"Gaming")
      gameClient ! Protocol.ControllerInitial()
      start()
    }
  }

  def start()={
    println("start---")
    val animationTimer = new AnimationTimer() {
      override def handle(now: Long): Unit = {
        //todo 游戏渲染
        val data=grid.getGridData(myId)
        val offsetTime=System.currentTimeMillis()-logicFrameTime
        gameScene.draw(myId,data,offsetTime)
      }
    }
    val timeline = new Timeline()
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(100),{ _ =>
      //todo 游戏循环
      addActionListenEvent
      gameLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    animationTimer.start()
    timeline.play()
  }

  def gameLoop(): Unit = {
  //  NetDelay.ping(webSocketClient)
    logicFrameTime = System.currentTimeMillis()
      //差不多每三秒同步一次
      //不同步
      if (!justSynced) {
        update()
      } else {
        //        println("back")
        if (syncGridData.nonEmpty) {
          //同步
          grid.setSyncGridData(syncGridData.get)
          syncGridData = None
        }
        justSynced = false
      }
  }

  def update(): Unit = {
    grid.update()
  }

  def addActionListenEvent = {
    gameScene.topCanvas.requestFocus()
    //在画布上监听键盘事件
    gameScene.topCanvas.setOnKeyPressed{ event => {
        val key=event.getCode
        if (key == KeyCode.ESCAPE && !isDead) {
          gameClose
        } else if (watchKeys.contains(e.keyCode)) {
          println(s"key down: [${e.keyCode}]")
          if (e.keyCode == KeyCode.Space) {
            println(s"down+${e.keyCode.toString}")
          } else {
            println(s"down+${e.keyCode.toString}")
            val keyCode = Protocol.KeyCode(myId, e.keyCode, grid.frameCount +advanceFrame+ delayFrame, getActionSerialNum)
            grid.addActionWithFrame(myId, keyCode.copy(frame=grid.frameCount + delayFrame))
            grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
            webSocketClient.sendMsg(keyCode)
          }
          e.preventDefault()
        }
      }
    }
    //在画布上监听鼠标事件
    def getDegree(x:Double,y:Double)={
      atan2(y - 48 -window.y/2,x  -window.x/2 )
    }
    var FormerDegree = 0D
    canvas3.onmousemove = { (e: dom.MouseEvent) => {
      val mp = MousePosition(myId, e.pageX - window.x / 2, e.pageY - 48 - window.y.toDouble / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(e.pageX,e.pageY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.pageX,e.pageY)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount+delayFrame ))
        grid.addUncheckActionWithFrame(myId, mp, mp.frame)
        webSocketClient.sendMsg(mp)
      }
    }
    }
  }

}
