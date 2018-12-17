package com.neo.sk.gypsy.holder

import akka.actor.typed.ActorRef
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.model.GridOnClient
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.util.Duration
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.scene.LayeredScene
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.GameScene
import java.awt.event.KeyEvent
import com.neo.sk.gypsy.common.Constant
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.BotActor.MsgToService
import com.neo.sk.gypsy.actor.GameClient._
import com.neo.sk.gypsy.actor.{BotActor, GameClient}
import org.seekloud.esheepapi.pb.actions._
import scala.math.atan2
import com.neo.sk.gypsy.utils.{ClientMusic, FpsComp}

object BotHolder {
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

  var myId = ""
  var usertype = 0


  val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.SPACE
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
class BotHolder(
                  stageCtx: StageContext,
                  layeredScene: LayeredScene,
                  serverActor: ActorRef[Protocol.WsSendMsg]
                ) {
  import GameHolder._

  private var stageWidth = stageCtx.getStage.getWidth.toInt
  private var stageHeight = stageCtx.getStage.getHeight.toInt


  def getActionSerialNum=layeredScene.actionSerialNumGenerator.getAndIncrement()

  def connectToGameServer() = {
    ClientBoot.addToPlatform{
      //显示人类视图+分层视图
      showScene()
      //启动GameClient
      gameClient ! ControllerInitialBot(this)
      //开始游戏
      ClientBoot.addToPlatform(
        start()
      )
    }
  }

  def showScene() = {
    stageCtx.showScene(layeredScene.scene,"BotGaming",false)
  }

  def init() = {
    layeredScene.gameView.drawGameOn()
    layeredScene.middleView.drawRankMap()
  }

  def start()={
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
      layeredScene.resetScreen(1200,600)
      stageCtx.getStage.setWidth(1200)
      stageCtx.getStage.setHeight(600)
      exitFullScreen = true
      layeredScene.middleView.drawRankMap()
    }
    if(stageWidth != stageCtx.getStage.getWidth.toInt || stageHeight != stageCtx.getStage.getHeight.toInt){
      stageWidth = stageCtx.getStage.getWidth.toInt
      stageHeight = stageCtx.getStage.getHeight.toInt
      layeredScene.resetScreen(stageWidth,stageHeight)
      stageCtx.getStage.setWidth(stageWidth)
      stageCtx.getStage.setHeight(stageHeight)
      layeredScene.middleView.drawRankMap()
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
        layeredScene.draw(myId,offsetTime)
      case GameState.dead if deadInfo.isDefined =>
        layeredScene.drawWhenDead(deadInfo.get)
      case GameState.allopatry =>
        layeredScene.drawWhenFinish("存在异地登录")
        gameClose
      case _ =>
    }
  }

  def update(): Unit = {
    grid.update()
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





  /**BotService的功能**/

  def gameActionReceiver(key:Int, swing:Option[Swing]) = {
    if(key != 0){
      //使用E、F
      val keyCode = Protocol.KeyCode(myId, key, grid.frameCount + advanceFrame + delayFrame, getActionSerialNum)
      grid.addActionWithFrame(myId, keyCode.copy(frame = grid.frameCount + delayFrame))
      //      grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
      serverActor ! keyCode
    }
    if(swing.nonEmpty){
      def getDegree(x:Double,y:Double)={
        atan2(y -layeredScene.gameView.realWindow.x/2,x - layeredScene.gameView.realWindow.y/2 )
      }
      var FormerDegree = 0D
      val (x,y) = Constant.swingToXY(swing.get)
      val mp = MousePosition(myId, x.toFloat - layeredScene.gameView.realWindow.x / 2, y.toFloat - layeredScene.gameView.realWindow.y / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(x,y)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(x,y)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount + delayFrame))
        serverActor ! mp
      }
    }
  }

  def getFrameCount = {
    grid.frameCount
  }

  def getInform(playerId:String) = {
    val player = grid.playerMap.find(_._1 == playerId).get._2
    val score = player.cells.map(_.newmass).sum
    val kill = player.kill
    (score,kill)
  }




}
