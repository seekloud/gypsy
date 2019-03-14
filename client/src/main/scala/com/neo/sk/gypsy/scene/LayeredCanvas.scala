package com.neo.sk.gypsy.scene

import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.common.Constant.{ColorsSetting, informHeight, viewRatio}
import com.neo.sk.gypsy.holder.BotHolder._
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.{Protocol, _}
import com.neo.sk.gypsy.shared.util.utils.{MTime2HMS, Mass2Radius, getZoomRate, normalization}
import com.neo.sk.gypsy.utils.{BotUtil, FpsComp}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text, TextAlignment}
import com.neo.sk.gypsy.shared.ptcl.Protocol.{GridDataSync, MP}
import javafx.geometry.VPos
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import scala.math.{abs, pow, sqrt}

/**
  * Created by YXY on Date: 2018/12/18
  */

class LayeredCanvas(canvas: Canvas,
                    ctx:GraphicsContext,
                    size:Point,
                    is2Byte:Boolean
                 ) {

  val img = new Image(ClientBoot.getClass.getResourceAsStream("/img/stone.png"))
  val  background1 = new Image(ClientBoot.getClass.getResourceAsStream("/img/b2small.jpg"))

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
  val deadbg = new Image(ClientBoot.getClass.getResourceAsStream("/img/deadbg.jpg"))
  val  youkill = new Image(ClientBoot.getClass.getResourceAsStream("/img/youkill.png"))
  private val Vicbg = new Image(ClientBoot.getClass.getResourceAsStream("/img/Victory.jpg"))

  private val  goldImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/gold.png"))
  private val  silverImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/silver.png"))
  private val bronzeImg = new Image(ClientBoot.getClass.getResourceAsStream("/img/cooper.png"))

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
    val kill = "#f27c02"
  }

  val littleMap = 120
  val mapMargin = 20
  val mapLeft = 160

  //文本高度
  val textLineHeight = 14

  var realWindow = Point(size.x,size.y)

  //TODO 可以考虑绘图和获取ByteString分开，但是目前测试阶段还是写到一起
  def resetScreen(width:Int,height:Int)={
    canvas.setHeight(height)
    canvas.setWidth(width)
    realWindow = Point(width,height)
  }

  /*********************分层视图400*200****************************/

  /*******************1.视野在整个地图中的位置***********************/
  def drawLocation(basePoint:(Double,Double))={
    ctx.setFill(Color.BLACK)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)
    //TODO 视野缩放
    ctx.setFill(Color.WHITE)
    ctx.fillRect((basePoint._1 - Window.w/2)/(Boundary.w/realWindow.x),(basePoint._2 - Window.h/2)/(Boundary.h/realWindow.y),
      realWindow.x *(Window.w/Boundary.w),realWindow.y *(Window.h/Boundary.h))
    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }
  }

  /********************2.视野内不可交互的元素（地图背景元素）*********************/
  def drawNonInteract(basePoint:(Double,Double),scale:Double) = {
    val layeredOffX = realWindow.x/2 - basePoint._1
    val layeredOffY = realWindow.y/2 - basePoint._2
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)
    ctx.save()
    centerScale(ctx,scale/viewRatio,realWindow.x /2,realWindow.y /2)
    ctx.setFill(Color.BLACK)
    //TODO 偏移要看
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)
    ctx.restore()
    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }
  }

  /********************3.视野内可交互的元素（food，mass，virus）是否包含边界****************/
  def drawInteract(data:Protocol.GridDataSync,basePoint:(Double,Double),scale:Double)={
    val layeredOffX = realWindow.x/2 - basePoint._1
    val layeredOffY = realWindow.y/2 - basePoint._2
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)

    ctx.save()
    centerScale(ctx,scale/viewRatio,realWindow.x /2,realWindow.y /2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    grid.food.map(f=>Food(f._2,f._1.x,f._1.y)).toList.groupBy(_.color).foreach{a=>
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
        ctx.fillRect(x + layeredOffX,y + layeredOffY,10,10)
      }
    }

    data.massDetails.groupBy(_.color).foreach{ a=>
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

        ctx.beginPath()
        ctx.arc( x+layeredOffX ,y+layeredOffY ,r*0.5,r*0.5,0,360)
        ctx.fill()
      }
    }

    data.virusDetails.values.toList.foreach { case Virus(vid,x,y,mass,radius,tx,ty,speed) =>
      ctx.drawImage(img,x-radius+layeredOffX,y-radius+layeredOffY,radius,radius)
    }

    ctx.restore()

    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /********************4.视野内的玩家实体******************************/
  def drawKernel(data:Protocol.GridDataSync,basePoint:(Double,Double),scale:Double) = {
    val layeredOffX = realWindow.x/2 - basePoint._1
    val layeredOffY = realWindow.y/2 - basePoint._2
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)
    ctx.save()
    centerScale(ctx,scale/viewRatio,realWindow.x /2,realWindow.y /2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    data.playerDetails.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,_,width,height,cells,startTime) =>
      var circleColor = color.toInt % 7 match{
        //纯色星球
        case 0 => "#b30e35"
        case 1 => "#a65d0a"
        case 2  => "#917600"
        case 3  => "#05851b"
        case 4  => "#037da6"
        case 5  => "#875a16"
        case 6  => "#4174ab"
        case _  => "#8f3284"

      }
      if(id==grid.myId){
        circleColor = "#f5c673"
      }
      ctx.setFill(Color.web(circleColor))
      cells.sortBy(_.id).foreach{ cell=>
        ctx.save()
        ctx.beginPath()
        ctx.arc( cell.x+layeredOffX ,cell.y+layeredOffY ,cell.radius,cell.radius,0,360)
        ctx.fill()

        if(protect){
          ctx.setFill(Color.web(MyColors.halo))
          ctx.beginPath()
          ctx.arc(cell.x+layeredOffX,cell.y+layeredOffY,(cell.radius+15),(cell.radius+15),0,360)
          ctx.fill()
        }
        ctx.restore()
      }
    }

    ctx.restore()

    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /********************5.视野内的所有权视图******************************/
  def drawAllPlayer(data:Protocol.GridDataSync,basePoint:(Double,Double),scale:Double) = {
    val layeredOffX = realWindow.x/2 - basePoint._1
    val layeredOffY = realWindow.y/2 - basePoint._2
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)

    ctx.save()
    centerScale(ctx,scale/viewRatio,realWindow.x/2,realWindow.y/2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    data.playerDetails.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,_,width,height,cells,startTime) =>
      var circleColor = color.toInt % 7 match{
        //纯色星球
        case 0 => "#b30e35"
        case 1 => "#a65d0a"
        case 2  => "#917600"
        case 3  => "#05851b"
        case 4  => "#037da6"
        case 5  => "#875a16"
        case 6  => "#4174ab"
        case _  => "#8f3284"

      }
      if(id==grid.myId){
        circleColor = "#f5c673"
      }
      ctx.setFill(Color.web(circleColor))
      cells.sortBy(_.id).foreach{ cell=>
        ctx.save()
        ctx.beginPath()
        ctx.arc( cell.x+layeredOffX ,cell.y+layeredOffY ,cell.radius,cell.radius,0,360)
        ctx.fill()

        if(protect){
          ctx.setFill(Color.web(MyColors.halo))
          ctx.beginPath()
          ctx.arc(cell.x+layeredOffX,cell.y+layeredOffY,cell.radius+15,cell.radius+15,0,360)
          ctx.fill()
        }
        ctx.restore()
      }
    }

    ctx.restore()

    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /*********************6.视野内的当前玩家资产视图**************************/
  def drawPlayer(data:Protocol.GridDataSync,basePoint:(Double,Double),scale:Double) = {
    val layeredOffX = realWindow.x/2 - basePoint._1
    val layeredOffY = realWindow.y/2 - basePoint._2
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)

    ctx.save()
    centerScale(ctx,scale/viewRatio,realWindow.x/2,realWindow.y/2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    val my = data.playerDetails.find(_.id == grid.myId).get
    val name = my.name
    val color = my.color
    val x = my.x
    val y = my.y
    val protect = my.protect
    val cells = my.cells


    val circleColor = "#f5c673"
    ctx.setFill(Color.web(circleColor))
    cells.sortBy(_.id).foreach{ cell=>
      ctx.save()
      ctx.beginPath()
      ctx.arc( x+layeredOffX ,y+layeredOffY ,cell.radius,cell.radius,0,360)
      ctx.fill()

      if(protect){
        ctx.setFill(Color.web(MyColors.halo))
        ctx.beginPath()
        ctx.arc(x+layeredOffX,y+layeredOffY,cell.radius+15,cell.radius+15,0,360)
        ctx.fill()
      }
      ctx.restore()
    }

    ctx.restore()
    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /*********************7.鼠标指针位置************************************/
  def drawPointer(mouseActionMap:Map[Int, Map[String, MP]],basePoint:(Double,Double),scale:Double) = {
    val layeredOffX = realWindow.x/2 - basePoint._1
    val layeredOffY = realWindow.y/2 - basePoint._2
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)
    ctx.save()
    centerScale(ctx,scale/viewRatio,realWindow.x/2,realWindow.y/2)
    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    if(!mouseActionMap.isEmpty){
      //myId --> MP
      val myMouseAction = mouseActionMap.toList.sortBy(_._1).reverse.head._2.toList.filter(_._1==grid.myId)
      if(myMouseAction.nonEmpty){
        val p  = myMouseAction.head
        ctx.setFill(Color.WHITE)
        ctx.beginPath()
        ctx.arc(p._2.cX + realWindow.x/2, p._2.cY + realWindow.y/2,15,15,0,360)
        ctx.fill()
      }
//      mouseActionMap.toList.sortBy(_._1).reverse.head._2.toList.filter(_._1==grid.myId).foreach{p=>
//        ctx.setFill(Color.WHITE)
//        ctx.beginPath()
//        ctx.arc(p._2.cX + layeredOffX, p._2.cY + layeredOffY,15,15,0,360)
//        ctx.fill()
//      }
    }

    ctx.restore()
    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /*********************8.当前用户状态视图************************************/
  def drawInform(data:Protocol.GridDataSync) = {
    /**包括总分数、分裂个数**/
    ctx.setFill(Color.BLACK)
    ctx.fillRect(0, 0, realWindow.x, realWindow.y)
    val player = data.playerDetails.filter(_.id ==grid.myId).head
    ctx.setFill(ColorsSetting.scoreColor)
    ctx.fillRect(0, informHeight*2, realWindow.x * player.cells.map(_.mass).sum /VictoryScore,informHeight*1.5)
    ctx.setFill(ColorsSetting.splitNumColor)
    ctx.fillRect(0, informHeight*5,realWindow.x * player.cells.length /VirusSplitNumber,informHeight*1.5)
    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /********************* 人类视图：800*400 ***********************************/

  def drawPlayState(data:Protocol.GridDataSync,basePoint:(Double,Double),zoom:(Double,Double))={
    val scale = drawPlayView(grid.myId,data,basePoint,zoom,grid)
    if(is2Byte){
      (BotUtil.canvas2byteArray(canvas),scale)
    }else{
      (BotUtil.emptyArray,scale)
    }

  }

  def drawPlayView(uid: String, data: GridDataSync,basePoint:(Double,Double),zoom:(Double,Double),grid: GridOnClient) = {

    /*
    正常游戏绘图
     */
    val players = data.playerDetails
    val foods =  grid.food.map(f=>Food(f._2,f._1.x,f._1.y)).toList
    val masses = data.massDetails
    val virus = data.virusDetails
    //计算偏移量
    val offx = realWindow.x/2 - basePoint._1
    val offy = realWindow.y/2 - basePoint._2
    val screenIndex = if( realWindow.x / Window.w > realWindow.y / Window.h) realWindow.y / Window.h else realWindow.x / Window.w
    val scale = getZoomRate(zoom._1,zoom._2,realWindow.x,realWindow.y) * screenIndex

    /**这两部分代码不能交换，否则视图会无限缩小or放大**/
    //01
    ctx.setFill(Color.web("rgba(181, 181, 181, 1)"))
    ctx.fillRect(0,0,realWindow.x,realWindow.y)
    ctx.save()
    //02
    centerScale(ctx,scale,realWindow.x /2.0, realWindow.y /2.0)

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
        ctx.beginPath()
        ctx.arc(x + offx,y+ offy ,r,r,0,360)
        ctx.fill()
      }
    }
    players.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,lastSplit,width,height,cells,startTime) =>
      val circleImg = color.toInt match{
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

        ctx.save()

        /**关键：根据mass来改变大小**/
        val radius = 4 + sqrt(cell.mass)*6
        ctx.drawImage(circleImg,cell.x+ offx-radius-6 ,cell.y+ offy-radius-6,2*(radius+6),2*(radius+6))
        //        ctx.drawImage(circleImg,xfix +offx-cell.radius-6,yfix+offy-cell.radius-6,2*(cell.radius+6),2*(cell.radius+6))
        if(protect){
          ctx.setFill(Color.web(MyColors.halo))
          ctx.beginPath()
          ctx.arc(cell.x+ offx,cell.y+ offy,cell.radius+15,cell.radius+15,0,360)
          ctx.fill()
        }

        var nameFont: Double = cell.radius * 2 / sqrt(4 + pow(name.length, 2))
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius*0.8 else nameFont
        ctx.setFont(Font.font("Helvetica",nameFont))
//        var playermass=cell.mass.toInt
        var playermass=cell.newmass.toInt
        val txt3=new Text(name)
        val txt4=new Text(playermass.toString)
        val nameWidth = txt3.getLayoutBounds.getWidth
        val massWidth = txt4.getLayoutBounds.getWidth
//        ctx.setStroke(Color.web("grey"))
//        ctx.strokeText(s"$name", cell.x + offx - nameWidth / 2, cell.y + offy - (nameFont.toInt / 2))
        ctx.setFill(Color.web(MyColors.background))
        ctx.fillText(s"${playermass.toString}",cell.x + offx - massWidth /2 -6, cell.y + offy + nameFont.toInt/2)
        ctx.fillText(s"$name", cell.x + offx- nameWidth /2 -6, cell.y + offy - (nameFont.toInt / 2))
        ctx.restore()
        /**膨胀、缩小效果**/
        var newcell = cell
        //        println(cell.mass +"  "+cell.newmass)
        if(cell.mass != cell.newmass){
          //根据差值来膨胀或缩小
          cellDifference = true
          val massSpeed:Short = if(cell.mass < cell.newmass) 1 else -1
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
        val player = Player(id,name,color,newX.toShort ,newY.toShort ,tx,ty,kill,protect,lastSplit,right - left,top - bottom,newcells,startTime)
        grid.playerMap += (id -> player)
      }
    }
    virus.values.foreach { case Virus(vid,x,y,mass,radius,tx,ty,speed) =>
      ctx.save()
      ctx.drawImage(img,x-radius+ offx,y-radius+ offy,radius*2,radius*2)
      ctx.restore()
    }

    ctx.restore()
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setTextBaseline(VPos.TOP)

    drawSmallMap()
    drawRankMap(players,basePoint)
    val paraBack = drawKill(grid.myId,grid,isDead,killList)
    killList=paraBack._1
    isDead=paraBack._2
    FpsComp.renderFps(ctx, 450, 10)
    scale

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
        ctx.fillText(killerName, realWindow.x*0.5 - allWidth, realWindow.y*0.15)
        ctx.drawImage(youkill,realWindow.x * 0.5 -allWidth + killNameLength + 50,realWindow.y*0.15,32,32)
        //        ctx.setStroke(Color.web("#f32705"))
        //        ctx.strokeText(deadName, realWindow.x * 0.5 -allWidth + killNameLength + 32 + 50, realWindow.y*0.15)
        //        ctx.setFill(Color.web("#f27c02"))
        ctx.fillText(deadName, realWindow.x * 0.5 -allWidth + killNameLength + 32 + 50, realWindow.y*0.15)
        //        ctx.strokeRect(12,375,50+killNameLength+deadNameLength+5+25+32,75)
        ctx.restore()
        val killList1 = if (showTime > 10) (showTime - 10, killerName, deadName) :: killList.tail else killList.tail
        if (killList1.isEmpty) (killList1,false) else (killList1,isKill)
      }else{
        (killList,isKill)
      }
    }else{
      (killList,isKill)
    }
  }

  def drawSmallMap()= {
    /*
  小地图，排行版蒙版
   */
    ctx.save()
    ctx.setFill(Color.web(MyColors.rankList))
    ctx.fillRect(realWindow.x -mapLeft,20,150,250)

    //绘制小地图
    ctx.setFont(Font.font("Helvetica",12))
    ctx.setFill(Color.web(MyColors.rankList))
    ctx.fillRect(mapMargin,mapMargin,littleMap,littleMap)
    ctx.setStroke(Color.web("rgba(255,255,255,0)"))

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

    ctx.restore()
  }

  def drawRankMap(players:List[Game.Player],basePoint:(Double,Double)) = {
    //绘制当前排行
    ctx.setFont(Font.font("Helvetica",12))
    val currentRankBaseLine = 3
    ctx.setFill(Color.web(MyColors.background))
    drawTextLine(s"————排行榜————", realWindow.x -mapLeft, 0, currentRankBaseLine)

    //这里过滤是为了防止回放的时候传全量的排行版数据
    val ranks = grid.currentRank
    ranks.zipWithIndex.filter(r=>r._2<GameConfig.rankShowNum || r._1.score.id == grid.myId).foreach{rank=>
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
        ctx.drawImage(img, realWindow.x-mapLeft, index * textLineHeight+30, 13, 13)
      }
      if(score.id == grid.myId){
        ctx.save()
        ctx.setFont(Font.font("Helvetica",12))
        ctx.setFill(Color.web("#FFFF33"))
        drawTextLine(s"【${index}】: ${score.n.+("   ").take(4)} 得分:${score.score.toInt}", realWindow.x-mapLeft + 7, if(index>GameConfig.rankShowNum)GameConfig.rankShowNum+1 else index , currentRankBaseLine)
        ctx.restore()

        ctx.save()
        ctx.setFont(Font.font(" Helvetica",24))
        ctx.fillText(s"KILL: ${score.k}", 150, 10)
        ctx.fillText(s"SCORE: ${score.score}", 250, 10)
        ctx.restore()


      }else{
        drawTextLine(s"【${index}】: ${score.n.+("   ").take(4)} 得分:${score.score.toInt}", realWindow.x-mapLeft + 7, index , currentRankBaseLine)
      }

    }

    //绘制小地图

    //    ctx.setFill(Color.web(MyColors.bigPlayer))
    //    bigPlayerPosition.filterNot(_.id==uid).map{player=>
    //      val offx = player.x.toDouble
    //      val offy = player.y.toDouble
    //      ctx.beginPath()
    //      ctx.arc(mapMargin + (offx/bounds.x) * littleMap,mapMargin + offy/bounds.y * littleMap,8,8,0,360)
    //      ctx.fill()
    //    }

    ctx.setFill(Color.web(MyColors.myHeader))
    players.find(_.id == grid.myId) match {
      case Some(player)=>
        ctx.beginPath()
        ctx.arc(mapMargin + (basePoint._1/bounds.x) * littleMap, mapMargin + basePoint._2/bounds.y * littleMap,8,8,0,360)
        ctx.fill()
      //        ctx.fillArc(mapMargin + (basePoint._1/bounds.x) * littleMap, mapMargin + basePoint._2/bounds.y * littleMap,10,10,0,360,ArcType.CHORD)
      case None=>
      // println(s"${basePoint._1},  ${basePoint._2}")
    }
  }

  //绘制一条信息
  def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
    ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
  }


  def drawDeadState(msg:Protocol.UserDeadMessage) = {
    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0, 0, realWindow.x , realWindow.y )
    ctx.drawImage(deadbg,0,0, realWindow.x, realWindow.y)
    ctx.setFont(Font.font("Helvetica",50))
    ctx.setFill(Color.web("#CD3700"))
    val Width = realWindow.x
    val Height = realWindow.y
    ctx.fillText(s"You Dead!", Width*0.3, Height*0.1)

    ctx.setFont(Font.font("Comic Sans MS",Window.w *0.02))

    var DrawLeft = Width*0.30
    var DrawHeight = Height*0.2
    ctx.fillText(s"The   Killer  Is       :", DrawLeft, DrawHeight + Height*0.07)
    ctx.fillText(s"Your  Final   Score    :", DrawLeft, DrawHeight + Height*0.07*2)
    ctx.fillText(s"Your  Final   LifeTime :", DrawLeft, DrawHeight+Height*0.07*3)
    ctx.fillText(s"Your  Kill    Num      :", DrawLeft, DrawHeight + Height*0.07*4)
    ctx.setFill(Color.WHITE)
    //    DrawLeft = Width*0.56+Width*0.12
    DrawLeft = Width*0.65
    //    DrawLeft = ctx.measureText("Your  Final   LifeTime  :").width +  Width*0.35 + 30
    ctx.fillText(s"${msg.killerName}", DrawLeft,DrawHeight + Height*0.07)
    ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07*2)
    ctx.fillText(s"${MTime2HMS (msg.lifeTime)}", DrawLeft, DrawHeight + Height * 0.07 * 3)
    ctx.fillText(s"${msg.killNum}", DrawLeft,DrawHeight + Height*0.07*4)
    val reStart = "Press Space to ReStart ￣へ￣#  "
    ctx.fillText(reStart, Width*0.3, DrawHeight + Height*0.07*5)

    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }
  }

  def drawFinishState() = {
    val msg = "存在异地登录"

    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0,0,realWindow.x,realWindow.y)
    //这里1600是窗口的长以后换
    ctx.setFont(Font.font("Helvetica",30 * realWindow.x / 1600))
    ctx.setFill(Color.web("#fff"))
    val txt = new Text(msg)
    ctx.fillText(msg, realWindow.x * 0.5 - txt.getLayoutBounds.getWidth* 0.5,realWindow.y * 0.5)

    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }
  }


  def centerScale(ctx:GraphicsContext,rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    //视角缩放
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
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
    var DrawLeft = Width*0.35
    var DrawHeight = BaseHeight + Height * 0.1

    val congratulation =if(isVictory){
      "Good Game!  Congratulations to: You~"
    }else{
      "Good Game!  Congratulations to: "
    }
    val text = new Text(congratulation)
    ctx.fillText(congratulation, Width * 0.5 - text.getLayoutBounds.getWidth.toInt/2, BaseHeight)

    ctx.save()
    ctx.setFill(Color.YELLOW)
    val winner = s"${msg.name}"
    val winnerText = new Text(winner)
    ctx.fillText(winner, Width * 0.5 - winnerText.getLayoutBounds.getWidth/2, BaseHeight+Height *0.1 )
    ctx.restore()
    DrawHeight = BaseHeight + Height * 0.15
    ctx.setFont(Font.font("Comic Sans MS",Width *0.02))

    val Time = MTime2HMS (msg.totalFrame * GameConfig.frameRate)
    ctx.fillText(s"The   Winner  Score  :", DrawLeft, DrawHeight + Height*0.07)
    ctx.fillText(s"Your  Final   Score  :", DrawLeft, DrawHeight + Height*0.07*2)
    ctx.fillText(s"Game  Time   :", DrawLeft, DrawHeight + Height*0.07*3)
    //    ctx.fillText(s"Your  Kill   Num  :", DrawLeft, DrawHeight + Height*0.07*3)
    ctx.setFill(Color.WHITE)
    val winnerScore = new Text("The   Winner  Score  :")
    DrawLeft = winnerScore.getLayoutBounds.getWidth +  Width*0.35 + 60
    ctx.fillText(s"${msg.score}", DrawLeft,DrawHeight + Height*0.07)
    ctx.fillText(s"${VictoryMsg._2}", DrawLeft,DrawHeight + Height*0.07*2)
    ctx.fillText(s"${Time}", DrawLeft,DrawHeight + Height*0.07*3)

    val reStart = s"Press Space to Start a New Game ୧(●⊙(工)⊙●)୨ "
    val reStartText = new Text(reStart)
    ctx.fillText(reStart, Width * 0.5 - reStartText.getLayoutBounds.getWidth / 2,DrawHeight + Height*0.07*5)

    if(is2Byte){
      BotUtil.canvas2byteArray(canvas)
    }else{
      BotUtil.emptyArray
    }

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
}
