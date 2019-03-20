package com.neo.sk.gypsy.front.gypsyClient

import java.awt.event.KeyEvent

import com.neo.sk.gypsy.shared.Grid
import com.neo.sk.gypsy.shared.util.Utils.{Mass2Radius, checkCollision, normalization}

import scala.collection.mutable
import scala.math._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Game._

/**
  * User: sky
  * Date: 2018/9/13
  * Time: 13:57
  */
class GameClient (override val boundary: Point) extends Grid {

  var myId = ""

  override def debug(msg: String): Unit = println(msg)

  override def info(msg: String): Unit = println(msg)

  override def feedApple(appleCount: Int): Unit = {} //do nothing.
  override def addVirus(v: Int): Unit = {}
  var currentRank = List.empty[RankInfo]
  //序列号->(frame,Id,GameAction)
  private[this] val uncheckActionWithFrame = new mutable.HashMap[Int,(Long,String,UserAction)]()
  private[this] val gameSnapshotMap = new mutable.HashMap[Long,GridDataSync]()
  var playerByte2IdMap = new mutable.HashMap[Byte, String]()

  override def getAllGridData: GridDataSync={
    GridDataSync(0, Nil, Nil, Map.empty, 1.0)
  }

  //这里用不到id！！！
  //键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: String, keyCode: KC) = {
    val map = actionMap.getOrElse(keyCode.f, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (keyCode.f -> tmp)
  }

  def addMouseActionWithFrame(id: String, mp:MP) = {
    val map = mouseActionMap.getOrElse(mp.f, Map.empty)
    val tmp = map + (id -> mp)
    mouseActionMap += (mp.f -> tmp)
  }

  def removeActionWithFrame(id: String, userAction: UserAction, frame: Int) = {
    userAction match {
      case k:KC=>
        val map = actionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && k.sN == t._2.sN)
        actionMap += (frame->actionQueue)
      case m:MP=>
        val map = mouseActionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && m.sN == t._2.sN)
        mouseActionMap += (frame->actionQueue)
    }
  }

  override def checkCellMerge: Boolean = {
    var mergeInFlame = false
    val newPlayerMap = playerMap.values.map {
      player =>
        val newSplitTime = player.lastSplit
        var mergeCells = List[Cell]()
        var deleteCells = List[Cell]()
        var playerIsMerge=false
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            //自身cell合并检测
            var cellX = cell.x
            var cellY = cell.y
            var newRadius = cell.radius
            var newMass = cell.newmass
             player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
              val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
              val deg= acos(abs(cell.x-cell2.x)/distance)
              val radiusTotal = cell.radius + cell2.radius
              if (distance < radiusTotal) {
                if ((newSplitTime > System.currentTimeMillis() - mergeInterval) && System.currentTimeMillis()>newSplitTime + 2*1000) {
//                  if (cell.x < cell2.x) cellX = (cellX - ((cell.radius+cell2.radius-distance)*cos(deg))/4).toShort
//                  else if (cell.x > cell2.x) cellX = (cellX + ((cell.radius+cell2.radius-distance)*cos(deg))/4).toShort
//                  if (cell.y < cell2.y) cellY = (cellY - ((cell.radius+cell2.radius-distance)*sin(deg))/4).toShort
//                  else if (cell.y > cell2.y) cellY = (cellY + ((cell.radius+cell2.radius-distance)*sin(deg))/4).toShort

                  if (cell.x < cell2.x) cellX -= 1
                  else if (cell.x > cell2.x) cellX += 1
                  if (cell.y < cell2.y) cellY -= 1
                  else if (cell.y > cell2.y) cellY += 1
                }
//                else if ((distance < radiusTotal / 2)&&(newSplitTime <= System.currentTimeMillis() - mergeInterval)) {
//                  /**融合实质上是吃与被吃的关系：大球吃小球，同等大小没办法融合**/
//                  if (cell.radius > cell2.radius) {
//                    //被融合的细胞不能再被其他细胞融合
//                    if (!mergeCells.exists(_.id == cell2.id) && !mergeCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell.id)) {
//                      playerIsMerge=true
//                      newMass = (newMass + cell2.newmass).toShort
//                      newRadius = Mass2Radius(newMass)
//                      mergeCells = cell2 :: mergeCells
//                    }
//                  }
//                  else if (cell.radius < cell2.radius && !deleteCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell2.id)) {
//                    playerIsMerge=true
//                    newMass = 0
//                    newRadius = 0
//                    deleteCells = cell :: deleteCells
//                  }
//                }
              }
            }
            List(Cell(cell.id, cellX, cellY, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner))
        }.filterNot(e=> e.newmass <= 0 && e.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        if(playerIsMerge) mergeInFlame = true
        player.copy(x = newX.toShort, y = newY.toShort, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    mergeInFlame
  }

  override def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit = {
//    var removeVirus = Map.empty[Long,Virus]
//    playerMap.values.map {
//      player =>
//        var isremoveVirus = false
////        var newSplitTime = player.lastSplit
//        player.cells.sortBy(_.radius).reverse.flatMap {
//          cell =>
////            var newMass = cell.newmass
////            var newRadius = cell.radius
//            if(!mergeInFlame && !isremoveVirus){
//              //病毒碰撞检测: 一个cell只能让一个病毒消失
//              val newvirusMap = virusMap.filter(v => (sqrt(pow(v._2.x - cell.x, 2.0) + pow(v._2.y - cell.y, 2.0)) < cell.radius)).
//                toList.sortBy(v => (sqrt(pow(v._2.x - cell.x, 2.0) + pow(v._2.y - cell.y, 2.0)))).reverse
//              newvirusMap.foreach { vi =>
//                val v = vi._2
//                if (cell.radius > v.radius * 1.2) {
//                  removeVirus += (vi._1->vi._2)
//                  isremoveVirus = true
//                }
//              }
//            }
//              List(cell)
//            List(Cell(cell.id, cell.x, cell.y,cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner))
//        }
//        val length = newCells.length
//        val newX = newCells.map(_.x).sum / length
//        val newY = newCells.map(_.y).sum / length
//        val left = newCells.map(a => a.x - a.radius).min
//        val right = newCells.map(a => a.x + a.radius).max
//        val bottom = newCells.map(a => a.y - a.radius).min
//        val top = newCells.map(a => a.y + a.radius).max
//        player.copy(x = newX.toShort, y = newY.toShort, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
//        player
//    }
//    virusMap --= removeVirus.keySet.toList
  }

  override def checkPlayer2PlayerCrash(): Unit = {}

  override def checkPlayerFoodCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var eaten  = Map[Point, Short]()
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            food.foreach {
              case (p, color) =>
                if (checkCollision(Point(cell.x, cell.y), p, cell.radius, 4, -1)) {
                  //食物被吃掉
                  newMass = (newMass+foodMass).toShort
                  newRadius = Mass2Radius(newMass)
                  food -= p
                  eaten += (p->color)
                  if (newProtected)
                  //吃食物后取消保护
                    newProtected = false
                }
            }
            Cell(cell.id, cell.x, cell.y, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
//        if(player.id.startsWith("user") && eaten.nonEmpty){
//          val score = player.cells.map(_.newmass).toList.sum
//          println(s"${player.id} ${frameCount} ==> ${eaten.keySet} $score ")
//        }
        player.copy(x = newX.toShort, y = newY.toShort, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  override def checkPlayerMassCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            massList.foreach {
              case m: Mass =>
                if (checkCollision(Point(cell.x, cell.y), Point(m.x, m.y), cell.radius, Mass2Radius(shotMass), coverRate)) {
                  if(m.id == player.id && m.speed>0){
                    //小球刚从玩家体内发射出，此时不吃小球
                  }else {
                    newMass = (newMass + shotMass).toShort
                    newRadius = Mass2Radius(newMass)
                    massList = massList.filterNot(l => l == m)
                    if(newProtected)
                      newProtected = false
                  }
                }
            }
            Cell(cell.id, cell.x, cell.y, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX.toShort, y = newY.toShort, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  override def checkVirusMassCrash(): Unit = {
  //TODO 这边病毒的运动有待商榷
  val virus1 = virusMap.flatMap{vi=>
    val v = vi._2
    var newMass = v.mass
    var newRadius = v.radius
    var newSpeed = v.speed
    var newTargetX = v.targetX
    var newTargetY = v.targetY
    var newX = v.x
    var newY = v.y
    var hasMoved = false
    val (nx,ny)= normalization(newTargetX,newTargetY)
    massList.foreach {
      case m: Mass =>
        if (checkCollision(Point(v.x, v.y), Point(m.x, m.y), v.radius, Mass2Radius(shotMass), coverRate)) {
          val (mx,my)=normalization(m.targetX,m.targetY)
          var vx = (nx * newMass * newSpeed + mx * shotMass * m.speed * initVirusRatio ) /(newMass + shotMass)
          var vy = (ny * newMass * newSpeed + my * shotMass * m.speed * initVirusRatio) /(newMass + shotMass)
          newSpeed = sqrt(pow(vx,2)+ pow(vy,2)).toFloat + initVirusSpeed
          val degree =atan2(vy,vx)
          vx = newSpeed * cos(degree)
          vy = newSpeed * sin(degree)
          hasMoved =true
          newMass = (newMass + shotMass).toShort
          newRadius = Mass2Radius(newMass)
          newTargetX = vx.toShort
          newTargetY = vy.toShort
          massList = massList.filterNot(l => l == m)
        }
    }
    if(newMass>virusMassLimit){
      newMass = (newMass/2).toShort
      newRadius = Mass2Radius(newMass)
      //分裂后新生成两个(多的那个由后台发)
      val v1 = vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
      List(v1)
    }else{
      val v1 =vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
      List(v1)
    }
  }
  virusMap ++= virus1
}

  override def checkPlayerSplit(actMap: Map[String,KC], mouseActMap: Map[String, MP]): Unit = {}


  def addUncheckActionWithFrame(id: String, gameAction: UserAction, frame: Int) = {
    uncheckActionWithFrame.put(gameAction.sN,(frame,id,gameAction))
  }


  def setSyncGridData(data:GridDataSync): Unit = {
    actionMap = actionMap.filterKeys(_ > data.frameCount- maxDelayFrame)
    mouseActionMap = mouseActionMap.filterKeys(_ > data.frameCount-maxDelayFrame)
    frameCount = data.frameCount
    playerMap = data.playerDetails.map(s => s.id -> s).toMap
    if(food.nonEmpty && data.eatenFoodDetails.nonEmpty){
      data.eatenFoodDetails.foreach{
        f=>
          food-=Point(f.x,f.y)
      }
    }
    //food改为增量传输这里暂时不用
    massList = data.massDetails
    virusMap = data.virusDetails
  }



  def reStart={
    frameCount = 0
    food = Map[Point, Short]()
    foodPool = 300
    playerMap = Map.empty[String,Player]
    virusMap = Map.empty[Long,Virus]
    massList = List[Mass]()
    tick = 0
    actionMap = Map.empty[Int, Map[String, KC]]
    mouseActionMap = Map.empty[Int, Map[String, MP]]
    deadPlayerMap=Map.empty[Long,Player]
  }

  override def clearAllData() = {
    super.clearAllData
    playerByte2IdMap.clear()
    currentRank = List.empty[RankInfo]
  }


  override def getActionEventMap(frame: Int): List[GameEvent] = {List.empty}

  override def getGameEventMap(frame: Int): List[Protocol.GameEvent] = {List.empty}

}