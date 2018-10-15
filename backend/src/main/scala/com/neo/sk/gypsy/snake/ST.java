package com.neo.sk.gypsy.snake;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;


/**
 * Created by yangxingyuan on 2018/10/14
 */
public class ST extends JPanel {
  int x=40,y=40;
  //  Graphics g;
  Graphics2D g2;
  String s = "123";
  ST(){
    JFrame frame = new JFrame();
    frame.setSize( 800, 600);
    frame.setLayout(null);
    this.setBounds(0, 0, 800, 700);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
//    this.setBackground(Color.GREEN); //画纯色背景

//    this.addMouseListener(new MouseAdapter() {
//      public void mousePressed(MouseEvent e) {
//        // 鼠标按下去会产生一个点
////        y++;
//        s = "preee";
////        g2.drawString("press le",10,10 );
//        repaint();
//      }
//
//    });
    this.addMouseListener(ma);
    this.addKeyListener(ka);

    frame.add(this);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
  public static void main(String[] args) {
    new ST();


  }
  public void paint(Graphics g){
    super.paint(g);
    System.out.println(System.getProperty("user.dir"));
//    Image bg = this.getToolkit().createImage("backend/src/main/resources/img/gold.png");
//    Image bg = this.getToolkit().createImage("./gold.png");
//    Image bg = this.getToolkit().createImage("G:\\gypsy\\backend\\src\\main\\resources\\img\\gold.png");
//    Image bg = this.getToolkit().createImage("resources/img/gold.png");
//    Image bg = this.getToolkit().createImage("../../../../../../resources/img/gold.png");
//    Image bg;
    Image aa = new ImageIcon("backend/src/main/resources/img/gold.png").getImage();
//    Image bg =Toolkit.getDefaultToolkit().getImage(Panel.class.getResource("backend/src/main/resources/img/gold.png"));

    try{
//      bg = ImageIO.read(new URL("http://avatar.profile.csdn.net/5/2/8/2_ufofind.jpg"));
//      bg = ImageIO.read(new URL("http://avatar.profile.csdn.net/5/2/8/2_ufofind.jpg"));
      g.drawImage(aa,50,50,200,200,Color.WHITE,null);
//      g.drawImage(bg,50,50,null);
    }catch (Exception e){
      e.printStackTrace();
    }



    g.setColor(Color.red);
    g.fillOval(40, 40, 20, 20);
//    y++;
    g2 = (Graphics2D) g;
    if(!s.equals("")){
      g2.drawString(s,10,10 );
    }
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
      System.out.println(pressCode);
      g2.drawString("press le",10,10 );
      g2.drawString(String.valueOf(pressCode),10,20);
      repaint();
//      switch(pressCode) {
//        case 'A' :
//
//      }





    }
  };


}
