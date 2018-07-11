package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.shared.ptcl.Protocol.{GridDataSync, MousePosition}
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl._
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.scalajs.js

/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 12:45 PM
  * 绘制画面，建立ws连接，接收消息
  */
object NetGameHolder extends js.JSApp {


  val bounds = Point(Boundary.w, Boundary.h)
  val window = Point(Window.w, Window.h)
  val textLineHeight = 14

  var currentRank = List.empty[Score]
  var historyRank = List.empty[Score]
  var myId = -1l

  val grid = new GridOnClient(bounds)

  var firstCome = true
  //长连接状态
  var wsSetup = false
  var justSynced = false
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
    KeyCode.F2
  )

  object MyColors {
    val rankList = "rgba(0, 0, 0, 0.64)"
    val background = "#fff"
    val stripe = "rgba(181, 181, 181, 0.5)"
    val myHeader = "#cccccc"
    val myBody = "#FFFFFF"
    val otherHeader = "rgba(78,69,69,0.82)"
    val otherBody = "#696969"
  }

  private[this] val nameField = dom.document.getElementById("name").asInstanceOf[HTMLInputElement]
  private[this] val joinButton = dom.document.getElementById("join").asInstanceOf[HTMLButtonElement]
  private[this] val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    drawGameOff()
    canvas.width = window.x
    canvas.height = window.y

    joinButton.onclick = { (event: MouseEvent) =>
      joinGame(nameField.value)
      event.preventDefault()
    }
    nameField.focus()
    nameField.onkeypress = { (event: KeyboardEvent) =>
      if (event.keyCode == 13) {
        joinButton.click()
        event.preventDefault()
      }
    }
//每隔一段间隔就执行gameLoop（同步更新，重画）
    dom.window.setInterval(() => gameLoop(), Protocol.frameRate)
  }

//绘制背景
  def drawGameOn(): Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }
//边框;提示文字
  def drawGameOff(): Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, window.x , window.y )
    ctx.fillStyle = "rgb(250, 250, 250)"
    if (firstCome) {
      ctx.font = "36px Helvetica"
      ctx.fillText("Welcome.", 150, 180)
    } else {
      ctx.font = "36px Helvetica"
      ctx.fillText("Ops, connection lost.", 150, 180)
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
      val data = grid.getGridData
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
    val basePoint= players.filter(_.id==uid).map(a=>(a.x,a.y)).headOption.getOrElse((bounds.x/2,bounds.y/2))
    //println(s"basePoint${basePoint}")
    val offx = window.x/2 - basePoint._1
    val offy =window.y/2 - basePoint._2
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
//绘制条纹
    ctx.strokeStyle = MyColors.stripe
    stripeX.map{l=>
      ctx.beginPath()
      ctx.moveTo(0,l +offy);
      ctx.lineTo(bounds.x,l +offy);
      ctx.stroke();
    }
    stripeY.map{l=>
      ctx.beginPath()
      ctx.moveTo(l +offx,0);
      ctx.lineTo(l +offx,bounds.y);
      ctx.stroke();
    }

//区分本玩家和其他玩家蛇身体的颜色
    ctx.fillStyle = MyColors.otherBody
    //TODO 拖尾效果
    players.foreach { case Player(id, name,color,x,y,tx,ty,kill,pro,_,cells) =>
      //println(s"draw body at $p body[$life]")
      cells.map{cell=>
          ctx.fillStyle = color.toInt match{
            case 0 => "red"
            case 1 => "orange"
            case 2  => "yellow"
            case 3  => "green"
            case 4  => "blue"
            case 5  => "purple"
            case 6  => "black"
            case _  => "blue"
          }
          ctx.beginPath()
          ctx.arc(cell.x +offx,cell.y +offy,cell.radius,0,2*Math.PI)
          ctx.fill()
        ctx.font = "24px Helvetica"
        ctx.fillStyle = MyColors.background
        ctx.fillText(s"${name}", cell.x +offx-12, cell.y +offy -18)
      }
    }
//为不同分值的苹果填充不同颜色
    foods.foreach { case Food(color, x, y) =>
      ctx.fillStyle = color match{
        case 0 => "red"
        case 1 => "orange"
        case 2  => "yellow"
        case 3  => "green"
        case 4  => "blue"
        case 5  => "purple"
        case 6  => "black"
        case _  => "blue"
      }
      ctx.beginPath()
      ctx.arc(x +offx,y +offy,4,0,2*Math.PI)
      ctx.fill()
    }
    masses.foreach { case Mass(x,y,_,_,color,mass,r,_) =>
      ctx.fillStyle = color match{
        case 0 => "red"
        case 1 => "orange"
        case 2  => "yellow"
        case 3  => "green"
        case 4  => "blue"
        case 5  => "purple"
        case 6  => "black"
        case _  => "blue"
      }
      ctx.beginPath()
      ctx.arc(x +offx,y +offy,r,0,2*Math.PI)
      ctx.fill()
    }
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
        ctx.fillText(s"KILL: ${myStar.kill}", 30, 10)
        ctx.fillText(s"SCORE: ${myStar.cells.map(_.mass).sum}", 300, 10)
        ctx.restore()
      case None =>
        if(firstCome) {
          ctx.font = "36px Helvetica"
          ctx.fillText("Please wait.", 150, 180)
        } else {
          ctx.font = "36px Helvetica"
          ctx.fillText("Ops, Press Space Key To Restart!", 150, 180)
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


  }
//绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

//新用户加入游戏
  def joinGame(name: String): Unit = {
    joinButton.disabled = true
    val playground = dom.document.getElementById("playground")
    playground.innerHTML = s"Trying to join game as '$name'..."
    val gameStream = new WebSocket(getWebSocketUri(dom.document, name))
    gameStream.onopen = { (event0: Event) =>
      println("come here")
      drawGameOn()
      playground.insertBefore(p("Game connection was successful!"), playground.firstChild)
      wsSetup = true
      canvas.focus()
      //在画布上监听键盘事件
      canvas.onkeydown = {
        (e: dom.KeyboardEvent) => {
          println(s"keydown: ${e.keyCode}")
          if (watchKeys.contains(e.keyCode)) {
            println(s"key down: [${e.keyCode}]")
            if (e.keyCode == KeyCode.F2) {
              gameStream.send("T" + System.currentTimeMillis())
            } else {
              println(s"down+${e.keyCode.toString}")
              gameStream.send(e.keyCode.toString)
            }
            e.preventDefault()
          }
        }
      }
      canvas.onmousemove = {(e:dom.MouseEvent)=>{
        gameStream.send(MousePosition(e.pageX,e.pageY).asJson.noSpaces)
      }

      }

//      canvas.onkeyup = {
//        (e: dom.KeyboardEvent) => {
//          println(s"up: ${e.keyCode}")
//          if (watchKeys.contains(e.keyCode)) {
//            println(s"key up: [${e.keyCode}]")
//            if (e.keyCode == KeyCode.F2) {
//              gameStream.send("T" + System.currentTimeMillis())
//            } else {
//              gameStream.send(e.keyCode.toString)
//            }
//            e.preventDefault()
//          }
//        }
//      }
      event0
    }

    gameStream.onerror = { (event: ErrorEvent) =>
      drawGameOff()
      playground.insertBefore(p(s"Failed: code: ${event.colno}"), playground.firstChild)
      joinButton.disabled = false
      wsSetup = false
      nameField.focus()
    }


    import io.circe.generic.auto._
    import io.circe.parser._

    gameStream.onmessage = { (event: MessageEvent) =>
      //val wsMsg = read[Protocol.GameMessage](event.data.toString)
      val wsMsg = decode[Protocol.GameMessage](event.data.toString).right.get
      wsMsg match {
        case Protocol.Id(id) => myId = id
        case Protocol.TextMsg(message) => writeToArea(s"MESSAGE: $message")
        case Protocol.NewSnakeJoined(id, user) => writeToArea(s"$user joined!")
        case Protocol.PlayerLeft(id, user) => writeToArea(s"$user left!")
        case a@Protocol.SnakeAction(id, keyCode, frame) =>
          if (frame > grid.frameCount) {
            writeToArea(s"!!! got snake action=$a when i am in frame=${grid.frameCount}")
          } else {
            writeToArea(s"got snake action=$a")
          }
          grid.addActionWithFrame(id, keyCode, frame)

        case a@Protocol.SnakeMouseAction(id, x, y, frame) =>
          if (frame > grid.frameCount) {
            writeToArea(s"!!! got snake mouse action=$a when i am in frame=${grid.frameCount}")
          } else {
            writeToArea(s"got snake mouse action=$a")
          }
          grid.addMouseActionWithFrame(id, x, y, frame)

        case Protocol.Ranks(current, history) =>
          //writeToArea(s"rank update. current = $current") //for debug.
          currentRank = current
          historyRank = history
        case Protocol.FeedApples(foods) =>
          writeToArea(s"food feeded = $foods") //for debug.
          grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)
        case data: Protocol.GridDataSync =>
          //writeToArea(s"grid data got: $msgData")
          //TODO here should be better code.
          grid.actionMap = grid.actionMap.filterKeys(_ > data.frameCount)
          grid.frameCount = data.frameCount
          grid.playerMap = data.playerDetails.map(s => s.id -> s).toMap
          grid.food = data.foodDetails.map(a => Point(a.x, a.y) -> a.color).toMap
          grid.massList = data.massDetails
//          val starMap = data.stars.map(b => Point(b.center.x, b.center.y) -> Center(b.id, b.radius,b.score)).toMap
//          val gridMap = appleMap ++ starMap
//          grid.grid = gridMap
          justSynced = true
        //drawGrid(msgData.uid, data)
        case Protocol.NetDelayTest(createTime) =>
          val receiveTime = System.currentTimeMillis()
          val m = s"Net Delay Test: createTime=$createTime, receiveTime=$receiveTime, twoWayDelay=${receiveTime - createTime}"
          writeToArea(m)
      }
    }

    gameStream.onclose = { (event: Event) =>
      drawGameOff()
      playground.insertBefore(p("Connection to game lost. You can try to rejoin manually."), playground.firstChild)
      joinButton.disabled = false
      wsSetup = false
      nameField.focus()
    }
//写入消息区
    def writeToArea(text: String): Unit =
      playground.insertBefore(p(text), playground.firstChild)
  }

  def getWebSocketUri(document: Document, nameOfChatParticipant: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/gypsy/netSnake/join?name=$nameOfChatParticipant"
  }

  def p(msg: String) = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = msg
    paragraph
  }


}
