package com.neo.sk.gypsy.front.gypsyClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.{ApiRoute, UserRoute}
import scala.util.Random
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.front.scalajs.NetDelay
import com.neo.sk.gypsy.front.utils.Shortcut
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.{Canvas, Document => _}
import org.scalajs.dom.raw._
import scala.math._


import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._

/**
  * User: sky
  * Date: 2018/9/13
  * Time: 13:26
  */
class GameHolder(replay:Boolean = false) {

  val bounds = Point(Boundary.w, Boundary.h)
  var window = Point(dom.window.innerWidth.toInt, dom.window.innerHeight.toInt)
  private[this] val canvas1 = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas1.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas2 = dom.document.getElementById("MiddleView").asInstanceOf[Canvas]
  private[this] val ctx2 = canvas2.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas3 = dom.document.getElementById("TopView").asInstanceOf[Canvas]
  private[this] val ctx3 = canvas3.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val offScreenCanvas = dom.document.getElementById("offScreen").asInstanceOf[Canvas]
  private[this] val offCtx = offScreenCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val actionSerialNumGenerator = new AtomicInteger(0)

  private[this] val drawGameView=DrawGame(ctx,canvas1,window)
  private[this] val drawMiddleView=DrawGame(ctx2,canvas2,window)
  private[this] val drawTopView=DrawGame(ctx3,canvas3,window)
//  private[this] val drawClockView=DrawGame(ctx4,canvas4,window)
  private[this] val drawOffScreen=DrawGame(offCtx,offScreenCanvas,bounds)

  /**
    * 状态值*/

  private[this] var justSynced = false
  var isDead = false
  var isTest = false
  private[this] var firstCome=true

  /**可变参数*/
  var myId = "" //myId变成String类型
  var usertype = 0
  var nextFrame = 0
  var nextInt = 0
  var FormerDegree = 0D
  private[this] var logicFrameTime = System.currentTimeMillis()
  private[this] var syncGridData: scala.Option[GridDataSync] = None
  private[this] var killList = List.empty[(Int,String,Player)]

  val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose,replay)

  val grid = new GameClient(bounds)

  //游戏状态
  private[this] var gameState = GameState.play
  var deadInfo :Option[Protocol.UserDeadMessage] = None

  private[this] val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.Space,
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down,
    KeyCode.Escape
  )

  def init(): Unit = {
    drawGameView.drawGameWelcome()
    drawOffScreen.drawBackground()
    drawGameView.drawGameOn()
    drawMiddleView.drawRankMap()
  }

  def getActionSerialNum=actionSerialNumGenerator.getAndIncrement()

  protected def checkScreenSize = {
    val newWidth=dom.window.innerWidth.toInt
    val newHeight=dom.window.innerHeight.toInt
    if(newWidth!= window.x || newHeight!= window.y){
      //屏幕适配
      window =  Point(newWidth, newHeight)
      drawGameView.updateCanvasSize(newWidth,newHeight)
      drawMiddleView.updateCanvasSize(newWidth,newHeight)
      drawTopView.updateCanvasSize(newWidth,newHeight)
      drawMiddleView.drawRankMap()
    }
  }

  //不同步就更新，同步就设置为不同步
  def gameLoop: Unit = {
    checkScreenSize
    NetDelay.ping(webSocketClient)
    logicFrameTime = System.currentTimeMillis()
    if (webSocketClient.getWsState) {
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
  }

  def update(): Unit = {
    grid.update()
  }

  def start(): Unit = {
    println("start---")
    /**
      * gameLoop: 150ms
      * gameRender: 约为16ms
      */
    nextInt=dom.window.setInterval(() => gameLoop, frameRate)
    dom.window.requestAnimationFrame(gameRender())
  }

  def animate():Double => Unit ={d =>
    drawGameView.drawGameOn2()
    if(myId == ""){
      dom.window.requestAnimationFrame(animate())
    }
  }

  def gameRender(): Double => Unit = { d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    gameState match {
      case GameState.play if myId!= ""=>
        draw(offsetTime)
      case GameState.dead if deadInfo.isDefined =>
        drawTopView.drawWhenDead(deadInfo.get)
//        drawTopView.drawEcharts()
      case GameState.allopatry =>
        drawTopView.drawWhenFinish("存在异地登录")
        gameClose
      case _ =>
    }
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  def watchRecord(
                 recordId:Long,
                 playerId:String,
                 frame:Int,
                 accessCode:String
                 ):Unit = {
    myId = playerId
    val url = ApiRoute.getwrWebSocketUri(recordId,playerId,frame,accessCode)
    webSocketClient.setUp(url)
    start()
  }

  //userType: 0(游客)，-1(观战模式)
  def joinGame(playerId: String,
               playerName:String,
               roomId: Long,
               accessCode:String,
               userType: Int = 0,
              ): Unit = {
    if (playerName.contains("TEST-")) {isTest=true}
    usertype = userType
    val url = ApiRoute.getpgWebSocketUri(dom.document,playerId,playerName,roomId,accessCode,userType)
    //开启websocket
    webSocketClient.setUp(url)
    //gameloop + gamerender
    start()
    //用户行为：使用键盘or鼠标(观战模式不响应键盘鼠标事件）
    if(userType != -1){
      addActionListenEvent
    }
  }

  def addActionListenEvent = {
    canvas3.focus()
    //在画布上监听键盘事件
    canvas3.onkeydown = {
      (e: dom.KeyboardEvent) => {
        println(s"keydown: ${e.keyCode}")
        if (e.keyCode == KeyCode.Escape && !isDead) {
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
    def getDegree(x:Double,y:Double)= {
      atan2(y - 48 - window.y/2,x  -window.x/2 )
    }

    if( !isTest){
      canvas3.onmousemove = { (e: dom.MouseEvent) => {
      val mp = MousePosition(myId, e.pageX - window.x / 2 - canvas3.offsetLeft, e.pageY - canvas3.offsetTop - window.y.toDouble / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(e.pageX,e.pageY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.pageX,e.pageY)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount+delayFrame ))
        grid.addUncheckActionWithFrame(myId, mp, mp.frame)
        webSocketClient.sendMsg(mp)
      }
      }
      }
    }else  {
      dom.window.setTimeout(() =>
        dom.window.setInterval(() => {
          testSend
        }, 2000), 3000)
    }
  }

  def testSend = {
    val px =  new Random(System.nanoTime()).nextInt(window.x)- window.x / 2 - canvas3.offsetLeft
    val py =  new Random(System.nanoTime()).nextInt(window.y)- window.y / 2 - canvas3.offsetTop
    val mp = MousePosition(myId,px.toDouble,py.toDouble,grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
    grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount+delayFrame ))
    grid.addUncheckActionWithFrame(myId, mp, mp.frame)
    webSocketClient.sendMsg(mp)
  }


  def draw(offsetTime:Long)={
    if (webSocketClient.getWsState) {
      var zoom = (30.0, 30.0)
      val data=grid.getGridData(myId, 1200, 600)
//      println(data.playerDetails.head.cells.head.mass+ "   "+ data.playerDetails.head.cells.head.newmass)
      data.playerDetails.find(_.id == myId) match {
        case Some(p) =>
          firstCome=false
          var sumX = 0.0
          var sumY = 0.0
          var xMax = 0.0
          var xMin = 10000.0
          var yMin = 10000.0
          var yMax = 0.0
          var kill = ""
          var Score = ""
          p.cells.foreach { cell =>
            val offx = cell.speedX * offsetTime.toDouble / frameRate
            val offy = cell.speedY * offsetTime.toDouble / frameRate
            val newX = if ((cell.x + offx) > bounds.x-15) bounds.x-15 else if ((cell.x + offx) <= 15) 15 else cell.x + offx
            val newY = if ((cell.y + offy) > bounds.y-15) bounds.y-15 else if ((cell.y + offy) <= 15) 15 else cell.y + offy
            if (newX>xMax) xMax=newX
            if (newX<xMin) xMin=newX
            if (newY>yMax) yMax=newY
            if (newY<yMin) yMin=newY
            zoom=(xMax-xMin+2*cell.radius,yMax-yMin+2*cell.radius)
            sumX += newX
            sumY += newY
          }
          val offx = sumX /p.cells.length
          val offy = sumY /p.cells.length
          val basePoint = (offx, offy)

          val foods = grid.food
          drawGameView.drawGrid(myId,data,foods,offsetTime,firstCome,offScreenCanvas,basePoint,zoom,grid,p)
          drawTopView.drawRankMapData(myId,grid.currentRank,data.playerDetails,basePoint,data.playersPosition,offsetTime)
//          ctx.save()
//          ctx.font = "34px Helvetica"
//          ctx.fillText(s"KILL: ${p.kill}", window.x * 0.18 + 30 , 10)
//          ctx.fillText(s"SCORE: ${p.cells.map(_.mass).sum.toInt}", window.x * 0.18 + 180, 10)
//          ctx.restore()
//          renderFps(ctx3,NetDelay.latency,window.x)
          //todo 解决返回值问题
          val paraBack = drawGameView.drawKill(myId,grid,isDead,killList)
          killList=paraBack._1
          isDead=paraBack._2
        case None =>
          drawGameView.drawGameWait(firstCome,myId)
      }
    }else{
      drawGameView.drawGameLost
    }
  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  private def wsConnectError(e:ErrorEvent) = {
    val playground = dom.document.getElementById("playground")
    println("----wsConnectError")
    drawGameView.drawGameLost
    playground.insertBefore(paraGraph(s"Failed: code: ${e.colno}"), playground.firstChild)
    e
  }

  def paraGraph(msg: String) = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = msg
    paragraph
  }

  private def wsConnectClose(e:Event) = {
    println("last Ws close")
    e
  }

  private def wsMessageHandler(data:GameMessage):Unit = {
    data match {
      case Protocol.Id(id) =>
        myId = id
        Shortcut.playMusic("bg")
        println(s"myID:$myId")

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
        println("获取全量数据  get ALL GRID===================")
        syncGridData = Some(data)
        justSynced = true

      //网络延迟检测
      case Protocol.Pong(createTime) =>
        NetDelay.receivePong(createTime ,webSocketClient)

      case Protocol.PlayerRestart(id) =>
        println(s" $id Receive  the ReStart &&&&&&&&&&&&& ")
        Shortcut.playMusic("bg")

      case Protocol.PlayerJoin(id,player) =>
        println(s"${id}  加入游戏 ${grid.frameCount}")
        //防止复活后又发了一条JOin消息
        if(!grid.playerMap.contains(id)){
          grid.playerMap += (id -> player)
        }
        if(myId == id){
          if(gameState == GameState.dead){
            println(s"发送复活确认")
            webSocketClient.sendMsg(ReLiveAck(id))
            gameState = GameState.play
          }
          drawTopView.cleanCtx()
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
        }else{
          Shortcut.playMusic("shutdownM")
        }
        if(killerId == myId){
          grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
            case 1 => Shortcut.playMusic("1Blood")
            case 2 => Shortcut.playMusic("2Kill")
            case 3 => Shortcut.playMusic("3Kill")
            case 4 => Shortcut.playMusic("4Kill")
            case 5 => Shortcut.playMusic("5Kill")
            case 6 => Shortcut.playMusic("godlikeM")
            case 7 => Shortcut.playMusic("legendaryM")
            case _ => Shortcut.playMusic("unstop")
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

      case Protocol.DecodeEvent(data)=>
        replayMessageHandler(data)

      case Protocol.DecodeEvents(data)=>
        data.list.foreach(item => replayMessageHandler(item))

      case Protocol.DecodeEventError(data) =>
        replayMessageHandler(data)

      case msg@_ =>
        println(s"unknown $msg")

    }
  }

  private def replayMessageHandler(data:Protocol.GameEvent):Unit = {
    data match {
      case e:Protocol.SyncGameAllState =>
        val data = e.gState
        syncGridData = Some(GridDataSync(data.frameCount,
          data.playerDetails,data.massDetails,
          data.virusDetails,0.toDouble,Nil,Nil))
        grid.food = e.gState.foodDetails.map(a => Point(a.x,a.y) -> a.color).toMap
        grid.currentRank = e.gState.currentRank
        justSynced = true

        //TODO 好像没用到
      case e: Protocol.CurrentRanks =>
        grid.currentRank = e.currentRank

      case e: Protocol.KeyPress =>
        grid.addActionWithFrame(e.userId,Protocol.KeyCode(e.userId,e.keyCode,e.frame,e.serialNum))

      case e: Protocol.MouseMove =>
        grid.addMouseActionWithFrame(e.userId,Protocol.MousePosition(e.userId,e.direct._1,e.direct._2,e.frame,e.serialNum))

      case e: Protocol.GenerateApples =>
        grid.food ++= e.apples

      case e: Protocol.GenerateVirus =>
        grid.virusMap ++= e.virus

      case e: Protocol.UserJoinRoom =>
        gameState = GameState.play
        grid.playerMap += e.playState.id -> e.playState

      case e: Protocol.UserLeftRoom =>
        grid.removePlayer(e.userId)
        if(e.userId == myId){
          gameClose
        }

      case e: Protocol.PlayerInfoChange =>
        grid.playerMap = e.player


      case killMsg:Protocol.KillMsg =>
        grid.removePlayer(killMsg.deadPlayer.id)
        val a = grid.playerMap.getOrElse(killMsg.killerId, Player("", "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
        grid.playerMap += (killMsg.killerId -> a.copy(kill = a.kill + 1))
        if(killMsg.deadPlayer.id != myId){
          if(!isDead){
            isDead = true
            killList :+=(200,killMsg.killerId,killMsg.deadPlayer)
          }else{
            killList :+=(200,killMsg.killerId,killMsg.deadPlayer)
          }
          if(killMsg.killerId == myId){
            grid.playerMap.getOrElse(killMsg.killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
              case 1 => Shortcut.playMusic("1Blood")
              case 2 => Shortcut.playMusic("2Kill")
              case 3 => Shortcut.playMusic("3Kill")
              case 4 => Shortcut.playMusic("4Kill")
              case 5 => Shortcut.playMusic("5Kill")
              case 6 => Shortcut.playMusic("godlikeM")
              case 7 => Shortcut.playMusic("legendaryM")
              case _ => Shortcut.playMusic("unstop")
            }
          }
        }else{
          val deadMsg = UserDeadMessage(myId,killMsg.killerId,killMsg.deadPlayer.killerName,killMsg.deadPlayer.kill,killMsg.score,killMsg.lifeTime)
          deadInfo = Some(deadMsg)
          gameState = GameState.dead
          //TODO 商榷
          grid.removePlayer(myId)
          Shortcut.playMusic("shutdownM")
        }

      case e:Protocol.PongEvent =>
        NetDelay.receivePong(e.timestamp ,webSocketClient)

      case e:Protocol.ReplayFinish=>
        //游戏回放结束
        drawTopView.drawWhenFinish("播放结束")
        gameClose

      case e:Protocol.DecodeError =>
        println("数据解析失败")
//        drawTopView.drawWhenFinish("数据解析失败")
//        gameClose

      case e:Protocol.InitReplayError =>
        drawTopView.drawWhenFinish(e.msg)
        gameClose

      case _ =>
        println(s"unknow msg: $data")
    }
  }

  def gameClose={
    webSocketClient.closeWs
    dom.window.cancelAnimationFrame(nextFrame)
    dom.window.clearInterval(nextInt)
    Shortcut.stopMusic("bg")
  }

}
