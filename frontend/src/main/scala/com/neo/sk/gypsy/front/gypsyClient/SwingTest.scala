package com.neo.sk.gypsy.front.gypsyClient

import java.awt.event.{MouseAdapter, MouseListener}
import java.awt._
import javax.swing.JFrame
import java.awt.Button
import javax.swing.JButton
import javax.swing.JLabel

import scala.swing.event.{KeyEvent, MouseEvent}

//import scala.swing.Frame

/**
  * Created by yangxingyuan on 2018/10/14
  */
object SwingTest extends JFrame {
  setTitle("Test")
  //设置大小
  setSize(300,300)
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  setVisible(true)
  val tf = new TextField("abcd")

  val label = new JLabel("Hello World")
  val canvas = new Canvas()
  //  canvas.addMouseListener(mouseListener)

  canvas.addMouseListener(new MouseAdapter() {

    override def mousePressed(e: event.MouseEvent): Unit = {
      //      ctx.setColor(Color.RED)
      //      ctx.drawLine(0,0,10,10)
      //      label.setText(s"I press X:${e.getX} Y:${e.getY}")
      // 鼠标按下去会产生一个点
      //      startDrag = new Point(e.point.getX, e.point.getY);
      //      endDrag = startDrag;
      //      repaint();
    }

  })
  var s = ""
  def main(args: Array[String]): Unit = {
    //    JFrame.setDefaultLookAndFeelDecorated(true)
    //    val frame = new JFrame("Test")
    //    frame.setSize(300,300)
    //    frame.add(label)
    val ctx = canvas.getGraphics
    //    val frame = new SwingTest()
    //    this.add(label, BorderLayout.NORTH)
    //    val ctx = canvas.getGraphics
    this.add(canvas,BorderLayout.CENTER)
    //canvas.getGraphics.drawLine(0,0,10,10)
    //    val ctx2 = ctx.asInstanceOf[Graphics2D]
    //    ctx2.setPaint(Color.RED)
    //    ctx2.drawString("asba",10,10)
    ctx.setColor(Color.RED)
    ctx.drawLine(0,0,10,10)


    //    frame.setVisible(true)
  }




  /*val mouseListener = new MouseAdapter() {
    override def mousePressed(e: event.MouseEvent): Unit = {
      label.setText(s"I press X:${e.getX} Y:${e.getY}")
      // 鼠标按下去会产生一个点


      //      startDrag = new Point(e.point.getX, e.point.getY);
      //      endDrag = startDrag;
      //      repaint();
    }
  }*/


  // 按键按下// 按键按下
  def keyPressed(e: KeyEvent): Unit = {
    label.setText("keyReleased")
  }


  // 按键抬起
  def keyReleased(e: KeyEvent): Unit = {
    label.setText("keyReleased")
    //    tf.setText("keyReleased")
  }


  // 响应键盘事件
  def keyTyped(e: KeyEvent): Unit = {
    //    tf.setText("keyTyped")
    label.setText("keyTyped")
    s += e.peer.getKeyChar
    canvas.getGraphics.drawString(s, 0, 20)
  }


  // 鼠标点击
  def mouseClicked(e: MouseEvent): Unit = {
    //    tf.setText("mouseClicked")
    label.setText("mouseClicked")
    // 画布取得焦点
    canvas.requestFocus
  }

  def mouseExit(e: MouseEvent)= {

  }

  // 鼠标进入
  def mouseEntered(e: MouseEvent): Unit = {
    //    tf.setText("mouseEntered")
    label.setText("mouseEntered")
  }


  // 鼠标退出
  def mouseExited(e: MouseEvent): Unit = {
    //    tf.setText("mouseExited")
    label.setText("mouseExited")
  }


  // 鼠标按下
  def mousePressed(e: MouseEvent): Unit = {
    //    tf.setText("mousePressed")
    label.setText("mousePressed")
  }


  // 鼠标松开
  def mouseReleased(e: MouseEvent): Unit = {
    //    tf.setText("mouseReleased")
    label.setText("mouseReleased")
  }


  def mouseDragged(e: MouseEvent): Unit = {
  }


  def mouseMoved(e: MouseEvent): Unit = {
  }

}
