package com.neo.sk.gypsy.front.gypsyClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.{ApiRoute, UserRoute}

import scala.util.Random

import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.{Canvas, Document => _}
import org.scalajs.dom.raw._
import scala.collection.mutable

import scala.math._
import com.neo.sk.gypsy.shared.ptcl.{Game, _}
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.util.Utils._
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.front.scalajs.NetDelay
import com.neo.sk.gypsy.front.utils.Shortcut
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
  var isDead = false   //判断该帧内有无玩家死亡
  var isTest = false
  private[this] var firstCome=true

  /**可变参数*/
//  var myId = "" //myId变成String类型
  var usertype = 0
  var nextFrame = 0
  var nextInt = 0
  var FormerDegree = 0D
  var mouseInFlame = false
  var keyInFlame = false
  //  var bigPlayerMass = 500.0
  var mp = MP(None,0,0,0,0)
  var fmp = MP(None,0,0,0,0)
  private[this] var logicFrameTime = System.currentTimeMillis()
  private[this] var syncGridData: scala.Option[GridDataSync] = None
  /**用于显示击杀弹幕**/
  private[this] var killList = List.empty[(Int,String,String)] //time,killerName,deadName

  val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose,replay)

  val grid = new GameClient(bounds)

  //游戏状态
  private[this] var gameState = GameState.firstcome
  var deadInfo :Option[Protocol.UserDeadMessage] = None

  //(胜利玩家信息，自己分数，自己是否是胜利者，是就是true)
  var victoryInfo :Option[(Protocol.VictoryMsg,Short,Boolean)] = None

  private[this] val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.Space,
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
      //不同步
      if (!justSynced) {
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
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  def gameRender(): Double => Unit = { d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    gameState match {
      case GameState.play if grid.myId != "" =>
        draw(offsetTime)
      case GameState.dead if deadInfo.isDefined =>
        drawTopView.drawWhenDead(deadInfo.get)
      case GameState.victory if victoryInfo.isDefined =>
        drawTopView.drawVictory(victoryInfo.get)
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
    grid.myId = playerId
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
        if(keyInFlame == false){
          if(gameState == GameState.dead){
            if (e.keyCode == KeyCode.Space) {
              println(s"down+${e.keyCode.toString} ReLive Press!")
              keyInFlame = true
              val reliveMsg = Protocol.ReLiveMsg(grid.frameCount +advanceFrame) //+ delayFrame
              webSocketClient.sendMsg(reliveMsg)
            }
          }else if(gameState ==  GameState.victory){
            if (e.keyCode == KeyCode.Space) {
              println(s"down+${e.keyCode.toString} Press After Success!")
              keyInFlame = true
              val rejoinMsg = ReJoinMsg(grid.frameCount +advanceFrame) //+ delayFrame
              webSocketClient.sendMsg(rejoinMsg)
            }
          } else{
            if(e.keyCode == KeyCode.E || e.keyCode == KeyCode.F ){
              println(s"down+${e.keyCode.toString}")
              keyInFlame = true
              val keyCode = Protocol.KC(None, e.keyCode, grid.frameCount +advanceFrame, getActionSerialNum) //+ delayFrame
              if(e.keyCode == KeyCode.E){
                grid.addActionWithFrame(grid.myId, keyCode.copy(f=grid.frameCount  + advanceFrame)) //+ delayFrame
              }
              webSocketClient.sendMsg(keyCode)
            }
          }
        }
      }
    }

    //在画布上监听鼠标事件
    def getDegree(x:Double,y:Double)= {
      atan2(y - 48 - window.y/2,x  -window.x/2 )
    }
    if(!isTest){
      canvas3.onmousemove = { (e: dom.MouseEvent) =>
        if(gameState == GameState.play){
          val mpx = e.pageX - window.x / 2 - canvas3.offsetLeft
          val mpy = e.pageY - canvas3.offsetTop - window.y / 2
          mp = MP(None, mpx.toShort, mpy.toShort, grid.frameCount + advanceFrame, getActionSerialNum) // + delayFrame
        }
      }
    }else  {
      dom.window.setTimeout(() =>
        dom.window.setInterval(() => {
          testSend
        }, 2000), 3000)
    }
  }

  def updateMousePos ={
    if(fmp != mp){
      fmp = mp
      grid.addMouseActionWithFrame(grid.myId, mp.copy(f = grid.frameCount + advanceFrame))
      webSocketClient.sendMsg(mp.copy(f = grid.frameCount + advanceFrame))
    }
  }

  def testSend = {
    val px =  new Random(System.nanoTime()).nextInt(window.x)- window.x / 2 - canvas3.offsetLeft
    val py =  new Random(System.nanoTime()).nextInt(window.y)- window.y / 2 - canvas3.offsetTop
    val mp = MP(None,px.toShort,py.toShort,grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
    grid.addMouseActionWithFrame(grid.myId, mp.copy(f = grid.frameCount+delayFrame ))
    grid.addUncheckActionWithFrame(grid.myId, mp, mp.f)
    webSocketClient.sendMsg(mp)
  }

  def draw(offsetTime:Long)={
    if (webSocketClient.getWsState) {
      var zoom = (30.0, 30.0)
      val data=grid.getGridData(grid.myId, dom.window.innerWidth.toInt, dom.window.innerHeight.toInt,drawGameView.screeScale)
      val bigPlayerPosition=grid.playerMap.values.toList.filter(i=>i.cells.map(_.newmass).sum>bigPlayerMass).map(i=>PlayerPosition(i.id,i.x,i.y))
      data.playerDetails.find(_.id == grid.myId) match {
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
            if (newX + cell.radius > xMax) xMax= newX + cell.radius
            if (newX - cell.radius < xMin) xMin= newX - cell.radius
            if (newY + cell.radius > yMax) yMax= newY + cell.radius
            if (newY - cell.radius < yMin) yMin= newY - cell.radius
            sumX += newX
            sumY += newY
          }
          val offx = sumX /p.cells.length
          val offy = sumY /p.cells.length
          val basePoint = (offx, offy)
          zoom=(xMax - xMin, yMax - yMin)
//        println("zoom:  " + zoom)
          val foods = grid.food
          drawGameView.drawGrid(grid.myId,data,foods,offsetTime,firstCome,offScreenCanvas,basePoint,zoom,grid,p)
          drawTopView.drawRankMapData(grid.myId,grid.currentRank,data.playerDetails,basePoint,bigPlayerPosition,offsetTime,grid.playerMap.size)
//          ctx.save()
//          ctx.font = "34px Helvetica"
//          ctx.fillText(s"KILL: ${p.kill}", window.x * 0.18 + 30 , 10)
//          ctx.fillText(s"SCORE: ${p.cells.map(_.mass).sum.toInt}", window.x * 0.18 + 180, 10)
//          ctx.restore()
//          renderFps(ctx3,NetDelay.latency,window.x)
          //todo 解决返回值问题
          val paraBack = drawGameView.drawKill(grid.myId,grid,isDead,killList)
          killList=paraBack._1
          isDead=paraBack._2
        case None =>
//          println("gameState:   "+gameState)
          drawGameView.drawGameWait(grid.myId)
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
        grid.myId = id
        gameState = GameState.play

      case m:Protocol.KC =>
        if(m.id.isDefined){
          val mid = m.id.get
          if(grid.playerByte2IdMap.get(mid).isDefined){
            val ID = grid.playerByte2IdMap(mid)
            if(!grid.myId.equals(ID) || usertype == -1){
              grid.addActionWithFrame(ID,m)
            }
          }
        }

      case m:Protocol.MP =>
        if(m.id.isDefined){
          val mid = m.id.get
          if(grid.playerByte2IdMap.get(mid).isDefined){
            val ID = grid.playerByte2IdMap(mid)
            if(!grid.myId.equals(ID) || usertype == -1){
              grid.addMouseActionWithFrame(ID,m)
            }
          }
        }

      case Protocol.Ranks(current) =>
        //发来的排行版含有我的排名
        if(current.exists(r=>r.score.id == grid.myId)){
          grid.currentRank = current
        }else{
        //发来的未含有我的
          grid.currentRank = current ::: grid.currentRank.filter(r=>r.score.id == grid.myId)
        }

      case Protocol.MyRank(rank) =>
        //把之前这个id的排行过滤掉
        grid.currentRank = grid.currentRank.filterNot(r=>r.score.id == grid.myId) :+ rank

      case Protocol.FeedApples(foods) =>
        grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)

      case Protocol.AddVirus(virus) =>
        grid.virusMap ++= virus

      case Protocol.RemoveVirus(virus) =>
        grid.virusMap --= virus.keySet.toList

      case data: Protocol.GridDataSync =>
        if(grid.playerMap.get(grid.myId).isDefined && data.playerDetails.find(_.id==grid.myId).isDefined){
          println(s"获取全量数据  now frame: ${grid.frameCount}" + "  player's xy:  " + grid.playerMap(grid.myId).x + "   " + grid.playerMap(grid.myId).y )
          println(s"            backend frame: ${data.frameCount}" + "  player's xy:  " + data.playerDetails.find(_.id==grid.myId).get.x+ "   " +
            data.playerDetails.find(_.id==grid.myId).get.y)
        }
        syncGridData = Some(data)
        justSynced = true

      case PlayerIdBytes(playerIdByteMap)=>
        playerIdByteMap.foreach(item =>{
          grid.playerByte2IdMap += item._2 -> item._1
        })

      //网络延迟检测
      case Protocol.Pong(createTime) =>
        NetDelay.receivePong(createTime ,webSocketClient)

      case Protocol.PlayerJoin(id,player) =>
//        println(s"${player.id}  加入游戏 ${grid.frameCount} MYID:${grid.myId} ")
        if(!grid.playerMap.contains(player.id)){
          if(grid.myId != "" && player.id != grid.myId){
            //别的玩家加入，自己加入是在同步全量数据里面加入的
            grid.playerMap += (player.id -> player)
            grid.playerByte2IdMap += (id-> player.id)
          }
        }
        if(grid.myId == player.id){
          //玩家复活或胜利后重新加入房间
          if(gameState == GameState.dead || gameState == GameState.victory){
            deadInfo = None
            victoryInfo = None
            gameState = GameState.play
          }
          drawTopView.cleanCtx()
        }

      case Protocol.PlayerSplit(player,frame) =>
        player.keys.foreach(item =>{
          if(grid.playerByte2IdMap.get(item).isDefined){
            grid.playerMap += (grid.playerByte2IdMap(item) -> player(item))
          }
        }
        )

        //只针对自己死亡发送的死亡消息
      case msg@Protocol.UserDeadMessage(killerName,deadId,killNum,score,lifeTime)=>
        if(deadId == grid.myId){
          deadInfo = Some(msg)
          gameState = GameState.dead
        }

      //针对所有玩家发送的死亡消息
      case Protocol.KillMessage(killerId,deadId)=>
        if(grid.playerMap.get(killerId).isDefined && grid.playerMap.get(deadId).isDefined) {
          val a = grid.playerMap.getOrElse(killerId, Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
          grid.playerMap += (killerId -> a.copy(kill = (a.kill + 1).toShort))
          if (deadId != grid.myId) {
            if (!isDead) {
              isDead = true
              killList :+= (200, grid.playerMap.get(killerId).get.name, grid.playerMap.get(deadId).get.name)
            } else {
              killList :+= (200, grid.playerMap.get(killerId).get.name, grid.playerMap.get(deadId).get.name)
            }
          }
          grid.removePlayer(deadId)
          var deadByte = 0.toByte
          grid.playerByte2IdMap.foreach { elem =>
            if (elem._2 == deadId) {
              deadByte = elem._1
            }
          }
          grid.playerByte2IdMap -= deadByte
        }

      case Protocol.UserMerge(playerMap,frame)=>
        val playerHashMap = mutable.HashMap[String,List[(Long,Long)]]()
        playerMap.foreach{player =>
          if(grid.playerByte2IdMap.get(player._1).isDefined){
            playerHashMap.put(grid.playerByte2IdMap(player._1), player._2)
          }
        }
        grid.playerMap = grid.playerMap.map{ player =>
            if(playerHashMap.get(player._1).nonEmpty){
              val mergeCells = playerHashMap.get(player._1).get
              val newCells = player._2.cells.sortBy(_.radius).reverse.map{cell=>
                var newRadius = cell.radius
                var newM = cell.newmass
               mergeCells.map{merge=>
                 if(cell.id == merge._1){
                   val cellOp = player._2.cells.filter(_.id == merge._2).headOption
                   if(cellOp.isDefined){
                     val cell2 = cellOp.get
                     newM = (newM + cell2.newmass).toShort
                     newRadius = Mass2Radius(newM)
                   }
                 }else if(cell.id == merge._2){
                   val cellOp = player._2.cells.filter(_.id == merge._1).headOption
                   newM = 0
                   newRadius = 0
                 }
               }
                cell.copy(newmass = newM,radius = newRadius)
              }.filterNot(e=> e.newmass <= 0 && e.mass <= 0)
              val length = newCells.length
              val newX = newCells.map(_.x).sum / length
              val newY = newCells.map(_.y).sum / length
              val left = newCells.map(a => a.x - a.radius).min
              val right = newCells.map(a => a.x + a.radius).max
              val bottom = newCells.map(a => a.y - a.radius).min
              val top = newCells.map(a => a.y + a.radius).max
              (player._1 -> Game.Player(player._2.id,player._2.name,player._2.color,newX,newY,player._2.targetX,player._2.targetY,player._2.kill,player._2.protect,player._2.lastSplit,
                right-left,top-bottom,newCells,player._2.startTime))
            }else{
              player
            }
          }

      case Protocol.UserCrash(crashMap,frame)=>
        crashMap.foreach{p=>
          if(grid.playerByte2IdMap.get(p._1).isDefined){
            val playerId = grid.playerByte2IdMap(p._1)
            val player = grid.playerMap(playerId)
            var newCells = player.cells
            p._2.foreach{c=>
              newCells = c :: newCells.filterNot(_.id == c.id)
            }
            newCells = newCells.filterNot(_.newmass == 0)
            grid.playerMap += (player.id -> player.copy(cells = newCells))
          }

        }

      case msg@VictoryMsg(id,name,score,time) =>
        println(s"Receive Victory Msg $id,$name,$score,$time")

        val myScore = if(grid.playerMap.get(grid.myId).isDefined){
          grid.playerMap(grid.myId).cells.map(_.newmass).sum
        }else{
          val a:Short = 0
          a
        }

        victoryInfo = if(id.equals(grid.myId)){
          Some((msg,myScore,true))
        }else{
          Some((msg,myScore,false))
        }
        gameState = GameState.victory
        grid.clearAllData()

      case Protocol.RebuildWebSocket =>
        println("存在异地登录")
        gameState = GameState.allopatry

        //某个用户离开
      case Protocol.PlayerLeft(id) =>
        if(grid.playerByte2IdMap.get(id).isDefined){
          grid.removePlayer(grid.playerByte2IdMap(id))
          if(grid.playerByte2IdMap(id) == grid.myId){
            gameClose
          }
          grid.playerByte2IdMap -= id
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
        grid.addActionWithFrame(e.userId,Protocol.KC(None,e.keyCode,e.frame,e.serialNum))

      case e: Protocol.MouseMove =>
        grid.addMouseActionWithFrame(e.userId,Protocol.MP(None,e.direct._1,e.direct._2,e.frame,e.serialNum))

      case e: Protocol.GenerateApples =>
        grid.food ++= e.apples

      case e: Protocol.GenerateVirus =>
        grid.virusMap ++= e.virus

      case e: Protocol.UserJoinRoom =>
        grid.playerMap += e.playState.id -> e.playState
        gameState = GameState.play

      case e: Protocol.UserLeftRoom =>
        grid.removePlayer(e.userId)
        if(e.userId == grid.myId){
          gameClose
        }

      case e: Protocol.PlayerInfoChange =>
        grid.playerMap = e.player


      case killMsg:Protocol.KillMsg =>
        val a = grid.playerMap.getOrElse(killMsg.killerId, Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
        grid.playerMap += (killMsg.killerId -> a.copy(kill = (a.kill + 1).toShort ))
        if(killMsg.deadPlayer.id != grid.myId){
          if(!isDead){
            isDead = true
            killList :+=(200,killMsg.killerName,grid.playerMap.get(killMsg.deadPlayer.name).get.name)
          }else{
            killList :+=(200,killMsg.killerName,grid.playerMap.get(killMsg.deadPlayer.name).get.name)
          }
          if(killMsg.killerId == grid.myId){
            Shortcut.playMusic("godlikeM")
          }
        }else{
          //根据map找到killerName
          val deadMsg = UserDeadMessage(killMsg.killerName, grid.myId, killMsg.deadPlayer.kill,killMsg.score,killMsg.lifeTime)
          deadInfo = Some(deadMsg)
          gameState = GameState.dead
        }
        grid.removePlayer(killMsg.deadPlayer.id)

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
  }

}
