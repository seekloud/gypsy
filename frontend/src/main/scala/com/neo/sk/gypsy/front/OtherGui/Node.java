package com.neo.sk.gypsy.front.OtherGui;

/**
 * @author zhaoyin
 * @date 2018/10/15  上午10:29
 */
public class Node {
    int x;
    int y;
    Node(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public void setXY(int newX, int newY){
        this.x = newX;
        this.y = newY;
    }
}

