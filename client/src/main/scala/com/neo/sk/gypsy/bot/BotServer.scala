package com.neo.sk.gypsy.bot

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.actor.BotActor
import com.neo.sk.gypsy.actor.BotActor._
import io.grpc.{Server, ServerBuilder}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent
import scala.concurrent.{ExecutionContext, Future}
import com.neo.sk.gypsy.utils.BotUtil._

object EsheepServer {

  def build(port: Int, executionContext: ExecutionContext, botActor:  ActorRef[BotActor.Command]): Server = {

    val service = new EsheepService(botActor)

    ServerBuilder.forPort(port).addService(
      EsheepAgentGrpc.bindService(service, executionContext)
    ).build

  }

}


class EsheepService(botActor:ActorRef[BotActor.Command]) extends EsheepAgent {
  override def createRoom(request: Credit): Future[CreateRoomRsp] = {
    println(s"createRoom Called by [$request")
    if(checkBotToken(request.playerId,request.apiToken)){
      botActor ! CreateRoom(request.playerId,request.apiToken)
      val state = State.init_game
      Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
    }else{
      Future.successful(CreateRoomRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    println(s"joinRoom Called by [$request")
    if(checkBotToken(request.credit.get.playerId,request.credit.get.apiToken)){
      botActor ! JoinRoom(request.roomId,request.credit.get.playerId,request.credit.get.apiToken)
      val state = State.in_game
      Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
    }else{
      Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }

  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    println(s"leaveRoom Called by [$request")
    if(checkBotToken(request.playerId,request.apiToken)){
      botActor ! LeaveRoom(request.playerId)
      val state = State.ended
      Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
    }else{
      Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    println(s"actionSpace Called by [$request")
    if(checkBotToken(request.playerId,request.apiToken)){
      val rsp = ActionSpaceRsp(swing = true, apply = List(0, 69,70),fire = List(), move = List(), errCode = 13, state = State.unknown, msg = "ok")
      Future.successful(rsp)
    }else{
      Future.successful(ActionSpaceRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.credit.get.playerId,request.credit.get.apiToken)){
      botHolder.gameActionReceiver(request.apply,request.swing)
      val rsp = ActionRsp(frameIndex = botHolder.getFrameCount.toInt, errCode = 13, state = State.unknown, msg = "ok")
      Future.successful(rsp)
    }else{
      Future.successful(ActionRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.playerId,request.apiToken)){
      //TODO
      val rsp = ObservationRsp()
      Future.successful(rsp)
    }else{
      Future.successful(ObservationRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def inform(request: Credit): Future[InformRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.playerId,request.apiToken)){
      val rsp = InformRsp(score = botHolder.getInform(request.playerId)._1.toInt, kills = botHolder.getInform(request.playerId)._2,
        state = State.unknown, msg = "ok")
      Future.successful(rsp)
    }else{
      Future.successful(InformRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }

  }
}




