package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.front.common.Routes.UserRoute
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
        //每隔一段间隔就执行gameLoop（同步更新，重画）
        dom.window.setInterval(() => gameLoop(), Protocol.frameRate)
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
      ctx.font = "36px Helvetica"
      ctx.fillText("Ops, connection lost....", 350, 250)
    }
  }

//不同步就更新，同步就设置为不同步
  def gameLoop(): Unit = {
    if (wsSetup) {
      if (!justSynced) {
        update()
      } else {
        justSynced = false
      }
    }
    draw()
  }

  def update(): Unit = {
    grid.update()
  }

  def draw(): Unit = {
   // println("开始绘画")
    if (wsSetup) {
    //  println(s"连接建立 ${wsSetup}")
      val data = grid.getGridData(myId)
      drawGrid(myId, data)
    } else {
      drawGameOff()
    }
  }

  def drawGrid(uid: Long, data: GridDataSync): Unit = {
    //计算偏移量
    val players = data.playerDetails
    val foods = data.foodDetails
    val masses = data.massDetails
    val virus = data.virusDetails
    val basePoint = players.filter(_.id==uid).map(a=>(a.x,a.y)).headOption.getOrElse((bounds.x/2,bounds.y/2))
    //println(s"basePoint${basePoint}")
    val offx = window.x/2 - basePoint._1
    val offy =window.y/2 - basePoint._2
    var scale = data.scale

    //println(s"scale:${scale}")
    //println(s"zoom${zoom}")
    //ctx.translate(window.x/2 - basePoint._1,window.y/2 - basePoint._2)
    //println(s"players ${players}")
//    val img = dom.document.getElementById("background").asInstanceOf[HTMLElement]
//    if(loop * speed >= 600){
//      loop = 0
//    }else{
//      loop += 1
//    }
//    val setoff = loop * speed
//    ctx.drawImage(img,0,setoff - 1800,3600,1800)
//    ctx.drawImage(img,0,setoff,3600,1800)
//绘制背景
    ctx.fillStyle = MyColors.background
    ctx.fillRect(0,0,window.x,window.y)
    ctx.save()
    centerScale(scale,window.x/2,window.y/2)
//绘制条纹
    ctx.strokeStyle = MyColors.stripe
    stripeX.map{l=>
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.moveTo(0,l +offy);
      ctx.lineTo(bounds.x,l +offy);
      ctx.stroke();
      ctx.restore()
    }
    stripeY.map{l=>
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.moveTo(l +offx,0);
      ctx.lineTo(l +offx,bounds.y);
      ctx.stroke();
      ctx.restore()
    }


    ctx.fillStyle = MyColors.otherBody

    players.foreach { case Player(id, name,color,x,y,tx,ty,kill,pro,_,killerName,width,height,cells) =>

   // players.foreach { case Player(id, name,color,x,y,tx,ty,kill,pro,_,cells) =>
      //println(s"draw body at $p body[$life]")
      cells.map{cell=>

          ctx.save()
          //centerScale(scale,window.x/2,window.y/2)
        println(s"${pro}")
        if(pro == true){
          println("true")
          ctx.fillStyle = MyColors.halo
          ctx.beginPath()
          ctx.arc(cell.x +offx,cell.y +offy,cell.radius+15,0,2*Math.PI)
          ctx.fill()
        }
          ctx.fillStyle = color.toInt match{
//            case 0 => "red"
//            case 1 => "orange"
//            case 2  => "yellow"
//            case 3  => "green"
//            case 4  => "blue"
//            case 5  => "purple"
//            case 6  => "black"
//            case _  => "blue"
            case 0 => "#f3456d"
            case 1 => "#f49930"
            case 2  => "#f4d95b"
            case 3  => "#4cd964"
            case 4  => "#9fe0f6"
            case 5  => "#bead92"
            case 6  => "#cfe6ff"
            case _  => "#de9dd6"
          }
          ctx.beginPath()
          ctx.arc(cell.x +offx,cell.y +offy,cell.radius,0,2*Math.PI)
          ctx.fill()

        ctx.font = "24px Helvetica"
        ctx.fillStyle = MyColors.background
        ctx.fillText(s"${name}", cell.x +offx-12, cell.y +offy -18)
        ctx.restore()
      }
    }
//为不同分值的苹果填充不同颜色
    foods.foreach { case Food(color, x, y) =>
      ctx.fillStyle = color match{
//        case 0 => "red"
//        case 1 => "orange"
//        case 2  => "yellow"
//        case 3  => "green"
//        case 4  => "blue"
//        case 5  => "purple"
//        case 6  => "black"
//        case _  => "blue"
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.arc(x +offx,y +offy,4,0,2*Math.PI)
      ctx.fill()
        ctx.restore()
    }
    masses.foreach { case Mass(x,y,_,_,color,mass,r,_) =>
      ctx.fillStyle = color match{
//        case 0 => "red"
//        case 1 => "orange"
//        case 2  => "yellow"
//        case 3  => "green"
//        case 4  => "blue"
//        case 5  => "purple"
//        case 6  => "black"
//        case _  => "blue"
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      ctx.beginPath()
      ctx.arc(x +offx,y +offy,r,0,2*Math.PI)
      ctx.fill()
      ctx.restore()
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
    players.filter(_.id==uid).headOption match {
      case Some(player)=>
        ctx.beginPath()
        ctx.arc(mapMargin + (basePoint._1.toDouble/bounds.x) * littleMap,mapMargin + basePoint._2.toDouble/bounds.y * littleMap,8,0,2*Math.PI)
        ctx.fill()
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
    drawGameOn()
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
              data match {
                case Protocol.Id(id) =>
                  myId = id
                  var timer = -1
                  val start = System.currentTimeMillis()
                  timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)
                case Protocol.NewSnakeJoined(id, user) => writeToArea(s"$user joined!")
                case Protocol.PlayerLeft(id, user) => writeToArea(s"$user left!")
                case Protocol.SnakeAction(id, keyCode, frame) =>
                  if (frame > grid.frameCount) {
                  } else {
                  }
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
                  //writeToArea(s"grid data got: $msgData")
                  //TODO here should be better code.
                  grid.actionMap = grid.actionMap.filterKeys(_ > data.frameCount)
                  grid.frameCount = data.frameCount
                  grid.playerMap = data.playerDetails.map(s => s.id -> s).toMap
                  grid.food = data.foodDetails.map(a => Point(a.x, a.y) -> a.color).toMap
                  grid.massList = data.massDetails
                  grid.virus = data.virusDetails
                  justSynced = true
                //drawGrid(msgData.uid, data)
                case Protocol.NetDelayTest(createTime) =>
                  val receiveTime = System.currentTimeMillis()
                  val m = s"Net Delay Test: createTime=$createTime, receiveTime=$receiveTime, twoWayDelay=${receiveTime - createTime}"
                //writeToArea(m)
                case Protocol.SnakeRestart(id) =>
                  var timer = -1
                  val start = System.currentTimeMillis()
                  timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)

                case msg@_ =>
                  println(s"unkown $msg")

              }
            case unknow =>
              println(s"recv unknow msg:$unknow")
          }
        }

      case unknow =>
        println(s"recv unknow msg:$unknow")
    }

  }

  gameStream.onclose = { (event: Event) =>
    println("gameStream close")
    //  wsSetup = false
  }

  //写入消息区
  def writeToArea(text: String): Unit = {
    //playground.insertBefore(p(text), playground.firstChild)
  }


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
    }

  }

  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(2048)

  def sendMsg(msg: GameMessage, gameStream: WebSocket) = {
    import com.neo.sk.gypsy.front.utils.byteObject.ByteObject._
    gameStream.send(msg.fillMiddleBuffer(sendBuffer).result())

  }


}
