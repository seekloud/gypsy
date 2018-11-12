package com.neo.sk.gypsy.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

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
import com.neo.sk.gypsy.core.UserActor.JoinRoom
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol

import com.neo.sk.gypsy.shared.ptcl.UserProtocol.{CheckNameRsp, GameState}
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
  trait Command
  case object TimeKey
  case object TimeOut extends Command
  val roomIdGenerator = new AtomicInteger(20000)
  case class JoinGame(roomId:Option[Long],sender:String,id:String,watchGame: Boolean, replyTo:ActorRef[Flow[Message,Message,Any]])extends Command
  case class LeftRoom(uid:String,name:String) extends Command
  case class CheckName(name:String,roomId:Long,replyTo:ActorRef[CheckNameRsp])extends Command
  case class RemoveRoom(id:Long) extends Command
  case class GetRoomId(playerId:String ,replyTo:ActorRef[RoomIdRsp]) extends Command
  case class GetGamePlayerList(roomId:Long ,replyTo:ActorRef[RoomPlayerInfoRsp]) extends Command
  case class GetRoomList(replyTo:ActorRef[RoomListRsp]) extends Command

  val behaviors:Behavior[Command] ={
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val roomIdGenerator = new AtomicLong(1L)
            val roomInUse = mutable.HashMap((1l,List.empty[(String,String)]))
            idle(roomIdGenerator,roomInUse)
        }
    }
  }

  /**
    * @param roomInUse (roomId,List(玩家id，玩家name))
    *
    *
    * */
  def idle(roomIdGenerator:AtomicLong,roomInUse:mutable.HashMap[Long,List[(String,String)]])(implicit timer:TimerScheduler[Command])=
    Behaviors.receive[Command]{
      (ctx,msg)=>
        msg match {
          //          case msg:JoinGame=>
          //            if(msg.roomId.toString.startsWith("1")){
          //              msg.replyTo ! webSocketChatFlow(getRoomActor(ctx,msg.roomId,true),msg.sender,msg.id,msg.watchGame)
          //            }else{
          //              msg.replyTo ! webSocketChatFlow(getRoomActor(ctx,msg.roomId,false),msg.sender,msg.id,msg.watchGame)
          //            }
          //            Behaviors.same

          case JoinRoom(uid,gameStateOpt,name,startTime,userActor,roomIdOpt,watchGame,watchId) =>
            roomIdOpt match{
              case Some(roomId) =>
                roomInUse.get(roomId) match{
                  case Some(ls) => roomInUse.put(roomId,(uid,name) :: ls)
                  case None => roomInUse.put(roomId,List((uid,name)))
                }
                getRoomActor(ctx,roomId,false) ! RoomActor.JoinRoom(uid,name,startTime,userActor,roomId,watchGame,watchId)
              //TODO  match room need to be headled in the futrue
              case None =>
                gameStateOpt match{
                  case Some(GameState.relive) =>
                    roomInUse.find(_._2.exists(_._1 == uid)) match{
                      case Some(t) =>getRoomActor(ctx,t._1,false) ! RoomActor.JoinRoom(uid,name,startTime,userActor,t._1,watchGame,watchId)
                      case None =>log.debug(s"${ctx.self.path} error:tank relives, but find no room")
                    }
                  case _ =>
                    roomInUse.find(p => p._2.length < AppSettings.limitCount).toList.sortBy(_._1).headOption match{
                      case Some(t) =>
                        roomInUse.put(t._1,(uid,name) :: t._2)
                        getRoomActor(ctx,t._1,false) ! RoomActor.JoinRoom(uid,name,startTime,userActor,t._1,watchGame,watchId)
                      case None =>
                        var roomId = roomIdGenerator.getAndIncrement()
                        while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                        roomInUse.put(roomId,List((uid,name)))
                        getRoomActor(ctx,roomId,false) ! RoomActor.JoinRoom(uid,name,startTime,userActor,roomId,watchGame,watchId)
                    }
                }
            }
            log.debug(s"now roomInUse:$roomInUse")
            Behaviors.same


          //          case msg:CheckName=>
          //            val curTime=System.currentTimeMillis()
          //            if(msg.roomId!=2){
          //              getRoomActor(ctx,msg.roomId,false) ! RoomActor.CheckName(msg.name,msg.replyTo)
          //            }else{
          //              val freeRoom=roomMap.filter(r=>(curTime-r._2._1<AppSettings.waitTime*60*1000)&&r._2._2<AppSettings.limitCount)
          //              if(freeRoom.isEmpty){
          //                val roomId=roomIdGenerator.getAndIncrement()
          //                getRoomActor(ctx,roomId,true) ! RoomActor.CheckName(msg.name,msg.replyTo)
          //                roomMap.put(roomId,(curTime,1))
          //              }else{
          //                import scala.util.Random
          //                val roomString=Random.shuffle(freeRoom.keys.toList).head
          //                getRoomActor(ctx,roomString,true) ! RoomActor.CheckName(msg.name,msg.replyTo)
          //                roomMap.get(roomString).foreach{ r=>
          //                  roomMap.update(roomString,(r._1,r._2+1))
          //                }
          //              }
          //            }
          //            Behavior.same

          case msg:RemoveRoom=>
            roomInUse.remove(msg.id)
            log.info(s"room---${msg.id}--remove")
            Behaviors.same

          case msg:GetGamePlayerList =>
            if(msg.roomId.toString.startsWith("1"))
            {
              getRoomActor(ctx,msg.roomId,false) ! RoomActor.getGamePlayerList(msg.roomId,msg.replyTo)
//              ctx.child(s"RoomActor-${msg.roomId.toString}").get.upcast[RoomActor.Command] ! RoomActor.getGamePlayerList(msg.roomId,msg.replyTo)
            }
            Behaviors.same

          case msg:GetRoomId =>
            ctx.children.foreach{
              i =>
                val roomActor=i.upcast[RoomActor.Command]
                roomActor ! RoomActor.getRoomId(msg.playerId,msg.replyTo)
            }
            Behaviors.same

          case msg:GetRoomList =>
            val RoomList=roomInUse.keys.toList
            println(s"roomlist$RoomList")
            msg.replyTo ! RoomListRsp(roomListInfo(RoomList),0,"ok")
            Behaviors.same

          case LeftRoom(uid,name) =>
            roomInUse.find(_._2.exists(_._1 == uid)) match{
              case Some(t) =>
                roomInUse.put(t._1,t._2.filterNot(_._1 == uid))
                getRoomActor(ctx,t._1,false) ! UserActor.Left(uid,name)
                if(roomInUse(t._1).isEmpty && t._1 > 1l)roomInUse.remove(t._1)
                log.debug(s"玩家：${uid}--$name remember to come back!!!$roomInUse")
              case None => log.debug(s"该玩家不在任何房间")
            }
            Behaviors.same


          case x=>
            log.debug(s"msg can't handle with ${x}")
            Behaviors.unhandled
        }
    }


  //  def webSocketChatFlow(actor:ActorRef[RoomActor.Command],sender: String, id: String, watchgame: Boolean): Flow[Message, Message, Any] ={
  //    import scala.language.implicitConversions
  //    import com.neo.sk.gypsy.utils.byteObject.MiddleBufferInJvm
  //    import com.neo.sk.gypsy.utils.byteObject.ByteObject._
  //    import io.circe.generic.auto._
  //    import io.circe.parser._
  //
  //    Flow[Message]
  //      .collect {
  //        case BinaryMessage.Strict(msg)=>
  //          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
  //          bytesDecode[WsMsgServer](buffer) match {
  //            case Right(req) => req
  //            case Left(e) =>
  //              log.error(s"decode binaryMessage failed,error:${e.message}")
  //              ErrorWsMsgServer
  //          }
  //        case TextMessage.Strict(msg) =>
  //          log.debug(s"msg from webSocket: $msg")
  //          ErrorWsMsgServer
  //
  //        // unpack incoming WS text messages...
  //        // This will lose (ignore) messages not received in one chunk (which is
  //        // unlikely because chat messages are small) but absolutely possible
  //        // FIXME: We need to handle TextMessage.Streamed as well.
  //      }
  //      .via(RoomActor.joinGame(actor,id, sender,watchgame)) // ... and route them through the chatFlow ...
  //      .map {
  //      case t:WsMsgProtocol.WsMsgFront =>
  //        val sendBuffer = new MiddleBufferInJvm(409600)
  //        BinaryMessage.Strict(ByteString(t.fillMiddleBuffer(sendBuffer).result()))
  //      case x =>
  //        TextMessage.apply("")
  //    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin
  //  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }

  private def getRoomActor(ctx: ActorContext[Command],roomId:Long,matchRoom:Boolean):ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse{
      ctx.spawn(RoomActor.create(roomId,matchRoom),childName)
    }.upcast[RoomActor.Command]
  }
}