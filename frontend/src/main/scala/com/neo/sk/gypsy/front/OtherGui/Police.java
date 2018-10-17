package com.neo.sk.gypsy.front.OtherGui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author zhaoyin
 * @date 2018/10/14  下午5:20
 */
public class Police implements KeyListener, MouseListener  {
    AgentModel agentmodel;
    GameStart gs;

    Police(AgentModel agentModel,GameStart gameStart){
        agentmodel = agentModel;
        gs = gameStart;
    }
    @Override
    public void keyPressed(KeyEvent e){
        int keyCode = e.getKeyCode();
        switch (keyCode){
            case KeyEvent.VK_UP:
                agentmodel.changeDirection(new Node(0,-1 * gs.distance));
                break;
            case KeyEvent.VK_DOWN:
                agentmodel.changeDirection(new Node(0,1 * gs.distance));
                break;
            case KeyEvent.VK_LEFT:
                agentmodel.changeDirection(new Node(-1 * gs.distance,0));
                break;
            case KeyEvent.VK_RIGHT:
                agentmodel.changeDirection(new Node(1 * gs.distance,0));
                break;
            default:
        }
    }
    //空函数
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}

    @Override
    public void mousePressed(MouseEvent e){
        int x = e.getX();
        int y = e.getY();
        agentmodel.changeTo(x, y);
    }
    //空函数
    public void mouseReleased(MouseEvent e){}
    public void mouseEntered(MouseEvent arg0){}
    public void mouseExited(MouseEvent arg0){}
    public void mouseClicked(MouseEvent e){}

}
