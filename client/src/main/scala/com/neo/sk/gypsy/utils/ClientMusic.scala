package com.neo.sk.gypsy.utils
import java.io.File

import javafx.scene.media._
import javafx.application.Application
import javafx.embed.swing.JFXPanel


/**
  * @author zhaoyin
  * 2018/11/23  2:01 PM
  */
object ClientMusic {

  new JFXPanel
  val bgurI = new File("client/src/main/resources/music/bg.mp3").toURI.toString
  private val bg = new Media(bgurI)
  private val bgPlayer = new MediaPlayer(bg)

  def playMusic(name:String)={
    name match {
      case "bg" =>
        bgPlayer.play()
      case x =>
        val urI = new File(s"client/src/main/resources/music/$x.mp3").toURI.toString
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