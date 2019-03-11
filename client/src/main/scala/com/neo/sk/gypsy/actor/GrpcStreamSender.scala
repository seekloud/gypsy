package com.neo.sk.gypsy.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.neo.sk.gypsy.botService.BotServer
import com.neo.sk.gypsy.holder.BotHolder
import org.slf4j.LoggerFactory
import io.grpc.stub.StreamObserver
import org.seekloud.esheepapi.pb.api.{CurrentFrameRsp, ObservationRsp, ObservationWithInfoRsp, State}
import com.neo.sk.gypsy.shared.ptcl.Game._

/**
  * create by zhaoyin
  * 2019/3/8  1:12 PM
  */
object GrpcStreamSender {

  private[this] val log = LoggerFactory.getLogger("GrpcStreamSender")

  sealed trait Command

  case class FrameObserver(frameObserver: StreamObserver[CurrentFrameRsp]) extends Command

  case class ObservationObserver(observationObserver: StreamObserver[ObservationWithInfoRsp]) extends Command

  case class NewFrame(frame: Long) extends Command

  case class NewObservation(observation: ObservationRsp) extends Command

  case object LeaveRoom extends Command

  def create(botHolder: BotHolder): Behavior[Command] ={
    Behaviors.setup[Command]{ ctx=>
      val fStream = new StreamObserver[CurrentFrameRsp] {
        override def onNext(value: CurrentFrameRsp): Unit = {}
        override def onCompleted(): Unit = {}
        override def onError(t: Throwable): Unit = {}
      }
      val oStream = new StreamObserver[ObservationWithInfoRsp] {
        override def onNext(value: ObservationWithInfoRsp): Unit = {}
        override def onCompleted(): Unit = {}
        override def onError(t: Throwable): Unit = {}
      }
      working(fStream, oStream, botHolder)
    }
  }

  def working(
    frameObserver: StreamObserver[CurrentFrameRsp],
    observationObserver: StreamObserver[ObservationWithInfoRsp],
    botHolder: BotHolder
  ): Behavior[Command] = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case FrameObserver(fObserver) =>
          working(fObserver, observationObserver, botHolder)

        case ObservationObserver(oObserver) =>
          working(frameObserver, oObserver, botHolder)

        case NewFrame(frame) =>
          val rsp = CurrentFrameRsp(frame)
          try {
            frameObserver.onNext(rsp)
            Behavior.same
          } catch {
            case e: Exception =>
              log.warn(s"frameObserver error: ${e.getMessage}")
              Behavior.stopped
          }

        case NewObservation(observation) =>
          if(BotHolder.gameState == GameState.play){
            BotServer.state = State.in_game
          }
          if(BotHolder.gameState == GameState.dead){
            BotServer.state = State.killed
          }
          val rsp = ObservationWithInfoRsp(
            observation.layeredObservation, observation.humanObservation,
            botHolder.getInform._1,
            botHolder.getInform._2,
            botHolder.getInform._3,
            botHolder.getFrameCount,
            0,
            BotServer.state,
            "ok")
          try {
            observationObserver.onNext(rsp)
            Behavior.same
          } catch {
            case e: Exception =>
              log.warn(s"ooObserver error: ${e.getMessage}")
              Behavior.stopped
          }

        case LeaveRoom =>
          observationObserver.onCompleted()
          frameObserver.onCompleted()
          Behaviors.stopped

        case unknown =>
          log.info(s"unknown in Sender: $unknown")
          Behaviors.same
      }

    }
  }

}
