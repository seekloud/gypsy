package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync
import com.neo.sk.gypsy.shared.ptcl._
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.{Canvas, Image}
import com.neo.sk.gypsy.shared.util.utils._
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.html
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

  private[this] val  img = dom.document.getElementById("virus").asInstanceOf[HTMLElement]
  private[this] val  kill = dom.document.getElementById("kill").asInstanceOf[HTMLElement]
  private[this] val  youkill = dom.document.getElementById("youkill").asInstanceOf[HTMLElement]
  private[this] val  shutdown = dom.document.getElementById("shutdown").asInstanceOf[HTMLElement]
  private[this] val  killingspree = dom.document.getElementById("killingspree").asInstanceOf[HTMLElement]
  private[this] val  dominating = dom.document.getElementById("dominating").asInstanceOf[HTMLElement]
  private[this] val  unstoppable = dom.document.getElementById("unstoppable").asInstanceOf[HTMLElement]
  private[this] val  godlike = dom.document.getElementById("godlike").asInstanceOf[HTMLElement]
  private[this] val  legendary = dom.document.getElementById("legendary").asInstanceOf[HTMLElement]
  private val goldImg = dom.document.createElement("img").asInstanceOf[html.Image]
  goldImg.setAttribute("src", "/gypsy/static/img/gold.png")
  private val silverImg = dom.document.createElement("img").asInstanceOf[html.Image]
  silverImg.setAttribute("src", "/gypsy/static/img/silver.png")
  private val bronzeImg = dom.document.createElement("img").asInstanceOf[html.Image]
  bronzeImg.setAttribute("src", "/gypsy/static/img/cooper.png")

  //屏幕尺寸
  val bounds = Point(Boundary.w, Boundary.h)
  this.canvas.width=this.size.x
  this.canvas.height=this.size.y

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

  private[this] val stripeX = scala.collection.immutable.Range(0,bounds.y+50,50)
  private[this] val stripeY = scala.collection.immutable.Range(0,bounds.x+100,100)

  //绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

  //绘制背景ctx
  def drawGameOn(): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }

  //等待提示文字
  def drawGameWait(): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, size.x , size.y )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Welcome.", 150, 180)
  }

  //离线提示文字
  def drawGameLost: Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, size.x , size.y )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Ops, connection lost....", 350, 250)
  }

  //背景绘制ctx3
  def drawBackground():Unit = {
    //绘制背景
    ctx.fillStyle = MyColors.background
    ctx.fillRect(0,0,size.x,size.y)
    ctx.save()
    //绘制条纹
    ctx.strokeStyle = MyColors.stripe
    stripeX.foreach{ l=>
      ctx.beginPath()
      ctx.moveTo(0 ,l )
      ctx.lineTo(size.x ,l )
      ctx.stroke()
    }
    stripeY.foreach{ l=>
      ctx.beginPath()
      ctx.moveTo(l ,0)
      ctx.lineTo(l ,size.y)
      ctx.stroke()
    }
  }

  //ctx2
  def drawRankMap():Unit = {
    //绘制当前排行
    ctx.fillStyle = MyColors.rankList
    ctx.fillRect(size.x-200,20,150,250)

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
  }

  def drawKill(myId:Long,grid:GameClient,isKill:Boolean,killList:List[(Int,Long,Player)])={
    if(isKill){
      val showTime = killList.head._1
      val killerId = killList.head._2
      val deadPlayer = killList.head._3
      println("kk"+killerId)
      println("dd"+deadPlayer)
      println("gg"+grid.playerMap)
      val killerName = grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).name
      val deadName = deadPlayer.name
      val killImg = if (deadPlayer.kill > 3) shutdown
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 3) killingspree
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 4) dominating
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 5) unstoppable
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 6) godlike
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill >= 7) legendary
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
        ctx.strokeText(deadName, 25+ctx.measureText(s"$killerName  ").width+32+50, 400)
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


  def drawGrid(uid: Long, data: GridDataSync,offsetTime:Long,firstCome:Boolean,offScreenCanvas:Canvas,basePoint:(Double,Double),zoom:(Double,Double))= {
    //计算偏移量
    val players = data.playerDetails
    val foods = data.foodDetails
    val masses = data.massDetails
    val virus = data.virusDetails

    val offx= size.x/2 - basePoint._1
    val offy =size.y/2 - basePoint._2
    //    println(s"zoom：$zoom")
    val scale = getZoomRate(zoom._1,zoom._2)
    //var scale = data.scale

    //绘制背景
    //    ctx.fillStyle = MyColors.background
    ctx.fillStyle = "rgba(181, 181, 181, 1)"
    ctx.fillRect(0,0,size.x,size.y)
    ctx.save()
    centerScale(scale,size.x/2,size.y/2)

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

        var nameFont: Double = cell.radius * 2 / sqrt(4 + pow(name.length, 2))
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius else nameFont
        // println(nameFont)
        ctx.font = s"${nameFont.toInt}px Helvetica"
        val nameWidth = ctx.measureText(name).width
        //        ctx.strokeStyle = "grey"
        //        ctx.strokeText(s"$name", xfix + offx - nameWidth / 2, yfix + offy - (nameFont.toInt / 2 + 2))
        ctx.fillStyle = MyColors.background
        ctx.fillText(s"$name", xfix + offx - nameWidth / 2, yfix + offy - (nameFont.toInt / 2 + 2))
        ctx.restore()
      }
    }

    virus.foreach { case Virus(x,y,mass,radius,_,tx,ty,speed) =>
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
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
  }

  //ctx3
  def drawRankMap(uid:Long,currentRank:List[Score],players:List[Player],goldImg:html.Image,silverImg:html.Image,bronzeImg:html.Image,basePoint:(Double,Double))={
    //绘制当前排行
    ctx.clearRect(0,0,size.x,size.y)
    ctx.font = "12px Helvetica"
    //    ctx.fillStyle = MyColors.rankList
    //    ctx.fillRect(window.x-200,20,150,250)
    val currentRankBaseLine = 4
    var index = 0
    ctx.fillStyle = MyColors.background
    drawTextLine(s"—————排行榜—————", size.x-200, index, currentRankBaseLine)
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
        ctx.drawImage(img, size.x-200, index * textLineHeight+32, 13, 13)
      }
      //      ctx3.strokeStyle = drawColor
      //      ctx3.lineWidth = 18
      drawTextLine(s"【$index】: ${score.n.+("   ").take(4)} 得分:${score.score}", size.x-193, index, currentRankBaseLine)
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

  def centerScale(rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
  }

}