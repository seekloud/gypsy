package com.neo.sk.gypsy.holder


import com.neo.sk.gypsy.ClientBoot
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import com.neo.sk.gypsy.model.GridOnClient
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.util.Duration
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.GameScene
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.GameClient.{ControllerInitial}
import java.awt.event.KeyEvent
import javafx.scene.image.Image
import scala.math.atan2
import com.neo.sk.gypsy.utils.ClientMusic

import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._


/**
  * @author zhaoyin
  * 2018/10/29  5:13 PM
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
  var deadInfo :Option[Protocol.UserDeadMessage] = None
  var gameState = GameState.play
  val timeline = new Timeline()

  var exitFullScreen = false

  var myId = "" //myId变成String类型
  var usertype = 0
  var FormerDegree = 0D

  val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.SPACE,
    KeyCode.LEFT,
    KeyCode.UP,
    KeyCode.RIGHT,
    KeyCode.DOWN,
    KeyCode.ESCAPE
  )

  def keyCode2Int(c: KeyCode) = {
    c match {
      case KeyCode.E => KeyEvent.VK_E
      case KeyCode.F => KeyEvent.VK_F
      case KeyCode.SPACE => KeyEvent.VK_SPACE
      case KeyCode.LEFT => KeyEvent.VK_LEFT
      case KeyCode.UP => KeyEvent.VK_UP
      case KeyCode.RIGHT => KeyEvent.VK_RIGHT
      case KeyCode.DOWN => KeyEvent.VK_DOWN
      case KeyCode.F2 => KeyEvent.VK_F2
      case _ => KeyEvent.VK_F2
    }
  }

}
class GameHolder(
                  stageCtx: StageContext,
                  gameScene: GameScene,
                  serverActor: ActorRef[Protocol.WsSendMsg]
                ) {
  import GameHolder._

  private var stageWidth = stageCtx.getStage.getWidth.toInt
  private var stageHeight = stageCtx.getStage.getHeight.toInt

  def getActionSerialNum=gameScene.actionSerialNumGenerator.getAndIncrement()

  def connectToGameServer() = {
    ClientBoot.addToPlatform{
      showScene()
      gameClient ! ControllerInitial(this)
      //TODO 写在这里未必合适
      ClientBoot.addToPlatform(
        start()
      )
    }
  }

  def showScene(): Unit ={
    stageCtx.showScene(gameScene.scene,"Gaming",false)
  }

  def init() = {
    //gameScene.gameView.drawGameWelcome()
    gameScene.gameView.drawGameOn()
   // gameScene.offView.drawBackgroundInit()
    gameScene.middleView.drawRankMap()
  }

  def start()={
    println("start---!!!")
    init()
    val animationTimer = new AnimationTimer() {
      override def handle(now: Long): Unit = {
        //游戏渲染
        gameRender()
      }
    }
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(150),{ _ =>
      //游戏循环
      gameLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    animationTimer.start()
    timeline.play()
  }

  def gameLoop(): Unit = {
    if(!stageCtx.getStage.isFullScreen && !exitFullScreen) {
      gameScene.resetScreen(1200,600)
      stageCtx.getStage.setWidth(1200)
      stageCtx.getStage.setHeight(600)
      exitFullScreen = true
      gameScene.middleView.drawRankMap()
    }
    if(stageWidth != stageCtx.getStage.getWidth.toInt || stageHeight != stageCtx.getStage.getHeight.toInt){
      stageWidth = stageCtx.getStage.getWidth.toInt
      stageHeight = stageCtx.getStage.getHeight.toInt
      gameScene.resetScreen(stageWidth,stageHeight)
      stageCtx.getStage.setWidth(stageWidth)
      stageCtx.getStage.setHeight(stageHeight)
      gameScene.middleView.drawRankMap()
    }
    serverActor ! Protocol.Ping(System.currentTimeMillis())
    logicFrameTime = System.currentTimeMillis()
      //差不多每三秒同步一次
      //不同步
      if (!justSynced) {
        update()
      } else {
        if (syncGridData.nonEmpty) {
          //同步
          grid.setSyncGridData(syncGridData.get)
          syncGridData = None
        }
        justSynced = false
      }
  }

  def gameRender() = {
    val offsetTime=System.currentTimeMillis()-logicFrameTime
    gameState match {
      case GameState.play if myId!= ""=>
        gameScene.draw(myId,offsetTime)
      case GameState.dead if deadInfo.isDefined =>
        gameScene.drawWhenDead(deadInfo.get)
      case GameState.allopatry =>
        gameScene.drawWhenFinish("存在异地登录")
        gameClose
      case _ =>
    }
  }

  def update(): Unit = {
    grid.update()
  }

  gameScene.setGameSceneListener(new GameScene.GameSceneListener {
    override def onKeyPressed(e: KeyCode): Unit = {
      val key=e
      if (key == KeyCode.ESCAPE && !isDead) {
        gameClose
      } else if (watchKeys.contains(key)) {
        if (key == KeyCode.SPACE) {
          println(s"down+${e.toString}")
        } else {
          println(s"down+${e.toString}")
          val keyCode = Protocol.KeyCode(myId, keyCode2Int(e), grid.frameCount + advanceFrame + delayFrame, getActionSerialNum)
          grid.addActionWithFrame(myId, keyCode.copy(frame = grid.frameCount + delayFrame))
          grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
          serverActor ! keyCode
        }
      }
    }

    override def OnMouseMoved(e: MouseEvent): Unit = {
      //在画布上监听鼠标事件
      def getDegree(x:Double,y:Double)={
//        atan2(y -gameScene.window.y/2,x - gameScene.window.x/2 )
        atan2(y -gameScene.gameView.realWindow.x/2,x - gameScene.gameView.realWindow.y/2 )
      }

      val mp = MousePosition(myId, e.getX.toFloat - gameScene.gameView.realWindow.x / 2, e.getY.toFloat - gameScene.gameView.realWindow.y / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
//      val mp = MousePosition(myId, e.getX.toFloat - gameScene.window.x / 2, e.getY.toFloat - gameScene.window.y.toDouble / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(e.getX,e.getY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.getX,e.getY)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount + delayFrame ))
        grid.addUncheckActionWithFrame(myId, mp, mp.frame)
        serverActor ! mp
      }
    }
  })

  def cleanCtx() = {
    gameScene.topView.cleanCtx()
  }

  def reLive(id: String) = {
    serverActor ! ReLiveAck(id)
  }

  def gameClose = {
    //停止gameLoop
    timeline.stop()
    //停止背景音乐
    ClientMusic.stopMusic()
  }
  stageCtx.setStageListener(new StageContext.StageListener {
    override def onCloseRequest(): Unit = {
      serverActor ! WsSendComplete
      stageCtx.closeStage()
    }
  })

}
