package com.neo.sk.gypsy.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.holder.GameHolder
import com.neo.sk.gypsy.shared.ptcl
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Protocol.GameMessage
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.WsMsgSource
import com.neo.sk.gypsy.holder.GameHolder._
import akka.actor.typed.scaladsl.StashBuffer
import com.neo.sk.gypsy.utils.Shortcut
import com.neo.sk.gypsy.ClientBoot
/**
  * @author zhaoyin
  * 2018/10/30  11:44 AM
  */
object GameClient {

  case class ControllerInitial(controller: GameHolder) extends GameBeginning
  private[this] val log = LoggerFactory.getLogger(this.getClass)
  private[this] var grid: GridOnClient = _

  var myId = "" //myId变成String类型
  var usertype = 0



  def create(): Behavior[WsMsgSource] = {
    Behaviors.setup[WsMsgSource]{ ctx =>
      implicit val stashBuffer: StashBuffer[WsMsgSource] = StashBuffer[WsMsgSource](Int.MaxValue)
      switchBehavior(ctx, "waitting", waitting("", -1L))
    }
  }

  private def waitting(playerId: String, roomId: Long)
                      (implicit stashBuffer: StashBuffer[WsMsgSource]): Behavior[WsMsgSource]= {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case ControllerInitial(gameHolder) =>
          grid = GameHolder.grid
          switchBehavior(ctx,"running",running(playerId,roomId,gameHolder))
        case x =>
          stashBuffer.stash(x)
          Behaviors.same
      }

    }
  }


  private def running(id:String,roomId:Long,gameHolder: GameHolder)
                     (implicit stashBuffer: StashBuffer[WsMsgSource]):Behavior[WsMsgSource]={
    Behaviors.receive[WsMsgSource]{ (ctx, msg) =>
      msg match {
        case Protocol.Id(id) =>
          myId = id
//          Shortcut.playMusic("bg")
          println(s"myID:$myId")
          Behaviors.same

        case m:Protocol.KeyCode =>
          if(myId!=m.id || usertype == -1){
            ClientBoot.addToPlatform{
              grid.addActionWithFrame(m.id,m)
            }
          }
          Behaviors.same

        case m:Protocol.MousePosition =>
          if(myId!=m.id || usertype == -1){
            ClientBoot.addToPlatform{
              grid.addMouseActionWithFrame(m.id,m)
            }
          }
          Behaviors.same


        case Protocol.Ranks(current, history) =>
          ClientBoot.addToPlatform{
            grid.currentRank = current
            grid.historyRank = history
          }
          Behaviors.same


        case Protocol.FeedApples(foods) =>
          ClientBoot.addToPlatform{
            grid.food ++= foods.map(a => Point(a.x, a.y) -> a.color)
          }
          Behaviors.same



        case Protocol.AddVirus(virus) =>
          ClientBoot.addToPlatform{
            println(s"接收新病毒 new Virus ${virus}")
            grid.virusMap ++= virus
          }
          Behaviors.same


        case Protocol.ReduceVirus(virus) =>
          ClientBoot.addToPlatform{
            grid.virusMap = virus
          }
          Behaviors.same


        case data: Protocol.GridDataSync =>
          ClientBoot.addToPlatform{
            syncGridData = Some(data)
            justSynced = true
          }
          Behaviors.same


        //drawGrid(msgData.uid, data)
        //网络延迟检测
          //todo
        case Protocol.Pong(createTime) =>
//          NetDelay.receivePong(createTime ,webSocketClient)
          Behaviors.same


        case Protocol.PlayerRestart(id) =>
//          Shortcut.playMusic("bg")
          Behaviors.same


        case Protocol.PlayerJoin(id,player) =>
          println(s"${id}  加入游戏 ${grid.frameCount}")
          ClientBoot.addToPlatform{
            grid.playerMap += (id -> player)
            if(myId == id){
              gameState = GameState.play
              gameHolder.cleanCtx()
            }
          }
          Behaviors.same



        //只针对某个死亡玩家发送的死亡消息
        case msg@Protocol.UserDeadMessage(id,_,killerName,killNum,score,lifeTime)=>
          if(id==myId){
            //          DeadPage.deadModel(this,id,killerName,killNum,score,lifeTime)
            ClientBoot.addToPlatform{
              deadInfo = Some(msg)
              gameState = GameState.dead
              gameHolder.reLive(id)
              grid.removePlayer(id)
            }
          }
          Behaviors.same



        //针对所有玩家发送的死亡消息
        case Protocol.KillMessage(killerId,deadPlayer)=>
          ClientBoot.addToPlatform{
            grid.removePlayer(deadPlayer.id)
            val a = grid.playerMap.getOrElse(killerId, Player("", "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
            grid.playerMap += (killerId -> a.copy(kill = a.kill + 1))
            if(deadPlayer.id != myId){
              if(!isDead){
                isDead = true
                killList :+=(200,killerId,deadPlayer)
              }else{
                killList :+=(200,killerId,deadPlayer)
              }
            }else{
              //            Shortcut.playMusic("shutdownM")
            }
            //          if(killerId == myId){
            //            grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
            //              case 1 => Shortcut.playMusic("1Blood")
            //              case 2 => Shortcut.playMusic("2Kill")
            //              case 3 => Shortcut.playMusic("3Kill")
            //              case 4 => Shortcut.playMusic("4Kill")
            //              case 5 => Shortcut.playMusic("5Kill")
            //              case 6 => Shortcut.playMusic("godlikeM")
            //              case 7 => Shortcut.playMusic("legendaryM")
            //              case _ => Shortcut.playMusic("unstop")
            //            }
            //          }
          }
          Behaviors.same


        case Protocol.UserMerge(id,player)=>
          if(grid.playerMap.get(id).nonEmpty){
            ClientBoot.addToPlatform{
              grid.playerMap = grid.playerMap - id + (id->player)
            }
          }
          Behaviors.same


        case Protocol.RebuildWebSocket =>
          println("存在异地登录")
          ClientBoot.addToPlatform{
            gameState = GameState.allopatry
          }
          Behaviors.same


        //某个用户离开
        case Protocol.PlayerLeft(id,name) =>
          ClientBoot.addToPlatform{
            grid.removePlayer(id)
            if(id == myId){
              gameHolder.gameClose
            }
          }
          Behaviors.same

        case msg@_ =>
          log.info(s"unknown $msg")
          Behaviors.same
      }
    }
  }


  private[this] def switchBehavior(ctx: ActorContext[WsMsgSource],
                                   behaviorName: String,
                                   behavior: Behavior[WsMsgSource])
                                  (implicit stashBuffer: StashBuffer[WsMsgSource]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

}