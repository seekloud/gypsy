package com.neo.sk.gypsy.snake

import java.awt.event.KeyEvent

import com.neo.sk.gypsy.snake.Protocol.MousePosition

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

  val step = 8

  val defaultLength = 5
  val appleNum = 6
  val appleLife = 100
  val historyRankLength = 5

  val slowDown = 2
//质量转半径率
  val mass2rRate = 6
  //吞噬覆盖率  (-1,1) 刚接触->完全覆盖
  val coverRate = 0
  var frameCount = 0l
  //每个网格上的点（Point）对应一种Spot状态（body，header，Apple，Nome）
 // var grid = Map[Point, Spot]()

  //食物列表
  var food = Map[Point, Int]()
  //食物池
  var foodPool = 30
  //玩家列表
  var playerMap = Map.empty[Long,Player]

  //var stars = Map.empty[Long,StarDt]
  //操作列表  帧数->(用户ID->操作)
  var actionMap = Map.empty[Long, Map[Long, Int]]

  var mouseActionMap = Map.empty[Long, Map[Long, MousePosition]]

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
    //println(s"-------- grid update frameCount= $frameCount ---------")
    updateSnakes()
    updateSpots()
    actionMap -= frameCount
    frameCount += 1
  }

  def feedApple(appleCount: Int): Unit
//食物更新
  private[this] def updateSpots() = {

    feedApple(foodPool + playerMap.size * 3 - food.size)
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
  private[this] def updateSnakes() = {
    def updateAStar(player: Player, actMap: Map[Long, Int], mouseActMap:Map[Long,MousePosition]): Either[Long, Player] = {
      //val keyCode = actMap.get(star.id)
      val mouseAct = mouseActMap.get(player.id) match{
        case Some(MousePosition(x,y))=>
          MousePosition(x-6,y-129)
        case _=>
          MousePosition(player.targetX,player.targetY)
      }

      var newKill = player.kill

      //对每一个cell单独计算速度、方向
      //此处算法针对只有一个cell的player
      val newCells = player.cells.map{cell=>
        var newSpeed = cell.speed
        val newDirection = {
          //println(s"鼠标x${mouseAct.clientX} 鼠标y${mouseAct.clientY} 小球x${star.center.x} 小球y${star.center.y}")
          val target = MousePosition(mouseAct.clientX - player.x,mouseAct.clientY-player.y)
          val distance = sqrt(pow(target.clientX,2) + pow(target.clientY, 2))
          val deg = atan2(target.clientY,target.clientX)
          val degX = if((cos(deg)).isNaN) 0 else (cos(deg))
          val degY = if((sin(deg)).isNaN) 0 else (sin(deg))

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
          println(s"x位移${(newSpeed*degX).toInt}，y位移${(newSpeed*degY).toInt}")
          Point((newSpeed*degX).toInt,(newSpeed*degY).toInt)
        }
        //cell移动+边界检测
        val newX = if((cell.x + newDirection.x) > boundary.x) boundary.x else if((cell.x + newDirection.x) <= 0) 0 else cell.x + newDirection.x
        val newY = if((cell.y + newDirection.y) > boundary.y) boundary.y else if ((cell.y + newDirection.y) <= 0) 0 else cell.y + newDirection.y

        //碰撞检测
        var newRadius = cell.radius
        var newMass = cell.mass

        food.foreach{
          case (p, mass)=>
            if(sqrt(pow((p.x-cell.x),2.0) + pow((p.y-cell.y),2.0)) < (cell.radius + 4)) {
              newMass += mass
              newRadius = 4 + sqrt(newMass) * mass2rRate
              food -= p
            }
        }
        playerMap.filterNot(_._1 == player.id).foreach{ p=>
          p._2.cells.map{otherCell=>
            if(cell.radius < otherCell.radius && sqrt(pow((cell.x-otherCell.x),2.0) + pow((cell.y-otherCell.y),2.0)) < (otherCell.radius - cell.radius * coverRate)){
              newMass = 0
            }else if(cell.radius > otherCell.radius && sqrt(pow((cell.x-otherCell.x),2.0) + pow((cell.y-otherCell.y),2.0)) < (cell.radius - otherCell.radius * coverRate)){
              newMass +=  otherCell.mass
              newRadius = 4 + sqrt(newMass) * 6
              newKill +=  1
            }
          }
        }
        Cell(newX,newY,newMass,newRadius,newSpeed)
      }.filterNot(_.mass == 0)

      if(newCells.length == 0){
        //println(s"newCells${newCells}")
         Left(0L)
      }else{
        //println(s"newCells2${newCells}")
        val length = newCells.length
        val newX = newCells.map(_.x).sum/length
        val newY = newCells.map(_.y).sum/length
        Right(player.copy( x = newX, y = newY, targetX = mouseAct.clientX.toInt, targetY = mouseAct.clientY.toInt, kill = newKill, cells = newCells))
      }

    }


    //var mapKillCounter = Map.empty[Long, Int]
    var updatedPlayers = List.empty[Player]

    val acts = actionMap.getOrElse(frameCount, Map.empty[Long, Int])

    val mouseAct = mouseActionMap.getOrElse(frameCount,Map.empty[Long, MousePosition])
    playerMap.values.map(updateAStar(_, acts,mouseAct)).foreach {
      case Right(s) => updatedPlayers ::= s
      case Left(killerId) =>
        //mapKillCounter += killerId -> (mapKillCounter.getOrElse(killerId, 0) + 1)
    }
    playerMap = updatedPlayers.map(s => (s.id, s)).toMap
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
      foodDetails
    )
  }
}
