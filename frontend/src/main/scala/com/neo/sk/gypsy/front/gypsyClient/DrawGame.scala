package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.front.scalajs.NetDelay
import scalatags.JsDom.short._
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas
import com.neo.sk.gypsy.shared.util.utils._
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.html
import com.neo.sk.gypsy.front.utils.EchartsJs

import scala.collection.mutable.ArrayBuffer
import scala.math._
import io.circe.generic.auto._
import io.circe.syntax._
import com.neo.sk.gypsy.shared.ptcl.Protocol.GridDataSync
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.front.scalajs.FpsComponent.renderFps

import scala.util.Random

/**
  * User: sky
  * Date: 2018/9/14
  * Time: 10:04
  */
case class DrawGame(
              ctx:CanvasRenderingContext2D,
              canvas:Canvas,
              size:Point
              ) {
//
//  private[this] val  virusImg = dom.document.getElementById("virus").asInstanceOf[HTMLElement]
  private[this]val virusImg = img(*.style := "width:3600px;height:1800px;display:none")(*.src := s"/gypsy/static/img/stone.png").render

  //  private[this] val  circle = dom.document.getElementById("circle").asInstanceOf[HTMLElement]
//  private[this] val  circle1 = dom.document.getElementById("circle1").asInstanceOf[HTMLElement]
//  private[this] val  circle2 = dom.document.getElementById("circle2").asInstanceOf[HTMLElement]
//  private[this] val  circle3 = dom.document.getElementById("circle3").asInstanceOf[HTMLElement]
//  private[this] val  circle4 = dom.document.getElementById("circle4").asInstanceOf[HTMLElement]
//  private[this] val  circle5 = dom.document.getElementById("circle5").asInstanceOf[HTMLElement]
//  private[this] val  circle6 = dom.document.getElementById("circle6").asInstanceOf[HTMLElement]
  private[this] val  star1 = dom.document.getElementById("yuzhouxingqiu-01").asInstanceOf[HTMLElement]
  private[this] val  star2 = dom.document.getElementById("yuzhouxingqiu-02").asInstanceOf[HTMLElement]
  private[this] val  star3 = dom.document.getElementById("yuzhouxingqiu-03").asInstanceOf[HTMLElement]
  private[this] val  star4 = dom.document.getElementById("yuzhouxingqiu-04").asInstanceOf[HTMLElement]
  private[this] val  star5 = dom.document.getElementById("yuzhouxingqiu-05").asInstanceOf[HTMLElement]
  private[this] val  star6 = dom.document.getElementById("yuzhouxingqiu-06").asInstanceOf[HTMLElement]
  private[this] val  star7 = dom.document.getElementById("yuzhouxingqiu-07").asInstanceOf[HTMLElement]
  private[this] val  star8 = dom.document.getElementById("yuzhouxingqiu-08").asInstanceOf[HTMLElement]
  private[this] val  star9 = dom.document.getElementById("yuzhouxingqiu-09").asInstanceOf[HTMLElement]
  private[this] val  star10 = dom.document.getElementById("yuzhouxingqiu-10").asInstanceOf[HTMLElement]
  private[this] val  star11= dom.document.getElementById("yuzhouxingqiu-11").asInstanceOf[HTMLElement]
  private[this] val  star12 = dom.document.getElementById("yuzhouxingqiu-12").asInstanceOf[HTMLElement]
  private[this] val  star13 = dom.document.getElementById("yuzhouxingqiu-13").asInstanceOf[HTMLElement]
  private[this] val  star14 = dom.document.getElementById("yuzhouxingqiu-14").asInstanceOf[HTMLElement]
  private[this] val  star15 = dom.document.getElementById("yuzhouxingqiu-15").asInstanceOf[HTMLElement]
  private[this] val  star16 = dom.document.getElementById("yuzhouxingqiu-16").asInstanceOf[HTMLElement]
  private[this] val  star17 = dom.document.getElementById("yuzhouxingqiu-17").asInstanceOf[HTMLElement]
  private[this] val  star18 = dom.document.getElementById("yuzhouxingqiu-18").asInstanceOf[HTMLElement]
  private[this] val  star19 = dom.document.getElementById("yuzhouxingqiu-19").asInstanceOf[HTMLElement]
  private[this] val  star20= dom.document.getElementById("yuzhouxingqiu-20").asInstanceOf[HTMLElement]
  private[this] val  star21= dom.document.getElementById("yuzhouxingqiu-21").asInstanceOf[HTMLElement]
  private[this] val  star22 = dom.document.getElementById("yuzhouxingqiu-22").asInstanceOf[HTMLElement]
  private[this] val  star23 = dom.document.getElementById("yuzhouxingqiu-23").asInstanceOf[HTMLElement]
  private[this] val  star24 = dom.document.getElementById("yuzhouxingqiu-24").asInstanceOf[HTMLElement]
  private[this] val  youkill = dom.document.getElementById("youkill").asInstanceOf[HTMLElement]
  private[this] val  background1 = img(*.style := "width:3600px;height:1800px;display:none")(*.src := s"/gypsy/static/img/b2.jpg").render
  private val goldImg = dom.document.createElement("img").asInstanceOf[html.Image]
  goldImg.setAttribute("src", "/gypsy/static/img/gold.png")
  private val silverImg = dom.document.createElement("img").asInstanceOf[html.Image]
  silverImg.setAttribute("src", "/gypsy/static/img/silver.png")
  private val bronzeImg = dom.document.createElement("img").asInstanceOf[html.Image]
  bronzeImg.setAttribute("src", "/gypsy/static/img/cooper.png")
//  private val deadbg = img(*.src := s"/paradise/static/img/king.png").render
  private[this] val deadbg = dom.document.getElementById("deadbg").asInstanceOf[HTMLElement]

  private[this] val Vicbg = dom.document.createElement("img").asInstanceOf[html.Image]
  Vicbg.setAttribute("src", "/gypsy/static/img/Victory.jpg")
  //  private[this] val echarts = dom.document.getElementById("ehcarts").asInstanceOf[HTMLElement]

//  private val Monster = img(*.style := "width:15px;")(*.src := s"/paradise/static/img/monster.png").render


  //屏幕尺寸
  val bounds = Point(Boundary.w, Boundary.h)
  this.canvas.width= size.x
  this.canvas.height= size.y
  var screeScale = if( this.canvas.width / Window.w > this.canvas.height/Window.h) (this.canvas.height/ Window.h) else (this.canvas.width / Window.w)

//  var Scale=1.0
  def updateCanvasSize(newWidth:Double, newHeight:Double)= {
    screeScale = if(newWidth / Window.w > newHeight/Window.h) {newHeight/ Window.h} else {newWidth/Window.w}
//    println(newWidth+ "   " + newHeight + "  "+ screeScale)
    this.canvas.width = newWidth.toInt
    this.canvas.height = newHeight.toInt
  }


  case object MyColors {
    val halo = "rgba(181, 211, 49, 0.51)"
    val rankList = "rgba(0, 0, 0, 0.64)"
    val background = "#fff"
    val stripe = "rgba(181, 181, 181, 0.5)"
    val myHeader = "#AEEEEE"
    val myBody = "#FFFFFF"
    val otherHeader = "rgba(78,69,69,0.82)"
    val otherBody = "#696969"
    val bigPlayer = "#FF8C69"
  }

  val mapMargin = 20

  //文本高度
  val textLineHeight = 18

  private[this] val stripeX = scala.collection.immutable.Range(0, bounds.y + 50,50)
  private[this] val stripeY = scala.collection.immutable.Range(0, bounds.x + 100,100)

  //绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

  //绘制背景ctx
  def drawGameOn(): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )

  }
 //绘制转圈动画
  var p =  ArrayBuffer()
  var particle = ArrayBuffer[Particle]()
  var angle = Math.PI/4
  var width = this.canvas.width
  var height = this.canvas.height
  def getRandomInt(min:Double, max:Double):Double= {
    return min + Math.floor(Math.random() * (max - min + 1))
  }

  def cleanCtx()={
    ctx.clearRect(0,0,size.x,size.y)
  }

  class Particle(x1:Double,y1:Double){
    var x= x1
    var y = y1
    var r = getRandomInt(10, 16)
    var vx:Double = 0
    var vy:Double= 0
    var ax:Double = 0
    var ay:Double = 0
    var al:Double = 1

    def update()={
      this.ax = getRandomInt(-0.001, 0.001)
      this.ay = getRandomInt(0.01, 0.02)
      this.vx =this.vx+this.ax
      this.vy =this.vy+this.ay
      this.x+=this.vx
      this.y+=this.vy

      if (this.r >= 0.01) {
        this.r -= 0.2
        this.al =this.al-0.001
      } else {
        this.r = 0
        this.al = 0
      }
    }

    def draw(): Unit ={
      ctx.fillStyle = "rgba(0,0,255," + this.al + ")"
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.r, 0, 2 * Math.PI, false)
      ctx.fill()
      //      ctx.fillStyle = "rgba(99, 99, 99, 1)"
      //      ctx.font = "60px Helvetica"
      //      ctx.fillText(""+time+"s", 710, 350)
      //      clock1 = dom.window.setInterval(()=>clock(timeNum),1000)
    }
  }

  def run(a:Double) {
    val r = 140
    val x = r * Math.sin(a) + width / 2
    val y = r * Math.cos(a) + ((height / 2)-80)
    val p = new Particle(x, y)
    particle.append(p)
  }

  def drawGameOn2()={
    ctx.clearRect(0, 0, width, height)
    run(angle)
    for ( j <- 1 until particle.length) {
      val p = particle(j)
      p.update()
      p.draw()
    }
    if (angle <= 2 * Math.PI) {
      angle += 0.04
    } else {
      angle = 0
    }
  }


  var timeNum = 0
  var clock1=0

  //绘制等待时间
  def drawClock():Unit={
    clock1 = dom.window.setInterval(()=>clock(timeNum),1000)
  }
  def clock(time:Int):Unit={
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    ctx.fillStyle = "rgba(99, 19, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("正在等待玩家进入", 640, 100)
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "70px Helvetica"
    ctx.fillText(""+time+"s", 718, 350)
    timeNum = timeNum+1
    println(timeNum)
  }
  //清除计数
  def cleanClock():Unit={
    dom.window.clearInterval(clock1)
  }


  //欢迎文字
  def drawGameWelcome(): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Welcome.", 150, 180)
  }

  //等待文字
  def drawGameWait(myID:String): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Please wait.", 350, 180)
  }


  //离线提示文字
  def drawGameLost: Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Ops, connection lost....", 350, 250)
  }

  //背景绘制ctx3
  def drawBackground():Unit = {
//    println(s"Draw BackGround ================================ ")
//    val pat = ctx.createPattern(background1,"repeat")
//    ctx.fillStyle = pat
//    ctx.fillRect(0,0,bounds.x,bounds.y)
    //绘制背景
    background1.onload = { _ =>
      ctx.drawImage(background1,0,0, bounds.x, bounds.y)
    }
//    ctx.drawImage(background1,0,0, bounds.x, bounds.y)
    ctx.save()
    //绘制条纹
//    ctx.strokeStyle = MyColors.stripe
//    ctx.strokeStyle = Color.White.toString()
//    stripeX.foreach{ l=>
//      ctx.beginPath()
//      ctx.moveTo(0 ,l )
//      ctx.lineTo(bounds.x,l )
//      ctx.stroke()
//    }
//    stripeY.foreach{ l=>
//      ctx.beginPath()
//      ctx.moveTo(l ,0)
//      ctx.lineTo(l ,bounds.y)
//      ctx.stroke()
//    }
  }

  //ctx2
  def drawRankMap():Unit = {
    //绘制当前排行
    val rankWidth = this.canvas.width * 0.14

    ctx.fillStyle = MyColors.rankList
    ctx.fillRect(this.canvas.width - this.canvas.width * 0.17,20,rankWidth+3,56+GameConfig.rankShowNum*17)

    //绘制小地图
    val littleMap = this.canvas.width * 0.18  // 200
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
  }

  def drawKill(myId:String,grid:GameClient,isKill:Boolean,killList:List[(Int,String,String)])={

    if(isKill){
      val showTime = killList.head._1
      val killerName = killList.head._2
      val deadName = killList.head._3
/*      val killImg = if (deadPlayer.kill > 3) shutdown
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 3) {killingspree}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 4) {dominating}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 5) {unstoppable}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 6) {godlike}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill >= 7) {legendary}
      else if (killerId == myId) youkill
      else kill*/
      if (showTime > 0) {
        ctx.save()
        ctx.font = "25px Helvetica"
        ctx.fillStyle = "#f27c02"
        val allWidth = (ctx.measureText(s"$killerName $deadName").width + 25 + 32 + 50)/2
        ctx.fillText(killerName, this.canvas.width * 0.5 - allWidth, this.canvas.height * 0.15)
        ctx.drawImage(youkill, this.canvas.width * 0.5 - allWidth + ctx.measureText(s"$killerName ").width + 25,this.canvas.height * 0.15,32,32)
        ctx.fillText(deadName, this.canvas.width * 0.5 -allWidth + ctx.measureText(s"$killerName  ").width+ 32 + 50, this.canvas.height * 0.15)
        ctx.restore()
        val killList1 = if (showTime > 1) (showTime - 1, killerName, deadName) :: killList.tail else killList.tail
        if (killList1.isEmpty) (killList1,false) else (killList1,isKill)
      }else{
        (killList,isKill)
      }
    }else{
      (killList,isKill)
    }
  }

  var frame=1

  def drawGrid(uid: String, data: GridDataSync,foodMap: Map[Point,Short], offsetTime:Long,firstCome:Boolean,offScreenCanvas:Canvas,basePoint:(Double,Double),zoom:(Double,Double),gird:GameClient,p:Player)= {
    //计算偏移量
    val players = data.playerDetails
    val foods = foodMap.map(f=>Food(f._2.toShort,f._1.x,f._1.y)).toList
    val masses = data.massDetails
    val virus = data.virusDetails

    val offx= this.canvas.width/2 - basePoint._1
    val offy =this.canvas.height/2 - basePoint._2

    val scale = getZoomRate(zoom._1,zoom._2,this.canvas.width,this.canvas.height) * screeScale
//    if(getZoomRate(zoom._1,zoom._2,this.canvas.width,this.canvas.height) * screeScale!=1){
//      Scale = getZoomRate(zoom._1,zoom._2,this.canvas.width,this.canvas.height) * screeScale
//    }
//    println(s"domwidth:${dom.window.innerWidth.toInt} domheight:${dom.window.innerHeight.toInt}")
    //绘制背景
    ctx.fillStyle = "rgba(181, 181, 181, 1)"
    ctx.fillRect(0,0,this.canvas.width,this.canvas.height)
    ctx.save()
    centerScale(scale,this.canvas.width/2,this.canvas.height/2)

    ctx.drawImage(offScreenCanvas,offx,offy,bounds.x,bounds.y)
    //ctx.drawImage(background,offx,offx,bounds.x,bounds.y)
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
        ctx.arc(x + offx,y + offy,10,0,2*Math.PI)
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

        val cellx = x +xPlus*offsetTime.toFloat / frameRate
        val celly = y  +yPlus*offsetTime.toFloat / frameRate
        val xfix  = if(cellx>bounds.x) bounds.x else if(cellx<0) 0 else cellx
        val yfix = if(celly>bounds.y) bounds.y else if(celly<0) 0 else celly
        //centerScale(scale,window.x/2,window.y/2)
        ctx.beginPath()
        ctx.arc( xfix + offx ,yfix + offy ,r,0,2*Math.PI)
        ctx.fill()
      }
    }

    players.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,lastSplit,width,height,cells,startTime) =>
      val circleImg = color.toInt match{
//        case 0 => circle //(243,69,109)   b30e35
//        case 1 => circle1 //(244, 153, 48)  a65d0a
//        case 2  => circle2 //244, 217, 91   917600
//        case 3  => circle3   //76, 217, 100  05851b
//        case 4  => circle4  //159, 224, 246  037da6
//        case 5  => circle5   //190, 173, 146   875a16
//        case 6  => circle6  //207, 230, 255   4174ab
//        case _  => circle3  //222, 157, 214   8f3284

        case 0 => star24 //(243,69,109)   b30e35
        case 1 => star1 //(244, 153, 48)  a65d0a
        case 2  => star2 //244, 217, 91   917600
        case 3  => star3   //76, 217, 100  05851b
        case 4  => star4  //159, 224, 246  037da6
        case 5  => star5   //190, 173, 146   875a16
        case 6  => star6  //207, 230, 255   4174ab
        case 7 => star7 //(243,69,109)   b30e35
        case 8 => star8 //(244, 153, 48)  a65d0a
        case 9  => star9 //244, 217, 91   917600
        case 10  => star10   //76, 217, 100  05851b
        case 11 => star11  //159, 224, 246  037da6
        case 12 => star12   //190, 173, 146   875a16
        case 13 => star13  //207, 230, 255   4174ab
        case 14 => star14  //222, 157, 214   8f3284
        case 15=> star15 //(243,69,109)   b30e35
        case 16=> star16 //(244, 153, 48)  a65d0a
        case 17 => star17 //244, 217, 91   917600
        case 18 => star18  //76, 217, 100  05851b
        case 19 => star19  //159, 224, 246  037da6
        case 20 => star20  //190, 173, 146   875a16
        case 21 => star21  //207, 230, 255   4174ab
        case 22=>star22 //(243,69,109)   b30e35
        case 23=> star23 //(244, 153, 48)  a65d0a
      }
      frame+=1

      var cellDifference = false
      val c = cells.sortBy(sortRule)(Ordering.Tuple2(Ordering.Short, Ordering.Long))
      val newcells = c.map{ cell =>
        val cellx = cell.x + cell.speedX *offsetTime.toFloat / frameRate
        val celly = cell.y + cell.speedY *offsetTime.toFloat / frameRate
        val xfix  = if(cellx>bounds.x-15) bounds.x-15 else if(cellx<15) 15 else cellx
        val yfix  = if(celly>bounds.y-15) bounds.y-15 else if(celly<15) 15 else celly
        ctx.save()
        /**关键：根据mass来改变大小**/
        val radius = 4 + sqrt(cell.mass)*6
        ctx.drawImage(circleImg,xfix +offx-radius-6,yfix+offy-radius-6,2*(radius+6),2*(radius+6))

        ctx.drawImage(circleImg,xfix +offx-radius-6,yfix+offy-radius-6,2*(radius+6),2*(radius+6))

        //ctx.drawImage(circleImg,xfix +offx-cell.radius-6,yfix+offy-cell.radius-6,2*(cell.radius+6),2*(cell.radius+6))
        if(protect){
          ctx.fillStyle = MyColors.halo
          ctx.beginPath()
          ctx.arc(xfix + offx,yfix + offy, cell.radius + 15,0,2*Math.PI)
          ctx.fill()
        }
        var nameFont = sqrt(cell.mass*3)+3
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius else nameFont
        ctx.font = s"${nameFont.toInt}px Helvetica"
        val nameWidth = ctx.measureText(name).width.toInt
        val playermass= cell.mass.toInt
        val massWidth = ctx.measureText(playermass.toString).width.toInt
        ctx.fillStyle = MyColors.background
        ctx.fillText(s"${playermass.toString}",xfix + offx - massWidth / 2, yfix + offy + nameFont.toInt/2)
        ctx.fillText(s"$name", xfix + offx - nameWidth / 2, yfix + offy - (nameFont.toInt / 2))
        ctx.restore()

        /**膨胀、缩小效果**/
        var newcell = cell
        if(cell.mass != cell.newmass){
          //根据差值来膨胀或缩小
          cellDifference = true
//          val massDifference = cell.mass - cell.newmass
//          val massSpeed = massDifference match {
//            case x if(-150 < x && x < 150) => if(cell.mass < cell.newmass) 1 else -1
//            case x if(150 < x || x < -150) => (cell.newmass - cell.mass)/15
//          }
//          val massSpeed = if(cell.mass < cell.newmass) (cell.mass/100)+1 else -((cell.mass/100)+1)
        val massSpeed = if(cell.mass < cell.newmass){
         if((cell.newmass-cell.mass)>=(cell.mass/50)+1)
           ((cell.newmass-cell.mass)/50)+1
         else cell.newmass-cell.mass
        } else {
         if((cell.mass-cell.newmass)>=(cell.mass/50)+1)
           -(((cell.mass-cell.newmass)/50)+1)
         else cell.newmass-cell.mass
        }
          newcell = cell.copy(mass = (cell.mass + massSpeed).toShort, radius = (4 + sqrt(cell.mass + massSpeed) * 6).toShort)
        }
        newcell
      }.filterNot(e=> e.mass <= 0)
        if(cellDifference){
          //改变player的x,y
          val length = newcells.length
          val newX = newcells.map(_.x).sum / length
          val newY = newcells.map(_.y).sum / length
          val left = newcells.map(a => a.x - a.radius).min
          val right = newcells.map(a => a.x + a.radius).max
          val bottom = newcells.map(a => a.y - a.radius).min
          val top = newcells.map(a => a.y + a.radius).max
          val player = Player(id,name,color,newX.toShort,newY.toShort,tx,ty,kill,protect,lastSplit, right - left, top - bottom, newcells, startTime)
          gird.playerMap += (id -> player)
        }
    }

    virus.values.foreach { case Virus(vid,x,y,mass,radius,tx,ty,speed) =>
      ctx.save()
      var xfix:Double=x
      var yfix:Double=y
      if(speed>0){
        val(nx,ny)= normalization(tx,ty)
        val cellx = x + nx*speed *offsetTime.toFloat / frameRate
        val celly = y + ny*speed *offsetTime.toFloat / frameRate
        xfix  = if(cellx>bounds.x-15) bounds.x-15 else if(cellx<15) 15 else cellx
        yfix = if(celly>bounds.y-15) bounds.y-15 else if(celly<15) 15 else celly
      }

      ctx.drawImage(virusImg,xfix-radius+offx,yfix-radius+offy,radius*2,radius*2)
      ctx.restore()
    }
    ctx.restore()
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.save()
    ctx.font = s"${ 34 * this.canvas.width / Window.w }px Helvetica"
    ctx.fillText(s"KILL: ${p.kill}", this.canvas.width * 0.18 + 30 , 10)
    ctx.fillText(s"SCORE: ${p.cells.map(_.newmass).sum.toInt}", this.canvas.width * 0.18 + 180 * this.canvas.width / Window.w , 10)
    ctx.restore()
    renderFps(ctx,NetDelay.latency,this.canvas.width)
  }

  //ctx3
  def drawRankMapData(uid:String,currentRank:List[RankInfo],players:List[Player],basePoint:(Double,Double),bigPlayerPosition:List[PlayerPosition],offsetTime:Long,playerNum:Int)={
    val littleMap = this.canvas.width * 0.18  // 200

    //绘制当前排行
    ctx.clearRect(0,0,this.canvas.width,this.canvas.height)
    ctx.fillStyle = MyColors.background
    ctx.font = s"${12 * this.canvas.width / Window.w }px Helvetica"
    ctx.fillText(s"Version:${version}",this.canvas.width * 0.86,16)
    ctx.font = s"${12 * this.canvas.width / Window.w }px Helvetica"
    val currentRankBaseLine = 3
//    drawTextLine(s"Version:${version}", (this.canvas.width- this.canvas.width * 0.17+5).toInt, 0, 0)
    drawTextLine(s"———排行榜———  人数:$playerNum", (this.canvas.width- this.canvas.width * 0.17+5).toInt, 0, currentRankBaseLine)

    //这里过滤是为了防止回放的时候传全量的排行版数据
    currentRank.zipWithIndex.filter(r=>r._2<GameConfig.rankShowNum || r._1.score.id == uid).foreach{rank=>
      val score = rank._1.score
      val index = rank._1.index

      val imgOpt = index match {
        case 1 => Some(goldImg)
        case 2 => Some(silverImg)
        case 3 => Some(bronzeImg)
        case _ => None
      }
      imgOpt.foreach{ img =>
        ctx.drawImage(img, this.canvas.width- 200* this.canvas.width / Window.w, index * textLineHeight+25, 13, 13)
      }
      val offx=ctx.measureText(index.toString).width.toInt
      if(score.id == uid){
        ctx.save()
        ctx.font = s"${12 * this.canvas.width / Window.w }px Helvetica"
        ctx.fillStyle = "#FFFF33"
        if(score.n.length>5)
          {
            drawTextLine(s"【${index}】${score.n.take(4)+"*"}", (this.canvas.width-188 * this.canvas.width / Window.w).toInt-(offx-7), if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
          }
        else{
          drawTextLine(s"【${index}】${score.n.+("    ").take(5)}", (this.canvas.width-188 * this.canvas.width / Window.w).toInt-(offx-7), if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
        }
        drawTextLine(s"得分:${score.score.toInt}", (this.canvas.width-90 * this.canvas.width / Window.w).toInt, if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
        ctx.restore()
      }else{
        if(score.n.length>5)
        {
          drawTextLine(s"【${index}】${score.n.take(4)+"*"}", (this.canvas.width-188 * this.canvas.width / Window.w).toInt-(offx-7), if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
        }
        else{
          drawTextLine(s"【${index}】${score.n.+("    ").take(5)}", (this.canvas.width-188 * this.canvas.width / Window.w).toInt-(offx-7), if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
        }
        drawTextLine(s"得分:${score.score.toInt}", (this.canvas.width-90 * this.canvas.width / Window.w).toInt, index , currentRankBaseLine)
      }

    }
    //绘制小地图
    ctx.fillStyle = MyColors.bigPlayer
    bigPlayerPosition.filterNot(_.id==uid).map{player=>
      val offx = player.x.toDouble
      val offy = player.y.toDouble
      ctx.beginPath()
      ctx.arc(mapMargin + (offx/bounds.x) * littleMap,mapMargin + offy/bounds.y * littleMap,8,0,2*Math.PI)
      ctx.fill()
    }
    ctx.fillStyle = MyColors.myHeader
    players.find(player=>player.id == uid) match {
      case Some(player)=>
        ctx.beginPath()
        ctx.arc(mapMargin + (basePoint._1/bounds.x) * littleMap,mapMargin + basePoint._2/bounds.y * littleMap,8,0,2*Math.PI)
        ctx.fill()
      case None=>
    }
  }


  def drawWhenDead(msg:Protocol.UserDeadMessage)={
    ctx.fillStyle = "#000"//Color.Black.toString()
    ctx.fillRect(0, 0, Boundary.w , Boundary.h )
    ctx.drawImage(deadbg,0,0, canvas.width, canvas.height)
    ctx.font = "50px Helvetica"
    ctx.fillStyle = "#CD3700"
    val Width = this.canvas.width
    val Height = this.canvas.height
//    val BaseHeight = Height*0.3
    val BaseHeight = Height*0.22
    ctx.fillText(s"You Dead!", Width*0.42, BaseHeight)

    ctx.font = s"${Window.w *0.02}px Comic Sans MS"

    var DrawLeft = Width*0.35
    val DrawHeight = BaseHeight
    ctx.fillText(s"The   Killer  Is    :", DrawLeft, DrawHeight + Height*0.07)
    ctx.fillText(s"Your  Final   Score:", DrawLeft, DrawHeight + Height*0.07*2)
    ctx.fillText(s"Your  Final   LifeTime  :", DrawLeft, DrawHeight+Height*0.07*3)
    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*4)
    ctx.fillStyle=Color.White.toString()
    DrawLeft = ctx.measureText("Your  Final   LifeTime  :").width +  Width*0.35 + 30
    ctx.fillText(s"${msg.killerName}", DrawLeft,DrawHeight + Height*0.07)
    ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07*2)
    ctx.fillText(s"${MTime2HMS (msg.lifeTime)}", DrawLeft, DrawHeight + Height * 0.07 * 3)
    ctx.fillText(s"${msg.killNum}", DrawLeft,DrawHeight + Height*0.07*4)
    val reStart = "Press Space to ReStart ￣へ￣#  "
    ctx.fillText(reStart, Width * 0.5 - ctx.measureText(reStart).width/2, DrawHeight + Height*0.07*5)

  }


//  def drawVictory(VictoryMsg:(Protocol.VictoryMsg,Int))={
  def drawVictory(VictoryMsg:(Protocol.VictoryMsg,Short,Boolean))={
    val msg = VictoryMsg._1
    val isVictory = VictoryMsg._3
    ctx.fillStyle = "#000"//Color.Black.toString()
    ctx.fillRect(0, 0, Boundary.w , Boundary.h )
    ctx.drawImage(Vicbg,0,0, canvas.width, canvas.height)
//    ctx.font = "30px Helvetica"
    ctx.fillStyle = "#CD3700"
    val Width = this.canvas.width
    val Height = this.canvas.height
    ctx.font = s"${Width *0.03}px Comic Sans MS"
    //    val BaseHeight = Height*0.3
    val BaseHeight = Height*0.15
    var DrawLeft = Width*0.35
    var DrawHeight = BaseHeight + Height * 0.1

    val congratulation =if(isVictory){
      s"Good Game!  Congratulations to: You~"
    }else{
      s"Good Game!  Congratulations to: "
    }
    ctx.fillText(congratulation, Width * 0.5 - ctx.measureText(congratulation).width/2, BaseHeight)

    ctx.save()
    ctx.fillStyle = Color.Yellow.toString()
    val winner = s"${msg.name}"
    ctx.fillText(winner, Width * 0.5 - ctx.measureText(winner).width/2, BaseHeight+Height *0.1 )
    ctx.restore()
    DrawHeight = BaseHeight + Height * 0.15
    ctx.font = s"${Width *0.02}px Comic Sans MS"

    val Time = MTime2HMS (msg.totalFrame * GameConfig.frameRate)
    if(isVictory){
      ctx.fillText(s"Your  Final   Score  :", DrawLeft, DrawHeight + Height*0.07)
      ctx.fillText(s"Game  Time   :", DrawLeft, DrawHeight + Height*0.07*2)
      ctx.fillStyle=Color.White.toString()
      DrawLeft = ctx.measureText("Your  Final   Score  :").width +  Width*0.35 + 60
      ctx.fillText(s"${VictoryMsg._2}", DrawLeft,DrawHeight + Height*0.07)
      ctx.fillText(s"${Time}", DrawLeft,DrawHeight + Height*0.07*2)
    }else{
      ctx.fillText(s"The   Winner  Score  :", DrawLeft, DrawHeight + Height*0.07)
      ctx.fillText(s"Your  Final   Score  :", DrawLeft, DrawHeight + Height*0.07*2)
      ctx.fillText(s"Game  Time   :", DrawLeft, DrawHeight + Height*0.07*3)
      //    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*3)
      ctx.fillStyle=Color.White.toString()
      DrawLeft = ctx.measureText("The   Winner  Score  :").width +  Width*0.35 + 60
      ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07)
      ctx.fillText(s"${VictoryMsg._2}", DrawLeft,DrawHeight + Height*0.07*2)
      ctx.fillText(s"${Time}", DrawLeft,DrawHeight + Height*0.07*3)
    }
    



/*    if(isVictory){
      val congratulation = s"Good Game!Congratulations to You~"
      ctx.fillText(congratulation, Width * 0.5 - ctx.measureText(congratulation).width/2, BaseHeight)
      //    ctx.fillText(s"第${VictoryMsg._2} 号文明在大爆炸中毁灭了", Width*0.3, BaseHeight)
      //    ctx.fillText(s"这次的毁灭者是 ${msg.name}", Width*0.35, BaseHeight + Height*0.05 )

      //    ctx.font = s"${Window.w *0.02}px Comic Sans MS"
      ctx.font = s"${Width *0.02}px Comic Sans MS"


      //    val WinnerName = msg.name
      val Time = MTime2HMS (msg.totalFrame * GameConfig.frameRate)
      ctx.fillText(s"Your  Final   Score  :", DrawLeft, DrawHeight + Height*0.07*1)
      ctx.fillText(s"Game  Time   :", DrawLeft, DrawHeight + Height*0.07*2)
      //    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*3)
      ctx.fillStyle=Color.White.toString()
      DrawLeft = ctx.measureText("Your  Final   Score  :").width +  Width*0.35 + 60
      ctx.fillText(s"${VictoryMsg._2}", DrawLeft,DrawHeight + Height*0.07*1)
      ctx.fillText(s"${Time}", DrawLeft,DrawHeight + Height*0.07*2)
    }else{
      val congratulation = s"Good Game!  Congratulations to:"
      ctx.fillText(congratulation, Width * 0.5 - ctx.measureText(congratulation).width/2, BaseHeight)

      ctx.save()
      ctx.fillStyle = Color.Yellow.toString()
      val winner = s"${msg.name}"
      ctx.fillText(winner, Width * 0.5 - ctx.measureText(winner).width/2, BaseHeight+Height *0.1 )
      ctx.restore()
      DrawHeight = BaseHeight + Height * 0.15
      ctx.font = s"${Width *0.02}px Comic Sans MS"

      val Time = MTime2HMS (msg.totalFrame * GameConfig.frameRate)
      ctx.fillText(s"The   Winner  Score  :", DrawLeft, DrawHeight + Height*0.07)
      ctx.fillText(s"Your  Final   Score  :", DrawLeft, DrawHeight + Height*0.07*2)
      ctx.fillText(s"Game  Time   :", DrawLeft, DrawHeight + Height*0.07*3)
      //    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*3)
      ctx.fillStyle=Color.White.toString()
      DrawLeft = ctx.measureText("The   Winner  Score  :").width +  Width*0.35 + 60
      ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07)
      ctx.fillText(s"${VictoryMsg._2}", DrawLeft,DrawHeight + Height*0.07*2)
      ctx.fillText(s"${Time}", DrawLeft,DrawHeight + Height*0.07*3)
    }*/

    val reStart = s"Press Space to Start a New Game ୧(●⊙(工)⊙●)୨ "
    ctx.fillText(reStart, Width * 0.5 - ctx.measureText(reStart).width/2,DrawHeight + Height*0.07*5)

  }


/*  def drawEcharts() = {
    val myChart = EchartsJs.echarts.init(echarts)
    val option = EchartOption(XAxis("category",false,List("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")),YAxis("value"),List(SeriesItem(List(820, 932, 901, 934, 1290, 1330, 1320),"line",AreaStyle()))).asJson
//    myChart.setOption(option)
    println(option)
    ctx.drawImage(echarts,0,0,400,200)
  }*/


  def drawWhenFinish(msg:String)={
    ctx.fillStyle = "#000"
    ctx.fillRect(0,0,this.canvas.width,this.canvas.height)
    ctx.font = s"${30 * this.canvas.width / Window.w}px Helvetica"
    ctx.fillStyle = "#fff"
    ctx.fillText(msg, 80, 30)
  }


  def centerScale(rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    //视角缩放
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
  }

  def sortRule(cell:Cell):(Short,Long) ={
    (cell.newmass,cell.id)
  }

//  def MTime2HMS(time:Long)={
//    var ts = (time/1000)
//    var result = ""
//    if(ts/3600>0){
//      result += s"${ts/3600}小时"
//    }
//    ts = ts % 3600
//    if(ts/60>0){
//      result += s"${ts/60}分"
//    }
//    ts = ts % 60
//    result += s"${ts}秒"
//    result
//  }

}
