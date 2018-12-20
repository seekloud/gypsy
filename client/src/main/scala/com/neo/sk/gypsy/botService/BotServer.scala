package com.neo.sk.gypsy.botService

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.gypsy.actor.BotActor
import io.grpc.{Server, ServerBuilder}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent
import scala.concurrent.{ExecutionContext, Future}
import com.neo.sk.gypsy.utils.BotUtil._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.ClientBoot.{executor, materializer, scheduler, system, timeout}
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
    if(checkBotToken(request.credit.get.apiToken)){
      state = State.init_game
      log.info(s"createRoom Called by [$request")
      val getRoomIdRsp: Future[JoinRoomRsp] = botActor ? (BotActor.CreateRoom(_))
      getRoomIdRsp.map{
        rsp =>
          if (rsp.errCode == 0) CreateRoomRsp(rsp.roomId.toString, 0, state, "ok")
          else CreateRoomRsp(rsp.roomId.toString, rsp.errCode, state,rsp.msg)
      }
    }else{
      Future.successful(CreateRoomRsp(errCode = 100001, state = State.unknown, msg = "auth error"))
    }
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    println(s"joinRoom Called by [$request")
    if(checkBotToken(request.credit.get.apiToken)){
      state = State.in_game
      val joinRoomRsp: Future[JoinRoomRsp] = botActor ? (BotActor.JoinRoom(request.roomId, _))
      joinRoomRsp.map{
        rsp =>
          if (rsp.errCode == 0) SimpleRsp(0, state, "ok")
          else SimpleRsp(rsp.errCode, state, rsp.msg)
      }
    }else{
      Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }

  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    println(s"leaveRoom Called by [$request")
    if(checkBotToken(request.apiToken)){
      botActor ! BotActor.LeaveRoom
      state = State.ended
      Future.successful(SimpleRsp(state = state, msg = "ok"))
    }else{
      Future.successful(SimpleRsp(errCode = 100003, state = State.unknown, msg = "auth error"))
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    println(s"actionSpace Called by [$request")
    if(checkBotToken(request.apiToken)){
      val rsp = ActionSpaceRsp(swing = true, apply = List(0, 69,70),fire = List(), move = List(), state = state, msg = "ok")
      Future.successful(rsp)
    }else{
      Future.successful(ActionSpaceRsp(errCode = 100004, state = State.unknown, msg = "auth error"))
    }
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.credit.get.apiToken)){
      val actionRsp: Future[ActionRsp] = botActor ? (BotActor.Action(request.apply, request.swing, _))
      actionRsp.map{
        rsp =>
          if (rsp.errCode == 0) rsp
          else ActionRsp(0,rsp.errCode,state,rsp.msg)
      }
    }else{
      Future.successful(ActionRsp(errCode = 100005, state = state, msg = "auth error"))
    }
  }

  //TODO
  override def observation(request: Credit): Future[ObservationRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.apiToken)){
      val observationRsp: Future[ObservationRsp] = botActor ? (BotActor.ReturnObservation(_))
      observationRsp.map {
        observation =>
          observation
      }
    }else{
      Future.successful(ObservationRsp(errCode = 100006, state = state, msg = "auth error"))
    }
  }

  override def inform(request: Credit): Future[InformRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.apiToken)){
      val informRsp: Future[InformRsp] = botActor ? (BotActor.Inform(_))
      informRsp.map{
        rsp =>
          //score kill health:0代表死亡
          if (rsp.errCode == 0) rsp
          else InformRsp(errCode = rsp.errCode, state = state, msg = rsp.msg)
      }
    }else{
      Future.successful(InformRsp(errCode = 100007, state = state, msg = "auth error"))
    }

  }

  //TODO 按空格键复活
  override def reincarnation(request: Credit):Future[SimpleRsp] = {
    Future.successful(SimpleRsp(errCode = 100008, state = state, msg = "auth error"))
  }

}




