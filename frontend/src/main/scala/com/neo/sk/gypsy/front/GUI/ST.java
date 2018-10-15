package com.neo.sk.gypsy.front.GUI;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import javax.swing.*;


/**
 * Created by yangxingyuan on 2018/10/14
 */
public class ST extends JPanel {
  int x=40,y=40;
  //  Graphics g;
  Graphics2D g2;
  String s = "123";
  Shape shape;
  Shape shapef;

  Image bg = new ImageIcon("backend/src/main/resources/img/b2.jpg").getImage();




  ST(){
    JFrame frame = new JFrame();
    //开始时窗口大小
    frame.setSize( 800, 600);
    frame.setLayout(null);
    //画布大小，起始位置
    this.setBounds(100, 100, 800, 700);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    //加这句为了获取键盘的监听事件，但网上又说要写setVisible(true)后面才行，试了下加前面也是可以
    this.setFocusable(true);
    this.setBackground(Color.WHITE); //画纯色背景

/*
    当使用addMouseListener()方法添加MouseAdpter时有效

    mouseClicked(MouseEvent e) 
    mouseEntered(MouseEvent e) --------当鼠标进入（添加了监听器的）组件时调用，包括子组件
    mouseExited(MouseEvent e) ----------当鼠标离开（添加了监听器的）组件时调用，包括子组件
    mousePressed(MouseEvent e) 
    mouseReleased(MouseEvent e) 

    当使用addMouseMotionListener()方法添加MouseAdapter时有效
    mouseDragged(MouseEvent e) 
    mouseMoved(MouseEvent e) 
    ---------------------
    原文：https://blog.csdn.net/zzidea/article/details/9849043?utm_source=copy
*/

//    this.addMouseListener(ma);   //鼠标点击进入等事件监听用这个
//    鼠标拖动移动这两个用这个
    this.addMouseMotionListener(ma);
    this.addKeyListener(ka);

    frame.add(this);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
  public static void main(String[] args) {
    new ST();
  }
  public void paint(Graphics g){
    super.paint(g);

//    绘制背景
    g.drawImage(bg,0,0,this.getWidth(),this.getWidth(),null);
    //以下g的绘图貌似参数的数据类型都是int，如果要用float请看g2的绘图

    //设置颜色
    g.setColor(Color.red);
    //绘制实心圆(int x, int y, int width, int height)
    g.fillOval(40, 40, 20, 20);
    //绘制空心圆(参数同上)
    g.drawOval(40, 70, 20, 20);

    // 绘制圆弧(int x, int y, int width, int height,int startAngle, int arcAngle)
    g.drawArc(40, 90, 100, 50, 270, 50);

    // 绘制线段(int x1, int y1, int x2, int y2)
    g.drawLine(40, 110, 80, 110);

    g.setColor(Color.BLUE);
    // 绘制矩形(int x, int y, int width, int height)
    g.fillRect(80, 40, 100, 50);

    // 绘制字符串
    g.drawString("Hello Swing", 80, 100);




    g2 = (Graphics2D) g;
    Shape s01 = new Ellipse2D.Float(50, 110, 20, 20);
    g2.draw(s01);

    //设置线条的粗细
//    Stroke stroke03 = new BasicStroke(5);
//    g2.setStroke(stroke03);
    //g2画空心
    if(shape != null){
      g2.draw(shape);
    }
    //g2画实心
    if(shapef != null){
      g2.fill(shapef);
    }

    g.drawString(s,200,200);

//    if(!s.equals("")){
//      g2.drawString(s,10,10 );
//    }
//    g2.drawString("press le",10,10 );
//    repaint();
//    try {
//      Thread.sleep(10);
//    } catch (InterruptedException e) {
//// TODO 自动生成的 catch 块
//      e.printStackTrace();
//    }
  }


  MouseAdapter ma = new MouseAdapter() {
    public void mousePressed(MouseEvent e) {
      // 鼠标按下去会产生一个点
      s="mouse click!";
      repaint();
    }

    public void mouseMoved(MouseEvent e){
      s= "point:"+"("+e.getX()+","+e.getY()+")";
      repaint();
    }

  };


  /*  keyPressed(KeyEvent e)
  按下某个键时调用此方法。
  void
  keyReleased(KeyEvent e)
  释放某个键时调用此方法。
  void
  keyTyped(KeyEvent e)
  键入某个键时调用此方法。*/


  KeyAdapter ka = new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
      char pressCode = e.getKeyChar();
//      s+=pressCode;
      System.out.println(pressCode);
      switch(pressCode){
//        画圆
        case 'o':
//          Float和Double
//          shape =new Ellipse2D.Float(100, 100, 20, 20);
          shape =new Ellipse2D.Double(200, 200, 20, 20);
          break;
        case 'l':
          shape = new Line2D.Float(200, 200, 200, 250);
          break;
        case 'a':
          /*
          * Arc2D.OPEN ：圆弧
          *
          * Arc2D.CHORD：圆弧头尾用线连起来
          *
          * Arc2D.PIE：扇形
          * */
          shape = new Arc2D.Float(200, 200, 40, 40, 0, 90, Arc2D.OPEN);
          break;

//        new Rectangle2D.Float(10, 250, 130, 30);
        case 'r':
//          shape = new RoundRectangle2D.Double(10, 250, 130, 30);
          shape = new Rectangle2D.Float(10, 250, 130, 30);
          shapef = new Rectangle2D.Float(150, 250, 130, 30);
          break;
      }

      repaint();
    }
  };




}