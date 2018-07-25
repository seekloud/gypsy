package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.shared.ptcl.Grid
import com.neo.sk.gypsy.shared.ptcl.Point
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
}