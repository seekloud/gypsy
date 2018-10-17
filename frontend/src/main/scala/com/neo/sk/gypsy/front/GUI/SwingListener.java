package com.neo.sk.gypsy.front.GUI;

import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;


/**
 * Created by YXY on Date: 2018/10/16
 */
public class SwingListener implements KeyListener, MouseListener,MouseMotionListener {

  ST st;

  SwingListener(ST st){
    this.st = st;
  }


  @Override
  public void keyTyped(KeyEvent e) {

  }

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
        st.shape =new Ellipse2D.Double(200, 200, 20, 20);
        break;
      case 'l':
        st.shape = new Line2D.Float(200, 200, 200, 250);
        break;
      case 'a':
          /*
          * Arc2D.OPEN ：圆弧
          *
          * Arc2D.CHORD：圆弧头尾用线连起来
          *
          * Arc2D.PIE：扇形
          * */
        st.shape = new Arc2D.Float(200, 200, 40, 40, 0, 90, Arc2D.OPEN);
        break;

//        new Rectangle2D.Float(10, 250, 130, 30);
      case 'r' :
//          shape = new RoundRectangle2D.Double(10, 250, 130, 30);
        st.shape = new Rectangle2D.Float(10, 250, 130, 30);
        st.shapef = new Rectangle2D.Float(150, 250, 130, 30);
        break;
      case KeyEvent.VK_ENTER:
        st.tfShow = st.jf.getText();
        System.out.println("enter!");
        break;

    }

    st.repaint();
  }

  @Override
  public void keyReleased(KeyEvent e) {

  }

  @Override
  public void mouseClicked(MouseEvent e) {

  }

  @Override
  public void mousePressed(MouseEvent e) {

  }

  @Override
  public void mouseReleased(MouseEvent e) {

  }

  @Override
  public void mouseEntered(MouseEvent e) {

  }

  @Override
  public void mouseExited(MouseEvent e) {

  }

  @Override
  public void mouseDragged(MouseEvent e) {

  }

  @Override
  public void mouseMoved(MouseEvent e) {
    st.s= "point:"+"("+e.getX()+","+e.getY()+")";
    st.repaint();
  }
}
