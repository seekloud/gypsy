package com.neo.sk.gypsy.front.gypsyClient


import com.neo.sk.gypsy.shared.ptcl.Protocol._

import com.neo.sk.gypsy.shared.ptcl._

import org.scalajs.dom.raw._


import scala.scalajs.js.typedarray.ArrayBuffer

import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJs

/**
  * User: sky
  * Date: 2018/9/13
  * Time: 11:23
  *
  */
case class WebSocketClient(
                            connectSuccessCallback: Event => Unit,
                            connectErrorCallback:ErrorEvent => Unit,
                            messageHandler:Protocol.GameMessage => Unit,
                            closeCallback:Event => Unit,
                            replay:Boolean = false
                          ) {
  private var wsSetup=false

  private var webSocketOpt:Option[WebSocket]=None

  def getWsState=wsSetup

  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(8192)

  def sendMsg(msg:Protocol.UserAction) = {
    import org.seekloud.byteobject.ByteObject._
    webSocketOpt.get.send(msg.fillMiddleBuffer(sendBuffer).result())
  }

  def setUp(url:String)=
    if(wsSetup){
      println("-----error")
    }else{
      val gameStream = new WebSocket(url)
      webSocketOpt = Some(gameStream)
      webSocketOpt.get.onopen = { event: Event =>
        wsSetup = true
        connectSuccessCallback(event)
      }
      webSocketOpt.get.onerror = { event: ErrorEvent =>
        wsSetup = false
        webSocketOpt = None
        connectErrorCallback(event)
      }

      webSocketOpt.get.onmessage = { event: MessageEvent =>
        event.data match {
          case blobMsg:Blob =>
            val fr = new FileReader()
//            println(s"Entet the onMSG !!")
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
//              println(s"Entet the onMSG ")
              val buf = fr.result.asInstanceOf[ArrayBuffer]
              if(replay) {
                messageHandler(replayEventDecode(buf))
              }else{
                val middleDataInJs = new MiddleBufferInJs(buf)
                val data = bytesDecode[Protocol.GameMessage](middleDataInJs).right.get
                messageHandler(data)
              }
            }
//          case jsonStringMsg:String =>
//            import io.circe.generic.auto._
//            import io.circe.parser._
//            val data = decode[Protocol.GameMessage](jsonStringMsg).right.get
//            messageHandler(data)

          case unknow =>  println(s"recv unknow msg:${unknow}")
        }

      }
      webSocketOpt.get.onclose = { event: Event =>
        wsSetup = false
        webSocketOpt=None
        closeCallback(event)
      }
    }

  def closeWs={
    wsSetup = false
    println("---close Ws active")
    webSocketOpt.foreach(_.close())
    webSocketOpt = None
  }

  import org.seekloud.byteobject.ByteObject._

  private def replayEventDecode(a:ArrayBuffer):Protocol.GameMessage= {
    val middleDataInJs = new MiddleBufferInJs(a)
    if(a.byteLength > 0){
      bytesDecode[List[Protocol.GameEvent]](middleDataInJs) match{
        case Right(r)=>
//          println(s"事件数据解析成功！！！$r")
          DecodeEvents(Protocol.EventData(r))
        case Left(e) =>
          replayStateDecode(a)
      }
    }else{
      println(s"事件数据解析不成功  因为长度为0")
      DecodeEventError(Protocol.DecodeError())
    }
  }

  private def replayStateDecode(a: ArrayBuffer):Protocol.GameMessage={
    val middleDataInJs = new MiddleBufferInJs(a)
    bytesDecode[Protocol.GameSnapshot](middleDataInJs) match {
      case Right(r)=>
        DecodeEvent(Protocol.SyncGameAllState(r.asInstanceOf[Protocol.GypsyGameSnapshot].state))
      case Left(e) =>
        println("全量数据解析错误： "+ e.message)
        DecodeEventError(Protocol.DecodeError())
    }
  }



}
