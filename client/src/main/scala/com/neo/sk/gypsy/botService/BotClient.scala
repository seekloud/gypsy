package com.neo.sk.gypsy.botService

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.seekloud.esheepapi.pb.actions.Move
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgentStub
import scala.concurrent.Future
import io.grpc.stub.StreamObserver

class BotClient(
  host: String,
  port: Int,
  playerId: String,
  apiToken: String
) {
  //1.创建gRPC channel,根据端口和IP连接服务端
  private[this] val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build

  //2.创建存根,存根是根据.proto文件中生成的类IFourKindGrpc的代理.
  private val esheepStub: EsheepAgentStub = EsheepAgentGrpc.stub(channel)

  //3.调用服务端的服务方法.相当于发送请求,并获取服务端的回应.
  val credit = Credit(apiToken = apiToken)

  //TODO
  val actionReq=ActionReq(Move.up,None,0,0,Some(credit))

  def createRoom(password:String): Future[CreateRoomRsp] = esheepStub.createRoom(CreateRoomReq(Some(credit),password))

  def joinRoom(roomId:String,password: String):Future[SimpleRsp]= esheepStub.joinRoom(JoinRoomReq(Some(credit),password,roomId))

  def leaveRoom():Future[SimpleRsp] =esheepStub.leaveRoom(credit)

  def actionSpace():Future[ActionSpaceRsp] =esheepStub.actionSpace(credit)

  def action() :Future[ActionRsp] =esheepStub.action(actionReq)

  val stream = new StreamObserver[ObservationWithInfoRsp] {
    override def onNext(value: ObservationWithInfoRsp): Unit = {
      println(value)
    }

    override def onCompleted(): Unit = {

    }

    override def onError(t: Throwable): Unit = {

    }
  }

  //建立接受后台推来的observation的流(stream)
  def observationWithInfo() = esheepStub.observationWithInfo(credit, stream)

  //主动去获取observation，一般不用
  def observation(): Future[ObservationRsp] = esheepStub.observation(credit)

  def inform():Future[InformRsp]=esheepStub.inform(credit)

  def reincarnation():Future[SimpleRsp]=esheepStub.reincarnation(credit)

  def systemInfo():Future[SystemInfoRsp]=esheepStub.systemInfo(credit)
}


object BotClient{


  def main(args: Array[String]): Unit = {
    //import concurrent.ExecutionContext.Implicits.global

    val host = "127.0.0.1"
    val port = 5321
    val playerId = "gogo"
    val apiToken = "lala"

    println("client DONE.")

  }

}










