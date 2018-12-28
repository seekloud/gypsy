package com.neo.sk.gypsy.scene

import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.common.Constant.{ColorsSetting, informWidth, layeredCanvasHeight, layeredCanvasWidth}
import com.neo.sk.gypsy.holder.BotHolder._
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.util.utils.{MTime2HMS, getZoomRate, normalization}
import com.neo.sk.gypsy.utils.{BotUtil, FpsComp}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text, TextAlignment}
import com.neo.sk.gypsy.common.Constant._
import com.neo.sk.gypsy.shared.ptcl.Protocol.GridDataSync
import javafx.geometry.VPos

import scala.math.{pow, sqrt}

/**
  * Created by YXY on Date: 2018/12/18
  */

object LayeredDraw{
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

}



class LayeredDraw(uid :String,layeredScene: LayeredScene,grid: GridOnClient,is2Byte:Boolean) {
  import LayeredDraw._

  val ls = layeredScene
//  val data = grid.getGridData(uid,Window.w.toInt,Window.h.toInt)
  val data = grid.getGridData(uid,Window.w.toInt,Window.h.toInt)
  val screeScale = if( layeredCanvasWidth / Window.w > layeredCanvasHeight / Window.h) layeredCanvasHeight / Window.h else layeredCanvasWidth / Window.w
  var scale = data.scale * screeScale
//  val scale2 = getZoomRate(zoom._1,zoom._2,layeredCanvasWidth/2,layeredCanvasHeight/2) * screeScale

  val food = grid.food.map(f=>Food(f._2,f._1.x,f._1.y)).toList
  val virus = data.virusDetails.values.toList
  val mass = data.massDetails
  val player = data.playerDetails
  val MyBallOpt = player.find(_.id == uid)
  val X = if(MyBallOpt.isDefined){
    MyBallOpt.get.x
  }else{
    0d
  }
  val Y = if(MyBallOpt.isDefined){
    MyBallOpt.get.y
  }else{
    0d
  }

  val layeredOffX = layeredCanvasWidth/2 - X
  val layeredOffY = layeredCanvasHeight/2 - Y

  val ranks = grid.currentRank


  //TODO 可以考虑绘图和获取ByteString分开，但是目前测试阶段还是写到一起

  /*********************分层视图400*200****************************/

  /*******************1.视野在整个地图中的位置***********************/
  def drawLocation()={
//    ls.locationCanvasCtx
    ls.locationCanvasCtx.setFill(Color.BLACK)
    ls.locationCanvasCtx.fillRect(0, 0, layeredCanvasWidth, layeredCanvasHeight)
    //TODO 视野缩放
    data.playerDetails.foreach{player=>
      if(player.id == uid){
        ls.locationCanvasCtx.setFill(Color.GRAY)
//        ls.locationCanvasCtx.fillRect((player.x-600)/12,(player.y - 300)/8,100,75)
        ls.locationCanvasCtx.fillRect((player.x-Window.w/2)/(Boundary.w/layeredCanvasWidth),(player.y - Window.h/2)/(Boundary.h/layeredCanvasHeight),layeredCanvasWidth*(Window.w/Boundary.w),layeredCanvasHeight*(Window.h/Boundary.h))
      }
    }
    if(is2Byte){
      BotUtil.canvas2byteArray(ls.locationCanvas)
    }else{
      BotUtil.emptyArray
    }
  }

  /********************2.视野内不可交互的元素（地图背景元素）*********************/
  def drawNonInteract() = {
//    ls.nonInteractCanvasCtx
    val ctx = ls.nonInteractCanvasCtx
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, layeredCanvasWidth, layeredCanvasHeight)
    ctx.save()
    centerScale(ctx,scale,layeredCanvasWidth/2,layeredCanvasHeight/2)
    ctx.setFill(Color.BLACK)
    //TODO 偏移要看
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    ctx.restore()
    if(is2Byte){
      BotUtil.canvas2byteArray(ls.nonInteractCanvas)
    }else{
      BotUtil.emptyArray
    }
  }

  /********************3.视野内可交互的元素（food，mass，virus）是否包含边界****************/
  def drawInteract()={
    val ctx = ls.interactCanvasCtx
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, layeredCanvasWidth, layeredCanvasHeight)

    ctx.save()
    centerScale(ctx,scale,layeredCanvasWidth/2,layeredCanvasHeight/2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

//    val viewFood = food.filter()
    food.groupBy(_.color).foreach{a=>
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

    mass.groupBy(_.color).foreach{ a=>
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

    virus.foreach { case Virus(vid,x,y,mass,radius,_,tx,ty,speed) =>
      ctx.drawImage(img,x-radius+layeredOffX,y-radius+layeredOffY,radius,radius)
    }

    ctx.restore()

    if(is2Byte){
      BotUtil.canvas2byteArray(ls.interactCanvas)
    }else{
      BotUtil.emptyArray
    }

  }


  /********************4.视野内包括自己的所有玩家******************************/
  def drawAllPlayer() = {
    val ctx = ls.allPlayerCanvasCtx

    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, layeredCanvasWidth, layeredCanvasHeight)

    ctx.save()
    centerScale(ctx,scale,layeredCanvasWidth/2,layeredCanvasHeight/2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    player.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,_,killerName,width,height,cells,startTime) =>
      val circleColor = color.toInt % 7 match{
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
        var nameFont: Double = cell.radius * 2 / sqrt(4 + pow(name.length, 2))
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius else nameFont
        ctx.setFont(Font.font("Helvetica",nameFont))
        val txt3=new Text(name)
        val nameWidth = txt3.getLayoutBounds.getWidth
        ctx.setStroke(Color.web("grey"))
        ctx.strokeText(s"$name", x + layeredOffX - (nameWidth*nameFont/12.0) / 2, y + layeredOffY - (nameFont.toInt / 2 + 2))
        ctx.setFill(Color.web(MyColors.background))
        ctx.fillText(s"$name", x + layeredOffX - (nameWidth*nameFont/12.0) / 2, y + layeredOffY - (nameFont.toInt / 2 + 2))
        ctx.restore()
      }
    }

    ctx.restore()

    if(is2Byte){
      BotUtil.canvas2byteArray(ls.allPlayerCanvas)
    }else{
      BotUtil.emptyArray
    }

  }

  /*********************5.视野内的自己***************************************/
  def drawPlayer() = {
    val ctx = ls.playerCanvasCtx
    ctx.setFill(Color.GRAY)
    ctx.fillRect(0, 0, layeredCanvasWidth, layeredCanvasHeight)

    ctx.save()
    centerScale(ctx,scale,layeredCanvasWidth/2,layeredCanvasHeight/2)

    ctx.setFill(Color.BLACK)
    ctx.fillRect(layeredOffX, layeredOffY, bounds.x, bounds.y)

    if(MyBallOpt.isDefined){
      val my = MyBallOpt.get
      val name = my.name
      val color = my.color
      val x = my.x
      val y = my.y
      val protect = my.protect
      val cells = my.cells


      val circleColor = color.toInt % 7 match{
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
        var nameFont: Double = cell.radius * 2 / sqrt(4 + pow(name.length, 2))
        nameFont = if (nameFont < 15) 15 else if (nameFont / 2 > cell.radius) cell.radius else nameFont
        ctx.setFont(Font.font("Helvetica",nameFont))
        val txt3=new Text(name)
        val nameWidth = txt3.getLayoutBounds.getWidth
        ctx.setStroke(Color.web("grey"))
        ctx.strokeText(s"$name", x + layeredOffX - (nameWidth*nameFont/12.0) / 2, y + layeredOffY - (nameFont.toInt / 2 + 2))
        ctx.setFill(Color.web(MyColors.background))
        ctx.fillText(s"$name", x + layeredOffX - (nameWidth*nameFont/12.0) / 2, y + layeredOffY - (nameFont.toInt / 2 + 2))
        ctx.restore()
      }

      ctx.restore()
      if(is2Byte){
        BotUtil.canvas2byteArray(ls.playerCanvas)
      }else{
        BotUtil.emptyArray
      }

    }else{
      ctx.restore()
      BotUtil.emptyArray
    }

  }


  /*********************6.面板状态信息图层************************************/
  def drawInform() = {
    val ctx = ls.informCanvasCtx

    ctx.setFill(Color.BLACK)
    ctx.fillRect(0, 0, layeredCanvasWidth, layeredCanvasHeight)
    //自己放第一个
    if(ranks.nonEmpty){
      //面板中最大分值的数值的显示占比
      val infoScale = 4.0/3.0
      val maxScore = ranks.map(_.score.score).max * infoScale
      val maxKill = ranks.map(_.score.k).max * infoScale

      val myRank = ranks.filter(_.score.id == uid).head.score
      val myScore = myRank.score
      val myKill = myRank.k

      def drawScoreKill(score: Double,kill: Int, index:Int) = {
        //score
        ctx.setFill(ColorsSetting.scoreColor)
//        ctx.fillRect(index * 35, layeredCanvasHeight - (280 * score / maxScore).toInt, informWidth,
        ctx.fillRect(index * 35, 0, informWidth,
          (layeredCanvasHeight * score / maxScore).toInt)
        //kill
        if(maxKill > 0) {
          ctx.setFill(ColorsSetting.killColor)
//          ctx.fillRect(index * 35 + informWidth, layeredCanvasHeight - 280 * kill / maxKill, informWidth, 280 * kill / maxKill)
          ctx.fillRect(index * 35 + informWidth, 0, informWidth, 280 * kill / maxKill)
        }
      }
      drawScoreKill(myScore,myKill,0)

      val othersRank = ranks.filterNot(_.score.id == uid)
      //currentRanks 本身的长度就是不超过11的
      for(i<-0 until othersRank.length){
        val playerRank = othersRank(i).score
        drawScoreKill(playerRank.score, playerRank.k, i+1)
      }
    }


/*    val sortedPlayerLists = grid.playerMap.filterNot(_._1 == id).values.toList.sortBy(_.cells.map(_.newmass).sum)
    if(sortedPlayerLists.length <= 10){
      for(i<-0 until sortedPlayerLists.length){
        val player = sortedPlayerLists(i)
        drawScoreKill(player.cells.map(_.newmass).sum, player.kill, i+1)
      }
    } else {
      //选前十个
      val sortedPlayerListsT = sortedPlayerLists.take(10)
      for(i<-0 until sortedPlayerListsT.length){
        val player = sortedPlayerListsT(i)
        drawScoreKill(player.cells.map(_.newmass).sum, player.kill, i+1)
      }
    }*/

    if(is2Byte){
      BotUtil.canvas2byteArray(ls.informCanvas)
    }else{
      BotUtil.emptyArray
    }

  }


  /********************* 人类视图：800*400 ***********************************/

  def drawPlayState()={
    var zoom = (30.0, 30.0)
//    val data = grid.getGridData(myId,1200,600)
    val MyBallOpt = player.find(_.id == uid)
    if(MyBallOpt.isDefined){
      val myInfo = MyBallOpt.get
      val myRank = ranks.find(_.score.id == uid)
      val myScore = if(myRank.isDefined) myRank.get.score.score else 0
      val myKill = if(myRank.isDefined) myRank.get.score.k else 0
      firstCome = false
//      ls.humanView.drawLayeredBg()
//      ls.humanView.drawRankMapData(uid,ranks,data.playerDetails,(X,Y),data.playersPosition)
//      ls.humanView.drawGrid(uid,data,0l,(X,Y),(myInfo.width,myInfo.height),grid)
      drawPlayView(uid,data,(X,Y),(myInfo.width,myInfo.height),grid)

      ls.humanCtx.save()
      ls.humanCtx.setFont(Font.font(" Helvetica",24))
      ls.humanCtx.fillText(s"KILL: ${myKill}", 250, 10)
      ls.humanCtx.fillText(s"SCORE: ${myScore}", 400, 10)
      ls.humanCtx.restore()

    }else{
      ls.humanView.drawGameWait(firstCome)
    }
//    FpsComp.renderFps(gameCanvasCtx, 550, 10)
    if(is2Byte){
      BotUtil.canvas2byteArray(ls.humanCanvas)
    }else{
      BotUtil.emptyArray
    }

  }


  def drawPlayView(uid: String, data: GridDataSync,basePoint:(Double,Double),zoom:(Double,Double),grid: GridOnClient) = {
    val ctx = ls.humanCtx
    val players = data.playerDetails
    val foods =  grid.food.map(f=>Food(f._2,f._1.x,f._1.y)).toList
    val masses = data.massDetails
    val virus = data.virusDetails
    //计算偏移量
    val offx = humanCanvasWidth/2 - basePoint._1
    val offy = humanCanvasHeight/2 - basePoint._2
    val screenIndex = if( humanCanvasWidth / Window.w > humanCanvasHeight / Window.h) humanCanvasHeight / Window.h else humanCanvasWidth / Window.w
    val scale = getZoomRate(zoom._1,zoom._2,humanCanvasWidth,humanCanvasHeight) * screenIndex

    /**这两部分代码不能交换，否则视图会无限缩小or放大**/
    //01
    ctx.setFill(Color.web("rgba(181, 181, 181, 1)"))
    ctx.fillRect(0,0,humanCanvasWidth,humanCanvasHeight)
    ctx.save()
    //02
    centerScale(ctx,scale,humanCanvasWidth /2.0,humanCanvasHeight/2.0)

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
    players.sortBy(_.cells.map(_.mass).sum).foreach { case Player(id, name,color,x,y,tx,ty,kill,protect,lastSplit,killerName,width,height,cells,startTime) =>
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
        var playermass=cell.mass.toInt
        val txt3=new Text(name)
        val txt4=new Text(playermass.toString)
        val nameWidth = txt3.getLayoutBounds.getWidth
        val massWidth = txt4.getLayoutBounds.getWidth
        ctx.setStroke(Color.web("grey"))
        ctx.strokeText(s"$name", cell.x + offx - nameWidth / 2, cell.y + offy - (nameFont.toInt / 2))
        ctx.setFill(Color.web(MyColors.background))
        ctx.fillText(s"${playermass.toString}",cell.x + offx - massWidth / 2, cell.y + offy + nameFont.toInt/2)
        ctx.fillText(s"$name", cell.x + offx- nameWidth / 2, cell.y + offy - (nameFont.toInt / 2))
        ctx.restore()
        /**膨胀、缩小效果**/
        var newcell = cell
        //        println(cell.mass +"  "+cell.newmass)
        if(cell.mass != cell.newmass){
          //根据差值来膨胀或缩小
          cellDifference = true
          val massSpeed = if(cell.mass < cell.newmass) 1 else -1
          newcell = cell.copy(mass = cell.mass + massSpeed, radius = 4 + sqrt(cell.mass + massSpeed) * 6)
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
        val player = Player(id,name,color,newX,newY,tx,ty,kill,protect,lastSplit,killerName,right - left,top - bottom,newcells,startTime)
        grid.playerMap += (id -> player)
      }
    }
    virus.values.foreach { case Virus(vid,x,y,mass,radius,_,tx,ty,speed) =>
      ctx.save()

      ctx.drawImage(img,x-radius+ offx,y-radius+ offy,radius*2,radius*2)
      ctx.restore()
    }

    ctx.restore()
    ctx.setFill(Color.web("rgba(99, 99, 99, 1)"))
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setTextBaseline(VPos.TOP)
  }


  def drawDeadState(msg:Protocol.UserDeadMessage) = {
    val ctx = ls.humanCtx

    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0, 0, humanCanvasWidth , humanCanvasHeight )
//    ctx.drawImage(deadbg,0,0, realWindow.x, realWindow.y)
    ctx.setFont(Font.font("Helvetica",50))
    ctx.setFill(Color.web("#CD3700"))
    val Width = humanCanvasWidth
    val Height = humanCanvasHeight
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

    if(is2Byte){
      BotUtil.canvas2byteArray(ls.humanCanvas)
    }else{
      BotUtil.emptyArray
    }
  }


  def drawFinishState() = {
    val ctx = ls.humanCtx
    val msg = "存在异地登录"

    ctx.setFill(Color.web("#000"))
    ctx.fillRect(0,0,humanCanvasWidth,humanCanvasHeight)
    //这里1600是窗口的长以后换
    ctx.setFont(Font.font("Helvetica",30 * humanCanvasWidth / 1600))
    ctx.setFill(Color.web("#fff"))
    val txt = new Text(msg)
    ctx.fillText(msg, humanCanvasWidth * 0.5 - txt.getLayoutBounds.getWidth* 0.5,humanCanvasHeight * 0.5)

    if(is2Byte){
      BotUtil.canvas2byteArray(ls.humanCanvas)
    }else{
      BotUtil.emptyArray
    }
  }


  def drawViewByState() ={
    gameState match {
      case GameState.play =>
        drawPlayState()
      case GameState.dead =>
        drawDeadState(deadInfo.get)
      case GameState.allopatry =>
        drawFinishState()
      case _ =>
        BotUtil.emptyArray
    }
  }

  def drawLayered():Unit={
    drawLocation()
    drawNonInteract()
    drawInteract()
    drawAllPlayer()
    drawPlayer()
    drawInform()
    drawViewByState()
  }

  def centerScale(ctx:GraphicsContext,rate:Double,x:Double,y:Double) = {
    ctx.translate(x,y)
    //视角缩放
    ctx.scale(rate,rate)
    ctx.translate(-x,-y)
  }
}
