package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.front.scalajs.NetGameHolder.gameRender
import com.neo.sk.gypsy.front.utils.{Http, LayuiJs}
import com.neo.sk.gypsy.front.utils.LayuiJs.{layer, ready}
import com.neo.sk.gypsy.shared.ptcl.Protocol.{GameMessage, GridDataSync, MousePosition, UserLeft}
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.{UserLoginInfo, UserLoginRsq}
import com.neo.sk.gypsy.shared.ptcl._

import scalatags.JsDom.all._
import scala.scalajs.js.JSApp
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Element, Document => _, _}
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.math._
import com.neo.sk.gypsy.front.utils.byteObject.MiddleBufferInJs
import com.neo.sk.gypsy.shared.ptcl.utils.getZoomRate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 12:45 PM
  * 绘制画面，建立ws连接，接收消息
  */
object NetGameHolder extends js.JSApp {


  val bounds = Point(Boundary.w, Boundary.h)
  val window = Point(Window.w, Window.h)

  val littleMap = 200
  val mapMargin = 20
  val textLineHeight = 14

  var currentRank = List.empty[Score]
  //fixme 此处变量未有实际用途
  var historyRank = List.empty[Score]
  var myId = -1l

  val grid = new GridOnClient(bounds)

  var firstCome = true
  //长连接状态
  var wsSetup = false
  var justSynced = false

  var isDead=true
//条纹
  val stripeX = scala.collection.immutable.Range(0,bounds.y+50,50)
  val stripeY = scala.collection.immutable.Range(0,bounds.x+100,100)
  //背景移动
  var loop = 0
  var speed = 2

  val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.Space,
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down,
   // KeyCode.F2,
    KeyCode.Escape
  )

  object MyColors {
    val halo = "rgba(181, 211, 49, 0.51)"
    val rankList = "rgba(0, 0, 0, 0.64)"
    val background = "#fff"
    val stripe = "rgba(181, 181, 181, 0.5)"
    val myHeader = "#cccccc"
    val myBody = "#FFFFFF"
    val otherHeader = "rgba(78,69,69,0.82)"
    val otherBody = "#696969"
  }

  private var nextFrame = 0
  private var logicFrameTime = System.currentTimeMillis()
  var syncGridData: scala.Option[Protocol.GridDataSync] = None

  private[this] val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val img = dom.document.getElementById("virus").asInstanceOf[HTMLElement]
  private[this] val windWidth = dom.window.innerWidth


  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    drawGameOff()
    canvas.width = window.x
    canvas.height = window.y


    dom.window.onload= {
      (_: Event) =>
      LoginPage.homePage()
    }
    }


  def startGame(): Unit = {
    println("start---")
    drawGameOn()
    dom.window.setInterval(() => gameLoop(), Protocol.frameRate)
    dom.window.requestAnimationFrame(gameRender())
  }

  def gameRender(): Double => Unit = { d =>
    //println("gameRender-gameRender")
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    if(myId != -1l) draw(offsetTime)

    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }


//绘制背景
  def drawGameOn(): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }
//边框;提示文字
  def drawGameOff(): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, window.x , window.y )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    if (firstCome) {
      ctx.font = "36px Helvetica"
      ctx.fillText("Welcome.", 150, 180)
    } else {
      dom.window.cancelAnimationFrame(nextFrame)
      ctx.font = "36px Helvetica"
      ctx.fillText("Ops, connection lost....", 350, 250)
    }
  }

//不同步就更新，同步就设置为不同步
  def gameLoop(): Unit = {
    logicFrameTime = System.currentTimeMillis()
    if (wsSetup) {
      if (!justSynced) {
        update()
      } else {
        println("back")
        if (syncGridData.nonEmpty) {
          setSyncGridData(syncGridData.get)
          syncGridData = None
        }
        justSynced = false
      }
    }
  }

  def update(): Unit = {
    grid.update()
  }

  def draw(offsetTime:Long): Unit = {
    //println("开始绘画")
    if (wsSetup) {
    //  println(s"连接建立 ${wsSetup}")
      //println(s"myid$myId")
      val data = grid.getGridData(myId)
      //println(s"data$data")
      drawGrid(myId, data,offsetTime)
    } else {
      drawGameOff()
    }

  }

  def drawGrid(uid: Long, data: GridDataSync,offsetTime:Long): Unit = {
    //计算偏移量
    val players = data.playerDetails
    val foods = data.foodDetails
    val masses = data.massDetails
    val virus = data.virusDetails
   // val basePoint = players.filter(_.id==uid).map(a=>(a.x,a.y)).headOption.getOrElse((bounds.x/2,bounds.y/2))
//    val basePoint = players.find(_.id == uid) match{
//      case Some(p)=>
//        val target = MousePosition(p.targetX  ,p.targetY)
//        val deg = atan2(target.clientY,target.clientX)
//        val degX = if((cos(deg)).isNaN) 0 else (cos(deg))
//        val degY = if((sin(deg)).isNaN) 0 else (sin(deg))
//        ((p.x + p.cells.head.speed *degX *offsetTime.toFloat / Protocol.frameRate).toFloat,(p.y + p.cells.head.speed *degY *offsetTime.toFloat / Protocol.frameRate).toFloat)
//      case None=>
//        (bounds.x.toFloat/2,bounds.y.toFloat/2)
//    }
    var zoom = (30.0, 30.0)

    val basePoint = players.find(_.id == uid) match {
      case Some(p) =>
        var sumX = 0.0
        var sumY = 0.0
        var length = 0
        zoom = (p.cells.map(a => a.x).max - p.cells.map(a => a.x).min, p.cells.map(a => a.y).max - p.cells.map(a => a.y).min)
        p.cells.foreach { cell =>
          val offx = cell.speedX * offsetTime.toDouble / Protocol.frameRate
          val offy = cell.speedY * offsetTime.toDouble / Protocol.frameRate
          val newX = if ((cell.x + offx) > bounds.x) bounds.x else if ((cell.x + offx) <= 0) 0 else cell.x + offx
          val newY = if ((cell.y + offy) > bounds.y) bounds.y else if ((cell.y + offy) <= 0) 0 else cell.y + offy

          sumX += newX
          sumY += newY
          length += 1
          println(s"编号${length},中心$newX,$newY")
        }
        val offx = sumX / length
        val offy = sumY / length

        println(s"offx${offx},中心$offx,$offy")
        (offx, offy)
      case None =>
        (bounds.x.toDouble / 2, bounds.y.toDouble / 2)
    }
    println(s"offsetTime：${offsetTime},basepoint${basePoint._1},${basePoint._2}")

    //println(s"basePoint${basePoint}")
    val offx= window.x/2 - basePoint._1
    val offy =window.y/2 - basePoint._2
    //    println(s"zoom：$zoom")
    val scale = getZoomRate(zoom._1,zoom._2)
    //var scale = data.scale



//绘制背景
    ctx.fillStyle = MyColors.background
    ctx.fillRect(0,0,window.x,window.y)
    ctx.save()
    centerScale(scale,window.x/2,window.y/2)
//绘制条纹
    ctx.strokeStyle = MyColors.stripe
    stripeX.foreach{ l=>
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.moveTo(0,l +offy)
      ctx.lineTo(bounds.x,l +offy)
      ctx.stroke()
      ctx.restore()
    }
    stripeY.foreach{ l=>
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.moveTo(l +offx,0)
      ctx.lineTo(l +offx,bounds.y)
      ctx.stroke()
      ctx.restore()
    }



//为不同分值的苹果填充不同颜色
    foods.foreach { case Food(color, x, y) =>
      ctx.fillStyle = color match{
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      //println("画一个苹果")
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.arc(x +offx,y +offy,4,0,2*Math.PI)
      ctx.fill()
        ctx.restore()
    }
    masses.foreach { case Mass(x,y,tx,ty,color,mass,r,speed) =>
      ctx.fillStyle = color match{
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      val deg = Math.atan2(ty, tx)
      val deltaY = speed * Math.sin(deg)
      val deltaX = speed * Math.cos(deg)
      val xPlus = if (!deltaX.isNaN) deltaX else 0
      val yPlus = if (!deltaY.isNaN) deltaY else 0
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.arc(x +offx + xPlus*offsetTime.toFloat / Protocol.frameRate,y +offy + yPlus*offsetTime.toFloat / Protocol.frameRate,r,0,2*Math.PI)
      ctx.fill()
      ctx.restore()
    }
    ctx.fillStyle = MyColors.otherBody

    players.foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,_,killerName,width,height,cells) =>

      // players.foreach { case Player(id, name,color,x,y,tx,ty,kill,pro,_,cells) =>
      //println(s"draw body at $p body[$life]")
      cells.foreach{ cell=>
//        val target = MousePosition(tx +x-cell.x ,ty+y-cell.y)
//        val deg = atan2(target.clientY,target.clientX)
//        val degX = if((cos(deg)).isNaN) 0 else (cos(deg))
//        val degY = if((sin(deg)).isNaN) 0 else (sin(deg))
        val cellx = cell.x + cell.speedX *offsetTime.toFloat / Protocol.frameRate
        val celly = cell.y + cell.speedY *offsetTime.toFloat / Protocol.frameRate
        val xfix  = if(cellx>bounds.x) bounds.x else if(cellx<0) 0 else cellx
        val yfix = if(celly>bounds.y) bounds.y else if(celly<0) 0 else celly
        //println(s"cellX$cellx,celly$celly")
        //(cell.x + cell.speed *degX *offsetTime.toFloat / Protocol.frameRate,cell.y + cell.speed *degY *offsetTime.toFloat / Protocol.frameRate)
        ctx.save()
        //centerScale(scale,window.x/2,window.y/2)
        // println(s"${pro}")
        if(protect){
          //println("true")
          ctx.fillStyle = MyColors.halo
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+15,0,2*Math.PI)
          ctx.fill()
        }
        ctx.fillStyle = color.toInt match{
          case 0 => "#f3456d"
          case 1 => "#f49930"
          case 2  => "#f4d95b"
          case 3  => "#4cd964"
          case 4  => "#9fe0f6"
          case 5  => "#bead92"
          case 6  => "#cfe6ff"
          case _  => "#de9dd6"
        }

        //ln(s"$cellx,$celly")
        ctx.beginPath()
        ctx.arc(xfix +offx,yfix+offy,cell.radius,0,2*Math.PI)
        ctx.fill()

        ctx.font = "24px Helvetica"
        ctx.fillStyle = MyColors.background
        ctx.fillText(s"${name}", xfix+offx-12, yfix+offy -18)
        ctx.restore()
      }
    }

    virus.foreach { case Virus(x,y,mass,radius,_) =>
//      ctx.fillStyle = "green"
//      ctx.beginPath()
//      ctx.arc(x +offx,y +offy,radius,0,2*Math.PI)
//      ctx.fill()
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.drawImage(img,x-radius+offx,y-radius+offy,radius*2,radius*2)
      ctx.restore()
    }

    ctx.restore()
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"

    val leftBegin = 10
    //val rightBegin = bounds.x - 150
    val rightBegin = 1000
//根据uid获取用户信息，绘制左上角个人信息；绘制等待提示和死亡提示
    players.find(_.id == uid) match {
      case Some(myStar) =>
        firstCome = false
        val baseLine = 1
        ctx.font = "12px Helvetica"
        ctx.save()
        ctx.font = "34px Helvetica"
        ctx.fillText(s"KILL: ${myStar.kill}", 250, 10)
        ctx.fillText(s"SCORE: ${myStar.cells.map(_.mass).sum}", 400, 10)
        ctx.restore()
      case None =>
        if(firstCome) {
          ctx.font = "36px Helvetica"
          ctx.fillText("Please wait.", 150, 180)
        } else {
          ctx.font = "36px Helvetica"
          ctx.fillText("Ops, Loading....", 350, 250)
        }
    }
//绘制当前排行
    ctx.font = "12px Helvetica"
    ctx.fillStyle = MyColors.rankList
    ctx.fillRect(window.x-200,20,150,250)
    val currentRankBaseLine = 3
    var index = 0
    ctx.fillStyle = MyColors.background
    drawTextLine(s"—————排行榜—————", rightBegin, index, currentRankBaseLine)
    currentRank.foreach { score =>
      index += 1
      drawTextLine(s"【$index】: ${score.n.+("   ").take(5)} score=${score.score}", rightBegin, index, currentRankBaseLine)
    }
    //绘制小地图
    ctx.font = "12px Helvetica"
    ctx.fillStyle = MyColors.rankList
    ctx.fillRect(mapMargin,mapMargin,littleMap,littleMap)
    ctx.strokeStyle = "black"
    for (i<- 0 to 3){
      ctx.beginPath()
      ctx.moveTo(mapMargin + i * littleMap/3, mapMargin)
      ctx.lineTo(mapMargin + i * littleMap/3,mapMargin+littleMap)
      ctx.stroke()

      ctx.beginPath()
      ctx.moveTo(mapMargin , mapMargin+ i * littleMap/3)
      ctx.lineTo(mapMargin+littleMap ,mapMargin+ i * littleMap/3)
      ctx.stroke()
    }
    val margin = littleMap/3
    ctx.fillStyle = MyColors.background
    for(i <- 0 to 2){
      for (j <- 1 to 3){
        ctx.fillText((i*3+j).toString,mapMargin + abs(j-1)*margin+0.5*margin,mapMargin + i*margin+0.5*margin)
      }
    }
    players.find(_.id == uid) match {
      case Some(player)=>
        ctx.beginPath()
        ctx.arc(mapMargin + (basePoint._1/bounds.x) * littleMap,mapMargin + basePoint._2/bounds.y * littleMap,8,0,2*Math.PI)
        ctx.fill()
      case None=>
       // println(s"${basePoint._1},  ${basePoint._2}")
    }
  }
  def centerScale(rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
  }
//绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

//新用户加入游戏
def joinGame(room: String, name: String, userType: Int = 0, maxScore: Int = 0): Unit = {
  val playground = dom.document.getElementById("playground")
  val gameStream = new WebSocket(UserRoute.getWebSocketUri(dom.document, room, name, userType))
  gameStream.onopen = { (event0: Event) =>
    println("come here")
    startGame()
    // playground.insertBefore(p("Game connection was successful!"), playground.firstChild)
    isDead = false
    wsSetup = true
    canvas.focus()
    //在画布上监听键盘事件
    canvas.onkeydown = {
      (e: dom.KeyboardEvent) => {
        println(s"keydown: ${e.keyCode}")
        if (watchKeys.contains(e.keyCode)) {
          println(s"key down: [${e.keyCode}]")
          if (e.keyCode == KeyCode.Escape && !isDead) {
            sendMsg(UserLeft, gameStream)
            LoginPage.homePage()
            gameStream.close()
            wsSetup = false
            isDead = true
          } else if (e.keyCode == KeyCode.Space) {
            println(s"down+${e.keyCode.toString}")
          } else {
            println(s"down+${e.keyCode.toString}")
            sendMsg(Protocol.KeyCode(e.keyCode), gameStream)
          }
          e.preventDefault()
        }
      }
    }
    //在画布上监听鼠标事件
    canvas.onmousemove = { (e: dom.MouseEvent) => {
      //gameStream.send(MousePosition(e.pageX-windWidth/2, e.pageY-48-window.y.toDouble/2).asJson.noSpaces)
      sendMsg(MousePosition(e.pageX - windWidth / 2, e.pageY - 48 - window.y.toDouble / 2), gameStream)

    }

    }

    event0
  }
  gameStream.onerror = { (event: ErrorEvent) =>
    drawGameOff()
    playground.insertBefore(p(s"Failed: code: ${event.colno}"), playground.firstChild)
    if (wsSetup) {
      wsSetup = false
    }
  }


  import io.circe.generic.auto._
  import io.circe.parser._

  gameStream.onmessage = { (event: MessageEvent) =>
    import com.neo.sk.gypsy.front.utils.byteObject.ByteObject._
    //val wsMsg = read[Protocol.GameMessage](event.data.toString)
    event.data match {
      case blobMsg: Blob =>
        val fr = new FileReader()
        fr.readAsArrayBuffer(blobMsg)
        fr.onloadend = { _: Event =>
          val buf = fr.result.asInstanceOf[ArrayBuffer]
          val middleDataInJs = new MiddleBufferInJs(buf)
          bytesDecode[GameMessage](middleDataInJs) match {
            case Right(data) =>
             // println(data)
              data match {
                case Protocol.Id(id) =>
                  myId = id
                  println(s"myID:${myId}")
                  var timer = -1
                  val start = System.currentTimeMillis()
                  timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)
               // case Protocol.NewSnakeJoined(id, user) => println(s"$user joined!")
               // case Protocol.PlayerLeft(id, user) => println(s"$user left!")
                case Protocol.SnakeAction(id, keyCode, frame) =>

                  grid.addActionWithFrame(id, keyCode, frame)

                case Protocol.SnakeMouseAction(id, x, y, frame) =>
                  if (frame > grid.frameCount) {
                    //writeToArea(s"!!! got snake mouse action=$a when i am in frame=${grid.frameCount}")
                  } else {
                    //writeToArea(s"got snake mouse action=$a")
                  }
                  grid.addMouseActionWithFrame(id, x, y, frame)

                case Protocol.Ranks(current, history) =>

                  currentRank = current
                  historyRank = history
                case Protocol.FeedApples(foods) =>

                  grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)
                case data: Protocol.GridDataSync =>
//                  writeToArea(s"grid data got: $data")
                  //TODO here should be better code.
                  syncGridData = Some(data)
                  justSynced = true


                //drawGrid(msgData.uid, data)
                case Protocol.NetDelayTest(createTime) =>
                  val receiveTime = System.currentTimeMillis()
                  val m = s"Net Delay Test: createTime=$createTime, receiveTime=$receiveTime, twoWayDelay=${receiveTime - createTime}"
                  println(m)
                //writeToArea(m)
                case Protocol.SnakeRestart(id) =>
                  var timer = -1
                  val start = System.currentTimeMillis()
                  timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)

                case msg@_ =>
                  println(s"unknown $msg")

              }
            case unknown =>
              println(s"recv unknown msg:$unknown")
          }
        }

      case unknown =>
        println(s"recv unknown msg:$unknown")
    }

  }

  gameStream.onclose = { (event: Event) =>
    println("gameStream close")
    //  wsSetup = false
  }

  //写入消息区
  def writeToArea(text: String): Unit = {
//    playground.insertBefore(p(text), playground.firstChild)
  }


}

  //fixme 此处存在重复计算问题

  //xxx 有待商榷
  def setSyncGridData(data: Protocol.GridDataSync): Unit = {

    grid.actionMap = grid.actionMap.filterKeys(_ > data.frameCount)
//    println(s"前端帧${grid.frameCount}，后端帧${data.frameCount}")
    grid.frameCount = data.frameCount
//    println(s"**********************前端帧${grid.frameCount}，后端帧${data.frameCount}")
    grid.playerMap = data.playerDetails.map(s => s.id -> s).toMap
    grid.food = data.foodDetails.map(a => Point(a.x, a.y) -> a.color).toMap
    grid.massList = data.massDetails
    grid.virus = data.virusDetails

  }

  def p(msg: String) = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = msg
    paragraph
  }

  def deadCheck(id:Long,timer:Int,start:Long,maxScore:Int,gameStream:WebSocket)={
    grid.getGridData(id).deadPlayer.find(_.id==id) match {
      case Some(player)=>
        val score=player.cells.map(_.mass).sum
        DeadPage.deadModel(player.id,player.killerName,player.kill,score.toInt,System.currentTimeMillis()-start,maxScore,gameStream)
        dom.window.clearInterval(timer)
        grid.removeDeadPlayer(id)
      case None =>
    }

  }

  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(2048)

  def sendMsg(msg: GameMessage, gameStream: WebSocket) = {
    import com.neo.sk.gypsy.front.utils.byteObject.ByteObject._
    gameStream.send(msg.fillMiddleBuffer(sendBuffer).result())

  }


}
