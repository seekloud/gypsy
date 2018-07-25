package com.neo.sk.gypsy.shared.ptcl

import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.shared.ptcl.Point
import Protocol.MousePosition
import com.neo.sk.gypsy.shared.ptcl

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

  val random = new Random(System.nanoTime())

  val cellIdgenerator = new AtomicInteger(1000000)
  val step = 8

  val defaultLength = 5
  val historyRankLength = 5

  val slowDown = 2
//质量转半径率
  val mass2rRate = 6
  //吞噬覆盖率  (-1,1) 刚接触->完全覆盖
  val coverRate = 0
  var frameCount = 0l
//合并时间间隔
  val mergeInterval = 8 * 1000
//最小分裂大小
  val splitLimit = 30
  //分裂初始速度
  val splitBaseSpeed = 40
  //食物质量
  val foodMass = 1
  //食物列表
  var food = Map[Point, Int]()
  //食物池
  var foodPool = 100
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
  var actionMap = Map.empty[Long, Map[Long, Int]]

  var mouseActionMap = Map.empty[Long, Map[Long, MousePosition]]

//  var quad = new Quadtree(0, new Rectangle(0,0,boundary.x,boundary.y))

//用户离开，从列表中去掉
  def removePlayer(id: Long): Option[Player] = {
    val r = playerMap.get(id)
    if (r.isDefined) {
      playerMap -= id
    }
    r
  }

  def addAction(id: Long, keyCode: Int) = {
    addActionWithFrame(id, keyCode, frameCount)
  }
//键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: Long, keyCode: Int, frame: Long) = {
    val map = actionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (frame -> tmp)
  }
  def addMouseAction(id: Long, x:Double, y:Double) = {
    addMouseActionWithFrame(id, x, y,  frameCount)
  }
  def addMouseActionWithFrame(id: Long, x:Double, y:Double,  frame: Long) = {
    val map = mouseActionMap.getOrElse(frame, Map.empty)
    val tmp = map + (id -> MousePosition(x,y))
    mouseActionMap += (frame -> tmp)
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
    massList = massList.map{mass=>
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

      val borderCalc = mass.radius.ceil.toInt

      if (newX > boundary.x - borderCalc) newX = boundary.x - borderCalc
      if (newY > boundary.y - borderCalc) newY = boundary.y - borderCalc
      if (newX < borderCalc) newX = borderCalc
      if (newY < borderCalc) newY = borderCalc

      mass.copy(x = newX,y = newY,speed = newSpeed)
    }
    feedApple(foodPool + playerMap.size * 3 - food.size)
    addVirus(virusNum - virus.size)
  }

//随机返回一个空点坐标
  def randomEmptyPoint(): Point = {
    val p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
//    while (grid.contains(p)) {
//      p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
//      //TODO 随机点的生成位置需要限制
//    }
    p
  }

//移动，碰撞检测
  private[this] def updatePlayer() = {
    def updateAStar(player: Player, actMap: Map[Long, Int], mouseActMap:Map[Long,MousePosition]): Either[Long, Player] = {

      val mouseAct = mouseActMap.get(player.id) match{
        case Some(MousePosition(x,y))=>
          //相对屏幕中央的位置
          //println(s"x${x},y${y}")
          //MousePosition(x-111-600,y-48-300)
          MousePosition(x,y)
        case _=>
          MousePosition(player.targetX,player.targetY)
      }
      var killer = 0L
      var shot = false
      var split = false
      val keyAct = actMap.get(player.id) match{
        case Some(KeyEvent.VK_E)=>
          shot = true
        case Some(KeyEvent.VK_F)=>
          split = true
        case _ =>
      }

      var newKill = player.kill
      var newSplitTime = player.lastSplit
      var mergeCells = List[Cell]()//已经被本体其他cell融合的cell
      var deleteCells = List[Cell]()//依据距离判断被删去的cell

      var mergeInFlame = false

      //对每一个cell单独计算速度、方向
      //此处算法针对只有一个cell的player
      var newCells = player.cells.sortBy(_.radius).reverse.flatMap{cell=>
        var newSpeed = cell.speed
        var vSplitCells = List[Cell]()//碰到病毒分裂出的cell列表
        //println(s"鼠标x${mouseAct.clientX} 鼠标y${mouseAct.clientY} 小球x${star.center.x} 小球y${star.center.y}")
        val target = MousePosition(mouseAct.clientX + player.x-cell.x ,mouseAct.clientY + player.y - cell.y)
        val distance = sqrt(pow(target.clientX,2) + pow(target.clientY, 2))
        val deg = atan2(target.clientY,target.clientX)
        val degX = if((cos(deg)).isNaN) 0 else (cos(deg))
        val degY = if((sin(deg)).isNaN) 0 else (sin(deg))
        val newDirection = {
          //指针在圆内，静止
          if(cell.speed > 8 + 20/cbrt(cell.radius)){
            newSpeed -= 2
          }else{
            if(distance < sqrt(pow((newSpeed*degX).toInt,2) + pow((newSpeed*degY).toInt,2))){
              newSpeed = target.clientX / degX
            }else{
              if(distance < cell.radius){
                //println("在圆内")
                if(cell.speed>0){
                  //println("come here")
                  newSpeed=cell.speed - slowDown
                }else newSpeed=0
                //println(s"new speed ${newSpeed} ,star.speed -slowDown${cell.speed - slowDown},slowDown${slowDown}")
              }else{
                newSpeed=if(cell.speed < 8 + 20/cbrt(cell.radius)){
                  cell.speed + slowDown
                }else 8 + 20/cbrt(cell.radius)
              }
            }
          }
          //println(s"x位移${(newSpeed*degX).toInt}，y位移${(newSpeed*degY).toInt}")
          Point((newSpeed*degX).toInt,(newSpeed*degY).toInt)
        }
        //cell移动+边界检测
        var newX = if((cell.x + newDirection.x) > boundary.x) boundary.x else if((cell.x + newDirection.x) <= 0) 0 else cell.x + newDirection.x
        var newY = if((cell.y + newDirection.y) > boundary.y) boundary.y else if ((cell.y + newDirection.y) <= 0) 0 else cell.y + newDirection.y
        //碰撞检测
        var newRadius = cell.radius
        var newMass = cell.mass

        food.foreach{
          case (p, color)=>
            if(sqrt(pow((p.x-cell.x),2.0) + pow((p.y-cell.y),2.0)) < (cell.radius + 4)) {
              newMass += foodMass
              newRadius = 4 + sqrt(newMass) * mass2rRate
              food -= p
            }
        }
        massList.foreach{
          case p:Mass=>
            if(sqrt(pow((p.x-cell.x),2.0) + pow((p.y-cell.y),2.0)) < (cell.radius - p.radius * coverRate)) {
              newMass += p.mass
              newRadius = 4 + sqrt(newMass) * mass2rRate
              massList = massList.filterNot(l=>l==p)
            }
        }
        playerMap.filterNot(_._1 == player.id).foreach{ p=>
          p._2.cells.foreach{ otherCell=>
            if(cell.radius < otherCell.radius && sqrt(pow((cell.x-otherCell.x),2.0) + pow((cell.y-otherCell.y),2.0)) < (otherCell.radius - cell.radius * coverRate)){
              newMass = 0
              killer = p._1
            }else if(cell.radius > otherCell.radius && sqrt(pow((cell.x-otherCell.x),2.0) + pow((cell.y-otherCell.y),2.0)) < (cell.radius - otherCell.radius * coverRate)){
              newMass +=  otherCell.mass
              newRadius = 4 + sqrt(newMass) * 6
            }
          }
        }

        //自身cell合并检测
        player.cells.filterNot(p=> p == cell).sortBy(_.radius).reverse.map{cell2=>
          val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
          val radiusTotal = cell.radius + cell2.radius
          if (distance < radiusTotal) {
            if (newSplitTime > System.currentTimeMillis() - mergeInterval) {
              if (cell.x < cell2.x) newX -= 1
              else if (cell.x > cell2.x) newX += 1
              if (cell.y < cell2.y) newY  -= 1
              else if (cell.y > cell2.y) newY += 1
            }
            else if (distance < radiusTotal / 2) {
              if(cell.radius > cell2.radius){
                if(mergeCells.filter(_.id==cell2.id).isEmpty && mergeCells.filter(_.id==cell.id).isEmpty && deleteCells.filter(_.id == cell.id).isEmpty){
                  mergeInFlame = true
                  newMass += cell2.mass
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  mergeCells = cell2 :: mergeCells
                }
              }
              else if(cell.radius < cell2.radius && deleteCells.filter(_.id == cell.id).isEmpty && deleteCells.filter(_.id == cell2.id).isEmpty){
                mergeInFlame = true
                newMass = 0
                newRadius = 0
                deleteCells = cell :: deleteCells
              }
            }
          }
        }

        //病毒碰撞检测
        virus.foreach{v=>
          if((sqrt(pow((v.x-cell.x),2.0) + pow((v.y-cell.y),2.0)) < (cell.radius - v.radius)) && (cell.radius > v.radius *1.2) &&  mergeInFlame == false && player.cells.size<5) {
            virus = virus.filterNot(_==v)
            val cellMass= (newMass/(v.splitNumber+1)).toInt
            val cellRadius =  4 + sqrt(cellMass) * mass2rRate
            newMass = (newMass/(v.splitNumber+1)).toInt + (v.mass*0.5).toInt
            newRadius = 4 + sqrt(newMass) * mass2rRate
            newSplitTime = System.currentTimeMillis()
            val baseAngle = 2*Pi/v.splitNumber
            for(i <- 0 until v.splitNumber){
              val degX = cos(baseAngle * i)
              val degY = sin(baseAngle * i)
              val startLen = (newRadius + cellRadius)*1.2
              vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong,(cell.x + startLen * degX).toInt,(cell.y + startLen * degY).toInt,cellMass,cellRadius,cell.speed)
            }
          }
        }

//喷射小球
        if (shot == true && newMass > shotMass*3){
          newMass -= shotMass
          newRadius = 4 + sqrt(newMass) * 6
          val massRadius = 4 + sqrt(shotMass) * 6
          val massX = (cell.x + (newRadius + massRadius) * degX).toInt
          val massY = (cell.y + (newRadius + massRadius) * degY).toInt
          massList ::= ptcl.Mass(massX,massY,player.targetX,player.targetY,player.color.toInt,shotMass,massRadius,shotSpeed)
        }
//分裂
        var splitX = 0
        var splitY = 0
        var splitMass = 0.0
        var splitRadius = 0.0
        var splitSpeed = 0.0
        var cellId = 0L
        if (split == true && cell.mass > splitLimit && player.cells.size<32){
          newSplitTime = System.currentTimeMillis()
          splitMass = (newMass/2).toInt
          newMass = newMass - splitMass
          splitRadius = 4 + sqrt(splitMass) * 6
          newRadius = 4 + sqrt(newMass) * 6
          splitSpeed = splitBaseSpeed + 2*cbrt(cell.radius)
          splitX = (cell.x + (newRadius + splitRadius) * degX).toInt
          splitY = (cell.y + (newRadius + splitRadius) * degY).toInt
          cellId = cellIdgenerator.getAndIncrement().toLong
        }
        List(Cell(cell.id,newX,newY,newMass,newRadius,newSpeed),Cell(cellId,splitX,splitY,splitMass,splitRadius,splitSpeed)) ::: vSplitCells
      }.filterNot(_.mass<=0)

      //val recoverCells = (deleteCells.distinct).diff(mergeCells.distinct)
      //println(s"mergeCells${mergeCells},deleteCells${deleteCells},recoverCells${recoverCells}")
      //newCells = newCells ::: recoverCells

      //newCells = newCells.filterNot(c =>mergeCellId.contains(c))

      if(newCells.length == 0){
        //println(s"newCells${newCells}")
         Left(killer)
      }else{
        //println(s"newCells2${newCells}")
        val length = newCells.length
        val newX = newCells.map(_.x).sum/length
        val newY = newCells.map(_.y).sum/length
        val left = newCells.map(a=>a.x-a.radius).min
        val right = newCells.map(a=>a.x+a.radius).max
        val bottom = newCells.map(a=>a.y-a.radius).min
        val top = newCells.map(a=>a.y+a.radius).max
        Right(player.copy( x = newX, y = newY, targetX = mouseAct.clientX.toInt, targetY = mouseAct.clientY.toInt, kill = newKill, lastSplit = newSplitTime, width =right-left ,height = top-bottom,cells = newCells))
      }

    }


    //var mapKillCounter = Map.empty[Long, Int]
    var updatedPlayers = List.empty[Player]

    var killerMap = List.empty[Long]
    val acts = actionMap.getOrElse(frameCount, Map.empty[Long, Int])

    val mouseAct = mouseActionMap.getOrElse(frameCount,Map.empty[Long, MousePosition])
    playerMap.values.map(updateAStar(_, acts,mouseAct)).foreach {
      case Right(s) => updatedPlayers ::= s
      case Left(killerId) => killerMap ::= killerId
        //mapKillCounter += killerId -> (mapKillCounter.getOrElse(killerId, 0) + 1)
    }
    playerMap = updatedPlayers.map(s => (s.id, s)).toMap
    killerMap.foreach{killer=>
      val a= playerMap.get(killer).getOrElse(Player(0,"","",0,0,cells = List(Cell(0L,0,0))))
      val killNumber = a.kill
      playerMap += (killer -> a.copy(kill = killNumber+1))
    }
  }




  def updateAndGetGridData() = {
    update()
    getGridData
  }

  def getGridData = {
    var foodDetails: List[Food] = Nil
    var playerDetails: List[Player] = Nil
    food.foreach{
      case (p,mass) => foodDetails ::= Food(mass, p.x, p.y)
    }
    playerMap.foreach{
      case (id,player) => playerDetails ::= player
    }
    Protocol.GridDataSync(
      frameCount,
      playerDetails,
      foodDetails,
      massList,
      virus
    )
  }
}