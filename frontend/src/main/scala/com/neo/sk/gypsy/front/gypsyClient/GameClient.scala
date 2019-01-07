package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.shared.Grid
import com.neo.sk.gypsy.shared.util.utils.{checkCollision, normalization}
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

  override def debug(msg: String): Unit = println(msg)

  override def info(msg: String): Unit = println(msg)

  override def feedApple(appleCount: Int): Unit = {} //do nothing.
  override def addVirus(v: Int): Unit = {}
  var currentRank = List.empty[RankInfo]
  //序列号->(frame,Id,GameAction)
  private[this] val uncheckActionWithFrame = new mutable.HashMap[Int,(Long,String,UserAction)]()
  private[this] val gameSnapshotMap = new mutable.HashMap[Long,GridDataSync]()

  override def getAllGridData: GridDataSync={
    GridDataSync(0l, Nil, Nil, Map.empty, 1.0,Nil)
  }



//  override def checkCellMerge(): Boolean = {false}
  override def checkCellMerge: Boolean = {
    var mergeInFlame = false
    val newPlayerMap = playerMap.values.map {
      player =>
        val newSplitTime = player.lastSplit
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            //自身cell合并检测
            var cellX = cell.x
            var cellY = cell.y
             player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
              val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
              val deg= acos(abs(cell.x-cell2.x)/distance)
              val radiusTotal = cell.radius + cell2.radius
              if (distance < radiusTotal) {
                if (newSplitTime > System.currentTimeMillis() - mergeInterval) {
                  if (cell.x < cell2.x) cellX -= ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
                  else if (cell.x > cell2.x) cellX += ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
                  if (cell.y < cell2.y) cellY -= ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
                  else if (cell.y > cell2.y) cellY += ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
                }
              }
            }
            List(Cell(cell.id, cellX, cellY, cell.mass, cell.newmass, cell.radius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner))
        }
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
    mergeInFlame
  }

  override def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit = {
    var removeVirus = Map.empty[Long,Virus]
    playerMap.values.map {
      player =>
        var newSplitTime = player.lastSplit
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var vSplitCells = List[Cell]()
            var newMass = cell.newmass
            var newRadius = cell.radius
            //病毒碰撞检测
            virusMap.foreach { vi =>
              val v = vi._2
              if ((sqrt(pow(v.x - cell.x, 2.0) + pow(v.y - cell.y, 2.0)) < cell.radius) && (cell.radius > v.radius * 1.2) && !mergeInFlame) {
                removeVirus += (vi._1->vi._2)
              }
            }
            List(Cell(cell.id, cell.x, cell.y,cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)) ::: vSplitCells
        }

        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
    }
    virusMap --= removeVirus.keySet.toList
  }

  override def checkPlayer2PlayerCrash(): Unit = {}

  override def checkPlayerFoodCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
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
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  food -= p
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
        player.copy(x = newX, y = newY, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
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
              case p: Mass =>
                if (checkCollision(Point(cell.x, cell.y), Point(p.x, p.y), cell.radius, p.radius, coverRate)) {
                  newMass = (newMass + p.mass).toShort
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  massList = massList.filterNot(l => l == p)
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
        player.copy(x = newX, y = newY, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
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
      case p: Mass =>
        if (checkCollision(Point(v.x, v.y), Point(p.x, p.y), v.radius, p.radius, coverRate)) {
          val (mx,my)=normalization(p.targetX,p.targetY)
          val vx = (nx*newMass*newSpeed + mx*p.mass*p.speed)/(newMass+p.mass)
          val vy = (ny*newMass*newSpeed + my*p.mass*p.speed)/(newMass+p.mass)
          hasMoved =true
          newMass += p.mass
          newRadius = 4 + sqrt(newMass) * mass2rRate
          newSpeed = sqrt(pow(vx,2)+ pow(vy,2))
          newTargetX = vx
          newTargetY = vy
          massList = massList.filterNot(l => l == p)
        }
    }
    if(newMass>virusMassLimit){
      newMass = newMass/2
      newRadius = 4 + sqrt(newMass) * mass2rRate
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

  def addUncheckActionWithFrame(id: String, gameAction: UserAction, frame: Long) = {
    uncheckActionWithFrame.put(gameAction.serialNum,(frame,id,gameAction))
  }

/*  def addActionWithFrameFromServer(id:String,gameAction:UserAction) = {
    val frame=gameAction.frame
    if(myId == id){
      uncheckActionWithFrame.get(gameAction.serialNum) match {
        case Some((f,playerId,a)) =>
          if(f == frame){ //fixme 此处存在advanceFrame差异
            uncheckActionWithFrame.remove(gameAction.serialNum)
          }else{ //与预执下的操作数据不一致，进行回滚
            uncheckActionWithFrame.remove(gameAction.serialNum)
            if(frame < frameCount){
//              rollback(frame)
            }else{
              removeActionWithFrame(playerId,a,f)
              gameAction match {
                case a:KeyCode=>
                  addActionWithFrame(id,a)
                case b:MousePosition=>
                  addMouseActionWithFrame(id,b)
                case _ =>
              }
            }
          }
        case None =>
          gameAction match {
            case a:KeyCode=>
              addActionWithFrame(id,a)
            case b:MousePosition=>
              addMouseActionWithFrame(id,b)
          }
      }
    }else{
      if(frame < frameCount && frameCount - maxDelayFrame >= frame){
        //回滚
//        rollback(frame)
      }else{
        gameAction match {
          case a:KeyCode=>
            addActionWithFrame(id,a)
          case b:MousePosition=>
            addMouseActionWithFrame(id,b)
        }
      }
    }
  }*/

/*  def rollback2State(d:GridDataSync) = {
    actionMap=actionMap.filterKeys(_>=frameCount)
    mouseActionMap=mouseActionMap.filterKeys(_>=frameCount)
    setSyncGridData(d)
  }*/

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

  //从第frame开始回滚到现在
/*  def rollback(frame:Long) = {
    gameSnapshotMap.get(frame) match {
      case Some(state) =>
        val curFrame = frameCount
        rollback2State(state)
        uncheckActionWithFrame.filter(_._2._1 > frame).foreach{t=>
          t._2._3 match {
            case a:KeyCode=>
              addActionWithFrame(t._2._2,a)
            case b:MousePosition=>
              addMouseActionWithFrame(t._2._2,b)
          }
        }
        (frame until curFrame).foreach{ f =>
          frameCount = f
          update()
        }
      case None =>
    }
  }*/

  def reStart={
    myId = ""
    frameCount = 0l
    food = Map[Point, Int]()
    foodPool = 300
    playerMap = Map.empty[String,Player]
    virusMap = Map.empty[Long,Virus]
    massList = List[Mass]()
    tick = 0
    actionMap = Map.empty[Long, Map[String, KeyCode]]
    mouseActionMap = Map.empty[Long, Map[String, MousePosition]]
    deadPlayerMap=Map.empty[Long,Player]
  }

  override def getActionEventMap(frame: Long): List[GameEvent] = {List.empty}

  override def getGameEventMap(frame: Long): List[Protocol.GameEvent] = {List.empty}

}