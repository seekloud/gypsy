package com.neo.sk.gypsy.front.OtherGui;

import java.util.Random;
import java.math.*;

/**
 * @author zhaoyin
 * @date 2018/10/15  上午11:20
 */
public class AgentModel {
    GameStart gs;
    Node agent;
    private int maxX;// 最大宽度
    private int maxY;// 最大长度
    Node direction = new Node(0,0);// 玩家运动方向,默认为静止

    //初始化玩家
    public AgentModel(GameStart game, int x, int y){
        gs = game;
        maxX = x;
        maxY = y;
        agent = creatAgent();
    }
    public void moveOn(){
        int x = agent.x + direction.x; //速度=distance/timeInterval
        int y = agent.y + direction.y;
        x = x <= 0? 0: x + gs.agentWidth >maxX? maxX - gs.agentWidth: x;
        y = y <= 0? 0: y + gs.agentHeight >maxY? maxY - gs.agentHeight: y;
        agent.setXY(x,y);
    }
    private Node creatAgent(){
        Random r = new Random();
        int x = r.nextInt(maxX);
        int y = r.nextInt(maxY);
        return new Node(x, y);
    }

    public void changeDirection(Node newDirection) {
        direction = newDirection;
    }
    public void changeTo(int x,int y){
        int distanceX = x - agent.x;
        int distanceY = y - agent.y;
        double angle = Math.atan2(distanceY,distanceX); //弧度
        int newX = (int)(Math.cos(angle) * gs.distance);
        int newY = (int)(Math.sin(angle) * gs.distance);

//        System.out.println("角度：  "+Math.toDegrees(angle));
//        System.out.println("x:   "+newX + "y:   "+ newY);
        changeDirection(new Node(newX,newY));
    }

}
