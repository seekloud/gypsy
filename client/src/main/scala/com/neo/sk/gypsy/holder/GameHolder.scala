package com.neo.sk.gypsy.holder

import com.neo.sk.gypsy.ClientBoot
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}

import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.model.GridOnClient
import javafx.scene.input.KeyCode
import javafx.util.Duration

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.scene.GameScene
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.GameScene
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.GameClient.{ControllerInitial, myId}
import org.scalajs.dom

import scala.math.atan2

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

}
class GameHolder(
                  stageCtx: StageContext,
                  gameScene: GameScene,
                  serverActor: ActorRef[Protocol.WsSendMsg]
                ) {
  import GameHolder._

  def getActionSerialNum=gameScene.actionSerialNumGenerator.getAndIncrement()

  def connectToGameServer(gameHolder:GameHolder) = {
    ClientBoot.addToPlatform{
      stageCtx.showScene(gameScene.scene,"Gaming")
      gameClient ! ControllerInitial(gameHolder)
      //TODO 写在这里未必合适
      ClientBoot.addToPlatform(
        start()
      )
    }
  }

  def init()= {
    gameScene.gameView.drawGameWelcome()
    gameScene.offView.drawBackground()
    gameScene.gameView.drawGameOn()
    gameScene.middleView.drawRankMap()
  }

  def start()={
    println("start---")
    init()
    val animationTimer = new AnimationTimer() {
      override def handle(now: Long): Unit = {
        //游戏渲染
        val data = grid.getGridData(myId,gameScene.window.x,gameScene.window.y)
        val offsetTime=System.currentTimeMillis()-logicFrameTime
        gameScene.draw(myId,data,offsetTime)
      }
    }
    val timeline = new Timeline()
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(150),{ _ =>
      //游戏循环
      gameLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    animationTimer.start()
    timeline.play()
  }

  //TODO 如何获取屏幕宽高
  protected def checkScreeSize = {

  }

  def gameLoop(): Unit = {
    checkScreeSize
  //  NetDelay.ping(webSocketClient)
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

  def update(): Unit = {
    grid.update()
  }


  def addActionListenEvent = {
    gameScene.topCanvas.requestFocus()
    //在画布上监听键盘事件
    gameScene.topCanvas.setOnKeyPressed{ e => {
        val key=e.getCode
        if (key == KeyCode.ESCAPE && !isDead) {
          gameClose
        } else if (watchKeys.contains(e.getCode)) {
          if (e.getCode == KeyCode.SPACE) {
            println(s"down+${e.getCode.toString}")
          } else {
            println(s"down+${e.getCode.toString}")
            val keyCode = Protocol.KeyCode(myId, e.getCode.toString.toInt, grid.frameCount +advanceFrame+ delayFrame, getActionSerialNum)
            grid.addActionWithFrame(myId, keyCode.copy(frame=grid.frameCount + delayFrame))
            grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
            serverActor ! keyCode
          }
        }
      }
    }
    //在画布上监听鼠标事件
    def getDegree(x:Double,y:Double)={
      atan2(y - 48 -gameScene.window.y/2,x - gameScene.window.x/2 )
    }
    var FormerDegree = 0D
    gameScene.topCanvas.setOnMouseMoved{ e => {
      val mp = MousePosition(myId, e.getX.toFloat - gameScene.window.x / 2, e.getY.toFloat - 48 - gameScene.window.y.toDouble / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(e.getX,e.getY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.getX,e.getY)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount+delayFrame ))
        grid.addUncheckActionWithFrame(myId, mp, mp.frame)
        serverActor ! mp
      }
    }
    }
  }

  def cleanCtx() = {
    gameScene.topView.cleanCtx()
  }

  def reLive(id: String) = {
    serverActor ! ReLive(id)
  }


  def gameClose = {
    //关闭webscoket
    serverActor ! WsSendComplete
    //停止前端渲染
    stageCtx.closeStage()
    //停止背景音乐
  }
  stageCtx.setStageListener(new StageContext.StageListener {
    override def onCloseRequest(): Unit = {
      serverActor ! WsSendComplete
      stageCtx.closeStage()
    }
  })

}
