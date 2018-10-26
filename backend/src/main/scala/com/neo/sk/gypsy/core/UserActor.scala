package com.neo.sk.gypsy.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import com.neo.sk.gypsy.shared.ptcl.GypsyGameEvent
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.gypsy.core.RoomActor.{CompleteMsgFront, FailMsgFront}
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * @author zhaoyin
  * @date 2018/10/25  下午10:27
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final case object BehaviorChangeKey
  private final val InitTime = Some(5.minutes)

  trait Command

  case class WebSocketMsg(reqOpt: Option[GypsyGameEvent.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  case class UserFrontActor(actor: ActorRef[GypsyGameEvent.WsMsgServer]) extends Command

  case class TimeOut(msg: String) extends Command
  case class StartReply(recordId:Long, playerId:Long, frame:Int) extends Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut  = TimeOut("busy time error")
                                   )(
                                  implicit stashBuffer: StashBuffer[Command],
                                  timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path}  becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg, GypsyGameEvent.WsMsgSource,Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[GypsyGameEvent.WsMsgSource](
        completionMatcher = {
          case GypsyGameEvent.CompleteMsgServe ⇒
        },
        failureMatcher = {
          case GypsyGameEvent.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(uId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        init(uId)
      }
    }
  }

  private def init(uId: Long)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle", idle(uId,frontActor))
      }

    }

  private def idle(
                    uId: Long,
                    frontActor: ActorRef[GypsyGameEvent.WsMsgServer]
                  )(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx,msg) =>
      msg match {
        case StartReply(recordId,playerId,frame) =>
          getGameReply(ctx,recordId) ! GamePlayer.InitReplay(frontActor,playerId,frame)
          Behaviors.same
      }
    }

  /**
    * replay-actor
    */
  private def getGameReply(ctx: ActorContext[Command], recordId:Long): ActorRef[GamePlayer.Command] = {
    val childName = s"gameReply-$recordId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(GamePlayer.create(recordId), childName)
      actor
    }
  }.upcast[GamePlayer.Command]


}
