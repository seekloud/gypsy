package com.neo.sk.gypsy.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.holder.GameHolder
import com.neo.sk.gypsy.shared.ptcl
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.model.GridOnClient
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.ClientBoot
/**
  * @author zhaoyin
  * @date 2018/10/30  11:44 AM
  */
object GameClient {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  private[this] var grid: GridOnClient = _


  sealed trait Command
  case class ControllerInitial(holder: GameHolder) extends Command


  def create(): Behavior[ptcl.WsMsgSource] = {
    Behaviors.setup[ptcl.WsMsgSource]{ ctx =>
      implicit val stashBuffer: StashBuffer[ptcl.WsMsgSource] = StashBuffer[ptcl.WsMsgSource](Int.MaxValue)
      switchBehavior(ctx, "waitting", waitting("", -1L))
    }
  }

  private def waitting(playerId: String, roomId: Long)
                      (implicit stashBuffer: StashBuffer[ptcl.WsMsgSource]): Behavior[ptcl.WsMsgSource]= {
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
    Behaviors.receive[ptcl.WsMsgSource]{(ctx,msg) =>
      msg match {
        case WsMsgProtocol.Id(id) =>
          ClientBoot.addToPlatform{
            grid.myId = id
          }
          Behavior.same

        //todo 接收后台传来的消息 同frontend里的gameHolder一样
        case x=>
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
