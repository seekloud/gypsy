package com.neo.sk.gypsy.front.GUI;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import javax.swing.JOptionPane;

public class SnakeModel implements Runnable {
  GreedSnake gs;
  boolean[][] matrix;// 界⾯面数据保存在数组⾥里里
  LinkedList nodeArray = new LinkedList();
  Node food;
  int maxX;// 最⼤大宽度
  int maxY;// 最⼤大⻓长度
  int direction = 2;// ⽅方向
  boolean running = false;
  int timeInterval = 200;// 间隔时间（速度）
  double speedChangeRate = 0.75;// 速度改变程度
  boolean paused = false;// 游戏状态
  int score = 0;
  int countMove = 0;
  // UP和DOWN是偶数，RIGHT和LEFT是奇数
  public static final int UP = 2;
  public static final int DOWN = 4;
  public static final int LEFT = 1;
  public static final int RIGHT = 3;
  // ----------------------------------------------------------------------
// GreedModel():初始化界⾯面
// ----------------------------------------------------------------------
  public SnakeModel(GreedSnake gs, int maxX, int maxY) {
    this.gs = gs;
    this.maxX = maxX;
    this.maxY = maxY;
    matrix = new boolean[maxX][];
    for (int i = 0; i < maxX; ++i) {
      matrix[i] = new boolean[maxY];
      Arrays.fill(matrix[i], false);// 没有蛇和⻝⾷食物的地区置false
    }
// 初始化贪吃蛇
    int initArrayLength = maxX > 20 ? 10 : maxX / 2;
    for (int i = 0; i < initArrayLength; ++i) {
      int x = maxX / 2 + i;
      int y = maxY / 2;
      nodeArray.addLast(new Node(x, y));
      matrix[x][y] = true;// 蛇身处置true
    }
    food = createFood();
    matrix[food.x][food.y] = true;// ⻝⾷食物处置true
  }
  // ----------------------------------------------------------------------
// changeDirection():改变运动⽅方向
// ----------------------------------------------------------------------
  public void changeDirection(int newDirection) {
    if (direction % 2 != newDirection % 2)// 避免冲突
    {
      direction = newDirection;
    }
  }
  // ----------------------------------------------------------------------
// moveOn():贪吃蛇运动函数
// ----------------------------------------------------------------------
  public boolean moveOn() {
    Node n = (Node) nodeArray.getFirst();
    int x = n.x;
    int y = n.y;
    switch (direction) {
      case UP:
        y--;
        break;
      case DOWN:
        y++;
        break;
      case LEFT:
        x--;
        break;
      case RIGHT:
        x++;
        break;
    }
    if ((0 <= x && x < maxX) && (0 <= y && y < maxY)) {
      if (matrix[x][y])// 吃到⻝⾷食物或者撞到身体
      {
        if (x == food.x && y == food.y)// 吃到⻝⾷食物
        {
          nodeArray.addFirst(food);// 在头部加上⼀一结点
// 计分规则与移动⻓长度和速度有关
          int scoreGet = (10000 - 200 * countMove) / timeInterval;
          score += scoreGet > 0 ? scoreGet : 10;
          countMove = 0;
          food = createFood();
          matrix[food.x][food.y] = true;
          return true;
        } else
          return false;// 撞到身体
      } else// 什什么都没有碰到
      {
        nodeArray.addFirst(new Node(x, y));// 加上头部
        matrix[x][y] = true;
        n = (Node) nodeArray.removeLast();// 去掉尾部
        matrix[n.x][n.y] = false;
        countMove++;
        return true;
      }
    }
    return false;// 越界（撞到墙壁）
  }
  // ----------------------------------------------------------------------
// run():贪吃蛇运动线程
// ----------------------------------------------------------------------
  public void run() {
    running = true;
    while (running) {
      try {
        Thread.sleep(timeInterval);
      } catch (Exception e) {
        break;
      }
      if (!paused) {
        if (moveOn())// 未结束
        {
          gs.repaint();
        } else// 游戏结束
        {
          JOptionPane.showMessageDialog(null, "GAME OVER",
            "Game Over", JOptionPane.INFORMATION_MESSAGE);
          break;
        }
      }
    }
    running = false;
  }
  // ----------------------------------------------------------------------
// createFood():⽣生成⻝⾷食物及放置地点
// ----------------------------------------------------------------------
  private Node createFood() {
    int x = 0;
    int y = 0;
    do {
      Random r = new Random();
      x = r.nextInt(maxX);
      y = r.nextInt(maxY);
    } while (matrix[x][y]);
    return new Node(x, y);
  }
  // ----------------------------------------------------------------------
// speedUp():加快蛇运动速度
// ----------------------------------------------------------------------
  public void speedUp() {
    timeInterval *= speedChangeRate;
  }
  // ----------------------------------------------------------------------
// speedDown():放慢蛇运动速度
// ----------------------------------------------------------------------
  public void speedDown() {
    timeInterval /= speedChangeRate;
  }
  // ----------------------------------------------------------------------
// changePauseState(): 改变游戏状态（暂停或继续）
// ----------------------------------------------------------------------
  public void changePauseState() {
    paused = !paused;
  }
}