package com.neo.sk.gypsy.front.OtherGui;

import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
//import java.awt.Canvas;


/**
 * @author zhaoyin
 * @date 2018/10/14  下午10:10
 */
public class GameStart extends JPanel{
    Frame mainFrame;
    Police police;
    Button startButton;
    int distance = 4;// 间隔位移
    private int timeInterval = 20;// 间隔时间
    private static final int panelWidth = 800;
    private static final int panelHeight = 450;
    int agentWidth = 40;
    int agentHeight = 40;
    AgentModel agentModel = null;// 玩家
    GameStart game = this;
    Node agent;
//    Canvas canvas;
//    Graphics g;


    private GameStart(){
        init();
    }

    private void init(){
        mainFrame = new Frame();
        mainFrame.setTitle("tryAWT");
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });//关闭窗口，程序结束
//        canvas = new Canvas();
//        canvas.setSize(canvasWidth,canvasHeight);
        game.setSize(panelWidth,panelHeight);
        startButton = new Button("game start!");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                new Roll().start();
            }
        });
        mainFrame.add(game,BorderLayout.CENTER);
        mainFrame.add(startButton,BorderLayout.SOUTH);
        mainFrame.setBounds(100,100,panelWidth,panelHeight+100);
        mainFrame.setVisible(true);
//        g = canvas.getGraphics();
    }

    public void paint(Graphics g){
        //清空
        super.paint(g);
        //绘制背景
        g.setColor(Color.PINK);
        g.fillRect(0, 0, panelWidth, panelHeight);
        //绘制玩家
        agent = agentModel.agent;
        drawNode(g,agent);
    }

    private void drawNode(Graphics g, Node agent){
        Image img = new ImageIcon("src/tryAwt/resources/agent.png").getImage();
        g.drawImage(img,agent.x,agent.y,agentWidth,agentHeight,null);
    }

    //新开一个线程用于重绘canvas
    class Roll extends Thread{
        @Override
        public void run(){
            if (agentModel == null) {
                agentModel = new AgentModel(game, panelWidth, panelHeight);
                police = new Police(agentModel,game);
                game.addKeyListener(police);  //添加按键规则
                game.addMouseListener(police); //添加鼠标规则
                game.requestFocus(); //JPanle获取焦点，否则无法响应键盘事件
//                canvas.addKeyListener(police);
//                canvas.addMouseListener(police);
//                canvas.requestFocus();//画布获得焦点

            }
            while(true){
                try {
                    Thread.sleep(timeInterval);
                } catch (Exception e) {
                    break;
                }
                //gameLoop
                agentModel.moveOn();
                //gameRender
                repaint();
            }
        }
    }

    public static void main(String[] args){
        new GameStart();
    }

}
