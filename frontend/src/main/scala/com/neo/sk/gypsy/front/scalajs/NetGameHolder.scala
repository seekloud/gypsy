package com.neo.sk.gypsy.front.scalajs

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.shared.ptcl._

import scalatags.JsDom.all._
import scala.scalajs.js.JSApp
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.math._
import com.neo.sk.gypsy.front.utils.byteObject.MiddleBufferInJs
import com.neo.sk.gypsy.shared.util.utils.getZoomRate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

import scala.collection.mutable
/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 12:45 PM
  * 绘制画面，建立ws连接，接收消息
  */
object NetGameHolder extends js.JSApp {


  val bounds = Point(Boundary.w, Boundary.h)
  val window = Point(dom.window.innerWidth.toInt, dom.window.innerHeight.toInt)

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
  var syncGridData: scala.Option[GridDataSync] = None

  private[this] val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas2 = dom.document.getElementById("MiddleView").asInstanceOf[Canvas]
  private[this] val ctx2 = canvas2.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas3 = dom.document.getElementById("TopView").asInstanceOf[Canvas]
  private[this] val ctx3 = canvas3.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val img = dom.document.getElementById("virus").asInstanceOf[HTMLElement]
  private[this] val windWidth = dom.window.innerWidth

  private[this] val offScreenCanvas = dom.document.getElementById("offScreen").asInstanceOf[Canvas]
  private[this] val offCtx = offScreenCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] final val maxRollBackFrames = 5
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private[this] val uncheckActionWithFrame = new mutable.HashMap[Int,(Long,Long,GameAction)]()
  private[this] val gameSnapshotMap = new mutable.HashMap[Long,GridDataSync]()


  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    drawGameOff()
    canvas.width = window.x
    canvas.height = window.y
    canvas2.width = window.x
    canvas2.height = window.y
    canvas3.width = window.x
    canvas3.height = window.y
    offScreenCanvas.width = bounds.x
    offScreenCanvas.height = bounds.y
    drawBackground()

    dom.window.onload= {
      (_: Event) =>
      LoginPage.homePage()
    }
    }

  def getActionSerialNum=actionSerialNumGenerator.getAndIncrement()


  def startGame(gameStream: WebSocket): Unit = {
    println("start---")
    drawGameOn()
    draw2()
    dom.window.setInterval(() => gameLoop(gameStream: WebSocket), frameRate)
    dom.window.requestAnimationFrame(gameRender())
  }

  def gameRender(): Double => Unit = { d =>
    //println("gameRender-gameRender")
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    if(myId != -1l) {
      draw(offsetTime)
    }

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
  def drawBackground():Unit = {
    //绘制背景
    offCtx.fillStyle = MyColors.background
    offCtx.fillRect(0,0,bounds.x,bounds.y)
    offCtx.save()
    //绘制条纹
    offCtx.strokeStyle = MyColors.stripe
    stripeX.foreach{ l=>
      //ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      offCtx.beginPath()
      offCtx.moveTo(0 ,l )
      offCtx.lineTo(bounds.x ,l )
      offCtx.stroke()
      //ctx.restore()
    }
    stripeY.foreach{ l=>
      //ctx.save()
      //centerScale(scale,window.x/2,window.y/2)
      offCtx.beginPath()
      offCtx.moveTo(l ,0)
      offCtx.lineTo(l ,bounds.y)
      offCtx.stroke()
      // ctx.restore()
    }
  }

//不同步就更新，同步就设置为不同步
  def gameLoop(gameStream: WebSocket): Unit = {
    NetDelay.ping(gameStream)
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

  def draw2():Unit = {
    //绘制当前排行
    ctx2.fillStyle = MyColors.rankList
    ctx2.fillRect(window.x-200,20,150,250)

    //绘制小地图
    ctx2.font = "12px Helvetica"
    ctx2.fillStyle = MyColors.rankList
    ctx2.fillRect(mapMargin,mapMargin,littleMap,littleMap)
    ctx2.strokeStyle = "black"
    for (i<- 0 to 3){
      ctx2.beginPath()
      ctx2.moveTo(mapMargin + i * littleMap/3, mapMargin)
      ctx2.lineTo(mapMargin + i * littleMap/3,mapMargin+littleMap)
      ctx2.stroke()

      ctx2.beginPath()
      ctx2.moveTo(mapMargin , mapMargin+ i * littleMap/3)
      ctx2.lineTo(mapMargin+littleMap ,mapMargin+ i * littleMap/3)
      ctx2.stroke()
    }
    val margin = littleMap/3
    ctx2.fillStyle = MyColors.background
    for(i <- 0 to 2){
      for (j <- 1 to 3){
        ctx2.fillText((i*3+j).toString,mapMargin + abs(j-1)*margin+0.5*margin,mapMargin + i*margin+0.5*margin)
      }
    }
  }
  def draw(offsetTime:Long): Unit = {
    //println("开始绘画")
    if (wsSetup) {
    //  println(s"连接建立 ${wsSetup}")
      //println(s"myid$myId")
      val data = grid.getGridData(myId)
      //println(s"data$data")
      drawGrid(myId, data,offsetTime)
      renderFps(ctx3,NetDelay.latency)
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
    var zoom = (30.0, 30.0)

    val basePoint = players.find(_.id == uid) match {
      case Some(p) =>
        var sumX = 0.0
        var sumY = 0.0
        var length = 0
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
          //println(s"编号${length},中心$newX,$newY")
        }
        val offx = sumX /p.cells.length
        val offy = sumY /p.cells.length

        //println(s"offx${offx},中心$offx,$offy")
        (offx, offy)
      case None =>
        (bounds.x.toDouble / 2, bounds.y.toDouble / 2)
    }
   // println(s"offsetTime：${offsetTime},basepoint${basePoint._1},${basePoint._2}")

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
//    ctx.strokeStyle = MyColors.stripe
    //    stripeX.foreach{ l=>
    //      ctx.beginPath()
    //      ctx.moveTo(0 +offx,l +offy)
    //      ctx.lineTo(bounds.x +offx,l +offy)
    //      ctx.stroke()
    //    }
    //    stripeY.foreach{ l=>
    //      ctx.beginPath()
    //      ctx.moveTo(l +offx,0+offy)
    //      ctx.lineTo(l +offx,bounds.y+offy)
    //      ctx.stroke()
    //    }
    ctx.drawImage(offScreenCanvas,offx,offy,bounds.x,bounds.y)
//为不同分值的苹果填充不同颜色
    //按颜色分类绘制，减少canvas状态改变
    foods.groupBy(_.color).foreach{a=>
      ctx.fillStyle = a._1 match{
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      a._2.foreach{ case Food(color, x, y)=>
        ctx.beginPath()
        ctx.arc(x +offx,y +offy,10,0,2*Math.PI)
        ctx.fill()
      }
    }
    masses.groupBy(_.color).foreach{ a=>
      ctx.fillStyle = a._1 match{
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      a._2.foreach{case Mass(x,y,tx,ty,color,mass,r,speed) =>
        val deg = Math.atan2(ty, tx)
        val deltaY = speed * Math.sin(deg)
        val deltaX = speed * Math.cos(deg)
        val xPlus = if (!deltaX.isNaN) deltaX else 0
        val yPlus = if (!deltaY.isNaN) deltaY else 0

        val cellx = x +xPlus*offsetTime.toFloat / WsMsgProtocol.frameRate
        val celly = y  +yPlus*offsetTime.toFloat / WsMsgProtocol.frameRate
        val xfix  = if(cellx>bounds.x) bounds.x else if(cellx<0) 0 else cellx
        val yfix = if(celly>bounds.y) bounds.y else if(celly<0) 0 else celly
        //centerScale(scale,window.x/2,window.y/2)
        ctx.beginPath()
        ctx.arc( xfix+offx ,yfix+offy ,r,0,2*Math.PI)
        ctx.fill()
      }
    }

    players.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,_,killerName,width,height,cells,startTime) =>
      ctx.fillStyle = color.toInt match{
        case 0 => "#f3456d"  //(243,69,109)   b30e35
        case 1 => "#f49930"  //(244, 153, 48)  a65d0a
        case 2  => "#f4d95b"  //244, 217, 91   917600
        case 3  => "#4cd964"   //76, 217, 100  05851b
        case 4  => "#9fe0f6"   //159, 224, 246  037da6
        case 5  => "#bead92"   //190, 173, 146   875a16
        case 6  => "#cfe6ff"  //207, 230, 255   4174ab
        case _  => "#de9dd6"   //222, 157, 214   8f3284
      }
      cells.foreach{ cell=>
        val cellx = cell.x + cell.speedX *offsetTime.toFloat / WsMsgProtocol.frameRate
        val celly = cell.y + cell.speedY *offsetTime.toFloat / WsMsgProtocol.frameRate
        val xfix  = if(cellx>bounds.x-15) bounds.x-15 else if(cellx<15) 15 else cellx
        val yfix = if(celly>bounds.y-15) bounds.y-15 else if(celly<15) 15 else celly
        ctx.save()

        //if(xfix>=cell.radius+5&&xfix<=bounds.x-cell.radius-5&&yfix>=cell.radius+5&&yfix<=bounds.y-cell.radius-5){
          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#bf3558" //粉
            case 1 => "#cc8a43"  //棕
            case 2  => "#bd9e2f"  //棕绿
            case 3  => "#22a834"   //绿
            case 4  => "#3da2c4"   //天蓝
            case 5  => "#b88237"   //深棕
            case 6  => "#558cc2"   //深蓝
            case _  => "#ad43a2"   //紫
          }
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+5,0,2*Math.PI)
         // DrawCircle.drawCircle(ctx,xfix+offx,yfix+offy,cell.radius+5)
          ctx.fill()
          ctx.restore()
          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#cc4a6c"
            case 1 => "#d69753"
            case 2  => "#ccae43"
            case 3  => "#3dbf4f"
            case 4  => "#50b1d1"
            case 5  => "#cf9748"
            case 6  => "#74a7de"
            case _  => "#cc64c2"
          }
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+2,0,2*Math.PI)
          //DrawCircle.drawCircle(ctx,xfix+offx,yfix+offy,cell.radius+2)
          ctx.fill()
          ctx.restore()

          ctx.beginPath()
          ctx.arc(xfix +offx,yfix+offy,cell.radius-1,0,2*Math.PI)
          //DrawCircle.drawCircle(ctx,xfix+offx,yfix+offy,cell.radius-1)
          ctx.fill()
          if(protect){
            ctx.fillStyle = MyColors.halo
            ctx.beginPath()
            ctx.arc(xfix+offx,yfix+offy,cell.radius+15,0,2*Math.PI)
            ctx.fill()
          }
        //Todo chubi jiya
        /*}else if(xfix<cell.radius+5){
          val deg=acos(xfix/(cell.radius+5))

          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#bf3558" //粉
            case 1 => "#cc8a43"  //棕
            case 2  => "#bd9e2f"  //棕绿
            case 3  => "#22a834"   //绿
            case 4  => "#3da2c4"   //天蓝
            case 5  => "#b88237"   //深棕
            case 6  => "#558cc2"   //深蓝
            case _  => "#ad43a2"   //紫
          }
          ctx.beginPath()
          ctx.lineWidth=10
          ctx.moveTo(offx,yfix+offy+sqrt(pow(cell.radius+5,2)-pow(xfix,2)))
          ctx.lineTo(offx,yfix+offy-sqrt(pow(cell.radius+5,2)-pow(xfix,2)))
          ctx.stroke()
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+5,deg-Math.PI,Math.PI-deg)
          ctx.closePath()
          ctx.fill()
          ctx.restore()

          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#cc4a6c"
            case 1 => "#d69753"
            case 2  => "#ccae43"
            case 3  => "#3dbf4f"
            case 4  => "#50b1d1"
            case 5  => "#cf9748"
            case 6  => "#74a7de"
            case _  => "#cc64c2"
          }
          ctx.beginPath()
          ctx.lineWidth=5
          ctx.moveTo(offx+5*cos(deg),yfix+offy+sqrt(pow(cell.radius+5,2)-pow(xfix,2))-3*sin(deg))
          ctx.lineTo(offx+5*cos(deg),yfix+offy-sqrt(pow(cell.radius+5,2)-pow(xfix,2))+3*sin(deg))
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+2,deg-Math.PI,Math.PI-deg)
          ctx.closePath()
          ctx.fill()
          ctx.restore()

          ctx.beginPath()
          ctx.moveTo(offx+10*cos(deg),yfix+offy+sqrt(pow(cell.radius,2)-pow(xfix,2))-5*sin(deg))
          ctx.lineTo(offx+10*cos(deg),yfix+offy-sqrt(pow(cell.radius,2)-pow(xfix,2))+5*sin(deg))
          ctx.beginPath()
          ctx.arc(xfix +offx,yfix+offy,cell.radius-1,deg-Math.PI,Math.PI-deg)
          ctx.closePath()
          ctx.fill()
        }else if(yfix<cell.radius+5){

          val deg=asin(yfix/(cell.radius+5))

          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#bf3558" //粉
            case 1 => "#cc8a43"  //棕
            case 2  => "#bd9e2f"  //棕绿
            case 3  => "#22a834"   //绿
            case 4  => "#3da2c4"   //天蓝
            case 5  => "#b88237"   //深棕
            case 6  => "#558cc2"   //深蓝
            case _  => "#ad43a2"   //紫
          }
          ctx.beginPath()
          ctx.lineWidth=10
          ctx.moveTo(xfix+sqrt(pow(cell.radius+5,2)-pow(yfix,2)),0)
          ctx.lineTo(xfix-sqrt(pow(cell.radius+5,2)-pow(yfix,2)),0)
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+5,-deg,deg-Math.PI)
          ctx.closePath()
          ctx.fill()
          ctx.restore()

          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#cc4a6c"
            case 1 => "#d69753"
            case 2  => "#ccae43"
            case 3  => "#3dbf4f"
            case 4  => "#50b1d1"
            case 5  => "#cf9748"
            case 6  => "#74a7de"
            case _  => "#cc64c2"
          }
          ctx.beginPath()
          ctx.lineWidth=5
          ctx.moveTo(xfix+sqrt(pow(cell.radius+2,2)-pow(yfix,2))-5*cos(deg),0+5*sin(deg))
          ctx.lineTo(xfix-sqrt(pow(cell.radius+2,2)-pow(yfix,2))+5*cos(deg),0+5*sin(deg))
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+2,-deg,deg-Math.PI)
          ctx.closePath()
          ctx.fill()
          ctx.restore()

          ctx.beginPath()
          ctx.moveTo(xfix+sqrt(pow(cell.radius,2)-pow(yfix,2))-5*cos(deg),0+10*sin(deg))
          ctx.lineTo(xfix-sqrt(pow(cell.radius,2)-pow(yfix,2))+5*cos(deg),0+10*sin(deg))
          ctx.beginPath()
          ctx.arc(xfix +offx,yfix+offy,cell.radius-1,-deg,deg-Math.PI)
          ctx.closePath()
          ctx.fill()

        }else if(xfix>bounds.x-cell.radius-5){

          val deg=asin((bounds.x-xfix)/(cell.radius+5))

          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#bf3558" //粉
            case 1 => "#cc8a43"  //棕
            case 2  => "#bd9e2f"  //棕绿
            case 3  => "#22a834"   //绿
            case 4  => "#3da2c4"   //天蓝
            case 5  => "#b88237"   //深棕
            case 6  => "#558cc2"   //深蓝
            case _  => "#ad43a2"   //紫
          }
          ctx.beginPath()
          ctx.lineWidth=10
          ctx.moveTo(bounds.x+offx-20,yfix+sqrt(pow(cell.radius+5,2)-pow(bounds.x-xfix,2)))
          ctx.lineTo(bounds.x+offx-20,yfix-sqrt(pow(cell.radius+5,2)-pow(bounds.x-xfix,2)))
          println(bounds.x+offx-20)
          println(xfix+offx)
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+5,deg,-deg,false)
          ctx.closePath()
          ctx.fill()
          ctx.restore()

          ctx.save()
          ctx.fillStyle = color.toInt match{
            case 0 => "#cc4a6c"
            case 1 => "#d69753"
            case 2  => "#ccae43"
            case 3  => "#3dbf4f"
            case 4  => "#50b1d1"
            case 5  => "#cf9748"
            case 6  => "#74a7de"
            case _  => "#cc64c2"
          }
          ctx.beginPath()
          ctx.lineWidth=5
          ctx.moveTo(bounds.x-5*cos(deg)+offx-20,yfix+sqrt(pow(cell.radius+5,2)-pow(bounds.x-xfix,2))-3*sin(deg))
          ctx.lineTo(bounds.x-5*cos(deg)+offx-20,yfix-sqrt(pow(cell.radius+5,2)-pow(bounds.x-xfix,2))+3*sin(deg))
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+2,deg,-deg,false)
          ctx.closePath()
          ctx.fill()
          ctx.restore()

          ctx.beginPath()
          ctx.moveTo(bounds.x-10*cos(deg)+offx-20,yfix+sqrt(pow(cell.radius,2)-pow(bounds.x-xfix,2))-5*sin(deg))
          ctx.lineTo(bounds.x+10*cos(deg)+offx-20,yfix-sqrt(pow(cell.radius,2)-pow(bounds.x-xfix,2))+5*sin(deg))
          ctx.beginPath()
          ctx.arc(xfix +offx,yfix+offy,cell.radius-1,deg,-deg,false)
          ctx.closePath()
          ctx.fill()

        }else if(yfix>bounds.y-cell.radius-5){

        }*/

        var nameFont: Double = cell.radius * 2 / sqrt(4 + pow(name.length, 2))
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius else nameFont
        // println(nameFont)
        ctx.font = s"${nameFont.toInt}px Helvetica"
        val nameWidth = ctx.measureText(name).width
        ctx.strokeStyle = "grey"
        ctx.strokeText(s"$name", xfix + offx - nameWidth / 2, yfix + offy - (nameFont.toInt / 2 + 2))
        ctx.fillStyle = MyColors.background
        ctx.fillText(s"$name", xfix + offx - nameWidth / 2, yfix + offy - (nameFont.toInt / 2 + 2))
        ctx.restore()
      }
    }

    virus.foreach { case Virus(x,y,mass,radius,_) =>
      ctx.save()
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
        //ctx.font = "12px Helvetica"
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
    ctx3.clearRect(0,0,window.x,window.y)
    ctx3.font = "12px Helvetica"
//    ctx.fillStyle = MyColors.rankList
//    ctx.fillRect(window.x-200,20,150,250)
    val currentRankBaseLine = 4
    var index = 0
    ctx3.fillStyle = MyColors.background
    drawTextLine(s"—————排行榜—————", window.x-200, index, currentRankBaseLine)
    currentRank.foreach { score =>
      index += 1
      drawTextLine(s"【$index】: ${score.n.+("   ").take(4)} score=${score.score}", window.x-195, index, currentRankBaseLine)
    }
    //绘制小地图

    ctx3.fillStyle = MyColors.background
    players.find(_.id == uid) match {
      case Some(player)=>
        ctx3.beginPath()
        ctx3.arc(mapMargin + (basePoint._1/bounds.x) * littleMap,mapMargin + basePoint._2/bounds.y * littleMap,8,0,2*Math.PI)
        ctx3.fill()
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
    ctx3.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

//新用户加入游戏
def joinGame(room: String, name: String, userType: Int = 0, maxScore: Int = 0): Unit = {
  val playground = dom.document.getElementById("playground")
  val gameStream = new WebSocket(UserRoute.getWebSocketUri(dom.document, room, name, userType))
  gameStream.onopen = { (event0: Event) =>
    println("come here")
    startGame(gameStream)
    // playground.insertBefore(p("Game connection was successful!"), playground.firstChild)
    isDead = false
    wsSetup = true
    canvas3.focus()
    //在画布上监听键盘事件
    canvas3.onkeydown = {
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
            val keyCode=WsMsgProtocol.KeyCode(myId,e.keyCode,grid.frameCount+advanceFrame,getActionSerialNum)
            grid.addActionWithFrame(myId, keyCode, grid.frameCount+advanceFrame)
            addUncheckActionWithFrame(myId,keyCode,grid.frameCount+advanceFrame)
            sendMsg(keyCode, gameStream)
          }
          e.preventDefault()
        }
      }
    }
    //在画布上监听鼠标事件
    canvas3.onmousemove = { (e: dom.MouseEvent) => {
      val mp=MousePosition(myId,e.pageX - window.x / 2, e.pageY - 48 - window.y.toDouble / 2,grid.frameCount+advanceFrame,getActionSerialNum)
      grid.addMouseActionWithFrame(myId,mp,mp.frame)
      addUncheckActionWithFrame(myId,mp,mp.frame)
      //gameStream.send(MousePosition(e.pageX-windWidth/2, e.pageY-48-window.y.toDouble/2).asJson.noSpaces)
      sendMsg(mp, gameStream)
      //println(s"pageX${e.pageX},pageY${e.pageY},X${e.pageX - windWidth / 2},Y${e.pageY - 48 - window.y.toDouble / 2}")

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
    event.data match {
      case blobMsg: Blob =>
        val fr = new FileReader()
        fr.readAsArrayBuffer(blobMsg)
        fr.onloadend = { _: Event =>
          val buf = fr.result.asInstanceOf[ArrayBuffer]
          val middleDataInJs = new MiddleBufferInJs(buf)
          bytesDecode[WsMsgFront](middleDataInJs) match {
            case Right(data) =>
              data match {
                case WsMsgProtocol.Id(id) =>
                  myId = id
                  println(s"myID:$myId")

                case m:WsMsgProtocol.KeyCode =>
                  addActionWithFrameFromServer(m.id,m)

                case m:WsMsgProtocol.MousePosition =>
                  addActionWithFrameFromServer(m.id,m)

                case WsMsgProtocol.Ranks(current, history) =>

                  currentRank = current
                  historyRank = history
                case WsMsgProtocol.FeedApples(foods) =>

                  grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)

                case data: WsMsgProtocol.GridDataSync =>
                  //TODO here should be better code.
                  if(data.frameCount<grid.frameCount){
                    println(s"丢弃同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
                  }else if(data.frameCount>grid.frameCount){
                    println(s"同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
                    syncGridData = Some(data)
                    justSynced = true
                  }

                //drawGrid(msgData.uid, data)
                  //网络延迟检测
                case WsMsgProtocol.Pong(createTime) =>
                  NetDelay.receivePong(createTime ,gameStream)

                case WsMsgProtocol.SnakeRestart(id) =>

                  //timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)

                case WsMsgProtocol.UserDeadMessage(id,_,killerName,killNum,score,lifeTime)=>
                  if(id==myId){
                    DeadPage.deadModel(id,killerName,killNum,score,lifeTime,maxScore,gameStream)
                    grid.removePlayer(id)
                  }

                case WsMsgProtocol.KillMessage(killerId,deadId)=>
                  grid.removePlayer(deadId)
                  val a = grid.playerMap.getOrElse(killerId, Player(0, "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
                  grid.playerMap += (killerId -> a.copy(kill = a.kill + 1))
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


  def addUncheckActionWithFrame(id: Long, gameAction: GameAction, frame: Long) = {
    uncheckActionWithFrame.put(gameAction.serialNum,(frame,id,gameAction))
  }

  def addActionWithFrameFromServer(id:Long,gameAction:GameAction) = {
    val frame=gameAction.frame
    if(myId == id){
      uncheckActionWithFrame.get(gameAction.serialNum) match {
        case Some((f,tankId,a)) =>
          if(f == frame){ //与预执行的操作数据一致
            uncheckActionWithFrame.remove(gameAction.serialNum)
          }else{ //与预执下的操作数据不一致，进行回滚
            uncheckActionWithFrame.remove(gameAction.serialNum)
            if(frame < grid.frameCount){
              rollback(frame)
            }else{
              grid.removeActionWithFrame(tankId,a,f)
              gameAction match {
                case a:KeyCode=>
                  grid.addActionWithFrame(id,a,frame)
                case b:MousePosition=>
                  grid.addMouseActionWithFrame(id,b,frame)
              }
            }
          }
        case None =>
          gameAction match {
            case a:KeyCode=>
              grid.addActionWithFrame(id,a,frame)
            case b:MousePosition=>
              grid.addMouseActionWithFrame(id,b,frame)
          }
      }
    }else{
      if(frame < grid.frameCount && grid.frameCount - maxRollBackFrames >= frame){
        //回滚
        rollback(frame)
      }else{
        gameAction match {
          case a:KeyCode=>
            grid.addActionWithFrame(id,a,frame)
          case b:MousePosition=>
            grid.addMouseActionWithFrame(id,b,frame)
        }
      }
    }
  }

  def rollback2State(d:GridDataSync) = {
    grid.actionMap=grid.actionMap.filterKeys(_>=grid.frameCount)
    grid.mouseActionMap=grid.mouseActionMap.filterKeys(_>=grid.frameCount)
    setSyncGridData(d)
  }


  //从第frame开始回滚到现在
  def rollback(frame:Long) = {
    gameSnapshotMap.get(frame) match {
      case Some(state) =>
        val curFrame = grid.frameCount
        rollback2State(state)
        uncheckActionWithFrame.filter(_._2._1 > frame).foreach{t=>
          t._2._3 match {
            case a:KeyCode=>
              grid.addActionWithFrame(t._2._2,a,frame)
            case b:MousePosition=>
              grid.addMouseActionWithFrame(t._2._2,b,frame)
          }
        }
        (frame until curFrame).foreach{ f =>
          grid.frameCount = f
          update()
        }
      case None =>
    }
  }

  //fixme 此处存在重复计算问题

  def setSyncGridData(data:GridDataSync): Unit = {

    grid.actionMap = grid.actionMap.filterKeys(_ > data.frameCount- maxDelayFrame)
    grid.mouseActionMap = grid.mouseActionMap.filterKeys(_ > data.frameCount-maxDelayFrame)
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


  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(8192)

  def sendMsg(msg:WsMsgServer, gameStream: WebSocket) = {
    import com.neo.sk.gypsy.front.utils.byteObject.ByteObject._
    gameStream.send(msg.fillMiddleBuffer(sendBuffer).result())

  }


}
