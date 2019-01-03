package com.neo.sk.gypsy.core


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.core.RoomActor.botAction
import com.neo.sk.gypsy.gypsyServer.GameServer
import com.neo.sk.gypsy.ptcl.EsheepProtocol.PlayerInfo
import com.neo.sk.gypsy.shared.ptcl.{ApiProtocol, Protocol}
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.ptcl.Protocol.{KeyCode, MousePosition, PressSpace}

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

  private final case object ChoseActionKey

  private final case object SpaceKey


  def create(botId:String):Behavior[Command] = {
    implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
    Behaviors.withTimers[Command]{implicit timer =>
      Behaviors.receive[Command]{(ctx,msg) =>
        msg match {
          case InitInfo(botName, grid, roomActor) =>
            roomActor ! RoomActor.JoinRoom4Bot(ApiProtocol.PlayerInfo(botId,botName), ctx.self)
            timer.startSingleTimer(ChoseActionKey, ChoseAction,(1 + scala.util.Random.nextInt(20)) * frameRate.millis)
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
        case ChoseAction =>
          timer.startSingleTimer(ChoseActionKey, ChoseAction, (1 + scala.util.Random.nextInt(20)) * frameRate.millis)
          //TODO 选择一个动作发给roomActor
          val px =  new Random(System.nanoTime()).nextInt(1200)- 600
          val py =  new Random(System.nanoTime()).nextInt(600)- 300
          val mp = MousePosition(botId,px.toDouble,py.toDouble,grid.frameCount, -1)
          roomActor ! botAction(botId,mp)
          Behaviors.same

        case BotDead =>
          timer.startSingleTimer(SpaceKey, Space, (2 + scala.util.Random.nextInt(8)) * frameRate.millis)
          timer.cancel(ChoseActionKey)
          dead(botId,grid,roomActor)

        case KillBot =>
          log.debug(s"botActor:$botId go to die...")
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
          roomActor ! botAction(botId, KeyCode(botId,spaceKeyCode,grid.frameCount,-1))
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

}
