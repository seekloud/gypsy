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
import com.neo.sk.gypsy.actor.GameClient.myId
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


}
class GameHolder(
                  stageCtx: StageContext,
                  gameScene: GameScene,
                  serverActor: ActorRef[Protocol.WsSendMsg]
                ) {
  import GameHolder._

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

  def getActionSerialNum=gameScene.actionSerialNumGenerator.getAndIncrement()

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
        val data=grid.getGridData(myId,gameScene.window.x,gameScene.window.y)
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
    gameScene.topCanvas.setOnKeyPressed{ e => {
        val key=e.getCode
        if (key == KeyCode.ESCAPE && !isDead) {
         // gameClose
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

}
