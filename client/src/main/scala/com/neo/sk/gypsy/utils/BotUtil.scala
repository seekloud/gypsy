package com.neo.sk.gypsy.utils

import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.common.AppSettings.botSecure
import javafx.embed.swing.SwingFXUtils
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
/**
  * @author zhaoyin
  * 2018/12/11  3:02 PM
  */
object BotUtil {

  def checkBotToken(apiToken: String) = {
    if(apiToken == botSecure)
      true
    else
      false
  }

  val emptyArray = new Array[Byte](0)

  val params = new SnapshotParameters
  def canvas2byteArray(canvas: Canvas):Array[Byte] = {
    try {
      val w = canvas.getWidth.toInt
      val h = canvas.getHeight.toInt
      //      val wi = new WritableImage(w, h)
      //      params.setFill(Color.TRANSPARENT)
      val wi = canvas.snapshot(params, null) //从画布中复制绘图并复制到writableImage
      val reader = wi.getPixelReader
      if(!AppSettings.isGray) {
        val byteBuffer = ByteBuffer.allocate(4 * w * h)
        for (y <- 0 until h; x <- 0 until w) {
          val color = reader.getArgb(x, y)
          byteBuffer.putInt(color)
        }
        byteBuffer.flip()
        byteBuffer.array().take(byteBuffer.limit)
      } else {
        //获取灰度图，每个像素点1Byte
        val byteArray = new Array[Byte](1 * w * h)
        for (y <- 0 until h; x <- 0 until w) {
          val color = reader.getColor(x, y).grayscale()
          val gray = (color.getRed * 255).toByte
          byteArray(y * h + x) = gray
        }
        byteArray
      }
    } catch {
      case e: Exception=>
        emptyArray
    }
  }


}
