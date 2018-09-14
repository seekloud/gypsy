package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.shared.Grid
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.GameAction
import com.neo.sk.gypsy.shared.ptcl.{Cell, Point}

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

  def addActionWithFrameFromServer(id:Long,gameAction:GameAction) = {
    val frame=gameAction.frame
    if(myId == id){
      uncheckActionWithFrame.get(gameAction.serialNum) match {
        case Some((f,tankId,a)) =>
          if(f == frame){ //与预执行的操作数据一致
            println("---------11")
            uncheckActionWithFrame.remove(gameAction.serialNum)
          }else{ //与预执下的操作数据不一致，进行回滚
            uncheckActionWithFrame.remove(gameAction.serialNum)
            if(frame < grid.frameCount){
              rollback(frame)
            }else{
              grid.removeActionWithFrame(tankId,a,f)
              gameAction match {
                case a:KeyCode=>
                  grid.addActionWithFrame(id,a,frame)
                case b:MousePosition=>
                  grid.addMouseActionWithFrame(id,b,frame)
              }
            }
          }
        case None =>
          gameAction match {
            case a:KeyCode=>
              grid.addActionWithFrame(id,a,frame)
            case b:MousePosition=>
              grid.addMouseActionWithFrame(id,b,frame)
          }
      }
    }else{
      if(frame < grid.frameCount && grid.frameCount - maxRollBackFrames >= frame){
        //回滚
        println("--------2")
        rollback(frame)
      }else{
        println("-------1")
        gameAction match {
          case a:KeyCode=>
            grid.addActionWithFrame(id,a,frame)
          case b:MousePosition=>
            grid.addMouseActionWithFrame(id,b,frame)
        }
      }
    }
  }

  def rollback2State(d:GridDataSync) = {
    grid.actionMap=grid.actionMap.filterKeys(_>=grid.frameCount)
    grid.mouseActionMap=grid.mouseActionMap.filterKeys(_>=grid.frameCount)
    setSyncGridData(d)
  }


  //从第frame开始回滚到现在
  def rollback(frame:Long) = {
    gameSnapshotMap.get(frame) match {
      case Some(state) =>
        val curFrame = grid.frameCount
        rollback2State(state)
        uncheckActionWithFrame.filter(_._2._1 > frame).foreach{t=>
          t._2._3 match {
            case a:KeyCode=>
              grid.addActionWithFrame(t._2._2,a,frame)
            case b:MousePosition=>
              grid.addMouseActionWithFrame(t._2._2,b,frame)
          }
        }
        (frame until curFrame).foreach{ f =>
          grid.frameCount = f
          update()
        }
      case None =>
    }
  }
}