package com.neo.sk.gypsy.core

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Materializer, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import com.neo.sk.gypsy.Boot.executor
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{ErrorWsMsgServer, WsMsgServer}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.CheckNameRsp
import io.circe.{Decoder, Encoder}
import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
import com.neo.sk.gypsy.utils.byteObject.ByteObject._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
/**
  * User: sky
  * Date: 2018/7/23
  * Time: 11:09
  */
object RoomManager {

  private val log=LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  case object TimeKey
  case object TimeOut extends Command
  val roomIdGenerator = new AtomicInteger(100)
  case class JoinGame(room:String,sender:String,id:Long, replyTo:ActorRef[Flow[Message,Message,Any]])extends Command
  case class CheckName(name:String,room:String,replyTo:ActorRef[CheckNameRsp])extends Command
  case class RemoveRoom(id:String) extends Command

  val behaviors:Behavior[Command] ={
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            idle(mutable.HashMap())
        }
    }
  }

  /**
    * @param roomMap (roomId,(房间创建时间，房内人数))
    * @author sky
    * */
  def idle(roomMap:mutable.HashMap[String,(Long,Int)])(implicit timer:TimerScheduler[Command])=
    Behaviors.receive[Command]{
      (ctx,msg)=>
        msg match {
          case msg:JoinGame=>
            if(msg.room.contains("match-")){
              msg.replyTo ! webSocketChatFlow(getRoomActor(ctx,msg.room,true),msg.sender,msg.id)
            }else{
              msg.replyTo ! webSocketChatFlow(getRoomActor(ctx,msg.room,false),msg.sender,msg.id)
            }
            Behaviors.same

          case msg:CheckName=>
            val curTime=System.currentTimeMillis()
            if(msg.room!="2"){
              getRoomActor(ctx,msg.room,false) ! RoomActor.CheckName(msg.name,msg.replyTo)
            }else{
              val freeRoom=roomMap.filter(r=>(curTime-r._2._1<AppSettings.waitTime*60*1000)&&r._2._2<AppSettings.limitCount)
              if(freeRoom.isEmpty){
                val roomId=roomIdGenerator.getAndIncrement()
                getRoomActor(ctx,"match-"+roomId,true) ! RoomActor.CheckName(msg.name,msg.replyTo)
                roomMap.put("match-"+roomId,(curTime,1))
              }else{
                import scala.util.Random
                val roomId=Random.shuffle(freeRoom.keys.toList).head
                getRoomActor(ctx,"match-"+roomId,true) ! RoomActor.CheckName(msg.name,msg.replyTo)
                roomMap.get(roomId).foreach{ r=>
                  roomMap.update(roomId,(r._1,r._2+1))
                }
              }
            }
            Behavior.same

          case msg:RemoveRoom=>
            roomMap.remove(msg.id)
            log.info(s"room---${msg.id}--remove")
            Behaviors.same

          case x=>
            log.debug(s"msg can't handle with ${x}")
            Behaviors.unhandled
        }
    }


  def webSocketChatFlow(actor:ActorRef[RoomActor.Command],sender: String, id: Long): Flow[Message, Message, Any] ={
    import scala.language.implicitConversions
    import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
    import com.neo.sk.gypsy.utils.byteObject.ByteObject._
    import io.circe.generic.auto._
    import io.circe.parser._

    Flow[Message]
      .collect {
        case BinaryMessage.Strict(msg)=>
          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
          bytesDecode[WsMsgServer](buffer) match {
            case Right(req) => req
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              ErrorWsMsgServer
          }
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          ErrorWsMsgServer

        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }
      .via(RoomActor.joinGame(actor,id, sender)) // ... and route them through the chatFlow ...
      .map {
      case t:WsMsgProtocol.WsMsgFront =>
        val sendBuffer = new MiddleBufferInJvm(409600)
        BinaryMessage.Strict(ByteString(t.fillMiddleBuffer(sendBuffer).result()))
      case x =>
        TextMessage.apply("")
    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin
  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }

  private def getRoomActor(ctx: ActorContext[Command],name:String,matchRoom:Boolean):ActorRef[RoomActor.Command] decider= {
    val childName = s"RoomActor-$name"
    ctx.child(childName).getOrElse{
      ctx.spawn(RoomActor.create(name,matchRoom),childName)
    }.upcast[RoomActor.Command]
  }
}
