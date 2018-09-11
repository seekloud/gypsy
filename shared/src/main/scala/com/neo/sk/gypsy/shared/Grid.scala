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

  val slowBase = 8
  val initMassLog = utils.logSlowDown(10,slowBase)
  val acceleration  = 2
//质量转半径率
  val mass2rRate = 6
  //吞噬覆盖率  (-1,1) 刚接触->完全覆盖
  val coverRate = 0
  var frameCount = 0l
//合并时间间隔
  val mergeInterval = 8 * 1000
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
  var virusNum:Int = 4
  //玩家列表
  var playerMap = Map.empty[Long,Player]
  //喷出小球列表
  var massList = List[Mass]()
  val shotMass = 10
  val shotSpeed = 40
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

  def addAction(id: Long, keyCode: KeyCode) = {
    addActionWithFrame(id, keyCode, frameCount)
  }
//键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: Long, keyCode: KeyCode, frame: Long) = {
    val map = actionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (frame -> tmp)
  }
  def addMouseAction(id: Long, mp:MousePosition) = {
    addMouseActionWithFrame(id, mp,frameCount)
  }
  def addMouseActionWithFrame(id: Long, mp:MousePosition,  frame: Long) = {
    val map = mouseActionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> mp)
    mouseActionMap += (frame -> tmp)
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
    newSpeed -= 2
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

      val deg1 = atan2(player.targetY + player.y - cell.y, player.targetX + player.x - cell.x)
     // val deg1 = atan2(player.targetY , player.targetX )
      val degX1 = if (cos(deg1).isNaN) 0 else cos(deg1)
      val degY1 = if (sin(deg1).isNaN) 0 else sin(deg1)
      val move = Point((newSpeed * degX1).toInt, (newSpeed * degY1).toInt)

      val target = Position(mouseAct.clientX + player.x - cell.x, mouseAct.clientY + player.y - cell.y)
      //val target = MousePosition(mouseAct.clientX + player.x - cell.x, mouseAct.clientY + player.y - cell.y,0l)
      //val target = MousePosition(mouseAct.clientX , mouseAct.clientY ,0l)
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

      List(Cell(cell.id, newX, newY, cell.mass, cell.radius, newSpeed, (newSpeed * degX).toFloat, (newSpeed * degY).toFloat))
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
  def checkPlayerFoodCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            food.foreach {
              case (p, color) =>
                if (checkCollision(Point(cell.x, cell.y), p, cell.radius, 4, -1)) {
                  //食物被吃掉
                  newMass += foodMass
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  food -= p
                  if (newProtected)
                  //吃食物后取消保护
                    newProtected = false
                }
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,newProtected,player.lastSplit,player.killerName,player.width,player.height,newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  //mass检测
  def checkPlayerMassCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            massList.foreach {
              case p: Mass =>
                if (checkCollision(Point(cell.x, cell.y), Point(p.x, p.y), cell.radius, p.radius, coverRate)) {
                  newMass += p.mass
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  massList = massList.filterNot(l => l == p)
                }
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,newProtected,player.lastSplit,player.killerName,player.width,player.height,newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  //与用户检测
  def checkPlayer2PlayerCrash(): Unit = {}

  //返回在这一帧是否融合过
  def checkCellMerge(): Boolean = {
    var mergeInFlame = false
    val newPlayerMap = playerMap.values.map {
      player =>
        val newSplitTime = player.lastSplit
        var mergeCells = List[Cell]()
        //已经被本体其他cell融合的cell
        var deleteCells = List[Cell]()
        //依据距离判断被删去的cell
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var newRadius = cell.radius
            var newMass = cell.mass
            var cellX = cell.x
            var cellY = cell.y
            //自身cell合并检测
            player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
              val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
              val radiusTotal = cell.radius + cell2.radius
              if (distance < radiusTotal) {
                if (newSplitTime > System.currentTimeMillis() - mergeInterval) {
                  if (cell.x < cell2.x) cellX -= 1
                  else if (cell.x > cell2.x) cellX += 1
                  if (cell.y < cell2.y) cellY -= 1
                  else if (cell.y > cell2.y) cellY += 1
                }
                else if (distance < radiusTotal / 2) {
                  if (cell.radius > cell2.radius) {
                    //被融合的细胞不能再被其他细胞融合
                    if (!mergeCells.exists(_.id == cell2.id) && !mergeCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell.id)) {
                      mergeInFlame = true
                      newMass += cell2.mass
                      newRadius = 4 + sqrt(newMass) * mass2rRate
                      mergeCells = cell2 :: mergeCells
                    }
                  }
                  else if (cell.radius < cell2.radius && !deleteCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell2.id)) {
                    mergeInFlame = true
                    newMass = 0
                    newRadius = 0
                    deleteCells = cell :: deleteCells
                  }
                }
              }
            }
            List(Cell(cell.id, cellX, cellY, newMass, newRadius, cell.speed, cell.speedX, cell.speedY))
        }.filterNot(_.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id, player.name, player.color, player.x, player.y, player.targetX, player.targetY, player.kill, player.protect, player.lastSplit, player.killerName, player.width, player.height, newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    mergeInFlame
  }

  //病毒碰撞检测
  def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newSplitTime = player.lastSplit
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var vSplitCells = List[Cell]()
            var newMass = cell.mass
            var newRadius = cell.radius
            //病毒碰撞检测
            virus.foreach { v =>
              if ((sqrt(pow(v.x - cell.x, 2.0) + pow(v.y - cell.y, 2.0)) < (cell.radius - v.radius)) && (cell.radius > v.radius * 1.2) && !mergeInFlame) {
                virus = virus.filterNot(_ == v)
                val cellMass = (newMass / (v.splitNumber + 1)).toInt
                val cellRadius = 4 + sqrt(cellMass) * mass2rRate
                newMass = (newMass / (v.splitNumber + 1)).toInt + (v.mass * 0.5).toInt
                newRadius = 4 + sqrt(newMass) * mass2rRate
                newSplitTime = System.currentTimeMillis()
                val baseAngle = 2 * Pi / v.splitNumber
                for (i <- 0 until v.splitNumber) {
                  val degX = cos(baseAngle * i)
                  val degY = sin(baseAngle * i)
                  val startLen = (newRadius + cellRadius) * 1.2
                  // vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong,(cell.x + startLen * degX).toInt,(cell.y + startLen * degY).toInt,cellMass,cellRadius,cell.speed)
                  val speedx = (cos(baseAngle * i) * cell.speed).toFloat
                  val speedy = (sin(baseAngle * i) * cell.speed).toFloat
                  vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong, (cell.x + startLen * degX).toInt, (cell.y + startLen * degY).toInt, cellMass, cellRadius, cell.speed, speedx, speedy)
                }
              }
            }
            List(Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)) ::: vSplitCells
        }

        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id, player.name, player.color, player.x, player.y, player.targetX, player.targetY, player.kill, player.protect, newSplitTime, player.killerName, player.width, player.height, newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

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
              val massX = (cell.x + (newRadius - 15) * degX).toInt
              val massY = (cell.y + (newRadius - 15) * degY).toInt
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
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,player.protect,player.lastSplit,player.killerName,player.width,player.height,newCells)
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
            if (split && cell.mass > splitLimit && player.cells.size < 32) {
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
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,player.protect,player.lastSplit,player.killerName,player.width,player.height,newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  def updatePlayerCrash(player: Player, actMap: Map[Long, Int], mouseActMap: Map[Long, MousePosition]): Either[Long, Player] = {

    var killer = 0L
    var shot = false
    var split = false
    val keyAct = actMap.get(player.id) match {
      case Some(KeyEvent.VK_E) =>
        shot = true
      case Some(KeyEvent.VK_F) =>
        split = true
      case _ =>
    }

    var newKill = player.kill
    var newSplitTime = player.lastSplit
    if (newSplitTime > System.currentTimeMillis() - splitInterval) split = false
    var mergeCells = List[Cell]()
    //已经被本体其他cell融合的cell
    var deleteCells = List[Cell]()
    //依据距离判断被删去的cell
    var newProtected = player.protect
    var mergeInFlame = false

    val newCells = player.cells.sortBy(_.radius).reverse.flatMap { cell =>
      var vSplitCells = List[Cell]()
      //碰到病毒分裂出的cell列表
      //println(s"鼠标x${mouseAct.clientX} 鼠标y${mouseAct.clientY} 小球x${star.center.x} 小球y${star.center.y}")
      //碰撞检测
      var newRadius = cell.radius
      var newMass = cell.mass
      //检测吃食物
      food.foreach {
        case (p, color) =>
          if (checkCollision(Point(cell.x, cell.y), p, cell.radius, 4, -1)) {
            //食物被吃掉
            newMass += foodMass
            newRadius = 4 + sqrt(newMass) * mass2rRate
            food -= p
            if (newProtected == true)
            //吃食物后取消保护
              newProtected = false
          }
      }
      //检测吃掉小球
      massList.foreach {
        case p: Mass =>
          if (checkCollision(Point(cell.x, cell.y), Point(p.x, p.y), cell.radius, p.radius, coverRate)) {
            newMass += p.mass
            newRadius = 4 + sqrt(newMass) * mass2rRate
            massList = massList.filterNot(l => l == p)
          }
      }
      //检测是否吃掉其他小球或被其他小球吃掉
      playerMap.filterNot(a => a._1 == player.id || a._2.protect).foreach { p =>
        p._2.cells.foreach { otherCell =>
          if (cell.radius * 1.1 < otherCell.radius && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (otherCell.radius - cell.radius * 0.8) && !player.protect) {
            //被吃了
            newMass = 0
            killer = p._1
          } else if (cell.radius > otherCell.radius * 1.1 && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (cell.radius - otherCell.radius * 0.8)) {
            //吃掉别人了
            newMass += otherCell.mass
            newRadius = 4 + sqrt(newMass) * 6
          }
        }
      }

      var cellX = cell.x
      var cellY = cell.y
      //自身cell合并检测
      player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
        val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
        val radiusTotal = cell.radius + cell2.radius
        if (distance < radiusTotal) {
          if (newSplitTime > System.currentTimeMillis() - mergeInterval) {
            if (cell.x < cell2.x) cellX -= 1
            else if (cell.x > cell2.x) cellX += 1
            if (cell.y < cell2.y) cellY -= 1
            else if (cell.y > cell2.y) cellY += 1
          }
          else if (distance < radiusTotal / 2) {
            if (cell.radius > cell2.radius) {
              if (!mergeCells.exists(_.id == cell2.id) && !mergeCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell.id)) {
                mergeInFlame = true
                newMass += cell2.mass
                newRadius = 4 + sqrt(newMass) * mass2rRate
                mergeCells = cell2 :: mergeCells
              }
            }
            else if (cell.radius < cell2.radius && !deleteCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell2.id)) {
              mergeInFlame = true
              newMass = 0
              newRadius = 0
              deleteCells = cell :: deleteCells
            }
          }
        }
      }

      //病毒碰撞检测
      virus.foreach { v =>
        if ((sqrt(pow(v.x - cell.x, 2.0) + pow(v.y - cell.y, 2.0)) < (cell.radius - v.radius)) && (cell.radius > v.radius * 1.2) && !mergeInFlame) {
          virus = virus.filterNot(_ == v)
          val cellMass = (newMass / (v.splitNumber + 1)).toInt
          val cellRadius = 4 + sqrt(cellMass) * mass2rRate
          newMass = (newMass / (v.splitNumber + 1)).toInt + (v.mass * 0.5).toInt
          newRadius = 4 + sqrt(newMass) * mass2rRate
          newSplitTime = System.currentTimeMillis()
          val baseAngle = 2 * Pi / v.splitNumber
          for (i <- 0 until v.splitNumber) {
            val degX = cos(baseAngle * i)
            val degY = sin(baseAngle * i)
            val startLen = (newRadius + cellRadius) * 1.2
            // vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong,(cell.x + startLen * degX).toInt,(cell.y + startLen * degY).toInt,cellMass,cellRadius,cell.speed)
            val speedx = (cos(baseAngle * i) * cell.speed).toFloat
            val speedy = (sin(baseAngle * i) * cell.speed).toFloat
            vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong, (cell.x + startLen * degX).toInt, (cell.y + startLen * degY).toInt, cellMass, cellRadius, cell.speed, speedx, speedy)
          }
        }
      }

      //喷射小球
      val mouseAct = mouseActMap.getOrElse(player.id,MousePosition(player.id,player.targetX, player.targetY,0l,0))
      val target = Position(mouseAct.clientX + player.x - cell.x, mouseAct.clientY + player.y - cell.y)
      val deg = atan2(target.clientY, target.clientX)
      val degX = if (cos(deg).isNaN) 0 else cos(deg)
      val degY = if (sin(deg).isNaN) 0 else sin(deg)

      if (shot && newMass > shotMass * 3) {
        newMass -= shotMass
        newRadius = 4 + sqrt(newMass) * 6
        val massRadius = 4 + sqrt(shotMass) * 6
        val massX = (cell.x + (newRadius - 15) * degX).toInt
        val massY = (cell.y + (newRadius - 15) * degY).toInt
        massList ::= ptcl.Mass(massX, massY, player.targetX, player.targetY, player.color.toInt, shotMass, massRadius, shotSpeed)
      }
      //分裂
      var splitX = 0
      var splitY = 0
      var splitMass = 0.0
      var splitRadius = 0.0
      var splitSpeed = 0.0
      var cellId = 0L
      if (split && cell.mass > splitLimit && player.cells.size < 32) {
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


      List(Cell(cell.id, cellX, cellY, newMass, newRadius, cell.speed, cell.speedX, cell.speedY), Cell(cellId, splitX, splitY, splitMass, splitRadius, splitSpeed, (splitSpeed * degX).toFloat, (splitSpeed * degY).toFloat)) ::: vSplitCells
    }.filterNot(_.mass <= 0)

    if (newCells.isEmpty) {
      //println(s"newCells${newCells}")
      playerMap.get(killer) match {
        case Some(killerPlayer) =>
          player.killerName = killerPlayer.name
        case _ =>
          player.killerName = "unknown"
      }
      deadPlayerMap += (player.id -> player)
      Left(killer)
    } else {
      //println(s"newCells2${newCells}")
      val length = newCells.length
      val newX = newCells.map(_.x).sum / length
      val newY = newCells.map(_.y).sum / length
      val left = newCells.map(a => a.x - a.radius).min
      val right = newCells.map(a => a.x + a.radius).max
      val bottom = newCells.map(a => a.y - a.radius).min
      val top = newCells.map(a => a.y + a.radius).max
      Right(player.copy(x = newX, y = newY, protect = newProtected, kill = newKill, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells))
    }

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
     checkPlayerSplit(keyAct,mouseAct)


  }




  def updateAndGetGridData() = {
    update()
    getGridData(myId)
  }

  def getGridData(id:Long) = {
    myId = id
//    println(s"玩家id：$myId")
//    println(s"玩家列表：$playerMap")
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
        if (checkScreenRange(Point(currentPlayer._1,currentPlayer._2),p,4,width,height))
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
  def getAllGridData = {
    var foodDetails: List[Food] = Nil
    var playerDetails: List[Player] = Nil
    food.foreach{
      case (p,mass) =>
          foodDetails ::= Food(mass, p.x, p.y)
    }
    playerMap.foreach{
      case (id,player) =>

          playerDetails ::= player
    }
    WsMsgProtocol.GridDataSync(
      frameCount,
      playerDetails,
      foodDetails,
      massList,
      virus,
      1.0
    )
  }

}
