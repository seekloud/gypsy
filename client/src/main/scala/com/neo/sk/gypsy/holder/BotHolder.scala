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
import com.neo.sk.gypsy.scene.{GameScene, LayeredScene}
import com.neo.sk.gypsy.common.StageContext
import com.neo.sk.gypsy.scene.GameScene
import java.awt.event.KeyEvent
import com.neo.sk.gypsy.common.Constant
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.actor.BotActor.MsgToService
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
                  gameScene: LayeredScene,
                  serverActor: ActorRef[Protocol.WsSendMsg]
                ) {
  import GameHolder._

  private var stageWidth = stageCtx.getStage.getWidth.toInt
  private var stageHeight = stageCtx.getStage.getHeight.toInt
  private val botActor = ClientBoot.system.spawn(BotActor.create(this,stageCtx),"BotActor")



  def getActionSerialNum=gameScene.actionSerialNumGenerator.getAndIncrement()


  def init() = {

    gameScene.gameView.drawGameOn()

    gameScene.middleView.drawRankMap()
  }

  def start()={
    println("start---~~~~~")
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

  def gameMessageHandler(data:GameMessage):Unit = {
    data match {
      case Protocol.Id(id) =>
        myId = id

      case m:Protocol.KeyCode =>
        if(myId!=m.id || usertype == -1){
          grid.addActionWithFrame(m.id,m)
        }
      case m:Protocol.MousePosition =>
        if(myId!=m.id || usertype == -1){
          grid.addMouseActionWithFrame(m.id,m)
        }

      case Protocol.Ranks(current) =>
        //发来的排行版含有我的排名
        if(current.exists(r=>r.score.id ==myId)){
          grid.currentRank = current
        }else{
          //          发来的未含有我的
          grid.currentRank = current ::: grid.currentRank.filter(r=>r.score.id == myId)
        }

      case Protocol.MyRank(rank) =>
        //把之前这个id的排行过滤掉
        grid.currentRank = grid.currentRank.filterNot(r=>r.score.id==myId) :+ rank

      case Protocol.FeedApples(foods) =>
        grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)

      case Protocol.AddVirus(virus) =>
        println(s"接收新病毒 new Virus ${virus}")
        grid.virusMap ++= virus

      case data: Protocol.GridDataSync =>
        syncGridData = Some(data)
        justSynced = true

      //网络延迟检测
      case p:Protocol.Pong =>
        FpsComp.receivePingPackage(p)


      case Protocol.PlayerJoin(id,player) =>
        println(s"${id}  加入游戏 ${grid.frameCount}")
        //防止复活后又发了一条JOin消息
        if(!grid.playerMap.contains(id)){
          grid.playerMap += (id -> player)
        }
        if(myId == id){
          if(gameState == GameState.dead){
            println(s"发送复活确认")
            botActor ! MsgToService(ReLiveAck(id))
            gameState = GameState.play
          }
          //TODO drawTopView.cleanCtx()
        }

      //只针对某个死亡玩家发送的死亡消息
      case msg@Protocol.UserDeadMessage(id,_,killerName,killNum,score,lifeTime)=>
        if(id==myId){
          deadInfo = Some(msg)
          gameState = GameState.dead
          grid.removePlayer(id)
        }

      //针对所有玩家发送的死亡消息
      case Protocol.KillMessage(killerId,deadPlayer)=>
        grid.removePlayer(deadPlayer.id)
        val a = grid.playerMap.getOrElse(killerId, Player("", "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
        grid.playerMap += (killerId -> a.copy(kill = a.kill + 1))
        if(deadPlayer.id != myId){
          if(!isDead){
            isDead = true
            killList :+=(200,killerId,deadPlayer)
          }else{
            killList :+=(200,killerId,deadPlayer)
          }
        }


      case Protocol.UserMerge(id,player)=>
        if(grid.playerMap.get(id).nonEmpty){
          grid.playerMap = grid.playerMap - id + (id->player)
        }

      case Protocol.RebuildWebSocket =>
        println("存在异地登录")
        gameState = GameState.allopatry

      //某个用户离开
      case Protocol.PlayerLeft(id,name) =>
        grid.removePlayer(id)
        if(id == myId){
          gameClose
        }
//
//      case Protocol.DecodeEvent(data)=>
//        replayMessageHandler(data)
//
//      case Protocol.DecodeEvents(data)=>
//        data.list.foreach(item => replayMessageHandler(item))
//
//      case Protocol.DecodeEventError(data) =>
//        replayMessageHandler(data)

      case msg@_ =>
        println(s"unknown $msg")

    }
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
//          grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
          serverActor ! keyCode
        }
      }
    }

    override def OnMouseMoved(e: MouseEvent): Unit = {
      //在画布上监听鼠标事件
      def getDegree(x:Double,y:Double)={
        atan2(y -gameScene.gameView.realWindow.x/2,x - gameScene.gameView.realWindow.y/2 )
      }
      var FormerDegree = 0D
      val mp = MousePosition(myId, e.getX.toFloat - gameScene.gameView.realWindow.x / 2, e.getY.toFloat - gameScene.gameView.realWindow.y / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(e.getX,e.getY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.getX,e.getY)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount + delayFrame ))
//        grid.addUncheckActionWithFrame(myId, mp, mp.frame)
        serverActor ! mp
      }
    }
  })

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
        atan2(y -gameScene.gameView.realWindow.x/2,x - gameScene.gameView.realWindow.y/2 )
      }
      var FormerDegree = 0D
      val (x,y) = Constant.swingToXY(swing.get)
      val mp = MousePosition(myId, x.toFloat - gameScene.gameView.realWindow.x / 2, y.toFloat - gameScene.gameView.realWindow.y / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
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
