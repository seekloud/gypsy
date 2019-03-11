package com.neo.sk.gypsy.holder

import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.model.GridOnClient
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.util.Duration
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.scene.{LayeredDraw, LayeredScene}
import com.neo.sk.gypsy.common.{AppSettings, Constant, StageContext}
import java.awt.event.KeyEvent

import com.neo.sk.gypsy.common.Constant
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.{BotActor, GrpcStreamSender}
import com.neo.sk.gypsy.actor.BotActor.GetByte
import com.neo.sk.gypsy.actor.GameClient._
import com.neo.sk.gypsy.botService.BotServer
import org.seekloud.esheepapi.pb.actions._

import scala.math.atan2
import com.neo.sk.gypsy.utils.{ClientMusic, FpsComp}
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import org.seekloud.esheepapi.pb.api.ObservationRsp

object BotHolder {

  val bounds = Point(Boundary.w, Boundary.h)
  val grid = new GridOnClient(bounds)
  var justSynced = false
  var isDead = false
  var firstCome = true
  var logicFrameTime = System.currentTimeMillis()
  var syncGridData: scala.Option[GridDataSync] = None
  //显示击杀弹幕
  var killList = List.empty[(Int, String, String)] //time killerName deadName
  var deadInfo: Option[Protocol.UserDeadMessage] = None
  var gameState = GameState.play
  val timeline = new Timeline()

  var exitFullScreen = false

  var usertype = 0

  sealed trait Command

  //(胜利玩家信息，自己分数，自己是否是胜利者，是就是true)
  var victoryInfo :Option[(Protocol.VictoryMsg,Short,Boolean)] = None


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
  serverActor: ActorRef[Protocol.WsSendMsg],
  botActor:ActorRef[BotActor.Command]
) {
  import BotHolder._

  private var stageWidth = stageCtx.getStage.getWidth.toInt
  private var stageHeight = stageCtx.getStage.getHeight.toInt

//  var getByte: GetByte = _


  def getActionSerialNum = layeredScene.actionSerialNumGenerator.getAndIncrement()

  def connectToGameServer() = {
    ClientBoot.addToPlatform {
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
    stageCtx.showScene(layeredScene.scene, "BotGaming", false)
  }

  def init() = {
    //    layeredScene.gameView.drawGameOn()
    //    layeredScene.middleView.drawRankMap()
  }

  def start() = {
    //TODO 这里改改
    init()
    //    val animationTimer = new AnimationTimer() {
    //      override def handle(now: Long): Unit = {
    //        //TODO Bot模式下不补帧的话，这里应该可以不用画，之后确认
    //        val ld = new LayeredDraw(botId, layeredScene, grid, false)
    //        ld.drawLayered()
    //      }
    //    }
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(frameRate), { _ =>
      //游戏循环
      gameLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    //    animationTimer.start()
    timeline.play()
  }

  def gameLoop(): Unit = {
    /*if(!stageCtx.getStage.isFullScreen && !exitFullScreen) {
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
    }*/

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
    //FIXME 主动推送帧数据 3/10
    if(BotServer.isFrameConnect) {
      BotServer.streamSender.foreach(_ ! GrpcStreamSender.NewFrame(getFrameCount))
    }

    //TODO 生成分层视图数据
    if(AppSettings.isLayer){
      ClientBoot.addToPlatform {
        val ld = new LayeredDraw(grid.myId, layeredScene, grid, false)
        val ByteInfo = ld.drawLayered()
        botActor ! GetByte(ByteInfo._1,ByteInfo._2,ByteInfo._3,ByteInfo._4,ByteInfo._5,ByteInfo._6,ByteInfo._7,ByteInfo._8,ByteInfo._9)
      }
    }
  }

  /*  def gameRender() = {
      val offsetTime=System.currentTimeMillis()-logicFrameTime
      gameState match {
        case GameState.play if botId!= ""=>
          layeredScene.draw(botId,offsetTime)
        case GameState.dead if deadInfo.isDefined =>
          layeredScene.drawWhenDead(deadInfo.get)
        case GameState.allopatry =>
          layeredScene.drawWhenFinish("存在异地登录")
          gameClose
        case _ =>
      }
    }*/

  def update(): Unit = {
    grid.update()
  }

//  def reLive(id: String) = {
//    serverActor ! ReLiveAck(id)
//  }

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


  /** BotService的功能 **/

  def gameActionReceiver(key: Int, swing: Option[Swing]) = {
    if (key != 0) {
      //使用E、F
      val keyCode = Protocol.KC(None, key, grid.frameCount + advanceFrame + delayFrame, getActionSerialNum)
      grid.addActionWithFrame(grid.myId, keyCode.copy(f = grid.frameCount + delayFrame))
      //      grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
      serverActor ! keyCode
    }
    if (swing.nonEmpty) {
      def getDegree(x: Double, y: Double) = {
        //        atan2(y -layeredScene.gameView.realWindow.x/2,x - layeredScene.gameView.realWindow.y/2 )
        atan2(y, x)
      }

      var FormerDegree = 0D
      val (x, y) = Constant.swingToXY(swing.get)
      //      val mp = MousePosition(botId, x.toFloat - layeredScene.gameView.realWindow.x / 2, y.toFloat - layeredScene.gameView.realWindow.y / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      val mp = MP(None, x.toShort, y.toShort, grid.frameCount + advanceFrame + delayFrame, getActionSerialNum)
      if (math.abs(getDegree(x, y) - FormerDegree) * 180 / math.Pi > 5) {
        FormerDegree = getDegree(x, y)
        grid.addMouseActionWithFrame(grid.myId, mp.copy(f = grid.frameCount + delayFrame))
        serverActor ! mp
      }
    }
  }

  def getFrameCount = {
    grid.frameCount
  }

  def getInform = {
    val player = grid.playerMap.find(_._1 == grid.myId).get._2
    val score = player.cells.map(_.newmass).sum
    val kill = player.kill
    val health = if (gameState == GameState.dead) 0 else 1
    (score, kill, health)
  }


}
