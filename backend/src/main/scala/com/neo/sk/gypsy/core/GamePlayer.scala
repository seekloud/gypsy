package com.neo.sk.gypsy.core

import java.io.FileReader

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.models.Dao.RecordDao

import scala.concurrent.duration.FiniteDuration
import com.neo.sk.gypsy.utils.ESSFSupport._
import org.seekloud.essf.io.FrameInputStream


/**
  * @author zhaoyin
  * @date 2018/10/25  下午12:54
  */
object GamePlayer {

  private final val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  case class TimeOut(msg:String) extends Command

  final case class SwitchBehavior(
                                 name: String,
                                 behavior: Behavior[Command],
                                 duration: Option[FiniteDuration],
                                 timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  private final case object BehaviorChangeKey
  private final case object BehaviorWaitKey
  private final case object GameLoopKey

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName:String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  def create(recordId: Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        //操作数据库
        RecordDao.getRecordById(recordId).map{
          case Some(r) =>
            val replay = initFileReader(r.filePath)
            val info = replay.init()
            try{
              ctx.self ! SwitchBehavior("work",
                Work()
                )
            }catch {
              case e:Throwable=>
                log.error("error--"+ e.getMessage)
            }
          case None =>
            log.debug(s"record--$recordId didn't exist!!")
        }
        switchBehavior(ctx,"busy",busy())
      }
    }
  }

  def work(fileReader: FrameInputStream,

          )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer: MiddleBufferInJvm
  ):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      case msg:InitReply =>

      case msg:TimeOut=>
        Behaviors.stopped

      case unKnowMsg =>
        stashBuffer.stash(unKnowMsg)
        Behavior.same

    }
  }

  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
          switchBehavior(ctx, name, behavior,durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behaviors.same
      }
    }



}
