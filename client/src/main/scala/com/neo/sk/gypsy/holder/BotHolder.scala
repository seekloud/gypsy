package com.neo.sk.gypsy.holder

import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.model.GameClient
import javafx.scene.input.{KeyCode, MouseEvent}
import javafx.util.Duration
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.scene.{LayeredCanvas, LayeredScene}
import com.neo.sk.gypsy.common.{AppSettings, Constant, StageContext}
import java.awt.event.KeyEvent

import com.neo.sk.gypsy.common.Constant
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.ClientBoot.gameClient
import com.neo.sk.gypsy.actor.{BotActor, GrpcStreamSender}
import com.neo.sk.gypsy.actor.BotActor.GetByte
import com.neo.sk.gypsy.actor.GameClient._
import com.neo.sk.gypsy.botService.{BotClient, BotServer}
import org.seekloud.esheepapi.pb.actions._

import scala.math.atan2
//import com.neo.sk.gypsy.utils.{ClientMusic, FpsComp}
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import org.seekloud.esheepapi.pb.api.{ActionReq, ObservationRsp}

object BotHolder {

  val bounds = Point(Boundary.w, Boundary.h)
  val grid = new GameClient(bounds)
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

  var FormerDegree = 0D

  //每帧动作限制
  var mouseInFlame = false
  var keyInFlame = false

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
                 botClient: BotClient,
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
//        val data = grid.getGridData(grid.myId,1200,600)
//        data.playerDetails.find(_.id == grid.myId) match {
//          case Some(p) =>
//            val zoom = (p.cells.map(a => a.x + a.radius).max - p.cells.map(a => a.x - a.radius).min, p.cells.map(a => a.y + a.radius).max - p.cells.map(a => a.y - a.radius).min)
//            val basePoint = (p.x.toDouble, p.y.toDouble)
//            layeredScene.humanView.drawViewByState(data,basePoint,(zoom._1.toDouble,zoom._2.toDouble))
//          case None =>
//        }
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
      keyInFlame = false
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
        val ByteInfo = layeredScene.drawLayered()
        botActor ! GetByte(ByteInfo._1,ByteInfo._2,ByteInfo._3,ByteInfo._4,ByteInfo._5,ByteInfo._6,ByteInfo._7,ByteInfo._8,ByteInfo._9)
      }
    }
  }
//
//    def gameRender() = {
//      val offsetTime=System.currentTimeMillis()-logicFrameTime
//      gameState match {
//        case GameState.play =>
//          layeredScene.
//        case GameState.dead if deadInfo.isDefined =>
//          layeredScene.drawWhenDead(deadInfo.get)
//        case GameState.allopatry =>
//          layeredScene.drawWhenFinish("存在异地登录")
//          gameClose
//        case _ =>
//      }
//    }

  def update(): Unit = {
    grid.update()
  }



  def gameClose = {
    //停止gameLoop
    timeline.stop()
    //停止背景音乐
//    ClientMusic.stopMusic()
  }

  stageCtx.setStageListener(new StageContext.StageListener {
    override def onCloseRequest(): Unit = {
      serverActor ! WsSendComplete
      stageCtx.closeStage()
    }
  })


  /** BotService的功能 **/

  layeredScene.setLayeredSceneListener(new LayeredScene.LayeredSceneListener {
    override def onKeyPressed(e: KeyCode): Unit = {
      if (e == KeyCode.ESCAPE && !isDead) {
        gameClose
      }
      else if (watchKeys.contains(e) && !keyInFlame) {
        if (e == KeyCode.SPACE) {
          keyInFlame = true
          botClient.reincarnation()
        }
        else {
          //TODO 分裂只做后台判断，到时候客户端有BUG这里确认下
          keyInFlame = true
          botClient.actionReq = ActionReq(Move.up, None, 0, keyCode2Int(e), Some(botClient.credit))
          botClient.action()
        }
      }
    }

    //TODO
    override def OnMouseMoved(e: MouseEvent): Unit = {
      //在画布上监听鼠标事件
      def getDegree(x:Double,y:Double)={
        atan2(y - layeredScene.humanView.realWindow.y/2, x - layeredScene.humanView.realWindow.x/2)
      }
//      println("degree:  " + getDegree(e.getX,e.getY))
      if(math.abs(getDegree(e.getX,e.getY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.getX,e.getY)
        botClient.actionReq = ActionReq(Move.up,Some(Swing(getDegree(e.getX,e.getY).toFloat,
          math.sqrt(math.pow(e.getX-layeredScene.humanView.realWindow.x/2,2)+math.pow(e.getY-layeredScene.humanView.realWindow.y/2,2)).toFloat)),0,0,Some(botClient.credit))
        botClient.action()
      }
    }
  })

  def gameActionReceiver(key: Int, swing: Option[Swing]) = {
    if (key != 0) {
      //使用E、F、Space
      if(key == KeyEvent.VK_SPACE){
        if(gameState == GameState.dead){
          val reliveMsg = Protocol.ReLiveMsg(grid.frameCount +advanceFrame+ delayFrame)
          serverActor ! reliveMsg
        }
        else if(gameState == GameState.victory){
          val rejoinMsg = ReJoinMsg(grid.frameCount +advanceFrame+ delayFrame)
          serverActor ! rejoinMsg
        }
      }
      else{
        val keyCode = Protocol.KC(None, key, grid.frameCount + advanceFrame + delayFrame, getActionSerialNum)
        grid.addActionWithFrame(grid.myId, keyCode.copy(f = grid.frameCount + delayFrame))
        serverActor ! keyCode
      }
    }
    if (swing.nonEmpty) {
      def getDegree(x: Double, y: Double) = {
        //        atan2(y -layeredScene.gameView.humanView.y/2,x - layeredScene.humanView.realWindow.x/2 )
        atan2(y, x)
      }

      var FormerDegree = 0D
      val (x, y) = Constant.swingToXY(swing.get)
      val mp = MP(None, x.toShort, y.toShort, grid.frameCount + advanceFrame + delayFrame, getActionSerialNum)
      if (math.abs(swing.get.radian - FormerDegree) * 180 / math.Pi > 5) {
        FormerDegree = swing.get.radian
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
