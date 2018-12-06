package com.neo.sk.gypsy.bot

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.actor.BotActor
import io.grpc.{Server, ServerBuilder}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent

import scala.concurrent.{ExecutionContext, Future}


object EsheepServer {

  def build(port: Int, executionContext: ExecutionContext, botActor:  ActorRef[BotActor.Command]): Server = {

    val service = new EsheepService(botActor)

    ServerBuilder.forPort(port).addService(
      EsheepAgentGrpc.bindService(service, executionContext)
    ).build

  }


//  def main(args: Array[String]): Unit = {
//
//    val executor = concurrent.ExecutionContext.Implicits.global
//    val port = 5321
//
//    val server = EsheepServer.build(port, executor)
//    server.start()
//    println(s"Server started at $port")
//
//    sys.addShutdownHook {
//      println("JVM SHUT DOWN.")
//      server.shutdown()
//      println("SHUT DOWN.")
//    }
//
//    server.awaitTermination()
//    println("DONE.")
//
//  }

}


class EsheepService(botActor:ActorRef[BotActor.Command]) extends EsheepAgent {
  override def createRoom(request: Credit): Future[CreateRoomRsp] = {
    println(s"createRoom Called by [$request")
    val state = State.init_game
    Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    println(s"joinRoom Called by [$request")
    val state = State.in_game
    Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    println(s"leaveRoom Called by [$request")
    val state = State.ended
    Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    println(s"actionSpace Called by [$request")
    val rsp = ActionSpaceRsp()
    Future.successful(rsp)
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    println(s"action Called by [$request")
    val rsp = ActionRsp()
    Future.successful(rsp)
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
    println(s"action Called by [$request")
    val rsp = ObservationRsp()
    Future.successful(rsp)
  }

  override def inform(request: Credit): Future[InformRsp] = {
    println(s"action Called by [$request")
    val rsp = InformRsp()
    Future.successful(rsp)
  }
}




