package com.neo.sk.gypsy.holder


import com.neo.sk.gypsy.ClientBoot
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}

import com.neo.sk.gypsy.model.GameClient
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.util.Duration
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.common.{AppSettings, StageContext}

import com.neo.sk.gypsy.scene.{GameScene}
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.GameClient.ControllerInitial
import java.awt.event.KeyEvent

import javafx.scene.image.Image
import scala.math.atan2

//import com.neo.sk.gypsy.utils.ClientMusic
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

  val grid = new GameClient(bounds)
  var justSynced = false
  var isDead = false //是否有玩家死亡
  var firstCome=true
  var logicFrameTime = System.currentTimeMillis()
  var syncGridData: scala.Option[GridDataSync] = None
  var killList = List.empty[(Int,String,String)] //time killerId deadId
  var deadInfo :Option[Protocol.UserDeadMessage] = None
  var gameState = GameState.firstcome
  val timeline = new Timeline()

  var exitFullScreen = false

  var usertype = 0
  var FormerDegree = 0D

  //每帧动作限制
  var mouseInFlame = false
  var keyInFlame = false

  //(胜利玩家信息，自己分数，自己是否是胜利者，是就是true)
  var victoryInfo :Option[(Protocol.VictoryMsg,Short,Boolean)] = None

  val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.SPACE,
    KeyCode.ESCAPE
  )

  def keyCode2Int(c: KeyCode) = {
    c match {
      case KeyCode.E => KeyEvent.VK_E
      case KeyCode.F => KeyEvent.VK_F
      case KeyCode.SPACE => KeyEvent.VK_SPACE
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
  var mp = MP(None,0,0,0,0)
  var fmp = MP(None,0,0,0,0)

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
        gameRender()
      }
    }
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(frameRate),{ _ =>
      //游戏循环
      gameLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    animationTimer.start()
    timeline.play()
  }

  def gameLoop(): Unit = {
    if(!stageCtx.getStage.isFullScreen && !exitFullScreen) {
      //从全屏模式退出
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
      mouseInFlame = false
      keyInFlame = false
      if(grid.frameCount % 2 ==0){
        updateMousePos
      }
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

  def updateMousePos ={
    if(fmp != mp){
      fmp = mp
      grid.addMouseActionWithFrame(grid.myId, mp.copy(f = grid.frameCount + advanceFrame))
      //      grid.addUncheckActionWithFrame(grid.myId, mp, mp.f)
      serverActor ! mp.copy(f = grid.frameCount + advanceFrame)
    }
  }

  def gameRender() = {
    val offsetTime=System.currentTimeMillis()-logicFrameTime
    gameState match {
      case GameState.play if grid.myId!= ""=>
        gameScene.draw(grid.myId,offsetTime)
      case GameState.dead if deadInfo.isDefined =>
        gameScene.drawWhenDead(deadInfo.get)
      case GameState.victory if victoryInfo.isDefined =>
        gameScene.drawVictory(victoryInfo.get)
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
      } else if (watchKeys.contains(key) && keyInFlame == false) {
        if(gameState == GameState.dead){
          if(key == KeyCode.SPACE){
            println(s"down+ Space ReLive Press!")
            keyInFlame = true
            val reliveMsg = Protocol.ReLiveMsg(grid.frameCount +advanceFrame) //+ delayFrame
            serverActor ! reliveMsg
          }
        }else if(gameState == GameState.victory){
          println(s"down+ Press After Success!!")
          keyInFlame = true
          val rejoinMsg = Protocol.ReJoinMsg(grid.frameCount +advanceFrame) //+ delayFrame
          serverActor ! rejoinMsg
        } else {
          println(s"down+${e.toString}")
          //TODO 分裂只做后台判断，到时候客户端有BUG这里确认下
          keyInFlame = true
          val keyCode = Protocol.KC(None, keyCode2Int(e), grid.frameCount + advanceFrame , getActionSerialNum) //+ delayFrame
          if(key == KeyCode.E){
            grid.addActionWithFrame(grid.myId, keyCode.copy(f = grid.frameCount + advanceFrame )) //+ delayFrame
          }
          serverActor ! keyCode
        }
      }
    }

    override def OnMouseMoved(e: MouseEvent): Unit = {
      //在画布上监听鼠标事件
      def getDegree(x:Double,y:Double)={
        atan2(y -gameScene.gameView.realWindow.x/2,x - gameScene.gameView.realWindow.y/2 )
      }
      if(gameState == GameState.play){
        if(math.abs(getDegree(e.getX,e.getY)-FormerDegree) * 180/math.Pi > 5   &&  mouseInFlame == false){
          mp = MP(None, (e.getX - gameScene.gameView.realWindow.x / 2).toShort, (e.getY - gameScene.gameView.realWindow.y / 2).toShort, grid.frameCount +advanceFrame, getActionSerialNum)
        }
      }
    }
  })

  def cleanCtx() = {
    gameScene.topView.cleanCtx()
  }
  def gameClose = {
    //停止gameLoop
    timeline.stop()
  }
  stageCtx.setStageListener(new StageContext.StageListener {
    override def onCloseRequest(): Unit = {
      serverActor ! WsSendComplete
      stageCtx.closeStage()
    }
  })

}
