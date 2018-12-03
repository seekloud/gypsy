package com.neo.sk.gypsy.utils
import java.io.File

import javafx.scene.media._
import javafx.embed.swing.JFXPanel
import com.neo.sk.gypsy.ClientBoot

/**
  * @author zhaoyin
  * 2018/11/23  2:01 PM
  */
object ClientMusic {

  new JFXPanel
  val bgurI = ClientBoot.getClass.getResource("/music/bg.mp3").toString
  private val bg = new Media(bgurI)
  private val bgPlayer = new MediaPlayer(bg)

  def playMusic(name:String)={
    name match {
      case "bg" =>
        bgPlayer.play()
      case x =>
        val urI = ClientBoot.getClass.getResource(s"/music/$x.mp3").toString
        val media = new Media(urI)
        val mPlayer = new MediaPlayer(media)
        mPlayer.play()
    }

  }

  def stopMusic() = {
    bgPlayer.pause()
  }

  def main(args: Array[String]): Unit = {
    println(bgurI)
    playMusic("bg")
  }

}
