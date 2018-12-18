package com.neo.sk.gypsy.botService

import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.actor.BotActor
import io.grpc.{Server, ServerBuilder}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent
import scala.concurrent.{ExecutionContext, Future}
import com.neo.sk.gypsy.utils.BotUtil._
import org.slf4j.LoggerFactory

import com.neo.sk.gypsy.shared.ptcl.Protocol4Bot._

object BotServer {

  def build(port: Int, executionContext: ExecutionContext, botActor:ActorRef[BotActor.Command]): Server = {
    val service = new BotServer(botActor)
    ServerBuilder.forPort(port).addService(
      EsheepAgentGrpc.bindService(service, executionContext)
    ).build

  }

}


class BotServer(
                 botActor:ActorRef[BotActor.Command]
               ) extends EsheepAgent {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  private var state: State = State.unknown

  override def createRoom(request: CreateRoomReq): Future[CreateRoomRsp] = {
    //TODO
    if(checkBotToken(request.credit.get.apiToken)){
      state = State.init_game
      log.info(s"createRoom Called by [$request")
      val getRoomIdRsp: Future[JoinRoomRsp] = botActor ？BotActor.CreateRoom(request.password, _)
      getRoomIdRsp.map{
        rsp =>
          if (rsp.errCode == 0) CreateRoomRsp(rsp.roomId.toString, 0, state, "ok")
          else CreateRoomRsp(rsp.roomId.toString, rsp.errCode, state,rsp.msg)
      }
      Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
    }else{
      Future.successful(CreateRoomRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    println(s"joinRoom Called by [$request")
    if(checkBotToken(request.credit.get.apiToken)){
//      botActor ! JoinRoom(request.roomId,request.credit.get.playerId,request.credit.get.apiToken)
      val state = State.in_game
      Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
    }else{
      Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }

  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    println(s"leaveRoom Called by [$request")
    if(checkBotToken(request.apiToken)){
//      botActor ! LeaveRoom(request.playerId)
      val state = State.ended
      Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
    }else{
      Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    println(s"actionSpace Called by [$request")
    if(checkBotToken(request.apiToken)){
      val rsp = ActionSpaceRsp(swing = true, apply = List(0, 69,70),fire = List(), move = List(), errCode = 13, state = State.unknown, msg = "ok")
      Future.successful(rsp)
    }else{
      Future.successful(ActionSpaceRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.credit.get.apiToken)){
      botHolder.gameActionReceiver(request.apply,request.swing)
      val rsp = ActionRsp(frameIndex = botHolder.getFrameCount.toInt, errCode = 13, state = State.unknown, msg = "ok")
      Future.successful(rsp)
    }else{
      Future.successful(ActionRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.apiToken)){
      //TODO
      val rsp = ObservationRsp()
      Future.successful(rsp)
    }else{
      Future.successful(ObservationRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }
  }

  override def inform(request: Credit): Future[InformRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.apiToken)){
//      val rsp = InformRsp(score = botHolder.getInform(request.playerId)._1.toInt, kills = botHolder.getInform(request.playerId)._2,
//        state = State.unknown, msg = "ok")
//      Future.successful(rsp)
      Future.successful(InformRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }else{
      Future.successful(InformRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }

  }

  override def reincarnation(request: Credit):Future[SimpleRsp] = {
    Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
  }

}




