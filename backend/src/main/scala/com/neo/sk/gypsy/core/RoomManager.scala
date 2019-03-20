package com.neo.sk.gypsy.core

import java.util.concurrent.atomic.AtomicLong
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import org.slf4j.LoggerFactory
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.core.UserActor._
import scala.collection.mutable

import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._

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
  case class LeftRoom4Watch(playerInfo: PlayerInfo,roomId:Long) extends Command
  case class CheckName(name:String,roomId:Long,replyTo:ActorRef[CheckNameRsp])extends Command
  case class RemoveRoom(id:Long) extends Command
  case class GetRoomId(playerId:String ,replyTo:ActorRef[RoomIdRsp]) extends Command
  case class GetGamePlayerList(roomId:Long ,replyTo:ActorRef[RoomPlayerInfoRsp]) extends Command
  case class GetRoomList(replyTo:ActorRef[RoomListRsp]) extends Command

  //FixMe 1房间默认密码123456
  val defaultPassword = "123456"

  val behaviors:Behavior[Command] ={
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val roomIdGenerator = new AtomicLong(1L)
            val roomPassword = mutable.HashMap((1l, defaultPassword))
            val roomInUse = mutable.HashMap((1l,List.empty[(String,String)]))
            idle(roomIdGenerator,roomPassword,roomInUse)
        }
    }
  }

  /**
    * @param roomInUse (roomId, List(玩家id，玩家name))
    * @param roomPassword (roomId, password)
    *
    * */
  def idle(
    roomIdGenerator:AtomicLong,
    roomPassword: mutable.HashMap[Long, String],
    roomInUse:mutable.HashMap[Long,List[(String,String)]]
  )(implicit timer:TimerScheduler[Command])=
    Behaviors.receive[Command]{
      (ctx,msg)=>
        msg match {
          case JoinRoom(passwordOpt, playerInfo,roomIdOpt,userActor) =>
            (passwordOpt, roomIdOpt) match{
                //2019.3.17 add: password join
              case (Some(password), Some(roomId)) =>
                roomInUse.get(roomId) match{
                  case Some(ls) =>
                    roomInUse.find(p => p._2.length < AppSettings.limitCount) match {
                      case Some(t) =>
                        roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match {
                          case Some(t) => /**此时是relive的情况**/
                          case None =>
                            if(password == roomPassword(roomId)){
                              roomInUse.put(roomId,(playerInfo.playerId,playerInfo.nickname) :: ls)
                              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
                            }
                            else{
                              userActor ! JoinRoomFailure(roomId, 123456, "password error")
                            }
                        }
                      case None =>
                    }
                  case None => //默认密码创建
                    roomPassword.put(roomId, defaultPassword)
                    roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                    getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
                }
                /**玩家要求加入指定房间, 无需密码**/
              case (None, Some(roomId)) =>
                roomInUse.get(roomId) match{
                  case Some(ls) =>
                    roomInUse.find(p => p._2.length < AppSettings.limitCount) match {
                      case Some(t) =>
                        roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match {
                          case Some(t) => /**此时是relive的情况**/
                          case None =>
                            roomInUse.put(roomId,(playerInfo.playerId,playerInfo.nickname) :: ls)
                            getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
                        }
                      case None =>
                    }
                  case None => //默认密码创建
                    roomPassword.put(roomId, defaultPassword)
                    roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                    getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
                }
              case (_, None) =>
                /**后台为玩家分配房间**/
//                val botNum = if(AppSettings.addBotPlayer) AppSettings.botNum else 0
                roomInUse.find(p => p._2.length < AppSettings.limitCount) match{
                  case Some(t) =>
//                    log.info(s"RoomSize :  ${t._2.length} ======== ")
                    roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match {
                      case Some(t) => /**此时是relive的情况**/
                      case None =>
                        roomInUse.put(t._1,(playerInfo.playerId,playerInfo.nickname) :: t._2)
                        getRoomActor(ctx,t._1) ! RoomActor.JoinRoom(playerInfo,userActor)
                    }
                  case None =>
                    var roomId = roomIdGenerator.getAndIncrement()
                    while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                    roomPassword.put(roomId, defaultPassword)
                    roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                    getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
                }
            }
            log.debug(s"now roomInUse:$roomInUse")
            Behaviors.same

          case JoinRoom4Watch(playerInfo,roomId,watchId,userActor) =>
            roomInUse.get(roomId) match {
              case Some(ls) =>
                //TODO deal 观察的用户不在房间里
                getRoomActor(ctx, roomId) ! RoomActor.JoinRoom4Watch(playerInfo,watchId,userActor)
              case _  =>
            }
            Behaviors.same

          case JoinRoomByCreate(playerInfo,userActor,password) =>
            var roomId = roomIdGenerator.getAndIncrement()
            while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
            roomPassword.put(roomId,password)
            roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
            getRoomActor(ctx, roomId) ! RoomActor.JoinRoom(playerInfo, userActor)
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

          case LeftRoom4Watch(playerInfo,roomId) =>
            getRoomActor(ctx, roomId) ! UserActor.Left4Watch(playerInfo)
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
            msg.replyTo ! RoomListRsp(roomListInfo(RoomList),0,"ok")
            Behaviors.same

          case x=>
            log.debug(s"msg can't handle with ${x}")
            Behaviors.unhandled
        }
    }


  private def getRoomActor(ctx: ActorContext[Command],roomId:Long):ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse{
      ctx.spawn(RoomActor.create(roomId),childName)
    }.upcast[RoomActor.Command]
  }
}