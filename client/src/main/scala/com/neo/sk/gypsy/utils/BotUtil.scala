package com.neo.sk.gypsy.utils

import java.awt.image.BufferedImage
import java.nio.ByteBuffer

import com.neo.sk.gypsy.common.AppSettings.botSecure
import javafx.embed.swing.SwingFXUtils
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
/**
  * @author zhaoyin
  * 2018/12/11  3:02 PM
  */
object BotUtil {

  def checkBotToken(apiToken: String) = {
    if(apiToken == botSecure._2)
      true
    else
      false
  }

  val emptyArray = new Array[Byte](0)

  def canvas2byteArray(canvas: Canvas):Array[Byte] = {
    try {
      val params = new SnapshotParameters
      val w = canvas.getWidth.toInt
      val h = canvas.getHeight.toInt
      val wi = new WritableImage(w, h)
      val bi = new BufferedImage(w, h, 2)
      canvas.snapshot(params, wi) //从画布中复制绘图并复制到writableImage
      SwingFXUtils.fromFXImage(wi, bi)
      val argb =  bi.getRGB(0, 0, w, h, null, 0, w)
      //TODO 这里800*400 总觉得应该改成w*h
      val byteBuffer = ByteBuffer.allocate(4 * 800 * 400)
      argb.foreach{ e =>
        byteBuffer.putInt(e)
      }
      byteBuffer.flip()
      byteBuffer.array().take(byteBuffer.limit)
      argb
      new Array[Byte](0)
    }catch {
      case e: Exception=>
        emptyArray
    }
  }


}
