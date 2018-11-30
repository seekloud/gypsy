package com.neo.sk.gypsy.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.ptcl.EsheepProtocol
import com.neo.sk.gypsy.shared.ptcl.ErrorRsp
import com.neo.sk.gypsy.utils.EsheepClient
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global


object EsheepSyncClient {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)
  private final val GetTokenTime = Some(5.minutes)
  private final val ReTryTime = 20.seconds
  private final case object BehaviorChangeKey
  private final case object RefreshTokenKey
  private final case object ErrorRefreshTokenKey

  sealed trait Command

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg:String) extends Command


  final case object RefreshToken extends Command

  final case class RefreshMsg(msg:Command) extends Command

  final case class VerifyAccessCode(accessCode:String, rsp:ActorRef[EsheepProtocol.VerifyAccessCodeRsp]) extends Command

  final case class InputRecord(playerId:String,nickname: String, killing: Int, killed:Int, score: Int, startTime: Long, endTime: Long ) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior) //到这个behavior处理buffer里的所有消息（处理能处理的）
  }


  def create: Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        ctx.self ! RefreshToken
        switchBehavior(ctx,"init",init(),InitTime,TimeOut("init"))
      }
    }
  }


  private def init()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case RefreshToken =>
          log.info("000000000000000001")
          if(AppSettings.esheepAuthToken){
            EsheepClient.gsKey2Token().onComplete{
              case Success(rst) =>
                rst match {
                  case Right(rsp) =>
                    ctx.self ! SwitchBehavior("work",work(rsp))

                  case Left(error) =>
                    log.error(s"${ctx.self.path} get token failed.error:${error.msg}")
                    ctx.self ! SwitchBehavior("stop", Behaviors.stopped)
                }
              case Failure(error) =>
                log.error(s"${ctx.self.path} get token failed.,error:${error.getMessage}")
                ctx.self ! SwitchBehavior("stop", Behaviors.stopped)
            }
            switchBehavior(ctx, "busy", busy(), GetTokenTime, TimeOut("Get Token"))
          } else{
            switchBehavior(ctx, "work", work(EsheepProtocol.GameServerKey2TokenInfo("",System.currentTimeMillis() + 2.days.toMillis)))
          }



        case TimeOut(m) =>
          log.error(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
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


  private def work(
                    tokenInfo:EsheepProtocol.GameServerKey2TokenInfo
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command]
                  ): Behavior[Command] = {
//    timer.startSingleTimer(RefreshTokenKey, RefreshToken, (tokenInfo.expireTime - System.currentTimeMillis()).millis)
    timer.startSingleTimer(RefreshTokenKey, RefreshToken, (tokenInfo.expireTime-2).seconds )
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case VerifyAccessCode(accessCode, rsp) =>
          EsheepClient.verifyAccessCode(accessCode, tokenInfo.token).onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) => rsp ! EsheepProtocol.VerifyAccessCodeRsp(Some(value))
                case Left(error) => log.info(s"===${error}");handleErrorRsp(ctx, msg, error,timer)(() => rsp ! error)
              }
            case Failure(exception) =>
              log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:${exception.getMessage}")
          }
          Behaviors.same

        case RefreshToken =>  //发消息给自己和转换状态哪个先？
          ctx.self ! RefreshToken
          log.info(s"Receive Refresh%%%%%%% ")
//          timer.cancel(ErrorRefreshTokenKey)
          timer.cancel(RefreshTokenKey)
          switchBehavior(ctx,"init",init(),InitTime,TimeOut("init"))

        case r:InputRecord =>
          EsheepClient.inputRecoder(tokenInfo.token,r.playerId,r.nickname,r.killing,r.killed,r.score,"",r.startTime,r.endTime).onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  log.info(s"${ctx.self.path} input record success")
                case Left(error) =>
                  log.error(s"${ctx.self.path} input record fail,error: ${error}")
              }
            case Failure(exception) =>
              log.warn(s"${ctx.self.path} input record fail,error: ${exception}")
          }
          Behaviors.same

        case refresh:RefreshMsg =>
          ctx.self ! RefreshToken
          ctx.self ! refresh.msg
          Behaviors.same

        case unknowMsg =>
          log.warn(s"${ctx.self.path} recv an unknow msg=${msg}")
          stashBuffer.stash(unknowMsg)
          Behaviors.same

      }


    }
  }

  implicit def errorRsp2VerifyAccessCodeRsp(errorRsp: ErrorRsp): EsheepProtocol.VerifyAccessCodeRsp =  EsheepProtocol.VerifyAccessCodeRsp(Some(EsheepProtocol.PlayerInfo("","")), errorRsp.errCode, errorRsp.msg)

  private def handleErrorRsp(ctx:ActorContext[Command],msg:Command,errorRsp:ErrorRsp,timer:TimerScheduler[Command])(unknownErrorHandler: => Unit) = {
    //TODO 这里逻辑有误

    /*
     *如果你看到一直打印收到Token的请求，请检查下application的gameTest和accessCODE获取的流程
     * 应为如果AccessCode验证失败的话,这个函数会往自身发VerifyAccessCode，导致这里会进入一个死循环
     */
    timer.startSingleTimer(ErrorRefreshTokenKey,RefreshMsg(msg),3.seconds)
//    ctx.self ! RefreshToken
//    ctx.self ! msg

  }
}
