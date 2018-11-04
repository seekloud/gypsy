package com.neo.sk.gypsy.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.holder.GameHolder
import com.neo.sk.gypsy.shared.ptcl
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Protocol.GameMessage
/**
  * @author zhaoyin
  * @date 2018/10/30  11:44 AM
  */
object GameClient {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  private[this] var grid: GridOnClient = _

  var myId = "" //myId变成String类型
  var usertype = 0



  sealed trait Command
  case class ControllerInitial(holder: GameHolder) extends Command


  def create(): Behavior[Command] = {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      switchBehavior(ctx, "waitting", waitting("", -1L))
    }
  }

  private def waitting(playerId: String, roomId: Long)
                      (implicit stashBuffer: StashBuffer[Command]): Behavior[Command]= {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case ControllerInitial(gameHolder) =>
          grid = GameHolder.grid
          switchBehavior(ctx,"running",running(playerId,roomId,gameHolder))
          Behaviors.same
      }

    }

  }


  private def running(id:String,roomId:Long,gameHolder: GameHolder)
                     (implicit stashBuffer: StashBuffer[ptcl.WsMsgSource]):Behavior[ptcl.WsMsgSource]={
    Behaviors.receive[GameMessage]{ (ctx, msg) =>
      msg match {
        case Protocol.Id(id) =>
          myId = id
//          Shortcut.playMusic("bg")
          Behaviors.same

        case m:Protocol.KeyCode =>
          //grid.addActionWithFrameFromServer(m.id,m)
          if(myId!=m.id || usertype == -1){
            grid.addActionWithFrame(m.id,m)
          }
          Behaviors.same
        case m:Protocol.MousePosition =>
          //grid.addActionWithFrameFromServer(m.id,m)
          if(myId!=m.id || usertype == -1){
            grid.addMouseActionWithFrame(m.id,m)
          }
          Behaviors.same

        case Protocol.Ranks(current, history) =>
          grid.currentRank = current
          grid.historyRank = history
          Behaviors.same

        case Protocol.FeedApples(foods) =>
          //        grid.food ++= foods
          grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)
          Behaviors.same

        case data: Protocol.GridDataSync =>
          //TODO here should be better code.
          println(s"同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
          /*if(data.frameCount<grid.frameCount){
            println(s"丢弃同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
          }else if(data.frameCount>grid.frameCount){
            // println(s"同步帧数据，grid frame=${grid.frameCount}, sync state frame=${data.frameCount}")
            syncGridData = Some(data)
            justSynced = true
          }*/
          syncGridData = Some(data)
          justSynced = true
          Behaviors.same

        //drawGrid(msgData.uid, data)
        //网络延迟检测
        case Protocol.Pong(createTime) =>
          NetDelay.receivePong(createTime ,webSocketClient)

        case Protocol.SnakeRestart(id) =>
          Shortcut.playMusic("bg")
          Behaviors.same
        //timer = dom.window.setInterval(() => deadCheck(id, timer, start, maxScore, gameStream), Protocol.frameRate)

        case Protocol.UserDeadMessage(id,_,killerName,killNum,score,lifeTime)=>
          if(id==myId){
            DeadPage.deadModel(this,id,killerName,killNum,score,lifeTime)
            grid.removePlayer(id)
          }
          Behaviors.same

        case Protocol.GameOverMessage(id,killNum,score,lifeTime)=>
          DeadPage.gameOverModel(this,id,killNum,score,lifeTime)
          Behaviors.stopped()

        case Protocol.KillMessage(killerId,deadPlayer)=>
          grid.removePlayer(deadPlayer.id)
          val a = grid.playerMap.getOrElse(killerId, Player("", "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
          grid.playerMap += (killerId -> a.copy(kill = a.kill + 1))
          if(deadPlayer.id!=myId){
            if(!isDead){
              isDead=true
              killList :+=(200,killerId,deadPlayer)
            }else{
              killList :+=(200,killerId,deadPlayer)
            }
          }else{
            Shortcut.playMusic("shutdownM")
          }
          if(killerId==myId){
            grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
              case 1 => Shortcut.playMusic("1Blood")
              case 2 => Shortcut.playMusic("2Kill")
              case 3 => Shortcut.playMusic("3Kill")
              case 4 => Shortcut.playMusic("4Kill")
              case 5 => Shortcut.playMusic("5Kill")
              case 6 => Shortcut.playMusic("godlikeM")
              case 7 => Shortcut.playMusic("legendaryM")
              case _ => Shortcut.playMusic("unstop")
            }
          }
          Behaviors.stopped


        case Protocol.UserMerge(id,player)=>
          if(grid.playerMap.get(id).nonEmpty){
            grid.playerMap=grid.playerMap - id + (id->player)
          }

        //      case Protocol.MatchRoomError()=>
        //        drawClockView.cleanClock()
        //        JsFunc.alert("超过等待时间请重新选择")
        //todo
        //        LoginPage.homePage()
          Behaviors.same

        case msg@_ =>
          log.info(s"unknown $msg")
          Behaviors.same
      }
    }
  }


  private[this] def switchBehavior(ctx: ActorContext[ptcl.WsMsgSource],
                                   behaviorName: String,
                                   behavior: Behavior[ptcl.WsMsgSource])
                                  (implicit stashBuffer: StashBuffer[ptcl.WsMsgSource]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

}
