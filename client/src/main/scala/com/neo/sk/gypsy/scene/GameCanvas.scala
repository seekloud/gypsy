package com.neo.sk.gypsy.scene

import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text, TextAlignment}
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.util.utils.{getZoomRate, normalization, MTime2HMS, Mass2Radius }
import com.neo.sk.gypsy.common.Constant._
import com.neo.sk.gypsy.utils.BotUtil

import scala.collection.mutable.ArrayBuffer
import scala.math.{abs, pow, sqrt}

import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._


class GameCanvas(canvas: Canvas,
                 ctx:GraphicsContext,
                 size:Point) {
  val  img = new Image(ClientBoot.getClass.getResourceAsStream("/img/stone.png"))
//  val  circle = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle.png"))
//  val  circle1 = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle1.png"))
//  val  circle2 = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle2.png"))
//  val  circle3 = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle3.png"))
//  val  circle4 = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle4.png"))
//  val  circle5 = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle5.png"))
//  val  circle6 = new Image(ClientBoot.getClass.getResourceAsStream("/img/circle6.png"))
  val  star1 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-01.png"))
  val  star2 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-02.png"))
  val  star3 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-03.png"))
  val  star4 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-04.png"))
  val  star5 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-05.png"))
  val  star6 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-06.png"))
  val  star7 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-07.png"))
  val  star8 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-08.png"))
  val  star9 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-09.png"))
  val  star10 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-10.png"))
  val  star11 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-11.png"))
  val  star12 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-12.png"))
  val  star13 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-13.png"))
  val  star14 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-14.png"))
  val  star15 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-15.png"))
  val  star16 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-16.png"))
  val  star17 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-17.png"))
  val  star18 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-18.png"))
  val  star19 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-19.png"))
  val  star20 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-20.png"))
  val  star21 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-21.png"))
  val  star22 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-22.png"))
  val  star23 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-23.png"))
  val  star24 = new Image(ClientBoot.getClass.getResourceAsStream("/img/yuzhouxingqiu-24.png"))
  val  youkill = new Image(ClientBoot.getClass.getResourceAsStream("/img/youkill.png"))
  val  background1 = new Image(ClientBoot.getClass.getResourceAsStream("/img/b2small.jpg"))
//  val  massImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/mass.png"))
  val deadbg = new Image(ClientBoot.getClass.getResourceAsStream("/img/deadbg.jpg"))
  private val  goldImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/gold.png"))
  private val  silverImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/silver.png"))
  private val bronzeImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/cooper.png"))
  private val Vicbg = new Image(ClientBoot.getClass.getResourceAsStream("/img/Victory.jpg"))


  val bounds = Point(Boundary.w, Boundary.h)

  var realWindow = Point(size.x,size.y)
  var screeScale = if( realWindow.x / Window.w > realWindow.y/Window.h) realWindow.y / Window.h else realWindow.x / Window.w


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

  val littleMap = 200
  val mapMargin = 20

  //文本高度
  val textLineHeight = 18

  private[this] val stripeX = scala.collection.immutable.Range(0, bounds.y + 50,50)
  private[this] val stripeY = scala.collection.immutable.Range(0, bounds.x + 100,100)

  def resetScreen(width:Int,height:Int)={
    canvas.setHeight(height)
    canvas.setWidth(width)
    realWindow = Point(width,height)
    screeScale = if( realWindow.x / Window.w > realWindow.y/Window.h) realWindow.y / Window.h else realWindow.x / Window.w
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

  //欢迎文字（无）
  def drawGameWelcome(): Unit = {
    ctx.setFill(Color.web("rgba(255, 255, 255, 0)"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setFont(Font.font("Helvetica",36))
    ctx.fillText("Welcome.", 150, 180)
  }

  //等待文字
  def drawGameWait(firstCome:Boolean): Unit = {
    ctx.setFill(Color.web("rgba(255, 255, 255, 0)"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    if(firstCome) {
      ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
      ctx.setFont(Font.font("Helvetica",36))
      ctx.fillText("Please wait.", 350, 180)
    } else {
      ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
      ctx.setFont(Font.font("Helvetica",36))
      ctx.fillText("Ops, Loading....", 350, 250)
    }
  }


  //离线提示文字（无）
  def drawGameLost: Unit = {
    ctx.setFill(Color.web("rgba(255,255,255,0"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setFont(Font.font("Helvetica",36))
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
    ctx.fillRect(realWindow.x-200,20,170,230)


    println(s"realWindow排行榜背景${realWindow}")
    //绘制小地图
    ctx.setFont(Font.font("Helvetica",12))
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

  def drawKill(myId:String,grid:GridOnClient,isKill:Boolean,killList:List[(Int,String,String)])={
    if(isKill){
      val showTime = killList.head._1
      val killerName = killList.head._2
      val deadName = killList.head._3
      val txt1=new Text(killerName)
      val txt2=new Text(deadName)
      val killNameLength=txt1.getLayoutBounds.getWidth
      val deadNameLength=txt2.getLayoutBounds.getWidth
      val allWidth = (killNameLength + deadNameLength + 32 + 25 + 50)/2
     /* val killImg = if (deadPlayer.kill > 3) shutdown
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 3) {killingspree}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 4) {dominating}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 5) {unstoppable}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 6) {godlike}
      else if (grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill >= 7) {legendary}
      else if (killerId == myId) youkill
      else kill*/
      if (showTime > 0) {
        ctx.save()
        ctx.setFont(Font.font("Helvetica",25))
//        ctx.setStroke(Color.web("#f32705"))
//        ctx.strokeText(killerName, realWindow.x*0.5 - allWidth, realWindow.y*0.15)
        ctx.setFill(Color.web("#f27c02"))
        ctx.setTextAlign(TextAlignment.RIGHT)
        ctx.fillText(killerName, realWindow.x*0.5 - 50, realWindow.y*0.15)
        ctx.setTextAlign(TextAlignment.CENTER)
        ctx.drawImage(youkill,realWindow.x * 0.5 - 16,realWindow.y*0.15,32,32)
//        ctx.setStroke(Color.web("#f32705"))
//        ctx.strokeText(deadName, realWindow.x * 0.5 -allWidth + killNameLength + 32 + 50, realWindow.y*0.15)
//        ctx.setFill(Color.web("#f27c02"))
        ctx.setTextAlign(TextAlignment.LEFT)
        ctx.fillText(deadName, realWindow.x * 0.5 + 50, realWindow.y*0.15)
//        ctx.strokeRect(12,375,50+killNameLength+deadNameLength+5+25+32,75)
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


//  def drawGrid(uid: String, data: GridDataSync,foodMap:Map[Point,Int],offsetTime:Long,firstCome:Boolean,basePoint:(Double,Double),zoom:(Double,Double),gird: GridOnClient)= {
  def drawGrid(uid: String, data: GridDataSync,offsetTime:Long,basePoint:(Double,Double),zoom:(Double,Double),grid: GridOnClient)= {
    //offScreenCanvas:Canvas
    val players = data.playerDetails
    val foods =  grid.food.map(f=>Food(f._2,f._1.x,f._1.y)).toList
    val masses = data.massDetails
    val virus = data.virusDetails
    //计算偏移量
    val offx= realWindow.x/2 - basePoint._1
    val offy =realWindow.y/2 - basePoint._2
    val scale = getZoomRate(zoom._1,zoom._2,realWindow.x,realWindow.y) * screeScale

    /**这两部分代码不能交换，否则视图会无限缩小or放大**/
    //01
    ctx.setFill(Color.web("rgba(181, 181, 181, 1)"))
    ctx.fillRect(0,0,realWindow.x,realWindow.y)
    ctx.save()
    //02
    centerScale(scale,realWindow.x/2,realWindow.y/2)

    //   /2
    ctx.drawImage(background1,offx,offy,bounds.x,bounds.y)
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
          ctx.fillRect(x + offx,y + offy,16,16)
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

        val cellx = x +xPlus*offsetTime.toFloat /frameRate
        val celly = y  +yPlus*offsetTime.toFloat / frameRate
        val xfix  = if(cellx>bounds.x) bounds.x else if(cellx<0) 0 else cellx
        val yfix = if(celly>bounds.y) bounds.y else if(celly<0) 0 else celly
        //centerScale(scale,window.x/2,window.y/2)
        ctx.beginPath()
        ctx.arc( xfix+offx ,yfix+offy ,r,r,0,360)
        ctx.fill()
      }
    }
    players.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,lastSplit,width,height,cells,startTime) =>
      val circleImg = color match{
          //经典星球
//        case 0 => circle //(243,69,109)   b30e35
//        case 1 => circle1 //(244, 153, 48)  a65d0a
//        case 2  => circle2 //244, 217, 91   917600
//        case 3  => circle3   //76, 217, 100  05851b
//        case 4  => circle4  //159, 224, 246  037da6
//        case 5  => circle5   //190, 173, 146   875a16
//        case 6  => circle6  //207, 230, 255   4174ab
//        case _  => circle3  //222, 157, 214   8f3284
          //卡通星球
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
      var cellDifference = false
      val newcells = cells.sortBy(_.id).map{ cell=>

        val cellx = cell.x + cell.speedX *offsetTime.toFloat / frameRate
        val celly = cell.y + cell.speedY *offsetTime.toFloat / frameRate
        val xfix  = if(cellx>bounds.x-15) bounds.x-15 else if(cellx<15) 15 else cellx
        val yfix = if(celly>bounds.y-15) bounds.y-15 else if(celly<15) 15 else celly
        ctx.save()

        /**关键：根据mass来改变大小**/
        val radius = 4 + sqrt(cell.mass)*6
        ctx.drawImage(circleImg,xfix +offx-radius-6,yfix+offy-radius-6,2*(radius+6),2*(radius+6))
//        ctx.drawImage(circleImg,xfix +offx-cell.radius-6,yfix+offy-cell.radius-6,2*(cell.radius+6),2*(cell.radius+6))
        if(protect){
          ctx.setFill(Color.web(MyColors.halo))
          ctx.beginPath()
          ctx.arc(xfix+offx,yfix+offy,cell.radius+15,cell.radius+15,0,360)
          ctx.fill()
        }

        var nameFont = sqrt(cell.newmass*3)+3
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > 72) 72 else nameFont
        ctx.setFont(Font.font("Helvetica",nameFont))
        var playermass=cell.newmass.toInt
        val txt3=new Text(name)
        val nameWidth = txt3.getLayoutBounds.getWidth.toInt
        val txt4=new Text(playermass.toString)
        val massWidth = txt4.getLayoutBounds.getWidth.toInt
//        ctx.setStroke(Color.web("grey"))
//        println(s"nameFont:$nameFont namelength:${name.length}")
//        ctx.strokeText(s"$name", xfix + offx - (name.length*nameFont/3).toInt, yfix + offy - (nameFont.toInt / 2))
//        ctx.strokeText(s"$name", xfix + offx - nameWidth / 2 -nameFont, yfix + offy - (nameFont.toInt / 2))
        ctx.setFill(Color.web(MyColors.background))
        ctx.fillText(s"${playermass.toString}",xfix + offx - (name.length*nameFont/6).toInt, yfix + offy + nameFont.toInt/4)
        ctx.fillText(s"$name", xfix + offx - (name.length*nameFont/3).toInt-5, yfix + offy - (nameFont.toInt / 2))
//        ctx.fillText(s"${playermass.toString}",xfix + offx - massWidth / 2 -nameFont, yfix + offy + nameFont.toInt/2)
//        ctx.fillText(s"$name", xfix + offx - nameWidth / 2 -nameFont, yfix + offy - (nameFont.toInt / 2))
        ctx.restore()
        /**膨胀、缩小效果**/
        var newcell = cell
        //        println(cell.mass +"  "+cell.newmass)
        if(cell.mass != cell.newmass){
          //根据差值来膨胀或缩小
          cellDifference = true
          val massSpeed:Short = if(cell.mass < cell.newmass) 1 else if(cell.mass>cell.newmass) -1 else 0
          val tempMass:Short = (cell.mass + massSpeed).toShort
          newcell = cell.copy(mass = tempMass, radius = Mass2Radius(tempMass))
        }
        newcell
      }
      if(cellDifference){
        //改变player的x,y
        val length = newcells.length
        val newX = newcells.map(_.x).sum / length
        val newY = newcells.map(_.y).sum / length
        val left = newcells.map(a => a.x - a.radius).min
        val right = newcells.map(a => a.x + a.radius).max
        val bottom = newcells.map(a => a.y - a.radius).min
        val top = newcells.map(a => a.y + a.radius).max
        val player = Player(id,name,color,newX.toShort,newY.toShort,tx,ty,kill,protect,lastSplit,right - left,top - bottom,newcells,startTime)
        grid.playerMap += (id -> player)
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

      ctx.drawImage(img,xfix-radius+offx,yfix-radius+offy,radius*2,radius*2)
      ctx.restore()
    }

    ctx.restore()
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setTextBaseline(VPos.TOP)
  }

  //ctx3
  def drawRankMapData(uid:String,currentRank:List[RankInfo],players:List[Player],basePoint:(Double,Double),bigPlayerPosition:List[PlayerPosition])={
    //绘制当前排行
    ctx.clearRect(0,0,realWindow.x,realWindow.y)
    ctx.setFont(Font.font("Helvetica",12))
    //    ctx.fillStyle = MyColors.rankList
    //    ctx.fillRect(window.x-200,20,150,250)
    val currentRankBaseLine = 3
    ctx.setFill(Color.web(MyColors.background))
    drawTextLine(s"————排行榜————", realWindow.x-190, 0, currentRankBaseLine)

    //这里过滤是为了防止回放的时候传全量的排行版数据
    currentRank.zipWithIndex.filter(r=>r._2<GameConfig.rankShowNum || r._1.score.id == uid).foreach{rank=>
      val score = rank._1.score
      //      val index = rank._2+1
      val index = rank._1.index

      val imgOpt = index match {
        case 1 => Some(goldImg)
        case 2 => Some(silverImg)
        case 3 => Some(bronzeImg)
        case _ => None
      }
      imgOpt.foreach{ img =>
        ctx.drawImage(img, realWindow.x-200, index * textLineHeight+24, 13, 13)
      }
      ctx.save()
      if(score.id == uid){
        ctx.setFont(Font.font("Helvetica",12))
        ctx.setFill(Color.web("#FFFF33"))
        drawTextLine(if(score.n.length < 5) s"【$index】: ${score.n.+("   ").take(4)}" else s"【$index】: ${score.n.take(4) + "..."}", realWindow.x-193, if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
        drawTextLine(s"得分:${score.score.toInt}", realWindow.x - 90, if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index, currentRankBaseLine)
      }else{
        drawTextLine(if(score.n.length < 5) s"【$index】: ${score.n.+("   ").take(4)}" else s"【$index】: ${score.n.take(4) + "..."}", realWindow.x-193, index , currentRankBaseLine)
        drawTextLine(s"得分:${score.score.toInt}", realWindow.x - 90, index, currentRankBaseLine)
      }
      ctx.restore()
    }

    //绘制小地图

    ctx.setFill(Color.web(MyColors.bigPlayer))
    bigPlayerPosition.filterNot(_.id==uid).map{player=>
      val offx = player.x.toDouble
      val offy = player.y.toDouble
      ctx.beginPath()
      ctx.arc(mapMargin + (offx/bounds.x) * littleMap,mapMargin + offy/bounds.y * littleMap,8,8,0,360)
      ctx.fill()
    }

    ctx.setFill(Color.web(MyColors.myHeader))
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
    ctx.setFont(Font.font("Helvetica",50))
    ctx.setFill(Color.web("#CD3700"))
    val Width = realWindow.x
    val Height = realWindow.y
    ctx.fillText(s"You Dead!", Width*0.42, Height*0.3)

    ctx.setFont(Font.font("Comic Sans MS",Window.w *0.02))

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

  def drawVictory(VictoryMsg:(Protocol.VictoryMsg,Short,Boolean))={
    val msg = VictoryMsg._1
    val isVictory = VictoryMsg._3
    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0, 0, Boundary.w , Boundary.h )
    ctx.drawImage(Vicbg, 0,0, realWindow.x, realWindow.y)
    //    ctx.font = "30px Helvetica"
    ctx.setFill(Color.web("#CD3700"))
    val Width = realWindow.x
    val Height = realWindow.y
    ctx.setFont(Font.font("Comic Sans MS",Width *0.03))
    //    val BaseHeight = Height*0.3
    val BaseHeight = Height*0.15
    var DrawLeft = Width*0.30
    var DrawHeight = BaseHeight + Height * 0.1

    val congratulation =if(isVictory){
      "Good Game!  Congratulations to: You~"
    }else{
      "Good Game!  Congratulations to: "
    }
//    val text = new Text(congratulation)
    ctx.save()
    ctx.setTextAlign(TextAlignment.CENTER)
    ctx.fillText(congratulation, Width * 0.5, BaseHeight)

    ctx.setFill(Color.YELLOW)
    val winner = s"${msg.name}"
//    val winnerText = new Text(winner)
    ctx.fillText(winner, Width * 0.5, BaseHeight+Height *0.1 )
    DrawHeight = BaseHeight + Height * 0.15
    ctx.setFont(Font.font("Comic Sans MS",Width *0.02))

    ctx.setTextAlign(TextAlignment.LEFT)
    val Time = MTime2HMS (msg.totalFrame * GameConfig.frameRate)
    ctx.fillText(s"The   Winner  Score  :", DrawLeft, DrawHeight + Height*0.07)
    ctx.fillText(s"Your  Final   Score  :", DrawLeft, DrawHeight + Height*0.07*2)
    ctx.fillText(s"Game  Time   :", DrawLeft, DrawHeight + Height*0.07*3)
    //    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*3)
    ctx.setFill(Color.WHITE)
    val winnerScore = new Text("The   Winner  Score  :")
    DrawLeft = DrawLeft + 350
    ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07)
    ctx.fillText(s"${VictoryMsg._2}", DrawLeft,DrawHeight + Height*0.07*2)
    ctx.fillText(s"${Time}", DrawLeft,DrawHeight + Height*0.07*3)

    val reStart = s"Press Space to Start a New Game ୧(●⊙(工)⊙●)୨ "
//    val reStartText = new Text(reStart)
    ctx.fillText(reStart, Width * 0.5, DrawHeight + Height*0.07*5)
    ctx.restore()

  }

  def drawWhenFinish(msg:String) = {
    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0,0,realWindow.x,realWindow.y)
    ctx.setFont(Font.font("Helvetica",30 * realWindow.x / Window.w))
    ctx.setFill(Color.web("#fff"))
    val txt = new Text(msg)
    ctx.fillText(msg, realWindow.x * 0.5 - txt.getLayoutBounds.getWidth* 0.5,realWindow.y * 0.5)
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

//  分层时候背景以及排行版等黑色蒙版
  def drawLayeredBg() = {
    ctx.setFill(Color.web("rgba(0,0,0,0.5)"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )

    ctx.setFill(Color.web(MyColors.rankList))
    ctx.fillRect(realWindow.x-200,20,150,250)

    //绘制小地图
    ctx.setFont(Font.font("Helvetica",12))
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

}
