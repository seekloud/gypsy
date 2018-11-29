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
import com.neo.sk.gypsy.core.UserActor.{JoinRoom, JoinRoom4Watch}
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.{CheckNameRsp, GameState}

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
  case class LeftRoom(playerInfo: PlayerInfo) extends Command
  case class LeftRoom4Watch(playerInfo: PlayerInfo) extends Command
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
          case JoinRoom(playerInfo,roomIdOpt,userActor) =>
            roomIdOpt match{
              case Some(roomId) =>
                roomInUse.get(roomId) match{
                  case Some(ls) => roomInUse.put(roomId,(playerInfo.playerId,playerInfo.nickname) :: ls)
                  case None => roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                }
                getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,roomId,userActor)
              //TODO  match room need to be headled in the futrue
              case None =>
                roomInUse.find(p => p._2.length < AppSettings.limitCount).toList.sortBy(_._1).headOption match{
                  case Some(t) =>
                    roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match {
                      case Some(t) => /**此时是relive的情况**/
                      case None =>
                        roomInUse.put(t._1,(playerInfo.playerId,playerInfo.nickname) :: t._2)
                    }
                    getRoomActor(ctx,t._1) ! RoomActor.JoinRoom(playerInfo,t._1,userActor)
                  case None =>
                    var roomId = roomIdGenerator.getAndIncrement()
                    while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                    roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                    getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,roomId,userActor)
                }
            }
            log.debug(s"now roomInUse:$roomInUse")
            Behaviors.same

          case JoinRoom4Watch(playerInfo,roomId,watchId,userActor) =>
            roomInUse.get(roomId) match {
              case Some(ls) =>
                ls.exists(p=> p._1 == watchId) match {
                  case false => //TODO 重构02
                  case _ => getRoomActor(ctx, roomId) ! RoomActor.JoinRoom4Watch(playerInfo,watchId,userActor)
                }
              case _  => //TODO 重构01
            }
            Behaviors.same

          case msg:RemoveRoom=>
            roomInUse.remove(msg.id)
            log.info(s"room---${msg.id}--remove")
            Behaviors.same

          case msg:GetGamePlayerList =>
            if(msg.roomId.toString.startsWith("1"))
            {
              getRoomActor(ctx,msg.roomId) ! RoomActor.GetGamePlayerList(msg.roomId,msg.replyTo)
            }
            Behaviors.same

          case msg:GetRoomId =>
            ctx.children.foreach{
              i =>
                val roomActor=i.upcast[RoomActor.Command]
                roomActor ! RoomActor.GetRoomId(msg.playerId,msg.replyTo)
            }
            Behaviors.same

          case msg:GetRoomList =>
            val RoomList=roomInUse.keys.toList
            println(s"roomlist$RoomList")
            msg.replyTo ! RoomListRsp(roomListInfo(RoomList),0,"ok")
            Behaviors.same

          case LeftRoom(playerInfo) =>
            roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match{
              case Some(t) =>
                roomInUse.put(t._1,t._2.filterNot(_._1 == playerInfo.playerId))
                getRoomActor(ctx,t._1) ! UserActor.Left(playerInfo)
                if(roomInUse(t._1).isEmpty && t._1 > 1l)roomInUse.remove(t._1)
              case None => log.debug(s"该玩家不在任何房间")
            }
            Behaviors.same

          case LeftRoom4Watch(playerInfo) =>
            getRoomActor(ctx, t._1)


          case x=>
            log.debug(s"msg can't handle with ${x}")
            Behaviors.unhandled
        }
    }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }

  private def getRoomActor(ctx: ActorContext[Command],roomId:Long):ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse{
      ctx.spawn(RoomActor.create(roomId),childName)
    }.upcast[RoomActor.Command]
  }
}