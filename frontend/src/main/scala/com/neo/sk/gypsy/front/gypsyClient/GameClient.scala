package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.shared.Grid
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{GameAction, GridDataSync, KeyCode, MousePosition,maxDelayFrame}
import com.neo.sk.gypsy.shared.ptcl.{Cell, Point, Score}

import scala.collection.mutable
import scala.math.{pow, sqrt}

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

  var currentRank = List.empty[Score]
  //fixme 此处变量未有实际用途
  var historyRank = List.empty[Score]

  private[this] val uncheckActionWithFrame = new mutable.HashMap[Int,(Long,Long,GameAction)]()
  private[this] val gameSnapshotMap = new mutable.HashMap[Long,GridDataSync]()

  override def checkPlayer2PlayerCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var killer = 0l
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
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
    playerMap = newPlayerMap.map { s=>(s.id,s)}.toMap

  }

  def addUncheckActionWithFrame(id: Long, gameAction: GameAction, frame: Long) = {
    uncheckActionWithFrame.put(gameAction.serialNum,(frame,id,gameAction))
  }

  def addActionWithFrameFromServer(id:Long,gameAction:GameAction) = {
    val frame=gameAction.frame
    if(myId == id){
      uncheckActionWithFrame.get(gameAction.serialNum) match {
        case Some((f,tankId,a)) =>
          if(f == frame){ //fixme 此处存在advanceFrame差异
            uncheckActionWithFrame.remove(gameAction.serialNum)
          }else{ //与预执下的操作数据不一致，进行回滚
            uncheckActionWithFrame.remove(gameAction.serialNum)
            if(frame < frameCount){
              rollback(frame)
            }else{
              removeActionWithFrame(tankId,a,f)
              gameAction match {
                case a:KeyCode=>
                  addActionWithFrame(id,a)
                case b:MousePosition=>
                  addMouseActionWithFrame(id,b)
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
        rollback(frame)
      }else{
        gameAction match {
          case a:KeyCode=>
            addActionWithFrame(id,a)
          case b:MousePosition=>
            addMouseActionWithFrame(id,b)
        }
      }
    }
  }

  def rollback2State(d:GridDataSync) = {
    actionMap=actionMap.filterKeys(_>=frameCount)
    mouseActionMap=mouseActionMap.filterKeys(_>=frameCount)
    setSyncGridData(d)
  }

  def setSyncGridData(data:GridDataSync): Unit = {

    actionMap = actionMap.filterKeys(_ > data.frameCount- maxDelayFrame)
    mouseActionMap = mouseActionMap.filterKeys(_ > data.frameCount-maxDelayFrame)
    //    println(s"前端帧${grid.frameCount}，后端帧${data.frameCount}")
    frameCount = data.frameCount
    //    println(s"**********************前端帧${grid.frameCount}，后端帧${data.frameCount}")
    playerMap = data.playerDetails.map(s => s.id -> s).toMap
    food = data.foodDetails.map(a => Point(a.x, a.y) -> a.color).toMap
    massList = data.massDetails
    virus = data.virusDetails

  }


  //从第frame开始回滚到现在
  def rollback(frame:Long) = {
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
  }
}