package com.neo.sk.gypsy.shared

import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer
import com.neo.sk.gypsy.shared.ptcl.Protocol.{UserAction, KeyCode, MousePosition}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.util.utils._
import com.neo.sk.gypsy.shared.util.utils
import scala.collection.mutable
import scala.math._
import scala.util.Random

import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.ptcl.Game._


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  var myId = ""
  val random = new Random(System.nanoTime())

  val cellIdgenerator = new AtomicInteger(1000000)

  var frameCount = 0
  //食物列表
  var food = Map[Point, Short]()
//  //病毒列表
//  var virus = List[Virus]()
  //病毒map (ID->Virus)
  var virusMap = Map.empty[Long,Virus]
  //玩家列表
  var playerMap = Map.empty[String,Player]
  //喷出小球列表
  var massList = List[Mass]()
  //衰减周期计数
  var tick = 0
  //操作列表  帧数->(用户ID->操作)
  var actionMap = Map.empty[Int, Map[String, KeyCode]]

  var mouseActionMap = Map.empty[Int, Map[String, MousePosition]]

  val ActionEventMap = mutable.HashMap[Int,List[GameEvent]]() //frame -> List[GameEvent]

  val GameEventMap = mutable.HashMap[Int,List[GameEvent]]() //frame -> List[GameEvent]

  val bigPlayerMass = 500.0

  var deadPlayerMap=Map.empty[Long,Player]

  //统计分数
  var Compress_times = 1
  var ScoreList = List.empty[Double]
  var tempScoreList = ListBuffer.empty[Int]
  var Scale=1.0

  //  var quad = new Quadtree(0, new Rectangle(0,0,boundary.x,boundary.y))

  //用户离开，从列表中去掉
  def removePlayer(id: String): Option[Player] = {
    val r = playerMap.get(id)
    if (r.isDefined) {
      playerMap -= id
    }
    r
  }

  //这里用不到id！！！
  //键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: String, keyCode: KeyCode) = {
    val map = actionMap.getOrElse(keyCode.frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (keyCode.frame -> tmp)
    val action = KeyPress(id,keyCode.keyCode,keyCode.frame,keyCode.serialNum)
    AddActionEvent(action)
  }

  def addMouseActionWithFrame(id: String, mp:MousePosition) = {
    val map = mouseActionMap.getOrElse(mp.frame, Map.empty)
    val tmp = map + (id -> mp)
    mouseActionMap += (mp.frame -> tmp)
    val direct = (mp.clientX,mp.clientY)
    val action = MouseMove(id,direct,mp.frame,mp.serialNum)
    AddActionEvent(action)
  }

  def removeActionWithFrame(id: String, userAction: UserAction, frame: Int) = {
    userAction match {
      case k:KeyCode=>
        val map = actionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && k.serialNum == t._2.serialNum)
        actionMap += (frame->actionQueue)
      case m:MousePosition=>
        val map = mouseActionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && m.serialNum == t._2.serialNum)
        mouseActionMap += (frame->actionQueue)
    }
  }


  def AddActionEvent(action: GameEvent):Unit ={
    ActionEventMap.get(action.frame) match {
      case Some(actionEvents) => ActionEventMap.put(action.frame,action :: actionEvents)
      case None => ActionEventMap.put(action.frame,List(action))
    }
  }

  def AddGameEvent(event:GameEvent):Unit ={
    GameEventMap.get(event.frame) match {
      case Some(gameEvents) => GameEventMap.put(event.frame,event :: gameEvents)
      case None => GameEventMap.put(event.frame,List(event))
    }
  }



  def update() = {
    updateSpots()
    updatePlayer()
//    updateScoreList()
    actionMap -= frameCount
    mouseActionMap -= frameCount
    ActionEventMap -= (frameCount-5)
    GameEventMap -= (frameCount-5)
    frameCount += 1
  }

  //统计分数跟新
  def updateScoreList() = {
    if(playerMap.get(myId).isDefined){
      val myInfo = playerMap(myId)
      var myScore = 0
      myInfo.cells.foreach{c=>
        myScore += c.newmass.toInt
      }
      tempScoreList += myScore
      if(tempScoreList.length > Compress_times){
        var TotalScore = tempScoreList.sum.toDouble / Compress_times
        tempScoreList.clear()
        ScoreList ::= TotalScore
      }

      if(ScoreList.length > GameConfig.ScoreListMax){
        var resultScore = List.empty[Double]
        val evenScore = ScoreList.zipWithIndex.filter(_._2 %2 == 0)
        val oddScore = ScoreList.zipWithIndex.filter(_._2 %2 == 1)
        val a = Array.empty[Int]
        resultScore = evenScore.map{temp =>
          val index = temp._2
          val EScore = temp._1
          //这里用index来获取感觉有点危险
          val OScore = oddScore(index)._2
          (EScore+OScore)/2
        }
        ScoreList = resultScore
        Compress_times *=2

//        var tempList = ScoreList match {
//          case a :: tail => (a+tail.head)/2 :: tail
//        }
      }

    }
  }

  //食物更新
  private[this] def updateSpots() = {
    //更新喷出小球的位置
    updateMass()
    //更新病毒位置
    updateVirus()
    feedApple(foodPool + playerMap.size * 3 - food.size)//增添食物（后端增添，前端不添）
    addVirus(virusNum - virusMap.size) //增添病毒(后端增添，前端不添）
  }
  //玩家更新
  private[this] def updatePlayer()={

    def updatePlayerMap(player: Player, mouseActMap: Map[String, MousePosition],decrease:Boolean)={
      if(decrease){
        updatePlayerMove(massDecrease(player),mouseActMap)
      }else{
        updatePlayerMove(player,mouseActMap)
      }
    }
    //TODO 确认下是不是frameCount

    val mouseAct = mouseActionMap.getOrElse(frameCount, Map.empty[String, MousePosition])
    val keyAct = actionMap.getOrElse(frameCount, Map.empty[String, KeyCode])
    tick = tick+1

    //先移动到指定位置，同时进行质量衰减
    playerMap = if(tick%10==1){
      tick = 1
      playerMap.values.map(updatePlayerMap(_,mouseAct,true)).map(s=>(s.id,s)).toMap
    }else{
      playerMap.values.map(updatePlayerMap(_,mouseAct,false)).map(s=>(s.id,s)).toMap
    }
    //碰撞检测
    checkCrash(keyAct,mouseAct)
  }
    //碰撞检测
  def checkCrash(keyAct: Map[String,KeyCode], mouseAct: Map[String, MousePosition])={
    checkPlayerFoodCrash() //已看 前后端都有
    checkPlayerMassCrash()  //已看  前后端都有
    checkPlayer2PlayerCrash() //已看  只后台
    checkVirusMassCrash()  //已看  前后端都有  但后台跟前端不同
    val mergeInFlame=checkCellMerge() //已看  前后端都有  同时后台还发送数据避免前后端不一致
    checkPlayerVirusCrash(mergeInFlame) //已看 只后台
    checkPlayerShotMass(keyAct,mouseAct)
    checkPlayerSplit(keyAct,mouseAct)
  }

  //更新病毒的位置
  def updateVirus() :Unit ={
    val NewVirus = virusMap.map{ vi=>
      val v =vi._2
      val (nx,ny)= normalization(v.targetX,v.targetY)
      var newX = v.x
      var newY = v.y
      var newSpeed = v.speed
      if(v.speed!=0){
        newX = (v.x + (nx*v.speed)).toShort
        newY = (v.y + (ny*v.speed)).toShort
        newSpeed = if(v.speed-virusSpeedDecayRate<0) 0f else (v.speed-virusSpeedDecayRate).toFloat
        val newPoint =ExamBoundary(newX,newY)
        newX = newPoint._1
        newY = newPoint._2
      }
      vi._1 -> v.copy(x = newX,y=newY,speed = newSpeed)
    }
    virusMap ++= NewVirus
  }
  //更新喷出小球的位置
  def updateMass():Unit = {
    massList = massList.map { mass =>
      val deg = Math.atan2(mass.targetY, mass.targetX)
      val deltaY = mass.speed * Math.sin(deg)
      val deltaX = mass.speed * Math.cos(deg)

      var newSpeed = mass.speed
      var newX = mass.x
      var newY = mass.y
      newSpeed -= massSpeedDecayRate
      if (newSpeed < 0) newSpeed = 0
      if (!(deltaY).isNaN) newY = (newY + deltaY).toShort
      if (!(deltaX).isNaN) newX = (newX + deltaX).toShort

      // val borderCalc = mass.radius.ceil.toInt

      val borderCalc = 0
      if (newX > boundary.x - borderCalc) newX = (boundary.x - borderCalc).toShort
      if (newY > boundary.y - borderCalc) newY = (boundary.y - borderCalc).toShort
      if (newX < borderCalc) newX = borderCalc.toShort
      if (newY < borderCalc) newY = borderCalc.toShort

      mass.copy(x = newX, y = newY, speed = newSpeed)
    }
  }
  //具体函数定义在GridOnServer
  def feedApple(appleCount: Int): Unit
  def addVirus(virus: Int): Unit

//边界超越校验
  def ExamBoundary(newX:Short,newY:Short)={
    val x = if(newX>boundary.x){
      boundary.x
    } else if(newX<0){
      0
    }else{
      newX
    }
    val y = if(newY>boundary.y){
      boundary.y
    } else if(newY<0){
      0
    }else{
      newY
    }

    (x.toShort ,y.toShort )
  }

  private[this] def updatePlayerMove(player: Player, mouseActMap: Map[String, MousePosition]) = {
    val mouseAct = mouseActMap.getOrElse(player.id,MousePosition(Some(player.id),player.targetX, player.targetY,0,0))
    //对每个cell计算新的方向、速度和位置
    val newCells = player.cells.sortBy(_.radius).reverse.flatMap { cell =>
      var newSpeed = cell.speed
      var target=Position(player.targetX,player.targetY)

      //转换成极坐标
      val deg1 = atan2(player.targetY + player.y - cell.y, player.targetX + player.x - cell.x)
      val degX1 = if (cos(deg1).isNaN) 0 else cos(deg1)
      val degY1 = if (sin(deg1).isNaN) 0 else sin(deg1)
      //速度*方向==向某个方向移动的距离
//      println(s"moveX:${newSpeed*degX1},moveY:${newSpeed*degY1}")
      val move = Point((newSpeed * degX1).toInt, (newSpeed * degY1).toInt)

      target = if(!cell.parallel) Position( (mouseAct.clientX + player.x - cell.x).toShort , (mouseAct.clientY + player.y - cell.y).toShort  ) else Position(mouseAct.clientX , mouseAct.clientY)

      val distance = sqrt(pow(target.clientX, 2) + pow(target.clientY, 2))
      val deg = atan2(target.clientY, target.clientX)
      val degX = if (cos(deg).isNaN) 0 else cos(deg)
      val degY = if (sin(deg).isNaN) 0 else sin(deg)
      val slowdown = utils.logSlowDown(cell.newmass, slowBase) - initMassLog + 1
//      println(s"slowdown:$slowdown")
      //指针在圆内，静止
      if (distance < sqrt(pow((newSpeed * degX).toInt, 2) + pow((newSpeed * degY).toInt, 2))) {
        newSpeed = (target.clientX / degX).toFloat
      } else {
        if (cell.speed > 30 / slowdown) {
          newSpeed -= acceleration
//          newSpeed = 30 / slowdown
        } else {
          if (distance < cell.radius) {
            if (cell.speed > 0) {
              newSpeed = cell.speed - acceleration
            } else newSpeed = 0
          } else {
            newSpeed = if (cell.speed < 30 / slowdown) {
              cell.speed + acceleration
            } else (30 / slowdown).toFloat
          }
        }
      }


      //cell移动+边界检测
      var newX = if ((cell.x + move.x) > boundary.x-15)  boundary.x-15 else if ((cell.x + move.x) <= 15) 15 else cell.x + move.x
      var newY = if ((cell.y + move.y) > boundary.y-15) boundary.y-15 else if ((cell.y + move.y) <= 15) 15 else cell.y + move.y

      var isCorner= false
      var isParallel =false
      if((newX<=15&&newY<=15)||
        (newX>=boundary.x-15&&newY<=15)||
        (newX<=15&&newY>=boundary.y-15)||
        (newX>=boundary.x-15&&newY>=boundary.y-15)){
        isCorner=true
      }
      //遍历计算每个cell的新速度
      player.cells.filterNot(p => p == cell).sortBy(_.isCorner).reverse.foreach { cell2 =>
        val distance = sqrt(pow(newY - cell2.y, 2) + pow(newX - cell2.x, 2))
        val deg= acos(abs(newX-cell2.x)/distance)
        val radiusTotal = cell.radius + cell2.radius+2
        if (distance < radiusTotal) {
          if (player.lastSplit > System.currentTimeMillis() - mergeInterval&&System.currentTimeMillis()-player.lastSplit>1000) {
            val mouseX=mouseAct.clientX+player.x
            val mouseY=mouseAct.clientY+player.y
            val cos1=((cell2.x-cell.x)*(mouseX-cell.x)+(cell2.y-cell.y)*(mouseY-cell.y))/sqrt((pow(newY - cell2.y, 2) + pow(newX - cell2.x, 2))*(pow(newY - mouseY, 2) + pow(newX - mouseX, 2)))
            val cos2=((cell.x-cell2.x)*(mouseX-cell2.x)+(cell.y-cell2.y)*(mouseY-cell2.y))/sqrt((pow(newY - cell2.y, 2) + pow(newX - cell2.x, 2))*(pow(cell2.y - mouseY, 2) + pow(cell2.x - mouseX, 2)))
            val cos3=((cell.x-mouseX)*(cell2.x-mouseX)+(cell.y-mouseY)*(cell2.y-mouseY))/sqrt((pow(newY - mouseY, 2) + pow(newX - mouseX, 2))*(pow(cell2.y - mouseY, 2) + pow(cell2.x - mouseX, 2)))
            if(cos1<=0){
              if(newX==15&&newY==15){}
              else if(newX==15&&newY==boundary.y-15){}
              else if(newX==boundary.x-15&&newY==15){}
              else if(newX==boundary.x-15&&newY==boundary.y-15){}
              else{
                newSpeed+=2
              }
            }else if(cos2<=0){
              if(!cell2.isCorner){
                if(newSpeed>cell2.speed){
                  newSpeed=if(cell2.speed-2>=0)cell2.speed-2 else 0
                }
                if((cell2.x<=15&&cell2.y<=15)||
                  (cell2.x>=boundary.x-15&&cell2.y<=15)||
                  (cell2.x<=15&&cell2.y>=boundary.y-15)||
                  (cell2.x>=boundary.x-15&&cell2.y>=boundary.y-15)){
                  newX=cell.x
                  newY=cell.y
                }
              }else{
                isCorner=true
                newSpeed = 0
                newX=cell.x
                newY=cell.y
              }
            }else if(cos3<=0){
              newSpeed=0
            }else{
              if (cell.x < cell2.x) newX -= ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
              else if (cell.x > cell2.x) newX += ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
              if (cell.y < cell2.y) newY -= ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
              else if (cell.y > cell2.y) newY += ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
              isParallel=true
            }
          }
        }
      }
      List(Cell(cell.id, newX.toShort , newY.toShort , cell.mass, cell.newmass, cell.radius, newSpeed, (newSpeed * degX).toFloat, (newSpeed * degY).toFloat,isParallel,isCorner))
    }
    val length = newCells.length
    val newX = newCells.map(_.x).sum / length
    val newY = newCells.map(_.y).sum / length
    val left = newCells.map(a => a.x - a.radius).min
    val right = newCells.map(a => a.x + a.radius).max
    val bottom = newCells.map(a => a.y - a.radius).min
    val top = newCells.map(a => a.y + a.radius).max
    player.copy(x = newX.toShort , y = newY.toShort , targetX = mouseAct.clientX , targetY = mouseAct.clientY , protect = player.protect, kill = player.kill, lastSplit = player.lastSplit, width = right - left, height = top - bottom, cells = newCells)
  }

  //TODO 前后
  //食物检测
  def checkPlayerFoodCrash(): Unit
  //TODO 前后
  //mass检测(前后端一样)
  def checkPlayerMassCrash(): Unit
  //TODO 前后
  //mass检测
  def checkVirusMassCrash(): Unit
  //TODO 只后台！！
  //与用户检测
  def checkPlayer2PlayerCrash(): Unit

  //TODO 前端做排斥后台判断
  //返回在这一帧是否融合过
  def checkCellMerge(): Boolean

  //TODO 前端后台
  //病毒碰撞检测
  def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit

  //TODO 前后
  //发射小球
  def checkPlayerShotMass(actMap: Map[String, KeyCode], mouseActMap: Map[String, MousePosition]): Unit = {
    //TODO 这里写下有哪些是分裂的
    val newPlayerMap = playerMap.values.map {
      player =>
        val mouseAct = mouseActMap.getOrElse(player.id, MousePosition(Some(player.id),player.targetX.toShort, player.targetY.toShort,0,0))
        val shot = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.keyCode==KeyEvent.VK_E
          case _ => false
        }
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            val target = Position( (mouseAct.clientX + player.x - cell.x).toShort , (mouseAct.clientY + player.y - cell.y).toShort )
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            var newMassList =  List.empty[Mass]
            if (shot && newMass > shotMass * 3) {
              newMass = (newMass - shotMass).toShort
              newRadius = Mass2Radius(newMass)
              val massRadius = Mass2Radius(shotMass)
              val massX = (cell.x + (newRadius - 50) * degX).toInt
              val massY = (cell.y + (newRadius - 50) * degY).toInt
//              massList ::= game.Mass(massX, massY, player.targetX, player.targetY, player.color.toInt, shotMass, massRadius, shotSpeed)
              newMassList ::= Game.Mass(massX.toShort , massY.toShort , player.targetX, player.targetY, player.color, shotMass, massRadius, shotSpeed)
            }
            massList :::=newMassList
//            println(cell.mass + "    " + newMass)
            Cell(cell.id, cell.x, cell.y, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }.filterNot(e=>e.newmass <= 0 && e.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX.toShort , y = newY.toShort , width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  //TODO 暂时前后
  //分裂检测
  def checkPlayerSplit(actMap: Map[String,KeyCode], mouseActMap: Map[String, MousePosition]): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newSplitTime = player.lastSplit
        val mouseAct = mouseActMap.getOrElse(player.id,MousePosition(Some(player.id),player.targetX, player.targetY,0,0))
        val split = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.keyCode==KeyEvent.VK_F
          case _ => false
        }
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            val target = Position( (mouseAct.clientX + player.x - cell.x).toShort , (mouseAct.clientY + player.y - cell.y).toShort )
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            var splitX = 0
            var splitY = 0
            var splitMass:Short = 0
            var splitRadius:Short = 0
            var splitSpeed = 0.0
            var cellId = 0L
            if (split && cell.newmass > splitLimit && player.cells.size < maxCellNum) {
              newSplitTime = System.currentTimeMillis()
              splitMass = (newMass / 2).toShort
              newMass = (newMass- splitMass).toShort
              splitRadius = Mass2Radius(splitMass)
              newRadius = Mass2Radius(newMass)
              splitSpeed = splitBaseSpeed + 2 * cbrt(cell.radius)
              splitX = (cell.x + (newRadius + splitRadius) * degX).toInt
              splitY = (cell.y + (newRadius + splitRadius) * degY).toInt
              cellId = cellIdgenerator.getAndIncrement().toLong
            }
            /**效果：大球：缩小，小球：从0碰撞，且从大球中滑出**/
//            println(cell.mass + "   " + newMass)
            List(Cell(cell.id, cell.x, cell.y, newMass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner),
                Cell(cellId,  cell.x, cell.y, splitMass, splitMass, splitRadius, splitSpeed.toFloat, (splitSpeed * degX).toFloat, (splitSpeed * degY).toFloat))
        }.filterNot(e=> e.newmass <= 0 && e.mass <=0 )
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX.toShort , y = newY.toShort , lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }


  def massDecrease(player:Player)={
    val newCells=player.cells.map{cell=>
      var newMass = cell.newmass
      if(cell.newmass > decreaseLimit)
        newMass = (cell.newmass * decreaseRate).toShort
      cell.copy(mass = newMass,newmass = newMass)
    }
    player.copy(cells = newCells)
  }

  /**
    * method: getGridData
    * describe: 获取自己视角中的全量数据
    */
  def getGridData(id:String,winWidth:Int,winHeight:Int) = {
    myId = id
    val currentPlayer = playerMap.get(id).map(a=>(a.x,a.y)).getOrElse(((winWidth/2).toShort,(winHeight/2).toShort ))
    val zoom = playerMap.get(id).map(a=>(a.width,a.height)).getOrElse((30.0,30.0))
//    val scale = getZoomRate(zoom._1,zoom._2,winWidth,winHeight)
//    val width = winWidth / scale / 2
//    val height = winHeight / scale / 2
    if(getZoomRate(zoom._1,zoom._2,winWidth,winHeight)!=1){
      Scale = getZoomRate(zoom._1,zoom._2,winWidth,winHeight)
    }
    val width = winWidth / Scale / 2
    val height = winHeight / Scale / 2

    //val allPlayerPosition = playerMap.values.toList.filter(i=>i.cells.map(_.newmass).sum>bigPlayerMass).map(i=>PlayerPosition(i.id,i.x,i.y,i.targetX,i.targetY))
    var playerDetails: List[Player] = Nil

    playerMap.foreach{
      case (id,player) =>
        if (checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(player.x,player.y),sqrt(pow(player.width/2,2.0)+pow(player.height/2,2.0)),width,height))
        playerDetails ::= player
    }

    Protocol.GridDataSync(
      frameCount,
      playerDetails,
      massList.filter(m=>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m.x,m.y),m.radius,width,height)),
      virusMap.filter(m =>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m._2.x,m._2.y),m._2.radius,width,height)),
//      scale,
      Scale,
     // allPlayerPosition
    )
  }

  def getAllGridData: Protocol.GridDataSync

  def getActionEventMap(frame:Int):List[GameEvent]

  def getGameEventMap(frame:Int):List[GameEvent]

}
