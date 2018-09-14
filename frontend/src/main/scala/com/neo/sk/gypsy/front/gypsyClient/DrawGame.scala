package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GridDataSync
import com.neo.sk.gypsy.shared.ptcl._
import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.{Canvas, Image}

import scala.math._
/**
  * User: sky
  * Date: 2018/9/14
  * Time: 10:04
  */
class DrawGame(
              ctx:CanvasRenderingContext2D,
              canvas:Canvas,
              size:Point
              ) {
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

  private[this] val stripeX = scala.collection.immutable.Range(0,size.y+50,50)
  private[this] val stripeY = scala.collection.immutable.Range(0,size.x+100,100)

  //绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }

  //绘制背景
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
  def drawGameLost(nextFrame:Int): Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, size.x , size.y )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    dom.window.cancelAnimationFrame(nextFrame)
    ctx.font = "36px Helvetica"
    ctx.fillText("Ops, connection lost....", 350, 250)
  }

  //背景绘制
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

  def draw(myId:Long,grid:GameClient,isKill:Boolean,killList:List[(Int,Long,Player)],offsetTime:Long)= {
    val data = grid.getGridData(myId)
    //println(s"data$data")
    drawGrid(myId, data,offsetTime)
    if(isKill){
      val showTime = killList.head._1
      val killerId = killList.head._2
      val deadPlayer = killList.head._3
      println("kk"+killerId)
      println("dd"+deadPlayer)
      println("gg"+grid.playerMap)
      val killerName = grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).name
      val deadName = deadPlayer.name
      val showText = if (killerId == myId) "You Killed"
      else if (deadPlayer.kill >3)"Shut Down"
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 3) "Killing Spree"
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 4) "Dominating"
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 5) "Unstoppable"
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill == 6) "Godlike"
      else if (grid.playerMap.getOrElse(killerId, Player(0, "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill >= 7) "Legendary"
      if (showTime > 0) {
        ctx.save()
        ctx.font = "25px Helvetica"
        ctx.strokeStyle = "#f32705"
        ctx.strokeText(s"$killerName $showText $deadName", 10, 400)
        ctx.fillStyle = "#f27c02"
        ctx.fillText(s"$killerName $showText $deadName", 10, 400)
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

        }
        val offx = sumX /p.cells.length
        val offy = sumY /p.cells.length


        (offx, offy)
      case None =>
        (bounds.x.toDouble / 2, bounds.y.toDouble / 2)
    }



    val offx= window.x/2 - basePoint._1
    val offy =window.y/2 - basePoint._2
    //    println(s"zoom：$zoom")
    val scale = getZoomRate(zoom._1,zoom._2)
    //var scale = data.scale

    //绘制背景
    //    ctx.fillStyle = MyColors.background
    ctx.fillStyle = "rgba(181, 181, 181, 1)"
    ctx.fillRect(0,0,window.x,window.y)
    ctx.save()
    centerScale(scale,window.x/2,window.y/2)

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
        //        ctx.strokeStyle = "grey"
        //        ctx.strokeText(s"$name", xfix + offx - nameWidth / 2, yfix + offy - (nameFont.toInt / 2 + 2))
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
        ctx3.drawImage(img, window.x-200, (index) * textLineHeight+32, 13, 13)
      }
      //      ctx3.strokeStyle = drawColor
      //      ctx3.lineWidth = 18
      drawTextLine(s"【$index】: ${score.n.+("   ").take(4)} 得分:${score.score}", window.x-193, index, currentRankBaseLine)
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

}
