package com.neo.sk.gypsy.holder

import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.model.GridOnClient
import javafx.scene.input.KeyCode
import javafx.util.Duration
/**
  * @author zhaoyin
  * @date 2018/10/29  5:13 PM
  */
object GameHolder {

  val bounds = Point(Boundary.w,Boundary.h)
  val grid = new GridOnClient(bounds)

  private[this] val watchKeys = Set(
    KeyCode.E,
    KeyCode.F,
    KeyCode.SPACE,
    KeyCode.LEFT,
    KeyCode.UP,
    KeyCode.RIGHT,
    KeyCode.DOWN,
    KeyCode.ESCAPE
  )

}
class GameHolder() {
  import GameHolder._

  def start()={
    println("start---")
    val animationTimer = new AnimationTimer() {
      override def handle(now: Long): Unit = {
        //todo 游戏渲染
        gameScene.draw()
      }
    }
    val timeline = new Timeline()
    timeline.setCycleCount(Animation.INDEFINITE)
    val keyFrame = new KeyFrame(Duration.millis(100),{ _ =>
      //todo 游戏循环
      gameLoop()
    })
    timeline.getKeyFrames.add(keyFrame)
    animationTimer.start()
    timeline.play()
  }

}
