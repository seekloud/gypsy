package com.neo.sk.gypsy.actor


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.common.{Api4GameAgent, AppSettings}
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol.TokenAndAcessCode
import org.slf4j.LoggerFactory

import scala.language.implicitConversions
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import com.neo.sk.gypsy.ClientBoot.executor

object TokenActor {
  sealed trait Command
  final case object RefreshToken extends Command
  final case class  InitToken(token: String, tokenExpireTime: Long, playerId: String) extends Command
  final case class  GetAccessCode(rsp:ActorRef[TokenAndAcessCode]) extends Command
  private final case object GetNewToken extends Command
  private val log = LoggerFactory.getLogger(this.getClass)
  private final case object RefreshTokenKey
  private final case object BehaviorChangeKey
  private final val refreshTime = 10.minutes
  private final val GetTokenTime = Some(10.minutes)

  case class TimeOut(msg:String) extends Command

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {

    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  def create(): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      log.debug(s"${ctx.self.path} is starting...")
      msg match {
        case t:InitToken =>
          Behaviors.withTimers[Command] { implicit timer =>
            ctx.self ! RefreshToken
            work(t.token,t.tokenExpireTime,t.playerId)
          }
        case _ =>
          Behaviors.same
      }
    }
  }

  def work(token: String, tokenExpireTime:Long,playerId: String)
          (implicit stashBuffer: StashBuffer[Command],
           timer:TimerScheduler[Command]
          ): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case RefreshToken=>
          println("playerId--------" + playerId)
          timer.startSingleTimer(RefreshTokenKey, RefreshToken, refreshTime)
          ctx.self ! GetNewToken
          //刷新token
          Behaviors.same

        case GetNewToken =>
          Api4GameAgent.refreshToken(playerId,token).onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  ctx.self !  SwitchBehavior("work",work(value.token,value.expireTime,playerId))
                case Left(error) =>
                  //异常
                  timer.startSingleTimer(RefreshTokenKey, RefreshToken, 5.minutes)
                  log.error(s"GetNewToken error,error is ${error}")
              }
            case Failure(exception) =>
              //异常
              timer.startSingleTimer(RefreshTokenKey, RefreshToken, 5.minutes)
              log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
          }
          switchBehavior(ctx, "busy", busy(), GetTokenTime, TimeOut("Get Token"))

        case GetAccessCode(rsp) =>
          Api4GameAgent.linkGameAgent(AppSettings.gameId,token,playerId).onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  rsp ! TokenAndAcessCode(token,tokenExpireTime,value.accessCode)
                case Left(error) =>
                  //异常
                  rsp ! TokenAndAcessCode("", 0l,"")
              }
            case Failure(exception) =>
              //异常
              log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
              rsp ! TokenAndAcessCode("", 0l,"")
          }

          Behaviors.same


        case _ =>
          Behaviors.same

      }
    }
  }

  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior,durationOpt,timeOut) =>
          switchBehavior(ctx,name,behavior,durationOpt,timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }
}
