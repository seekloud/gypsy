package com.neo.sk.gypsy.front.gypsyClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.front.scalajs.{DeadPage, LoginPage, NetDelay}
import com.neo.sk.gypsy.front.utils.{JsFunc, Shortcut}
import com.neo.sk.gypsy.shared.ptcl._
import scalatags.JsDom.all._
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Canvas, Document => _}
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.math._
import com.neo.sk.gypsy.front.utils.byteObject.MiddleBufferInJs
import com.neo.sk.gypsy.shared.util.utils.getZoomRate
import org.scalajs.dom.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.collection.mutable
/**
  * User: sky
  * Date: 2018/9/13
  * Time: 13:26
  */
class GameHolder {

  val bounds = Point(Boundary.w, Boundary.h)
  val window = Point(dom.window.innerWidth.toInt, dom.window.innerHeight.toInt)
  private[this] val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas2 = dom.document.getElementById("MiddleView").asInstanceOf[Canvas]
  private[this] val ctx2 = canvas2.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas3 = dom.document.getElementById("TopView").asInstanceOf[Canvas]
  private[this] val ctx3 = canvas3.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]


  private[this] val offScreenCanvas = dom.document.getElementById("offScreen").asInstanceOf[Canvas]
  private[this] val offCtx = offScreenCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val actionSerialNumGenerator = new AtomicInteger(0)

  private[this] val draw1=DrawGame(ctx,canvas,window)
  private[this] val draw2=DrawGame(ctx2,canvas2,window)
  private[this] val draw3=DrawGame(ctx3,canvas3,window)
  private[this] val drawOff=DrawGame(offCtx,offScreenCanvas,bounds)

  /**
    * 状态值*/

  private[this] var justSynced = false
  var isDead = false
  private[this] var firstCome=true

  /**可变参数*/
  var myId = -1l
  var nextFrame = 0
  var nextInt = 0
  private[this] var logicFrameTime = System.currentTimeMillis()
  private[this] var syncGridData: scala.Option[GridDataSync] = None
  private[this] var killList = List.empty[(Int,Long,Player)]

  val webSocketClient=WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)

  val grid = new GameClient(bounds)

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
    draw1.drawGameWelcome()
    drawOff.drawBackground()
    draw1.drawGameOn()
    draw2.drawRankMap()
  }

  def getActionSerialNum=actionSerialNumGenerator.getAndIncrement()

  //不同步就更新，同步就设置为不同步
  def gameLoop: Unit = {
    NetDelay.ping(webSocketClient)
    logicFrameTime = System.currentTimeMillis()
    if (webSocketClient.getWsState) {
      if (!justSynced) {
        update()
      } else {
//        println("back")
        if (syncGridData.nonEmpty) {
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

  def startGame: Unit = {
    println("start---")
    draw1.drawGameWait(firstCome)
    nextInt=dom.window.setInterval(() => gameLoop, frameRate)
    dom.window.requestAnimationFrame(gameRender())
  }

  def gameRender(): Double => Unit = { d =>
//    println("gameRender-gameRender")
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    if(myId != -1l) {
      draw(offsetTime)
    }
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  def joinGame(room: String, name: String, userType: Int = 0, maxScore: Int = 0): Unit = {
    val url = UserRoute.getWebSocketUri(dom.document, room, name, userType)
    webSocketClient.setUp(url,maxScore)
    startGame
    addActionListenEvent
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
            val keyCode = WsMsgProtocol.KeyCode(myId, e.keyCode, grid.frameCount +advanceFrame+ delayFrame, getActionSerialNum)
            grid.addActionWithFrame(myId, keyCode.copy(frame=grid.frameCount + delayFrame))
            grid.addUncheckActionWithFrame(myId, keyCode, keyCode.frame)
            webSocketClient.sendMsg(keyCode)
          }
          e.preventDefault()
        }
      }
    }
    //在画布上监听鼠标事件
    def getDegree(x:Double,y:Double)={
      atan2(y - 48 -window.y/2,x  -window.x/2 )
    }
    var FormerDegree = 0D
    canvas3.onmousemove = { (e: dom.MouseEvent) => {
      val mp = MousePosition(myId, e.pageX - window.x / 2, e.pageY - 48 - window.y.toDouble / 2, grid.frameCount +advanceFrame +delayFrame, getActionSerialNum)
      if(math.abs(getDegree(e.pageX,e.pageY)-FormerDegree)*180/math.Pi>5){
        FormerDegree = getDegree(e.pageX,e.pageY)
        grid.addMouseActionWithFrame(myId, mp.copy(frame = grid.frameCount+delayFrame ))
        grid.addUncheckActionWithFrame(myId, mp, mp.frame)
        webSocketClient.sendMsg(mp)
      }
    }
    }
  }

  def draw(offsetTime:Long)={
    if (webSocketClient.getWsState) {
      var zoom = (30.0, 30.0)
      val data=grid.getGridData(myId)
      data.playerDetails.find(_.id == myId) match {
        case Some(p) =>
          firstCome=false
          var sumX = 0.0
          var sumY = 0.0
          var xMax = 0.0
          var xMin = 10000.0
          var yMin = 10000.0
          var yMax = 0.0
          //zoom = (p.cells.map(a => a.x+a.radius).max - p.cells.map(a => a.x-a.radius).min, p.cells.map(a => a.y+a.radius).max - p.cells.map(a => a.y-a.radius).min)
          p.cells.foreach { cell =>
            val offx = cell.speedX * offsetTime.toDouble / WsMsgProtocol.frameRate
            val offy = cell.speedY * offsetTime.toDouble / WsMsgProtocol.frameRate
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

          draw1.drawGrid(myId,data,offsetTime,firstCome,offScreenCanvas,basePoint,zoom)
          draw3.drawRankMapData(myId,grid.currentRank,data.playerDetails,basePoint)
          ctx.save()
          ctx.font = "34px Helvetica"
          ctx.fillText(s"KILL: ${p.kill}", 250, 10)
          ctx.fillText(s"SCORE: ${p.cells.map(_.mass).sum.toInt}", 400, 10)
          ctx.restore()
          renderFps(ctx3,NetDelay.latency)
          //todo 解决返回值问题
          val paraBack = draw1.drawKill(myId,grid,isDead,killList)
          killList=paraBack._1
          isDead=paraBack._2
        case None =>
          draw1.drawGameWait(firstCome)
      }


    }else{
      draw1.drawGameLost
    }
  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  private def wsConnectError(e:ErrorEvent) = {
    val playground = dom.document.getElementById("playground")
    println("----wsConnectError")
    draw1.drawGameLost
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

  private def wsMessageHandler(data:WsMsgFront,maxScore:Int):Unit = {
    data match {
      case WsMsgProtocol.Id(id) =>
        myId = id
        Shortcut.playMusic("bg")
        println(s"myID:$myId")

      case m:WsMsgProtocol.KeyCode =>
        //grid.addActionWithFrameFromServer(m.id,m)
        if(myId!=m.id){
          grid.addActionWithFrame(m.id,m)
        }
      case m:WsMsgProtocol.MousePosition =>
//        grid.addActionWithFrameFromServer(m.id,m)
        if(myId!=m.id){
          grid.addMouseActionWithFrame(m.id,m)
        }

      case WsMsgProtocol.Ranks(current, history) =>

        grid.currentRank = current
        grid.historyRank = history
      case WsMsgProtocol.FeedApples(foods) =>

        grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)

      case data: WsMsgProtocol.GridDataSync =>
        //TODO here should be better code.
        println(s"同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
        /*if(data.frameCount<grid.frameCount){
          println(s"丢弃同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
        }else if(data.frameCount>grid.frameCount){
          // println(s"同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
          syncGridData = Some(data)
          justSynced = true
        }*/
        syncGridData = Some(data)
        justSynced = true

      //drawGrid(msgData.uid, data)
      //网络延迟检测
      case WsMsgProtocol.Pong(createTime) =>
        NetDelay.receivePong(createTime ,webSocketClient)

      case WsMsgProtocol.SnakeRestart(id) =>
        Shortcut.stopMusic("bg")
        Shortcut.playMusic("bg")
      //timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)

      case WsMsgProtocol.UserDeadMessage(id,_,killerName,killNum,score,lifeTime)=>
        if(id==myId){
          DeadPage.deadModel(this,id,killerName,killNum,score,lifeTime,maxScore)
          grid.removePlayer(id)
        }

      case WsMsgProtocol.GameOverMessage(id,killNum,score,lifeTime)=>
        DeadPage.gameOverModel(this,id,killNum,score,lifeTime,maxScore)

      case WsMsgProtocol.KillMessage(killerId,deadPlayer)=>
        grid.removePlayer(deadPlayer.id)
        val a = grid.playerMap.getOrElse(killerId, Player(0, "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
        grid.playerMap += (killerId -> a.copy(kill = a.kill + 1))
        if(deadPlayer.id!=myId){
          if(!isDead){
            isDead=true
            killList :+=(200,killerId,deadPlayer)
          }else{
            killList :+=(200,killerId,deadPlayer)
          }
        }else{
          Shortcut.playMusic("shutdownM")
        }
        if(killerId==myId){
          grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
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


      case WsMsgProtocol.UserMerge(id,player)=>
        if(grid.playerMap.get(id).nonEmpty){
          grid.playerMap=grid.playerMap - id + (id->player)
        }

      case WsMsgProtocol.MatchRoomError=>
        JsFunc.alert("超过等待时间请重新选择")
        LoginPage.homePage()

      case msg@_ =>
        println(s"unknown $msg")

    }
  }



  def gameClose={
    webSocketClient.closeWs
    dom.window.cancelAnimationFrame(nextFrame)
    dom.window.clearInterval(nextInt)
    Shortcut.stopMusic("bg")
    LoginPage.homePage()
  }
}
