package com.neo.sk.gypsy.scene

import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text, TextAlignment}

import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.util.utils.{getZoomRate, normalization}

import scala.collection.mutable.ArrayBuffer
import scala.math.{abs, pow, sqrt}
import javafx.scene.shape.ArcType

import com.neo.sk.gypsy.utils.FpsComp

class GameCanvas(canvas: Canvas,
                 ctx:GraphicsContext,
                 size:Point) {
  val  img = new Image("file:client/src/main/resources/img/virus.png")
  val  circle = new Image("file:client/src/main/resources/img/circle.png")
  val  circle1 = new Image("file:client/src/main/resources/img/circle1.png")
  val  circle2 = new Image("file:client/src/main/resources/img/circle2.png")
  val  circle3 = new Image("file:client/src/main/resources/img/circle3.png")
  val  circle4 = new Image("file:client/src/main/resources/img/circle4.png")
  val  circle5 = new Image("file:client/src/main/resources/img/circle5.png")
  val  circle6 = new Image("file:client/src/main/resources/img/circle6.png")
  val  kill = new Image("file:client/src/main/resources/img/kill.png")
  val  youkill = new Image("file:client/src/main/resources/img/youkill.png")
  val  shutdown = new Image("file:client/src/main/resources/img/shutdown.png")
  val  killingspree = new Image("file:client/src/main/resources/img/killingspree.png")
  val  dominating = new Image("file:client/src/main/resources/img/dominating.png")
  val  unstoppable = new Image("file:client/src/main/resources/img/unstoppable.png")
  val  godlike = new Image("file:client/src/main/resources/img/godlike.png")
  val  legendary = new Image("file:client/src/main/resources/img/legendary.png")
  val  background = new Image("file:client/src/main/resources/img/background.jpg")
  val  background1 = new Image("file:client/src/main/resources/img/b2.jpg")
  val  massImg = new Image("file:client/src/main/resources/img/mass.png")
  val deadbg = new Image("file:client/src/main/resources/img/deadbg.jpg")
  private val goldImg =new Image("file:client/src/main/resources/img/gold.png")
  private val silverImg = new Image("file:client/src/main/resources/img/silver.png")
  private val bronzeImg = new Image("file:client/src/main/resources/img/cooper.png")

  val bounds = Point(Boundary.w, Boundary.h)



  var realWindow = Point(size.x,size.y)
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

  val littleMap = 200
  val mapMargin = 20

  //文本高度
  val textLineHeight = 14

  private[this] val stripeX = scala.collection.immutable.Range(0, bounds.y + 50,50)
  private[this] val stripeY = scala.collection.immutable.Range(0, bounds.x + 100,100)

  def resetScreen(width:Int,height:Int)={
    canvas.setHeight(height)
    canvas.setWidth(width)
    realWindow = Point(width,height)

  }
  //绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

  //绘制背景ctx
  def drawGameOn(): Unit = {
    ctx.setFill(Color.web("rgba(255,255,255,0)"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )

  }
  //绘制转圈动画
  var p =  ArrayBuffer()
  var particle = ArrayBuffer[Particle]()
  var angle = Math.PI/4
  var width = realWindow.x
  var height = realWindow.y
  def getRandomInt(min:Double, max:Double):Double= {
    return min + Math.floor(Math.random() * (max - min + 1))
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
      ctx.setFill(Color.web("rgba(0,0,255," + this.al + ")"))
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.r,this.r, 0, 2 * Math.PI)
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
 /* def drawClock():Unit={
    clock1 = dom.window.setInterval(()=>clock(timeNum),1000)
  }

  def clock(time:Int):Unit={
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
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
  }*/


  //欢迎文字
  def drawGameWelcome(): Unit = {
    ctx.setFill(Color.web("rgba(255, 255, 255, 0)"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setFont(Font.font("36px Helvetica"))
    ctx.fillText("Welcome.", 150, 180)
  }

  //等待文字
  def drawGameWait(firstCome:Boolean): Unit = {
    ctx.setFill(Color.web("rgba(255, 255, 255, 0)"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    if(firstCome) {
      ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
      ctx.setFont(Font.font("36px Helvetica"))
      ctx.fillText("Please wait.", 350, 180)
    } else {
      ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
      ctx.setFont(Font.font("36px Helvetica"))
      ctx.fillText("Ops, Loading....", 350, 250)
    }
  }


  //离线提示文字
  def drawGameLost: Unit = {
    ctx.setFill(Color.web("rgba(255,255,255,0"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setFont(Font.font("36px Helvetica"))
    ctx.fillText("Ops, connection lost....", 350, 250)
  }

  //背景绘制ctx3
  def drawBackground():Unit = {
    //绘制背景
    ctx.drawImage(background1,0,0,realWindow.x,realWindow.y)
    ctx.save()
    //绘制条纹
    ctx.setStroke(Color.web(MyColors.stripe))
    stripeX.foreach{ l=>
      ctx.beginPath()
      ctx.moveTo(0 ,l )
      ctx.lineTo(realWindow.x ,l )
      ctx.stroke()
    }
    stripeY.foreach{ l=>
      ctx.beginPath()
      ctx.moveTo(l ,0)
      ctx.lineTo(l ,realWindow.y)
      ctx.stroke()
    }
  }

  //ctx2
  def drawRankMap():Unit = {
    //绘制当前排行
    ctx.clearRect(0,0,realWindow.x,realWindow.y)
    ctx.setFill(Color.web(MyColors.rankList))
    ctx.fillRect(realWindow.x-200,20,150,250)

    println(s"realWindow排行榜背景${realWindow}")
    //绘制小地图
    ctx.setFont(Font.font("12px Helvetica"))
    ctx.setFill(Color.web(MyColors.rankList))
    ctx.fillRect(mapMargin,mapMargin,littleMap,littleMap)
    ctx.setStroke(Color.web("rgba(0,0,0,0)"))
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
    ctx.setFill(Color.web(MyColors.background))
    for(i <- 0 to 2){
      for (j <- 1 to 3){
        ctx.fillText((i*3+j).toString,mapMargin + abs(j-1)*margin+0.5*margin,mapMargin + i*margin+0.5*margin)
      }
    }
  }

  def drawKill(myId:String,grid:GridOnClient,isKill:Boolean,killList:List[(Int,String,Player)])={
    if(isKill){
      val showTime = killList.head._1
      val killerId = killList.head._2
      val deadPlayer = killList.head._3
      //      println("kk"+killerId)
      //      println("dd"+deadPlayer)
      //      println("gg"+grid.playerMap)
      val killerName = grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).name
      val deadName = deadPlayer.name
      val txt1=new Text(killerName)
      val txt2=new Text(deadName)
      val killNameLength=txt1.getLayoutBounds().getWidth()
      val deadNameLength=txt2.getLayoutBounds().getWidth()
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
        ctx.setFont(Font.font("25px Helvetica"))
        ctx.setStroke(Color.web("#f32705"))
        ctx.strokeText(killerName, 25, 400)
        ctx.setFill(Color.web("#f27c02"))
        ctx.fillText(killerName, 25, 400)
        ctx.drawImage(killImg,25+killNameLength+25,400,32,32)
        ctx.setStroke(Color.web("#f32705"))
        ctx.strokeText(deadName, 25+killNameLength+32+50, 400)
        ctx.setFill(Color.web("#f27c02"))
        ctx.fillText(deadName, 25+killNameLength+32+50+20, 400)
        ctx.strokeRect(12,375,50+killNameLength+deadNameLength+5+25+32,75)
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

  //offScreenCanvas:Canvas
  def drawGrid(uid: String, data: GridDataSync,foodMap:Map[Point,Int],offsetTime:Long,firstCome:Boolean,basePoint:(Double,Double),zoom:(Double,Double))= {
    //计算偏移量
    val players = data.playerDetails
    val foods = foodMap.map(f=>Food(f._2,f._1.x,f._1.y)).toList
    val masses = data.massDetails
    val virus = data.virusDetails

    val offx= realWindow.x/2 - basePoint._1
    val offy =realWindow.y/2 - basePoint._2
    //    println(s"zoom：$zoom")
    val scale = getZoomRate(zoom._1,zoom._2,1200,600)
    //var scale = data.scale

    //绘制背景
    //    ctx.fillStyle = MyColors.background


    ctx.setFill(Color.web("rgba(181, 181, 181, 1)"))
    ctx.fillRect(0,0,realWindow.x,realWindow.y)
    ctx.save()
    centerScale(scale,realWindow.x/2,realWindow.y/2)

    //TODO /2
    ctx.drawImage(background1,offx,offy,bounds.x,bounds.y)
    //    ctx.drawImage(offScreenCanvas, offx, offy, bounds.x, bounds.y)
//    ctx.drawImage(background1,)
    //为不同分值的苹果填充不同颜色
    //按颜色分类绘制，减少canvas状态改变
    foods.groupBy(_.color).foreach{a=>
      val foodColor = a._1 match{
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2 => "#f4d95b"
        case 3 => "#4cd964"
        case 4 => "#9fe0f6"
        case 5 => "#bead92"
        case 6 => "#cfe6ff"
        case _ => "#de9dd6"
      }
      ctx.setFill(Color.web(foodColor))
      a._2.foreach{ case Food(color, x, y)=>
//        ctx.beginPath()
//        ctx.arc(x + offx,y + offy,10,10,0,360)
//        ctx.fill()
          ctx.fillRect(x + offx,y + offy,8,8)
      }
    }
    masses.groupBy(_.color).foreach{ a=>
      a._1 match{
        case 0 => ctx.setFill(Color.web("#f3456d"))
        case 1 => ctx.setFill(Color.web("#f49930"))
        case 2  => ctx.setFill(Color.web("#f4d95b"))
        case 3  => ctx.setFill(Color.web("#4cd964"))
        case 4  => ctx.setFill(Color.web("#9fe0f6"))
        case 5  => ctx.setFill(Color.web("#bead92"))
        case 6  => ctx.setFill(Color.web("#cfe6ff"))
        case _  => ctx.setFill(Color.web("#de9dd6"))
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
        ctx.arc( xfix+offx ,yfix+offy ,r,r,0,360)
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
          ctx.setFill(Color.web(MyColors.halo))
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+15,cell.radius+15,0,360)
          ctx.fill()
        }

        var nameFont: Double = cell.radius * 2 / sqrt(4 + pow(name.length, 2))
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius else nameFont
        // println(nameFont)
        ctx.setFont(Font.font(s"${nameFont.toInt}px Helvetica"))
        val txt3=new Text(name)
        val nameWidth = txt3.getLayoutBounds().getWidth()
        ctx.setStroke(Color.web("grey"))
        ctx.strokeText(s"$name", xfix + offx - (nameWidth*nameFont/12.0) / 2, yfix + offy - (nameFont.toInt / 2 + 2))

        ctx.setFill(Color.web(MyColors.background))
        ctx.fillText(s"$name", xfix + offx - (nameWidth*nameFont/12.0) / 2, yfix + offy - (nameFont.toInt / 2 + 2))
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

      ctx.drawImage(img,xfix-radius+offx,yfix-radius+offy,radius*2,radius*2)
      ctx.restore()
    }

    ctx.restore()
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setTextBaseline(VPos.TOP)
  }

  //ctx3
  def drawRankMapData(uid:String,currentRank:List[Score],players:List[Player],basePoint:(Double,Double))={
    //绘制当前排行
    ctx.clearRect(0,0,realWindow.x,realWindow.y)
    ctx.setFont(Font.font("12px Helvetica"))
    //    ctx.fillStyle = MyColors.rankList
    //    ctx.fillRect(window.x-200,20,150,250)
    val currentRankBaseLine = 4
    var index = 0
    ctx.setFill(Color.web(MyColors.background))
    drawTextLine(s"————排行榜————", realWindow.x-200, index, currentRankBaseLine)
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
        ctx.drawImage(img, realWindow.x-200, index * textLineHeight+32, 13, 13)
      }
      //      ctx3.strokeStyle = drawColor
      //      ctx3.lineWidth = 18
      drawTextLine(s"【$index】: ${score.n.+("   ").take(4)} 得分:${score.score.toInt}", realWindow.x-193, index, currentRankBaseLine)
    }
    //绘制小地图

    ctx.setFill(Color.web(MyColors.background))
    players.find(_.id == uid) match {
      case Some(player)=>
        ctx.beginPath()
        ctx.arc(mapMargin + (basePoint._1/bounds.x) * littleMap, mapMargin + basePoint._2/bounds.y * littleMap,8,8,0,360)
        ctx.fill()
      //        ctx.fillArc(mapMargin + (basePoint._1/bounds.x) * littleMap, mapMargin + basePoint._2/bounds.y * littleMap,10,10,0,360,ArcType.CHORD)
      case None=>
      // println(s"${basePoint._1},  ${basePoint._2}")
    }
  }

  def drawWhenDead(msg:Protocol.UserDeadMessage) = {
    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0, 0, Boundary.w , Boundary.h )
    ctx.drawImage(deadbg,0,0, realWindow.x, realWindow.y)
    ctx.setFont(Font.font("50px Helvetica"))
    ctx.setFill(Color.web("#CD3700"))
    val Width = realWindow.x
    val Height = realWindow.y
    ctx.fillText(s"You Dead!", Width*0.42, Height*0.3)

    ctx.setFont(Font.font(s"${Window.w *0.02}px Comic Sans MS"))

    var DrawLeft = Width*0.35
    var DrawHeight = Height*0.3
    ctx.fillText(s"The   Killer  Is    :", DrawLeft, DrawHeight + Height*0.07)
    ctx.fillText(s"Your  Final   Score:", DrawLeft, DrawHeight + Height*0.07*2)
    ctx.fillText(s"Your  Final   LifeTime  :", DrawLeft, DrawHeight+Height*0.07*3)
    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*4)
    ctx.setFill(Color.WHITE)
    //    DrawLeft = Width*0.56+Width*0.12
    DrawLeft = Width*0.56
//    DrawLeft = ctx.measureText("Your  Final   LifeTime  :").width +  Width*0.35 + 30
    ctx.fillText(s"${msg.killerName}", DrawLeft,DrawHeight + Height*0.07)
    ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07*2)
    ctx.fillText(s"${MTime2HMS (msg.lifeTime)}", DrawLeft, DrawHeight + Height * 0.07 * 3)
    ctx.fillText(s"${msg.killNum}", DrawLeft,DrawHeight + Height*0.07*4)
  }

  def drawWhenFinish(msg:String) = {
    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0,0,realWindow.x,realWindow.y)
    ctx.setFont(Font.font(s"${30 * realWindow.x / Window.w}px Helvetica"))
    ctx.setFill(Color.web("#fff"))
    ctx.fillText(msg, realWindow.x/2 - 20,realWindow.y/2)
  }

  def centerScale(rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    //视角缩放
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
  }

  def cleanCtx()={
    ctx.clearRect(0,0,realWindow.x,realWindow.y)
  }

  def MTime2HMS(time:Long)={
    var ts = (time/1000)
    //    println(s"一共有 $ts 秒！")
    var result = ""
    if(ts/3600>0){
      result += s"${ts/3600}小时"
    }
    ts = ts % 3600
    //    println(s"第一次 $ts 秒！")
    if(ts/60>0){
      result += s"${ts/60}分"
    }
    ts = ts % 60
    //    println(s"第二次 $ts 秒！")
    result += s"${ts}秒"
    result
  }

}
