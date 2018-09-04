package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.shared.ptcl.{Cell, Grid, Player, Point}

import scala.math.{pow, sqrt}
/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 10:13 PM
  */
class GridOnClient(override val boundary: Point) extends Grid {

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
}
