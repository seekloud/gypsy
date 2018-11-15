package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.front.scalajs.DrawCircle

import scalatags.JsDom.short.{*, img, s}
//import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync
import com.neo.sk.gypsy.shared.ptcl.Protocol.GridDataSync
import com.neo.sk.gypsy.shared.ptcl._
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.{Canvas, Image}
import com.neo.sk.gypsy.shared.util.utils._
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.html

import scala.collection.mutable.ArrayBuffer
import scala.math._
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

  private[this] val  virusImg = dom.document.getElementById("virus").asInstanceOf[HTMLElement]
  private[this] val  circle = dom.document.getElementById("circle").asInstanceOf[HTMLElement]
  private[this] val  circle1 = dom.document.getElementById("circle1").asInstanceOf[HTMLElement]
  private[this] val  circle2 = dom.document.getElementById("circle2").asInstanceOf[HTMLElement]
  private[this] val  circle3 = dom.document.getElementById("circle3").asInstanceOf[HTMLElement]
  private[this] val  circle4 = dom.document.getElementById("circle4").asInstanceOf[HTMLElement]
  private[this] val  circle5 = dom.document.getElementById("circle5").asInstanceOf[HTMLElement]
  private[this] val  circle6 = dom.document.getElementById("circle6").asInstanceOf[HTMLElement]
  private[this] val  kill = dom.document.getElementById("kill").asInstanceOf[HTMLElement]
  private[this] val  youkill = dom.document.getElementById("youkill").asInstanceOf[HTMLElement]
  private[this] val  shutdown = dom.document.getElementById("shutdown").asInstanceOf[HTMLElement]
  private[this] val  killingspree = dom.document.getElementById("killingspree").asInstanceOf[HTMLElement]
  private[this] val  dominating = dom.document.getElementById("dominating").asInstanceOf[HTMLElement]
  private[this] val  unstoppable = dom.document.getElementById("unstoppable").asInstanceOf[HTMLElement]
  private[this] val  godlike = dom.document.getElementById("godlike").asInstanceOf[HTMLElement]
  private[this] val  legendary = dom.document.getElementById("legendary").asInstanceOf[HTMLElement]
  private[this] val  background = dom.document.getElementById("background").asInstanceOf[HTMLElement]
  private[this] val  background1 = dom.document.getElementById("background1").asInstanceOf[HTMLElement]
  private[this] val  massImg = dom.document.getElementById("mass").asInstanceOf[HTMLElement]
  private val goldImg = dom.document.createElement("img").asInstanceOf[html.Image]
  goldImg.setAttribute("src", "/gypsy/static/img/gold.png")
  private val silverImg = dom.document.createElement("img").asInstanceOf[html.Image]
  silverImg.setAttribute("src", "/gypsy/static/img/silver.png")
  private val bronzeImg = dom.document.createElement("img").asInstanceOf[html.Image]
  bronzeImg.setAttribute("src", "/gypsy/static/img/cooper.png")
//  private val deadbg = img(*.src := s"/paradise/static/img/king.png").render
  private[this] val deadbg = dom.document.getElementById("deadbg").asInstanceOf[HTMLElement]

  //屏幕尺寸
  val bounds = Point(Boundary.w, Boundary.h)
  this.canvas.width= size.x
  this.canvas.height= size.y
  var screeScale = if( this.canvas.width / Window.w > this.canvas.height/Window.h) (this.canvas.height/ Window.h) else (this.canvas.width / Window.w)

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
    val myHeader = "#cccccc"
    val myBody = "#FFFFFF"
    val otherHeader = "rgba(78,69,69,0.82)"
    val otherBody = "#696969"
  }

  val mapMargin = 20

  //文本高度
  val textLineHeight = 14

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
  def drawGameWait(firstCome:Boolean): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    if(firstCome) {
      ctx.fillStyle = "rgba(99, 99, 99, 1)"
      ctx.font = "36px Helvetica"
      ctx.fillText("Please wait.", 350, 180)
    } else {
      ctx.fillStyle = "rgba(99, 99, 99, 1)"
      ctx.font = "36px Helvetica"
      ctx.fillText("Ops, Loading....", 350, 250)
    }
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
    //绘制背景
    println("drawBackground22222222")
    ctx.drawImage(background1,0,0, bounds.x, bounds.y)
    ctx.save()
    //绘制条纹
    ctx.strokeStyle = MyColors.stripe
    stripeX.foreach{ l=>
      ctx.beginPath()
      ctx.moveTo(0 ,l )
      ctx.lineTo(bounds.x,l )
      ctx.stroke()
    }
    stripeY.foreach{ l=>
      ctx.beginPath()
      ctx.moveTo(l ,0)
      ctx.lineTo(l ,bounds.y)
      ctx.stroke()
    }
  }

  //ctx2
  def drawRankMap():Unit = {
    //绘制当前排行
    val littleMap = this.canvas.width * 0.18  // 200

    ctx.fillStyle = MyColors.rankList
    ctx.fillRect(this.canvas.width - 200,20,150,250)

    //绘制小地图
    println("littleMap:   " + littleMap)
    println("canvas:   " + this.canvas.width)
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

  def drawKill(myId:String,grid:GameClient,isKill:Boolean,killList:List[(Int,String,Player)])={
    if(isKill){
      val showTime = killList.head._1
      val killerId = killList.head._2
      val deadPlayer = killList.head._3
      val killerName = grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).name
      val deadName = deadPlayer.name
      val killImg = if (deadPlayer.kill > 3) shutdown
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 3) {killingspree}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 4) {dominating}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 5) {unstoppable}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 6) {godlike}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill >= 7) {legendary}
      else if (killerId == myId) youkill
      else kill
      if (showTime > 0) {
        ctx.save()
        ctx.font = "25px Helvetica"
        ctx.strokeStyle = "#f32705"
        ctx.strokeText(killerName, 25, 400)
        ctx.fillStyle = "#f27c02"
        ctx.fillText(killerName, 25, 400)
        ctx.drawImage(killImg,25+ctx.measureText(s"$killerName ").width+25,400,32,32)
        ctx.strokeStyle = "#f32705"
        ctx.strokeText(deadName, 25+ ctx.measureText(s"$killerName  ").width+32+50, 400)
        ctx.fillStyle = "#f27c02"
        ctx.fillText(deadName, 25+ctx.measureText(s"$killerName  ").width+32+50, 400)
        ctx.strokeRect(12,375,50+ctx.measureText(s"$killerName $deadName").width+25+32,75)
        ctx.restore()
        val killList1 = if (showTime > 1) (showTime - 1, killerId, deadPlayer) :: killList.tail else killList.tail
        if (killList1.isEmpty) (killList1,false) else (killList1,isKill)
      }else{
        (killList,isKill)
      }
    }else{
      (killList,isKill)
    }
  }

  def drawGrid(uid: String, data: GridDataSync,foodMap: Map[Point,Int], offsetTime:Long,firstCome:Boolean,offScreenCanvas:Canvas,basePoint:(Double,Double),zoom:(Double,Double))= {
    //计算偏移量
    val players = data.playerDetails
    val foods = foodMap.map(f=>Food(f._2,f._1.x,f._1.y)).toList
    val masses = data.massDetails
    val virus = data.virusDetails

    val offx= this.canvas.width/2 - basePoint._1
    val offy =this.canvas.height/2 - basePoint._2
    //    println(s"zoom：$zoom")

    val scale = getZoomRate(zoom._1,zoom._2,this.canvas.width,this.canvas.height) * screeScale

    //绘制背景
    ctx.fillStyle = "rgba(181, 181, 181, 1)"
    ctx.fillRect(0,0,this.canvas.width,this.canvas.height)
    ctx.save()
    centerScale(scale,this.canvas.width/2,this.canvas.height/2)

    //TODO /2
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

        val cellx = x +xPlus*offsetTime.toFloat / WsMsgProtocol.frameRate
        val celly = y  +yPlus*offsetTime.toFloat / WsMsgProtocol.frameRate
        val xfix  = if(cellx>bounds.x) bounds.x else if(cellx<0) 0 else cellx
        val yfix = if(celly>bounds.y) bounds.y else if(celly<0) 0 else celly
        //centerScale(scale,window.x/2,window.y/2)
        ctx.beginPath()
        ctx.arc( xfix + offx ,yfix + offy ,r,0,2*Math.PI)
        ctx.fill()
      }
    }

    players.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,_,killerName,width,height,cells,startTime) =>
      val circleImg = color.toInt match{
        case 0 => circle //(243,69,109)   b30e35
        case 1 => circle1 //(244, 153, 48)  a65d0a
        case 2  => circle2 //244, 217, 91   917600
        case 3  => circle3   //76, 217, 100  05851b
        case 4  => circle4  //159, 224, 246  037da6
        case 5  => circle5   //190, 173, 146   875a16
        case 6  => circle6  //207, 230, 255   4174ab
        case _  => circle3  //222, 157, 214   8f3284
      }
      cells.sortBy(_.id).foreach{ cell=>

        val cellx = cell.x + cell.speedX *offsetTime.toFloat / WsMsgProtocol.frameRate
        val celly = cell.y + cell.speedY *offsetTime.toFloat / WsMsgProtocol.frameRate
        val xfix  = if(cellx>bounds.x-15) bounds.x-15 else if(cellx<15) 15 else cellx
        val yfix = if(celly>bounds.y-15) bounds.y-15 else if(celly<15) 15 else celly
        ctx.save()
        ctx.drawImage(circleImg,xfix +offx-cell.radius-6,yfix+offy-cell.radius-6,2*(cell.radius+6),2*(cell.radius+6))
        //ctx.arc(xfix +offx,yfix+offy,cell.radius-1,0,2*Math.PI)
        //DrawCircle.drawCircle(ctx,xfix+offx,yfix+offy,cell.radius-1)
        if(protect){
          ctx.fillStyle = MyColors.halo
          ctx.beginPath()
          ctx.arc(xfix + offx,yfix + offy, cell.radius + 15,0,2*Math.PI)
          ctx.fill()
        }

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

    virus.values.foreach { case Virus(vid,x,y,mass,radius,_,tx,ty,speed) =>
      ctx.save()
      var xfix:Double=x
      var yfix:Double=y
      if(speed>0){
        val(nx,ny)= normalization(tx,ty)
        val cellx = x + nx*speed *offsetTime.toFloat / WsMsgProtocol.frameRate
        val celly = y + ny*speed *offsetTime.toFloat / WsMsgProtocol.frameRate
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
  }

  //ctx3
  def drawRankMapData(uid:String,currentRank:List[Score],players:List[Player],basePoint:(Double,Double))={
    val littleMap = this.canvas.width * 0.18  // 200

    //绘制当前排行
    ctx.clearRect(0,0,this.canvas.width,this.canvas.height)
    ctx.font = "12px Helvetica"
    //    ctx.fillStyle = MyColors.rankList
    //    ctx.fillRect(window.x-200,20,150,250)
    val currentRankBaseLine = 4
    var index = 0
    ctx.fillStyle = MyColors.background
    drawTextLine(s"—————排行榜—————", this.canvas.width-200, index, currentRankBaseLine)
    currentRank.foreach { score =>
      index += 1
      val drawColor = index match {
        case 1 => "#FFD700"
        case 2 => "#D1D1D1"
        case 3 => "#8B5A00"
        case _ => "#CAE1FF"
      }
      val imgOpt = index match {
        case 1 => Some(goldImg)
        case 2 => Some(silverImg)
        case 3 => Some(bronzeImg)
        case _ => None
      }
      imgOpt.foreach{ img =>
        ctx.drawImage(img, this.canvas.width-200, index * textLineHeight+32, 13, 13)
      }
      //      ctx3.strokeStyle = drawColor
      //      ctx3.lineWidth = 18
      drawTextLine(s"【$index】: ${score.n.+("   ").take(4)} 得分:${score.score.toInt}", this.canvas.width-193, index, currentRankBaseLine)
    }
    //绘制小地图

    ctx.fillStyle = MyColors.background
    players.find(_.id == uid) match {
      case Some(player)=>
        ctx.beginPath()
        ctx.arc(mapMargin + (basePoint._1/bounds.x) * littleMap,mapMargin + basePoint._2/bounds.y * littleMap,8,0,2*Math.PI)
        ctx.fill()
      case None=>
      // println(s"${basePoint._1},  ${basePoint._2}")
    }
  }


  def drawWhenDead(msg:Protocol.UserDeadMessage)={
//    ctx.fillStyle = "#ccc"//Color.Black.toString()
//    val showTime = MTime2HMS(msg.lifeTime)

    ctx.fillStyle = "#000"//Color.Black.toString()
    ctx.fillRect(0, 0, Boundary.w , Boundary.h )
    ctx.drawImage(deadbg,0,0, canvas.width, canvas.height)
    ctx.font = "50px Helvetica"
    ctx.fillStyle = "#CD3700"
    val Width = this.canvas.width
    val Height = this.canvas.height
    ctx.fillText(s"You Dead!", Width*0.42, Height*0.3)

    ctx.font = s"${Window.w *0.02}px Comic Sans MS"

    var DrawLeft = Width*0.35
    var DrawHeight = Height*0.3
    ctx.fillText(s"The   Killer  Is    :", DrawLeft, DrawHeight + Height*0.07)
    ctx.fillText(s"Your  Final   Score:", DrawLeft, DrawHeight + Height*0.07*2)
    ctx.fillText(s"Your  Final   LifeTime  :", DrawLeft, DrawHeight+Height*0.07*3)
    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*4)
    ctx.fillStyle=Color.White.toString()
//    DrawLeft = Width*0.56+Width*0.12
//    DrawLeft = Width*0.56
    DrawLeft = ctx.measureText("Your  Final   LifeTime  :").width +  Width*0.35 + 30
    ctx.fillText(s"${msg.killerName}", DrawLeft,DrawHeight + Height*0.07)
    ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07*2)
    ctx.fillText(s"${MTime2HMS(msg.lifeTime)}", DrawLeft,DrawHeight+Height*0.07*3)
    ctx.fillText(s"${msg.killNum}", DrawLeft,DrawHeight + Height*0.07*4)
  }


  def centerScale(rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    //视角缩放
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
  }

  def MTime2HMS(time:Long)={
    var ts = (time/1000)
    println(s"一共有 $ts 秒！")
    var result = ""
    if(ts/3600>0){
      result += s"${ts/3600}小时"
    }
    ts = ts % 3600
    println(s"第一次 $ts 秒！")
    if(ts/60>0){
      result += s"${ts/60}分"
    }
    ts = ts % 60
    println(s"第二次 $ts 秒！")
    result += s"${ts}秒"
    result
  }

}
