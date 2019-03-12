package com.neo.sk.gypsy.botService

import java.util.concurrent.TimeUnit

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.seekloud.esheepapi.pb.actions.{Move, Swing}
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
  val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build

  //2.创建存根,存根是根据.proto文件中生成的类IFourKindGrpc的代理.
  private val esheepStub: EsheepAgentStub = EsheepAgentGrpc.stub(channel)

  //3.调用服务端的服务方法.相当于发送请求,并获取服务端的回应.
  val credit = Credit(apiToken = apiToken)

  var actionReq=ActionReq(Move.up,Some(Swing((Math.PI/2).toFloat,2.5F)),0,0,Some(credit))

  def createAction(r:Float,d:Float) = { actionReq = ActionReq(Move.up,Some(Swing(r,d)),0,0,Some(credit))}

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

  val fStream = new StreamObserver[CurrentFrameRsp] {
    override def onNext(value: CurrentFrameRsp): Unit = {
      println(value)
    }

    override def onCompleted(): Unit = {

    }

    override def onError(t: Throwable): Unit = {

    }
  }

  //建立接受后台推来的observation的流(stream)
  def observationWithInfo() = esheepStub.observationWithInfo(credit, stream)

  def currentFrame() = esheepStub.currentFrame(credit, fStream)

  //主动去获取observation，一般不用
  def observation(): Future[ObservationRsp] = esheepStub.observation(credit)

  def inform():Future[InformRsp]=esheepStub.inform(credit)

  def reincarnation():Future[SimpleRsp]=esheepStub.reincarnation(credit)

  def systemInfo():Future[SystemInfoRsp]=esheepStub.systemInfo(credit)
}


object BotClient{


  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global

    val host = "127.0.0.1"
    val port = 5321
    val playerId = "test"
    val apiToken = "test"
    val  b = new BotClient(host,port,playerId,apiToken)
//    b.createRoom("").onComplete{
//      a => println(a)
//    }
    Thread.sleep(3000)
    var i = 0
    while(true){
      i += 1
      if(i%2==0){
        b.createAction((Math.PI/2).toFloat,2.5F+i)
      }else if(i%3==0){
        b.createAction((Math.PI).toFloat,2.5F+i)
      }else if(i%5==0){
        b.createAction((-Math.PI/2).toFloat,2.5F+i)
      }
      b.action()
      Thread.sleep(100)
    }
//    b.channel.shutdown().awaitTermination(1,TimeUnit.SECONDS)
//    println("client DONE.")

  }

}










