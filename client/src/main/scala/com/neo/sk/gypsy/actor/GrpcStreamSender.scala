package com.neo.sk.gypsy.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import io.grpc.stub.StreamObserver
import org.seekloud.esheepapi.pb.api.{CurrentFrameRsp, ObservationRsp, ObservationWithInfoRsp,State}

/**
  * create by zhaoyin
  * 2019/3/8  1:12 PM
  */
object GrpcStreamSender {

  private[this] val log = LoggerFactory.getLogger("GrpcStreamSender")

  sealed trait Command

  def create(): Behavior[Command] ={
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
      working(fStream, oStream)
    }
  }

  def working(frameObserver: StreamObserver[CurrentFrameRsp], oObserver: StreamObserver[ObservationWithInfoRsp] ): Behavior[Command] = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        //TODO
      }

    }
  }

}
