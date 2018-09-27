package com.neo.sk.gypsy.shared

import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.shared.ptcl.{Point, WsMsgProtocol}
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{GameAction, KeyCode, MousePosition}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.util.utils._
import com.neo.sk.gypsy.shared.util.utils

import scala.math._
import scala.util.Random


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  * 背景网格
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  var myId = 0L
  val random = new Random(System.nanoTime())

  val cellIdgenerator = new AtomicInteger(1000000)

  val historyRankLength = 5

  val slowBase = 10
  val initMassLog = utils.logSlowDown(10,slowBase)
  val acceleration  = 2
//质量转半径率
  val mass2rRate = 6
  //吞噬覆盖率  (-1,1) 刚接触->完全覆盖
  val coverRate = 0
  var frameCount = 0l
//合并时间间隔
  val mergeInterval = 18 * 1000
  //分裂时间间隔
  val splitInterval = 2 * 1000
//最小分裂大小
  val splitLimit = 30
  //分裂初始速度
  val splitBaseSpeed = 40
  //食物质量
  val foodMass = 1
  //食物列表
  var food = Map[Point, Int]()
  //食物池
  var foodPool = 300
  //病毒列表
  var virus = List[Virus]()
  //病毒数量
  var virusNum:Int = 8
  //病毒质量上限
  var virusMassLimit:Int = 200
  //玩家列表
  var playerMap = Map.empty[Long,Player]
  //喷出小球列表
  var massList = List[Mass]()
  val shotMass = 10
  val shotSpeed = 100
  //最大分裂个数
  val maxCellNum = 16
  //质量衰减下限
  val decreaseLimit = 200
  //衰减率
  val decreaseRate = 0.995
  //衰减周期计数
  var tick = 0
  //操作列表  帧数->(用户ID->操作)
  var actionMap = Map.empty[Long, Map[Long, KeyCode]]

  var mouseActionMap = Map.empty[Long, Map[Long, MousePosition]]

  var deadPlayerMap=Map.empty[Long,Player]

//  var quad = new Quadtree(0, new Rectangle(0,0,boundary.x,boundary.y))

//用户离开，从列表中去掉
  def removePlayer(id: Long): Option[Player] = {
    val r = playerMap.get(id)
    if (r.isDefined) {
      playerMap -= id
    }
    r
  }

//键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: Long, keyCode: KeyCode) = {
    val map = actionMap.getOrElse(keyCode.frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (keyCode.frame -> tmp)
  }

  def addMouseActionWithFrame(id: Long, mp:MousePosition) = {
    val map = mouseActionMap.getOrElse(mp.frame, Map.empty)
    val tmp = map + (id -> mp)
    mouseActionMap += (mp.frame -> tmp)
  }

  def removeActionWithFrame(id: Long, gameAction: GameAction, frame: Long) = {
    gameAction match {
      case k:KeyCode=>
        val map = actionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && gameAction.serialNum == t._2.serialNum)
        actionMap += (frame->actionQueue)
      case m:MousePosition=>
        val map = mouseActionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && gameAction.serialNum == t._2.serialNum)
        mouseActionMap += (frame->actionQueue)
    }
  }
//
  def update() = {
    updatePlayer()
    updateSpots()
    actionMap -= frameCount
    frameCount += 1
  }

  def feedApple(appleCount: Int): Unit
  def addVirus(virus: Int): Unit
//食物更新
  private[this] def updateSpots() = {
  //更新喷出小球的位置
  massList = massList.map { mass =>
    val deg = Math.atan2(mass.targetY, mass.targetX)
    val deltaY = mass.speed * Math.sin(deg)
    val deltaX = mass.speed * Math.cos(deg)

    var newSpeed = mass.speed
    var newX = mass.x
    var newY = mass.y
    newSpeed -= 25
    if (newSpeed < 0) newSpeed = 0
    if (!(deltaY).isNaN) newY += deltaY.toInt
    if (!(deltaX).isNaN) newX += deltaX.toInt

   // val borderCalc = mass.radius.ceil.toInt

    val borderCalc = 0
    if (newX > boundary.x - borderCalc) newX = boundary.x - borderCalc
    if (newY > boundary.y - borderCalc) newY = boundary.y - borderCalc
    if (newX < borderCalc) newX = borderCalc
    if (newY < borderCalc) newY = borderCalc

    mass.copy(x = newX, y = newY, speed = newSpeed)
  }
  feedApple(foodPool + playerMap.size * 3 - food.size)
  addVirus(virusNum - virus.size)
}

  private[this] def updatePlayerMove(player: Player, mouseActMap: Map[Long, MousePosition]) = {
    val mouseAct = mouseActMap.getOrElse(player.id,MousePosition(player.id,player.targetX, player.targetY,0l,0))
    //对每个cell计算新的方向、速度和位置
    val newCells = player.cells.sortBy(_.radius).reverse.flatMap { cell =>
      var newSpeed = cell.speed
      var target=Position(player.targetX,player.targetY)

      val deg1 = atan2(player.targetY + player.y - cell.y, player.targetX + player.x - cell.x)
     // val deg1 = atan2(player.targetY , player.targetX )
      val degX1 = if (cos(deg1).isNaN) 0 else cos(deg1)
      val degY1 = if (sin(deg1).isNaN) 0 else sin(deg1)
      val move = Point((newSpeed * degX1).toInt, (newSpeed * degY1).toInt)

      target = if(!cell.parallel) Position(mouseAct.clientX + player.x - cell.x, mouseAct.clientY + player.y - cell.y) else Position(mouseAct.clientX , mouseAct.clientY )

      val distance = sqrt(pow(target.clientX, 2) + pow(target.clientY, 2))
      val deg = atan2(target.clientY, target.clientX)
      val degX = if (cos(deg).isNaN) 0 else cos(deg)
      val degY = if (sin(deg).isNaN) 0 else sin(deg)
      val slowdown = utils.logSlowDown(cell.mass, slowBase) - initMassLog + 1
      //指针在圆内，静止
      if (distance < sqrt(pow((newSpeed * degX).toInt, 2) + pow((newSpeed * degY).toInt, 2))) {
        newSpeed = target.clientX / degX
      } else {
        if (cell.speed > 30 / slowdown) {
          newSpeed -= 2
        } else {
          if (distance < cell.radius) {
            //println("在圆内")
            if (cell.speed > 0) {
              //println("come here")
              newSpeed = cell.speed - acceleration
            } else newSpeed = 0
            //println(s"new speed ${newSpeed} ,star.speed -slowDown${cell.speed - slowDown},slowDown${slowDown}")
          } else {
            newSpeed = if (cell.speed < 30 / slowdown) {
              cell.speed + acceleration
            } else 30 / slowdown
          }
        }
      }

      //cell移动+边界检测
      var newX = if ((cell.x + move.x) > boundary.x-15) boundary.x-15 else if ((cell.x + move.x) <= 15) 15 else cell.x + move.x
      var newY = if ((cell.y + move.y) > boundary.y-15) boundary.y-15 else if ((cell.y + move.y) <= 15) 15 else cell.y + move.y

      var isParallel =false
      player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
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
              newSpeed+=2
            }else if(cos2<=0){
              if(newSpeed>cell2.speed){
                newSpeed=if(cell2.speed-2>=0)cell2.speed else 0
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

      List(Cell(cell.id, newX, newY, cell.mass, cell.radius, newSpeed, (newSpeed * degX).toFloat, (newSpeed * degY).toFloat,isParallel))
    }
    val length = newCells.length
    val newX = newCells.map(_.x).sum / length
    val newY = newCells.map(_.y).sum / length
    val left = newCells.map(a => a.x - a.radius).min
    val right = newCells.map(a => a.x + a.radius).max
    val bottom = newCells.map(a => a.y - a.radius).min
    val top = newCells.map(a => a.y + a.radius).max

    player.copy(x = newX, y = newY, targetX = mouseAct.clientX.toInt, targetY = mouseAct.clientY.toInt, protect = player.protect, kill = player.kill, lastSplit = player.lastSplit, width = right - left, height = top - bottom, cells = newCells)

  }

  //食物检测
  def checkPlayerFoodCrash(): Unit
  //mass检测
  def checkPlayerMassCrash(): Unit
  //mass检测
  def checkVirusMassCrash(): Unit
  //与用户检测
  def checkPlayer2PlayerCrash(): Unit

  //返回在这一帧是否融合过
  def checkCellMerge(): Boolean

  //病毒碰撞检测
  def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit

  //发射小球
  def checkPlayerShotMass(actMap: Map[Long, KeyCode], mouseActMap: Map[Long, MousePosition]): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        val mouseAct = mouseActMap.getOrElse(player.id, MousePosition(player.id,player.targetX, player.targetY,0l,0))
        val shot = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.keyCode==KeyEvent.VK_E
          case _ => false
        }
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            val target = Position(mouseAct.clientX + player.x - cell.x, mouseAct.clientY + player.y - cell.y)
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            if (shot && newMass > shotMass * 3) {
              newMass -= shotMass
              newRadius = 4 + sqrt(newMass) * 6
              val massRadius = 4 + sqrt(shotMass) * 6
              val massX = (cell.x + (newRadius - 50) * degX).toInt
              val massY = (cell.y + (newRadius - 50) * degY).toInt
              massList ::= ptcl.Mass(massX, massY, player.targetX, player.targetY, player.color.toInt, shotMass, massRadius, shotSpeed)
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)
        }.filterNot(_.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap

  }

  //分裂检测
  def checkPlayerSplit(actMap: Map[Long,KeyCode], mouseActMap: Map[Long, MousePosition]): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newSplitTime = player.lastSplit
        val mouseAct = mouseActMap.getOrElse(player.id,MousePosition(player.id,player.targetX, player.targetY,0l,0))
        val split = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.keyCode==KeyEvent.VK_F
          case _ => false
        }
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            val target = Position(mouseAct.clientX + player.x - cell.x, mouseAct.clientY + player.y - cell.y)
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            var splitX = 0
            var splitY = 0
            var splitMass = 0.0
            var splitRadius = 0.0
            var splitSpeed = 0.0
            var cellId = 0L
            if (split && cell.mass > splitLimit && player.cells.size < maxCellNum) {
              newSplitTime = System.currentTimeMillis()
              splitMass = (newMass / 2).toInt
              newMass = newMass - splitMass
              splitRadius = 4 + sqrt(splitMass) * 6
              newRadius = 4 + sqrt(newMass) * 6
              splitSpeed = splitBaseSpeed + 2 * cbrt(cell.radius)
              splitX = (cell.x + (newRadius + splitRadius) * degX).toInt
              splitY = (cell.y + (newRadius + splitRadius) * degY).toInt
              cellId = cellIdgenerator.getAndIncrement().toLong
            }
            List(Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY), Cell(cellId, splitX, splitY, splitMass, splitRadius, splitSpeed, (splitSpeed * degX).toFloat, (splitSpeed * degY).toFloat))
        }.filterNot(_.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }



  def massDerease():Unit={
    val newPlayerMap = playerMap.values.map{player=>
      val newCells=player.cells.map{cell=>
        var newMass = cell.mass
        if(cell.mass > decreaseLimit)
          newMass = cell.mass*decreaseRate
        cell.copy(mass = newMass)
      }
      player.copy(cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap

  }
  def updatePlayer()={

     val mouseAct = mouseActionMap.getOrElse(frameCount, Map.empty[Long, MousePosition])
     val keyAct = actionMap.getOrElse(frameCount, Map.empty[Long, KeyCode])

     //先移动到指定位置
     playerMap=playerMap.values.map(updatePlayerMove(_, mouseAct)).map(s=>(s.id,s)).toMap
     checkPlayerFoodCrash()
     checkPlayerMassCrash()
     checkPlayer2PlayerCrash()
     val mergeInFlame=checkCellMerge()
     checkPlayerVirusCrash(mergeInFlame)
     checkPlayerShotMass(keyAct,mouseAct)
     checkVirusMassCrash()
     checkPlayerSplit(keyAct,mouseAct)
     tick = tick+1
    if(tick%10==1){
      tick =1
      massDerease()
    }


  }




  def updateAndGetGridData() = {
    update()
    getGridData(myId)
  }

  def getGridData(id:Long) = {
    myId = id
    val currentPlayer = playerMap.get(id).map(a=>(a.x,a.y)).getOrElse((500,500))
    val zoom = playerMap.get(id).map(a=>(a.width,a.height)).getOrElse((30.0,30.0))
//    println(s"zoom：$zoom")
    val scale = getZoomRate(zoom._1,zoom._2)
    val width = Window.w/scale/2
    val height = Window.h/scale/2

    var foodDetails: List[Food] = Nil
    var playerDetails: List[Player] = Nil
    food.foreach{
      case (p,mass) =>
        foodDetails ::= Food(mass, p.x, p.y)
    }
    playerMap.foreach{
      case (id,player) =>
        if (checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(player.x,player.y),sqrt(pow(player.width/2,2.0)+pow(player.height/2,2.0)),width,height))
        playerDetails ::= player
    }
    WsMsgProtocol.GridDataSync(
      frameCount,
      playerDetails,
      foodDetails,
      massList.filter(m=>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m.x,m.y),m.radius,width,height)),
      virus.filter(m=>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m.x,m.y),m.radius,width,height)),
      scale
    )
  }
  def getAllGridData: WsMsgProtocol.GridDataSync

}
