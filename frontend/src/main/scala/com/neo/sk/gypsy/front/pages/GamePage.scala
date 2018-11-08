package com.neo.sk.gypsy.front.pages

import com.neo.sk.gypsy.front.common.Page
import com.neo.sk.gypsy.front.gypsyClient.GameHolder
import com.neo.sk.gypsy.front.utils.Http
import com.neo.sk.gypsy.shared.ptcl.UserProtocol._
import com.neo.sk.gypsy.front.common.Routes.{ApiRoute, UserRoute}
import com.neo.sk.gypsy.shared.ptcl.{UserProtocol, _}
import com.neo.sk.gypsy.front.utils.Shortcut
import scala.concurrent.ExecutionContext.Implicits.global

import scala.xml.Elem
/**
  * @author zhaoyin
  * 2018/10/24  下午2:02
  */
class GamePage(playerId:String, playerName:String, roomId:Long, accessCode:String,userType:Int) extends Page{

  private val gameView = <canvas id ="GameView" tabindex="1"></canvas>
  private val middleView = <canvas id="MiddleView" tabindex="2" ></canvas>
  private val topView = <canvas id="TopView" tabindex="3" style="cursor: url(/gypsy/static/img/hand.png),auto;"></canvas>
  private val clockView = <canvas id="ClockView" tabindex="4"></canvas>
  private val offScreen = <canvas id="offScreen" style="width:3600px;height:1800px; display:none" tabindex="4"></canvas>


  def init()={
    val gameHolder = new GameHolder
    gameHolder.init()
    //直接建立websocket连接
    gameHolder.joinGame(playerId,playerName, roomId, accessCode,userType)
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      {gameView}
      {middleView}
      {topView}
      {clockView}
      {offScreen}
    </div>
  }

}
