package com.neo.sk.gypsy.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.botService.BotServer
import io.grpc.Server
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
/**
  * create by zhaoyin
  * 2019/3/8  11:12 AM
  */
object SdkServer {

  trait Command

  private val log = LoggerFactory.getLogger("sdkserver")
  case object Shutdown extends Command

  case class BuildServer(port: Int,
    executionContext: ExecutionContext,
                         act:ActorRef[BotActor.Command]
    ) extends Command


  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String,
                                   behavior: Behavior[Command])
                                  (implicit stashBuffer: StashBuffer[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)

        switchBehavior(ctx, "idle", idle())
      }
    }
  }

  private def idle()
                  (implicit stashBuffer: StashBuffer[Command],
                   timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case BuildServer(port, executor,act) =>
          //FIXME 启动BotServer服务
          val port = 5321
          val server = BotServer.build(port, executor, act)
          server.start()
          log.debug(s"Server started at $port")
          sys.addShutdownHook {
            log.debug("JVM SHUT DOWN.")
            server.shutdown()
            log.debug("SHUT DOWN.")
          }
          //          server.awaitTermination()
          //          log.debug("DONE.")
          working(server)
      }
    }
  }

  private def working(server: Server)
                     (implicit stashBuffer: StashBuffer[Command],
                      timer: TimerScheduler[Command]
                     ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Shutdown =>
          //TODO 关闭BotServer服务
          server.shutdown()
          Behaviors.stopped
      }
    }
  }


}
