package com.neo.sk.gypsy.front.gypsyClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.front.scalajs.FpsComponent._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl
import scalatags.JsDom.all._
import scala.scalajs.js.JSApp
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.math._
import com.neo.sk.gypsy.shared.util.utils.getZoomRate
import org.scalajs.dom.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.collection.mutable

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
                            messageHandler:ptcl.WsMsgServer => Unit,
                            closeCallback:Event => Unit,
                            replay:Boolean = false
                          ) {
  private var wsSetup=false

  private var webSocketOpt:Option[WebSocket]=None

  def getWsState=wsSetup

  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(8192)

  def sendMsg(msg:WsMsgServer) = {
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
        connectErrorCallback(event)
      }

      webSocketOpt.get.onmessage = { event: MessageEvent =>
        //        println(s"recv msg:${event.data.toString}")
        event.data match {
          case blobMsg:Blob =>
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
              val buf = fr.result.asInstanceOf[ArrayBuffer]
              if(replay) {
                messageHandler(replayEventDecode(buf))
              }else{
                val middleDataInJs = new MiddleBufferInJs(buf)
                val data = bytesDecode[ptcl.WsMsgServer](middleDataInJs).right.get
                messageHandler(data)
              }
            }
          case jsonStringMsg:String =>
            import io.circe.generic.auto._
            import io.circe.parser._
            val data = decode[ptcl.WsMsgServer](jsonStringMsg).right.get
            messageHandler(data)
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
    sendMsg(UserLeft)
    println("---close Ws active")
    webSocketOpt.get.close()
  }

  import org.seekloud.byteobject.ByteObject._

  private def replayEventDecode(a:ArrayBuffer):ptcl.WsMsgServer= {
    val middleDataInJs = new MiddleBufferInJs(a)
    if(a.byteLength > 0){
      bytesDecode[List[ptcl.WsMsgServer]](middleDataInJs) match{
        case Right(r)=>
          GypsyGameEvent.EventData(r)
        case Left(e) =>
          println(e.message)
          replayStateDecode(a)
      }
    }else{
      GypsyGameEvent.DecodeError()
    }
  }

  private def replayStateDecode(a: ArrayBuffer):ptcl.WsMsgServer={
    val middleDataInJs = new MiddleBufferInJs(a)
    bytesDecode[GypsyGameEvent.GameSnapshot](middleDataInJs) match {
      case Right(r)=>
        GypsyGameEvent.SyncGameAllState(r.asInstanceOf[GypsyGameEvent.GypsyGameSnapshot].state)
      case Left(e) =>
        println(e.message)
        GypsyGameEvent.DecodeError()
    }
  }



}
