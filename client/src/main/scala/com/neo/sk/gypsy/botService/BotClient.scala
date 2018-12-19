package com.neo.sk.gypsy.botService

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.seekloud.esheepapi.pb.api
import org.seekloud.esheepapi.pb.api.{CreateRoomReq, CreateRoomRsp, Credit, ObservationRsp}
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgentStub

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
  val createRoomReq = api.CreateRoomReq(Some(credit),"")

  def createRoom(): Future[CreateRoomRsp] = esheepStub.createRoom(createRoomReq)

  def observation(): Future[ObservationRsp] = esheepStub.observation(credit)
}


object BotClient{


  def main(args: Array[String]): Unit = {
    //import concurrent.ExecutionContext.Implicits.global

    val host = "127.0.0.1"
    val port = 5321
    val playerId = "gogo"
    val apiToken = "lala"

    val client = new BotClient(host, port, playerId, apiToken)

    val rsp1 = client.createRoom()

    val rsp2 = client.observation()

    println("--------  begin sleep   ----------------")
    Thread.sleep(10000)
    println("--------  end sleep   ----------------")

    println(rsp1)
    println("------------------------")
    println(rsp2)
    println("client DONE.")

  }

}










