package com.neo.sk.gypsy.common

import java.awt.event.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import scala.math._

import org.seekloud.esheepapi.pb.actions.{Move, Swing}


object Constant {



  object ColorsSetting {
    val backgroundColor: Color = Color.rgb(245, 245, 245)
    val fontColor: Color = Color.BLACK
    val gameNameColor: Color = Color.rgb(91, 196, 140)
    val defaultColor: Color = Color.rgb(0, 0, 128)
    val borderColor: Color = Color.rgb(105, 105, 105)
    val mapColor: Color = Color.rgb(192, 192, 192)
    val redColor: Color = Color.RED
    val greenColor: Color = Color.GREEN
    val yellowColor: Color = Color.YELLOW
    val dieInfoBackgroundColor: Color = Color.rgb(51, 51, 51)
    val dieInfoFontColor: Color = Color.rgb(224, 238, 253)
    val scoreColor: Color = Color.rgb(119,205,251)
    val splitNumColor: Color = Color.rgb(246,186,113)
  }

  def hex2Rgb(hex: String) = {
    val red = hexToDec(hex.slice(1, 3))
    val green = hexToDec(hex.slice(3, 5))
    val blue = hexToDec(hex.takeRight(2))
    Color.rgb(red, green, blue)
  }

  def hexToDec(hex: String): Int = {
    val hexString: String = "0123456789ABCDEF"
    var target = 0
    var base = Math.pow(16, hex.length - 1).toInt
    for (i <- 0 until hex.length) {
      target = target + hexString.indexOf(hex(i)) * base
      base = base / 16
    }
    target
  }

  def moveToKeyCode(move: Move): Int = {
    move match {
      case Move.left => KeyEvent.VK_LEFT
      case Move.up => KeyEvent.VK_UP
      case Move.right => KeyEvent.VK_RIGHT
      case Move.down => KeyEvent.VK_DOWN
      case _ => -1
    }
  }

  def swingToXY(swing:Swing):(Int,Int)={
    val angle = swing.radian
    val distan = swing.distance
    ((cos(angle)*distan).toInt,(sin(angle)*distan).toInt)
  }


  val CanvasWidth = 800
  val CanvasHeight = 600

  val layeredCanvasWidth = 400
  val layeredCanvasHeight = 200

//  val layeredCanvasWidth = 300
//  val layeredCanvasHeight = 150

  val informWidth = 15

  val humanCanvasWidth = 800
  val humanCanvasHeight = 400

  val viewRatio = humanCanvasHeight/layeredCanvasHeight



  //  val humanCanvasWidth = 400
//  val humanCanvasHeight = 200

}
