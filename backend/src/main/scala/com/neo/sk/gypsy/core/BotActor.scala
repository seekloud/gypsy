package com.neo.sk.gypsy.core


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.core.RoomActor.{DeleteBot, GetBotInfo, botAction}
import com.neo.sk.gypsy.gypsyServer.GameServer
import com.neo.sk.gypsy.ptcl.EsheepProtocol.PlayerInfo
import com.neo.sk.gypsy.shared.ptcl.{ApiProtocol, Protocol}
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.ptcl.Protocol.{GridData4Bot, KC, MP, PressSpace}
import com.neo.sk.gypsy.shared.util.Utils._
import scala.math._
import concurrent.duration._
import scala.util.Random

/**
  * @author zhaoyin
  * 2018/12/24  11:26 AM
  */
object BotActor {
  //在某个房间生成后生成陪玩bot
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class InitInfo(botName: String, grid: GameServer, roomActor: ActorRef[RoomActor.Command]) extends Command

  case object ChoseAction extends Command

  case object KillBot extends Command

  case object BotDead extends Command

  case object Space extends Command

  case object StartTimer extends Command

  case object GetInfo4Bot extends Command

  case class InfoReply(data:GridData4Bot) extends Command

  private final case object ChoseActionKey

  private final case object SpaceKey

  val actionInterval = 2*frameRate

  var lastSplitTime = 0L




  def create(botId:String):Behavior[Command] = {
    implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
    Behaviors.withTimers[Command]{implicit timer =>
      Behaviors.receive[Command]{(ctx,msg) =>
        msg match {
          case InitInfo(botName, grid, roomActor) =>
            roomActor ! RoomActor.JoinRoom4Bot(ApiProtocol.PlayerInfo(botId,botName), ctx.self)
            println(s"$botId :init")
            gaming(botId,grid,roomActor)

          case unknownMsg@_ =>
            log.warn(s"${ctx.self.path} unknown msg: $unknownMsg")
            stashBuffer.stash(unknownMsg)
            Behaviors.unhandled
        }
      }
    }
  }

  def gaming(botId: String, grid: GameServer, roomActor: ActorRef[RoomActor.Command])
            (implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case StartTimer=>
          timer.startPeriodicTimer(ChoseActionKey,GetInfo4Bot,actionInterval.millis)
          Behaviors.same

        case GetInfo4Bot=>
          roomActor ! GetBotInfo(botId,ctx.self)
          Behaviors.same

        case ChoseAction =>
          timer.startSingleTimer(ChoseActionKey, ChoseAction, (1 + scala.util.Random.nextInt(20)) * frameRate.millis)
          //TODO 选择一个动作发给roomActor
          val px =  new Random(System.nanoTime()).nextInt(1200) - 600
          val py =  new Random(System.nanoTime()).nextInt(600) - 300
          val mp = MP(grid.playerId2ByteMap.get(botId),px.toShort,py.toShort,grid.frameCount, -1)
          roomActor ! botAction(botId,mp)
          Behaviors.same

        case InfoReply(data)=>
          if(data.playerDetails.filter(_.id==botId).nonEmpty){
            val bot = data.playerDetails.filter(_.id==botId).head
            val botCell = bot.cells.sortBy(_.newmass).reverse.head
            val food = data.foodDetails
            val virus = data.virusDetails
            val mass = data.massDetails
            var move = false
            //TODO 这边的策略可以改一下 小球会怼在角落里
            val otherPlayers = data.playerDetails.filterNot(a=>(a.id==botId || a.protect==true))
            //躲避、追赶其他玩家
            if (otherPlayers.nonEmpty){
              val closestP = otherPlayers.map(_.cells).flatten.sortBy(c=>getDis(botCell.x,botCell.y,c.x,c.y,c.radius)).head
              if(botCell.mass > closestP.mass * 2.2){
                  val mp = MP(grid.playerId2ByteMap.get(botId),(closestP.x-botCell.x).toShort,(closestP.y-botCell.y).toShort,grid.frameCount, -1)
                  roomActor ! botAction(botId,mp)
                move = true
                if(System.currentTimeMillis()-lastSplitTime > 2*1000 && random() < 0.6){
                  lastSplitTime = System.currentTimeMillis()
                  val kc = KC(grid.playerId2ByteMap.get(botId),70,grid.frameCount,-1)
                  roomActor ! botAction(botId,kc)
                }
              }
              else if(botCell.mass>closestP.mass*1.1){
                if(getDis(botCell.x,botCell.y,closestP.x,closestP.y,closestP.radius) > 0){
                  val mp = MP(grid.playerId2ByteMap.get(botId),(closestP.x-botCell.x).toShort,(closestP.y-botCell.y).toShort,grid.frameCount, -1)
                  roomActor ! botAction(botId,mp)
                  move = true
                }
              }
              else if(botCell.mass * 1.1 < closestP.mass){
                val mp = MP(grid.playerId2ByteMap.get(botId),(botCell.x-closestP.x).toShort,(botCell.y-closestP.y).toShort,grid.frameCount, -1)
                roomActor ! botAction(botId,mp)
              }
            }
              //吃mass
            else if(mass.nonEmpty){
              val closestP = mass.sortBy(c=>getDis(botCell.x,botCell.y,c.x,c.y,Mass2Radius(shotMass))).head
              val mp = MP(grid.playerId2ByteMap.get(botId),(closestP.x-botCell.x).toShort,(closestP.y-botCell.y).toShort,grid.frameCount, -1)
              roomActor ! botAction(botId,mp)
              move = true
            }
              //吃食物
            else if(food.nonEmpty){
              val closestP = food.sortBy(c=>getDis(botCell.x,botCell.y,c.x,c.y,0)).head
              val mp = MP(grid.playerId2ByteMap.get(botId),(closestP.x-botCell.x).toShort,(closestP.y-botCell.y).toShort,grid.frameCount, -1)
              roomActor ! botAction(botId,mp)
              move = true
            }

            if (move == false &&random()<0.1){
              val px =  new Random(System.nanoTime()).nextInt(1200)- 600
              val py =  new Random(System.nanoTime()).nextInt(600)- 300
              val mp = MP(Some(grid.playerId2ByteMap(botId)),px.toShort,py.toShort,grid.frameCount, -1)
              roomActor ! botAction(botId,mp)
            }
          }
          Behaviors.same

        case BotDead =>
          timer.startSingleTimer(SpaceKey, Space, (2 + scala.util.Random.nextInt(8)) * frameRate.millis)
          timer.cancel(ChoseActionKey)
          dead(botId,grid,roomActor)

        case KillBot =>
          log.info(s"botActor:$botId go to die...")
          roomActor ! DeleteBot(botId)
          Behaviors.stopped

        case unknownMsg@_ =>
          log.warn(s"${ctx.self.path} unknown msg: $unknownMsg")
          stashBuffer.stash(unknownMsg)
          Behaviors.unhandled
      }

    }

  }

  def dead(botId: String, grid: GameServer, roomActor: ActorRef[RoomActor.Command])
          (implicit stashBuffer: StashBuffer[Command], timer: TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case Space =>
          //TODO 复活
          val spaceKeyCode=32
          roomActor ! botAction(botId, KC(grid.playerId2ByteMap.get(botId),spaceKeyCode,grid.frameCount,-1))
          Behaviors.same

        case KillBot =>
          Behaviors.stopped

        case unknownMsg@_ =>
          log.warn(s"${ctx.self.path} unknown msg: $unknownMsg")
          stashBuffer.stash(unknownMsg)
          Behaviors.unhandled
      }

    }
  }

  def getDis(x1:Int,y1:Int,x2:Int,y2:Int,r:Int):Double={
    sqrt(pow(x1-x2,2.0)+pow(y1-y2,2.0))-r
  }

}
