package com.neo.sk.gypsy.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.holder.{BotHolder, GameHolder}
import com.neo.sk.gypsy.shared.ptcl.Game._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.Protocol.GameMessage
import com.neo.sk.gypsy.holder.GameHolder
import com.neo.sk.gypsy.holder.BotHolder
import akka.actor.typed.scaladsl.StashBuffer
import com.neo.sk.gypsy.ClientBoot
import com.neo.sk.gypsy.utils.{ClientMusic, FpsComp}

import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Protocol4Bot._

/**
  * @author zhaoyin
  * 2018/10/30  11:44 AM
  */
object GameClient {

  case class ControllerInitial(controller: GameHolder) extends GameBeginning
  case class ControllerInitialBot(controller: BotHolder) extends GameBeginning

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  private[this] var grid: GridOnClient = _

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
        case ControllerInitialBot(botHolder) =>
          grid = BotHolder.grid
          println("runningBot")
          switchBehavior(ctx,"runningBot",runningBot(playerId,roomId,botHolder))
        case x =>
          stashBuffer.stash(x)
          Behaviors.same
      }

    }
  }


  private def running(id:String,roomId:Long, gameHolder: GameHolder)
                        (implicit stashBuffer: StashBuffer[WsMsgSource]):Behavior[WsMsgSource]={
    Behaviors.receive[WsMsgSource]{ (ctx, msg) =>
      msg match {
        case Protocol.Id(id) =>
          GameHolder.myId = id
          ClientMusic.playMusic("bg")
          println(s"myID:${GameHolder.myId}")
          Behaviors.same

        case m:Protocol.KC =>
          if(m.id.isDefined){
            val ID = grid.playerByte2IdMap(m.id.get)
            if(!GameHolder.myId.equals(ID) || GameHolder.usertype == -1){
              ClientBoot.addToPlatform{
                grid.addActionWithFrame(ID,m)
              }
            }
          }
          Behaviors.same

        case m:Protocol.MP =>
          if(m.id.isDefined){
            val ID = grid.playerByte2IdMap(m.id.get)
            if(!GameHolder.myId.equals(ID) || GameHolder.usertype == -1){
              ClientBoot.addToPlatform{
                grid.addMouseActionWithFrame(ID,m)
              }
            }
          }
          Behaviors.same


        case Protocol.Ranks(current) =>
          ClientBoot.addToPlatform{
            //发来的排行版含有我的排名
            if(current.exists(r=>r.score.id ==GameHolder.myId)){
              grid.currentRank = current
            }else{
              //          发来的未含有我的
              grid.currentRank = current ::: grid.currentRank.filter(r=>r.score.id == GameHolder.myId)
            }
            //            grid.currentRank = current
          }
          Behaviors.same

        case Protocol.MyRank(rank) =>
          ClientBoot.addToPlatform{
            //把之前这个id的排行过滤掉
            grid.currentRank = grid.currentRank.filterNot(r=>r.score.id==GameHolder.myId) :+ rank
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

        case data: Protocol.GridDataSync =>
          ClientBoot.addToPlatform{
            GameHolder.syncGridData = Some(data)
            GameHolder.justSynced = true
          }
          Behaviors.same

        //网络延迟检测
        //todo
        case p:Protocol.Pong =>
          ClientBoot.addToPlatform{
            FpsComp.receivePingPackage(p)
          }
          Behaviors.same


        case Protocol.PlayerRestart(id) =>
          ClientMusic.playMusic("bg")
          Behaviors.same


        case Protocol.PlayerJoin(id,player) =>
          println(s"${id}  加入游戏 ${grid.frameCount}")
          ClientBoot.addToPlatform{
            grid.playerMap += (player.id -> player)
            if(GameHolder.myId == id){
              if(GameHolder.gameState == GameState.dead){
//                gameHolder.reLive(id)
                GameHolder.gameState = GameState.play
              }
              gameHolder.cleanCtx()
            }
          }
          Behaviors.same

        case Protocol.PlayerSplit(player) =>
          ClientBoot.addToPlatform{
            player.keys.foreach(item =>
              grid.playerMap += (item -> player(item))
            )
          }
          Behaviors.same

        //只针对某个死亡玩家发送的死亡消息
        case msg@Protocol.UserDeadMessage(killerId,deadId,killNum,score,lifeTime)=>
          if(deadId == GameHolder.myId){
            ClientBoot.addToPlatform{
              GameHolder.deadInfo = Some(msg)
              GameHolder.gameState = GameState.dead
//              grid.removePlayer(id)
              ClientMusic.playMusic("godlike")
            }
          }
          Behaviors.same

        //针对所有玩家发送的死亡消息
        case Protocol.KillMessage(killerId,deadId)=>
          ClientBoot.addToPlatform{
            val a = grid.playerMap.getOrElse(killerId, Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
            grid.playerMap += (killerId -> a.copy(kill = (a.kill + 1).toShort ))
            if(deadId != GameHolder.myId){
              if(!GameHolder.isDead){
                GameHolder.isDead = true
                GameHolder.killList :+=(200,grid.playerMap.get(killerId).get.name,grid.playerMap.get(deadId).get.name)
              }else{
                GameHolder.killList :+=(200,grid.playerMap.get(killerId).get.name,grid.playerMap.get(deadId).get.name)
              }
            }else{
//              ClientMusic.playMusic("shutdown")
            }
            grid.removePlayer(deadId)
            //            if(killerId == GameHolder.myId){
//              grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
//                case 1 => ClientMusic.playMusic("1Blood")
//                case 2 => ClientMusic.playMusic("2Kill")
//                case 3 => ClientMusic.playMusic("3Kill")
//                case 4 => ClientMusic.playMusic("4Kill")
//                case 5 => ClientMusic.playMusic("5Kill")
//                case 6 => ClientMusic.playMusic("godlike")
//                case 7 => ClientMusic.playMusic("legendary")
//                case _ => ClientMusic.playMusic("unstop")
//              }
//            }
          }
          Behaviors.same


        case Protocol.UserMerge(id,player)=>
          if(grid.playerMap.get(id).nonEmpty){
            ClientBoot.addToPlatform{
              grid.playerMap = grid.playerMap - id + (id->player)
            }
          }
          Behaviors.same

        case Protocol.UserCrash(crashMap)=>
          crashMap.map{p=>
            if(grid.playerMap.get(p._1).nonEmpty){
              ClientBoot.addToPlatform {
                var newPlayer = grid.playerMap.getOrElse(p._1, Player("", "unknown", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
                var newCells = newPlayer.cells
                p._2.map { cell =>
                  newCells = cell :: newCells.filterNot(_.id == cell.id)
                }
                newPlayer = newPlayer.copy(cells = newCells)
                grid.playerMap = grid.playerMap - p._1 + (p._1 -> newPlayer)
              }
            }
          }
          Behaviors.same


        case Protocol.RebuildWebSocket =>
          println("存在异地登录")
          ClientBoot.addToPlatform{
            GameHolder.gameState = GameState.allopatry
          }
          Behaviors.same


        //某个用户离开
        case Protocol.PlayerLeft(id,name) =>
          ClientBoot.addToPlatform{
            grid.removePlayer(id)
            if(id == GameHolder.myId){
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

  private def runningBot(id:String,roomId:Long, botHolder: BotHolder)
                     (implicit stashBuffer: StashBuffer[WsMsgSource]):Behavior[WsMsgSource]={
    Behaviors.receive[WsMsgSource]{ (ctx, msg) =>
      msg match {
        //for bot
        case Protocol.JoinRoomSuccess(id,roomId) =>
          log.info(s"$id join room$roomId success")
          ClientBoot.addToPlatform{
            if(BotActor.SDKReplyTo != null){
              BotActor.SDKReplyTo ! JoinRoomRsp(roomId)
            }
          }
          Behaviors.same

        case Protocol.JoinRoomFailure(_,_,errorCode,errMsg) =>
          log.error(s"join room failed $errorCode: $errMsg")
          ClientBoot.addToPlatform{
            if(BotActor.SDKReplyTo != null){
              BotActor.SDKReplyTo ! JoinRoomRsp(-1,errorCode,errMsg)
            }
          }
          Behaviors.stopped

        case Protocol.Id(id) =>
          BotHolder.botId = id
          ClientMusic.playMusic("bg")
          Behaviors.same

        case m:Protocol.KC =>
          if(m.id.isDefined){
            val ID = grid.playerByte2IdMap(m.id.get)
            if(!BotHolder.botId.equals(ID) || BotHolder.usertype == -1){
              ClientBoot.addToPlatform{
                grid.addActionWithFrame(ID,m)
              }
            }
          }
          Behaviors.same

        case m:Protocol.MP =>
          if(m.id.isDefined){
            val ID = grid.playerByte2IdMap(m.id.get)
            if(!BotHolder.botId.equals(ID) || BotHolder.usertype == -1){
              ClientBoot.addToPlatform{
                grid.addMouseActionWithFrame(ID,m)
              }
            }
          }
          Behaviors.same


        case Protocol.Ranks(current) =>
          ClientBoot.addToPlatform{
            //发来的排行版含有我的排名
            if(current.exists(r=>r.score.id ==BotHolder.botId)){
              grid.currentRank = current
            }else{
              //          发来的未含有我的
              grid.currentRank = current ::: grid.currentRank.filter(r=>r.score.id == BotHolder.botId)
            }
            //            grid.currentRank = current
          }
          Behaviors.same

        case Protocol.MyRank(rank) =>
          ClientBoot.addToPlatform{
            //把之前这个id的排行过滤掉
            grid.currentRank = grid.currentRank.filterNot(r=>r.score.id==BotHolder.botId) :+ rank
          }
          Behaviors.same

        case Protocol.FeedApples(foods) =>
//          log.info("ClientFood:  " + foods)
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

        case data: Protocol.GridDataSync =>
          ClientBoot.addToPlatform{
            BotHolder.syncGridData = Some(data)
            BotHolder.justSynced = true
          }
          Behaviors.same

        //网络延迟检测
        //todo
        case p:Protocol.Pong =>
          ClientBoot.addToPlatform{
            FpsComp.receivePingPackage(p)
          }
          Behaviors.same


        case Protocol.PlayerRestart(id) =>
          ClientMusic.playMusic("bg")
          Behaviors.same


        case Protocol.PlayerJoin(id,player) =>
          println(s"${id}  加入游戏 ${grid.frameCount}")
          ClientBoot.addToPlatform{
            grid.playerMap += (player.id -> player)
            grid.playerByte2IdMap += (id -> player.id)
            if(BotHolder.botId == id){
              if(BotHolder.gameState == GameState.dead){
//                botHolder.reLive(id)
                BotHolder.gameState = GameState.play
              }
//              botHolder.cleanCtx()
            }
          }
          Behaviors.same

        case Protocol.PlayerSplit(player) =>
          ClientBoot.addToPlatform{
            player.keys.foreach(item =>
              grid.playerMap += (item -> player(item))
            )
          }
          Behaviors.same

        //只针对某个死亡玩家发送的死亡消息
        case msg@Protocol.UserDeadMessage(killerId,deadId,killNum,score,lifeTime)=>
          if(deadId == BotHolder.botId){
            ClientBoot.addToPlatform{
              BotHolder.deadInfo = Some(msg)
              BotHolder.gameState = GameState.dead
              grid.removePlayer(deadId)
              var playerIdByte = 0.toByte
              grid.playerByte2IdMap.foreach{item =>
                if(item._2 == deadId){
                  playerIdByte = item._1
                }
              }
              grid.playerByte2IdMap -= playerIdByte
              ClientMusic.playMusic("godlikeM")
            }
          }
          Behaviors.same

        //针对所有玩家发送的死亡消息
        case Protocol.KillMessage(killerId,deadId)=>
          ClientBoot.addToPlatform{
            val a = grid.playerMap.getOrElse(killerId, Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
            grid.playerMap += (killerId -> a.copy(kill = (a.kill + 1).toShort ))
            if(deadId != BotHolder.botId){
              if(!BotHolder.isDead){
                BotHolder.isDead = true
                BotHolder.killList :+=(200,grid.playerMap.get(killerId).get.name,grid.playerMap.get(deadId).get.name)
              }else{
                BotHolder.killList :+=(200,grid.playerMap.get(killerId).get.name,grid.playerMap.get(deadId).get.name)
              }
            }else{
//              ClientMusic.playMusic("shutdownM")
            }
            grid.removePlayer(deadId)
            //            if(killerId == BotHolder.botId){
/*              grid.playerMap.getOrElse(killerId, Player("", "unknown", "", 0, 0, cells = List(Cell(0L, 0, 0)))).kill match {
                case 1 => ClientMusic.playMusic("1Blood")
                case 2 => ClientMusic.playMusic("2Kill")
                case 3 => ClientMusic.playMusic("3Kill")
                case 4 => ClientMusic.playMusic("4Kill")
                case 5 => ClientMusic.playMusic("5Kill")
                case 6 => ClientMusic.playMusic("godlikeM")
                case 7 => ClientMusic.playMusic("legendaryM")
                case _ => ClientMusic.playMusic("unstop")
              }*/
//            }
          }
          Behaviors.same


        case Protocol.UserMerge(id,player)=>
          if(grid.playerMap.get(id).nonEmpty){
            ClientBoot.addToPlatform{
              grid.playerMap = grid.playerMap - id + (id->player)
            }
          }
          Behaviors.same

        case Protocol.UserCrash(crashMap)=>
          crashMap.map{p=>
            if(grid.playerMap.get(p._1).nonEmpty){
              ClientBoot.addToPlatform {
                var newPlayer = grid.playerMap.getOrElse(p._1, Player("", "unknown", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
                var newCells = newPlayer.cells
                p._2.map { cell =>
                  newCells = cell :: newCells.filterNot(_.id == cell.id)
                }
                newPlayer = newPlayer.copy(cells = newCells)
                grid.playerMap = grid.playerMap - p._1 + (p._1 -> newPlayer)
              }
            }
          }
          Behaviors.same

        case Protocol.RebuildWebSocket =>
          println("存在异地登录")
          ClientBoot.addToPlatform{
            BotHolder.gameState = GameState.allopatry
          }
          Behaviors.same


        //某个用户离开
        case Protocol.PlayerLeft(id,name) =>
          ClientBoot.addToPlatform{
            grid.removePlayer(id)
            if(id == BotHolder.botId){
              botHolder.gameClose
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
