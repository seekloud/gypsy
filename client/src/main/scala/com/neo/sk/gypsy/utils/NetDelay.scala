package com.neo.sk.gypsy.utils

//import com.neo.sk.gypsy.front.gypsyClient.WebSocketClient
//import com.neo.sk.gypsy.front.utils.Shortcut
import com.neo.sk.gypsy.shared.ptcl.Protocol.Ping

/**
  * @author zhaoyin
  * 2018/11/15  4:29 PM
  */
object NetDelay {

  final case class NetworkLatency(latency: Long)

  private var lastPingTime=System.currentTimeMillis()
  private val PingTimes=10
  var latency  = 0L
  private var receiveNetworkLatencyList : List[NetworkLatency] = Nil

//  def ping(webSocket: WebSocketClient): Unit ={
//    val curTime = System.currentTimeMillis()
//    if(curTime-lastPingTime>=1000){
//      // println(curTime-lastPingTime+"ddddd")
//      startPing(webSocket)
//      lastPingTime=curTime
//    }
//  }
//
//  private def startPing(webSocket: WebSocketClient): Unit ={
//    webSocket.sendMsg(Ping(System.currentTimeMillis()))
//  }
//
//  def receivePong(createTime: Long,webSocket: WebSocketClient): Unit ={
//    receiveNetworkLatencyList = NetworkLatency(System.currentTimeMillis() - createTime) :: receiveNetworkLatencyList
//    if(receiveNetworkLatencyList.size < PingTimes){
//      Shortcut.scheduleOnce(() => startPing(webSocket),10)
//    }else{
//      latency = receiveNetworkLatencyList.map(_.latency).sum / receiveNetworkLatencyList.size
//      receiveNetworkLatencyList = Nil
//    }
//  }
}