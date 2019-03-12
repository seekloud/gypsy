package com.neo.sk.gypsy.botService


import java.awt.event.KeyEvent

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.gypsy.actor.{BotActor, GrpcStreamSender}
import io.grpc.{Server, ServerBuilder}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent

import scala.concurrent.{ExecutionContext, Future}
import com.neo.sk.gypsy.utils.BotUtil._
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.ClientBoot.{executor, materializer, scheduler, system, timeout}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.holder.BotHolder
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Protocol4Bot._
import io.grpc.stub.StreamObserver

object BotServer {

  var streamSender: Option[ActorRef[GrpcStreamSender.Command]] = None
  var isObservationConnect = false
  var isFrameConnect = false
  var state: State = State.unknown

  def build(port: Int, executionContext: ExecutionContext, botActor:ActorRef[BotActor.Command], botHolder: BotHolder): Server = {
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

  override def createRoom(request: CreateRoomReq): Future[CreateRoomRsp] = {
    if(checkBotToken(request.credit.get.apiToken)){
      BotServer.state = State.init_game
      log.info(s"createRoom Called by [$request")
      val getRoomIdRsp: Future[JoinRoomRsp] = botActor ? (BotActor.CreateRoom(_))
      getRoomIdRsp.map{
        rsp =>
          if (rsp.errCode == 0) CreateRoomRsp(rsp.roomId.toString, 0, BotServer.state, "ok")
          else CreateRoomRsp(rsp.roomId.toString, rsp.errCode, BotServer.state,rsp.msg)
      }
    }else{
      Future.successful(CreateRoomRsp(errCode = 100001, state = State.unknown, msg = "auth error"))
    }
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    println(s"joinRoom Called by [$request")
    if(checkBotToken(request.credit.get.apiToken)){
      BotServer.state = State.in_game
      val joinRoomRsp: Future[JoinRoomRsp] = botActor ? (BotActor.JoinRoom(request.roomId, _))
      joinRoomRsp.map{
        rsp =>
          if (rsp.errCode == 0) SimpleRsp(0, BotServer.state, "ok")
          else SimpleRsp(rsp.errCode, BotServer.state, rsp.msg)
      }
    }else{
      Future.successful(SimpleRsp(errCode = 100002, state = State.unknown, msg = "auth error"))
    }

  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    println(s"leaveRoom Called by [$request")
    if(checkBotToken(request.apiToken)){
      BotServer.isFrameConnect = false
      BotServer.isObservationConnect = false
      BotServer.streamSender.foreach(s=> s ! GrpcStreamSender.LeaveRoom)
      botActor ! BotActor.LeaveRoom
      BotServer.state = State.ended
      Future.successful(SimpleRsp(state = BotServer.state, msg = "ok"))
    }else{
      Future.successful(SimpleRsp(errCode = 100003, state = State.unknown, msg = "auth error"))
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    println(s"actionSpace Called by [$request")
    if(checkBotToken(request.apiToken)){
      val rsp = ActionSpaceRsp(swing = true, apply = List(0, 69,70),fire = List(), move = List(), state = BotServer.state, msg = "ok")
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
          else ActionRsp(0,rsp.errCode,BotServer.state,rsp.msg)
      }
    }else{
      Future.successful(ActionRsp(errCode = 100005, state = BotServer.state, msg = "auth error"))
    }
  }

  /**废弃的接口**/
  override def observation(request: Credit): Future[ObservationRsp] = {
    println(s"action Called by [$request")
    if(checkBotToken(request.apiToken)){
      val observationRsp: Future[ObservationRsp] = botActor ? (BotActor.ReturnObservation(_))
      observationRsp.map {
        observation =>
          observation
      }
    }else{
      Future.successful(ObservationRsp(errCode = 100006, state = BotServer.state, msg = "auth error"))
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
          else InformRsp(errCode = rsp.errCode, state = BotServer.state, msg = rsp.msg)
      }
    }else{
      Future.successful(InformRsp(errCode = 100007, state = BotServer.state, msg = "auth error"))
    }

  }

  /**按空格键复活**/
  override def reincarnation(request: Credit):Future[SimpleRsp] = {
    if (checkBotToken(request.apiToken)) {
      //统一为给BotActor发消息
      val simpleRsp : Future[SimpleRsp] = botActor ? (BotActor.Reincarnation(_))
      simpleRsp.map{
        rsp =>
          if(rsp.errCode == 0) SimpleRsp(state = BotServer.state, msg = rsp.msg)
          else SimpleRsp(state = BotServer.state, msg = "reincarnation error")
      }
    } else {
      Future.successful(SimpleRsp(errCode = 100005, state = State.unknown, msg = "auth error"))
    }
  }

  /**获取帧号、玩家状态**/
  override def systemInfo(request: Credit): Future[SystemInfoRsp] = {
    if(checkBotToken(request.apiToken)) {
      val rsp = SystemInfoRsp(framePeriod = AppSettings.framePeriod, state = BotServer.state, msg = "ok")
      Future.successful(rsp)
    } else {
      Future.successful(SystemInfoRsp(errCode = 100006, state = State.unknown, msg = "auth error"))
    }
  }
  /**建立当前帧号流gRPC**/
  override def currentFrame(request: Credit, responseObserver: StreamObserver[CurrentFrameRsp]): Unit ={
    if(checkBotToken(request.apiToken)){
      BotServer.isFrameConnect = true
      if(BotServer.streamSender.isDefined){
        BotServer.streamSender.get ! GrpcStreamSender.FrameObserver(responseObserver)
      }
      else{
        BotServer.streamSender = Some(system.spawn(GrpcStreamSender.create(BotActor.botHolder), "grpcStreamSender"))
        BotServer.streamSender.get ! GrpcStreamSender.FrameObserver(responseObserver)
      }
    }
    else{
      responseObserver.onCompleted()
    }
  }

  override def observationWithInfo(request: Credit, responseObserver: StreamObserver[ObservationWithInfoRsp]): Unit ={
    if(checkBotToken(request.apiToken)){
      BotServer.isObservationConnect = true
      if(BotServer.streamSender.isDefined){
        BotServer.streamSender.get ! GrpcStreamSender.ObservationObserver(responseObserver)
      }
      else{
        BotServer.streamSender = Some(system.spawn(GrpcStreamSender.create(BotActor.botHolder), "grpcStreamSender"))
        BotServer.streamSender.get ! GrpcStreamSender.ObservationObserver(responseObserver)
      }
    }
    else{
      responseObserver.onCompleted()
    }
  }



}




