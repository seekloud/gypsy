package com.neo.sk.gypsy.front.GUI;

/**
 * Created by YXY on Date: 2018/10/15
 */

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GreedSnake implements KeyListener {
  JFrame mainFrame;
  Canvas paintCanvas;
  JLabel labelScore;// 计分牌
  SnakeModel snakeModel = null;// 蛇
  public static final int canvasWidth = 200;
  public static final int canvasHeight = 300;
  public static final int nodeWidth = 10;
  public static final int nodeHeight = 10;
  // ----------------------------------------------------------------------
// GreedSnake():初始化游戏界⾯面
// ----------------------------------------------------------------------
  public GreedSnake() {
// 设置界⾯面元素
    mainFrame = new JFrame("GreedSnake");
    Container cp = mainFrame.getContentPane();
    labelScore = new JLabel("Score:");
    cp.add(labelScore, BorderLayout.NORTH);
    paintCanvas = new Canvas();
    paintCanvas.setSize(canvasWidth + 1, canvasHeight + 1);
    paintCanvas.addKeyListener(this);
    cp.add(paintCanvas, BorderLayout.CENTER);
    JPanel panelButtom = new JPanel();
    panelButtom.setLayout(new BorderLayout());
    JLabel labelHelp;// 帮助信息
    labelHelp = new JLabel("PageUp, PageDown for speed;",
      JLabel.CENTER);
    panelButtom.add(labelHelp, BorderLayout.NORTH);
    labelHelp = new JLabel("ENTER or R or S for start;", JLabel.CENTER);
    panelButtom.add(labelHelp, BorderLayout.CENTER);
    labelHelp = new JLabel("SPACE or P for pause", JLabel.CENTER);
    panelButtom.add(labelHelp, BorderLayout.SOUTH);
    cp.add(panelButtom, BorderLayout.SOUTH);
    mainFrame.addKeyListener(this);
    mainFrame.pack();
    mainFrame.setResizable(false);
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setVisible(true);
    begin();
  }
  // ----------------------------------------------------------------------
// keyPressed():按键检测
// ----------------------------------------------------------------------
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    if (snakeModel.running)
      switch (keyCode) {
        case KeyEvent.VK_UP:
          snakeModel.changeDirection(SnakeModel.UP);
          break;
        case KeyEvent.VK_DOWN:
          snakeModel.changeDirection(SnakeModel.DOWN);
          break;
        case KeyEvent.VK_LEFT:
          snakeModel.changeDirection(SnakeModel.LEFT);
          break;
        case KeyEvent.VK_RIGHT:
          snakeModel.changeDirection(SnakeModel.RIGHT);
          break;
        case KeyEvent.VK_ADD:
        case KeyEvent.VK_PAGE_UP:
          snakeModel.speedUp();// 加速
          break;
        case KeyEvent.VK_SUBTRACT:
        case KeyEvent.VK_PAGE_DOWN:
          snakeModel.speedDown();// 减速
          break;
        case KeyEvent.VK_SPACE:
        case KeyEvent.VK_P:
          snakeModel.changePauseState();// 暂停或继续
          break;
        default:
      }
// 重新开始
    if (keyCode == KeyEvent.VK_R || keyCode == KeyEvent.VK_S
      || keyCode == KeyEvent.VK_ENTER) {
      snakeModel.running = false;
      begin();
    }
  }
  // ----------------------------------------------------------------------
// keyReleased（）：空函数
// ----------------------------------------------------------------------
  public void keyReleased(KeyEvent e) {
  }
  // ----------------------------------------------------------------------
// keyTyped（）：空函数
// ----------------------------------------------------------------------
  public void keyTyped(KeyEvent e) {
  }
  // ----------------------------------------------------------------------
// repaint（）：绘制游戏界⾯面（包括蛇和⻝⾷食物）
// ----------------------------------------------------------------------
  void repaint() {
    Graphics g = paintCanvas.getGraphics();
// draw background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, canvasWidth, canvasHeight);
// draw the snake
    g.setColor(Color.BLACK);
    LinkedList na = snakeModel.nodeArray;
    Iterator it = na.iterator();
    while (it.hasNext()) {
      Node n = (Node) it.next();
      drawNode(g, n);
    }
// draw the food
    g.setColor(Color.RED);
    Node n = snakeModel.food;
    drawNode(g, n);
    updateScore();
  }
  // ----------------------------------------------------------------------
// drawNode（）：绘画某⼀一结点（蛇身或⻝⾷食物）
// ----------------------------------------------------------------------
  private void drawNode(Graphics g, Node n) {
    g.fillRect(n.x * nodeWidth, n.y * nodeHeight, nodeWidth - 1,
      nodeHeight - 1);
  }
  // ----------------------------------------------------------------------
// updateScore（）：改变计分牌
// ----------------------------------------------------------------------
  public void updateScore() {
    String s = "Score: " + snakeModel.score;
    labelScore.setText(s);
  }
  // ----------------------------------------------------------------------
// begin（）：游戏开始，放置贪吃蛇
// ----------------------------------------------------------------------
  void begin() {
    if (snakeModel == null || !snakeModel.running) {
      snakeModel = new SnakeModel(this, canvasWidth / nodeWidth,
        this.canvasHeight / nodeHeight);
      (new Thread(snakeModel)).start();
    }
  }
  // ----------------------------------------------------------------------
// main（）：主函数
// ----------------------------------------------------------------------
  public static void main(String[] args) {
    GreedSnake gs = new GreedSnake();
  }
}