package com.neo.sk.gypsy.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer}
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

/**
  * @author zhaoyin
  * @date 2018/10/25  下午10:27
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command




  def create(uId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx, "init",init(uid))

      }
    }
  }
}
