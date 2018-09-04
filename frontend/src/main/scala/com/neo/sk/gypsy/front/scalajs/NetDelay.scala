package com.neo.sk.gypsy.front.scalajs


import com.neo.sk.gypsy.front.utils.Shortcut
import com.neo.sk.gypsy.shared.ptcl.WsFrontProtocol.Ping
import org.scalajs.dom.raw.WebSocket


object NetDelay {

  final case class NetworkLatency(latency: Long)

  private var lastPingTime=System.currentTimeMillis()
  private val PingTimes=10
  var latency  = 0L
  private var receiveNetworkLatencyList : List[NetworkLatency] = Nil

  def ping(gameStream: WebSocket): Unit ={
    val curTime = System.currentTimeMillis()
    if(curTime-lastPingTime>=1000){
     // println(curTime-lastPingTime+"ddddd")
      startPing(gameStream)
      lastPingTime=curTime
    }
  }

  private def startPing(gameStream: WebSocket): Unit ={
    NetGameHolder.sendMsg(Ping(System.currentTimeMillis()),gameStream)

  }

   def receivePong(createTime: Long,gameStream: WebSocket): Unit ={
    receiveNetworkLatencyList = NetworkLatency(System.currentTimeMillis() - createTime) :: receiveNetworkLatencyList
    if(receiveNetworkLatencyList.size < PingTimes){
      Shortcut.scheduleOnce(() => startPing(gameStream),10)
    }else{
      latency = receiveNetworkLatencyList.map(_.latency).sum / receiveNetworkLatencyList.size
      receiveNetworkLatencyList = Nil
    }
  }

}
