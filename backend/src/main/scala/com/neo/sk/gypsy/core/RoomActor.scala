package com.neo.sk.gypsy.core

import java.awt.event.KeyEvent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.gypsy.Boot._
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.shared.ptcl.{Boundary, Food, Point, Protocol, WsMsgProtocol, _}
import com.neo.sk.gypsy.core.RoomManager.RemoveRoom
import com.neo.sk.gypsy.core.UserActor.JoinRoomSuccess
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.CheckNameRsp
import com.neo.sk.gypsy.gypsyServer.GameServer
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable
import scala.language.postfixOps
import scala.concurrent.duration._
/**
  * User: sky
  * Date: 2018/8/11
  * Time: 9:22
  */
object RoomActor {

  val log = LoggerFactory.getLogger(this.getClass)

  trait Command
  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  private case object SyncTimeKey

  private case object Sync extends Command

  private case object TimeOutKey

  private case class ReliveTimeOutKey(id:String)

  private case object TimeOut extends Command

  case class JoinRoom(uid:String,name:String,startTime:Long,userActor:ActorRef[UserActor.Command],roomId:Long,watchgame:Boolean,watchId:Option[String]) extends Command

  case class WebSocketMsg(uid:String,req:Protocol.UserAction) extends Command with RoomManager.Command

  private case class ReStart(id: String) extends Command

  case class ReStartAck(id: String) extends Command

  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  private case object UnKnowAction extends Command

  case class CheckName(name:String,replyTo:ActorRef[CheckNameRsp])extends Command
  case class GetGamePlayerList(roomId:Long ,replyTo:ActorRef[RoomPlayerInfoRsp]) extends Command
  case class GetRoomId(playerId:String,replyTo:ActorRef[RoomIdRsp]) extends Command

  case class UserInfo(id:String, name:String, shareList:mutable.ListBuffer[String]) extends Command

  val bounds = Point(Boundary.w, Boundary.h)

  val ballId = new AtomicLong(100000)

  def create(roomId:Long,matchRoom:Boolean):Behavior[Command] = {
    log.debug(s"RoomActor-$roomId start...")
    Behaviors.setup[Command] { ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[String,ActorRef[UserActor.Command]]()
            val userMap = mutable.HashMap[String, (String,Long)]()
            val userList = mutable.ListBuffer[UserInfo]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val grid = new GameServer(bounds)
            grid.setRoomId(roomId)

            if (AppSettings.gameRecordIsWork) {
              getGameRecorder(ctx, grid, roomId.toInt)
            }
            timer.startPeriodicTimer(SyncTimeKey, Sync, WsMsgProtocol.frameRate millis)
            idle(roomId, userList, userMap, subscribersMap, grid, 0l)
        }
    }
  }

  def idle(
            roomId:Long,
            userList:mutable.ListBuffer[UserInfo],
            userMap:mutable.HashMap[String,(String,Long)],
            subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]],
            grid:GameServer,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command],
            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case JoinRoom(id, name, startTime,subscriber,roomId,watchgame,watchId) =>
          log.info(s"got $msg")
          if(watchgame){
            //观战
//            ctx.watchWith(subscriber,UserActor.Left(id,name))
            subscribersMap.put(id,subscriber)
            subscriber ! JoinRoomSuccess(id,ctx.self)
            watchId match{
              case Some(wid) =>
                for(i<- 0 until userList.length){
                  if(userList(i).id == wid && !userList(i).shareList.contains(id)){
                    userList(i).shareList.append(id)
                  }
                }
                dispatchTo(subscribersMap)(id,Protocol.Id(wid))
                dispatchTo(subscribersMap)(id,grid.getAllGridData)
              case None =>
                val x = (new util.Random).nextInt(userList.length)
                userList(x).shareList.append(id)
                //观察者前端的id是其观察对象的id
                //TODO userMap和userLists可以合并
                dispatchTo(subscribersMap)(id,Protocol.Id(userList(x).id))
                dispatchTo(subscribersMap)(id,grid.getAllGridData)
            }
          }else{
//            if (!userMap.contains(id)) {
              val createBallId = ballId.incrementAndGet()
              //TODO 讨论
              println(s" ballId:${createBallId} id:${id} fra:${grid.frameCount}")
              userList.append(UserInfo(id, name, mutable.ListBuffer[String]()))
              userMap.put(id, (name, createBallId))
//              ctx.watchWith(subscriber, UserActor.Left(id, name))
              subscribersMap.put(id, subscriber)
              grid.addPlayer(id, name)
              val event = UserWsJoin(roomId, id, name, createBallId, grid.frameCount,-1)
//              println(s"UserJoin  ${event} ")
              grid.AddGameEvent(event)

              subscriber ! JoinRoomSuccess(id, ctx.self)
              dispatchTo(subscribersMap)(id, Protocol.Id(id))
              dispatchTo(subscribersMap)(id, grid.getAllGridData)
//            }else{
//              println("ID重了")
//            }
          }
          val foodlists = grid.getApples.map(i=>Food(i._2,i._1.x,i._1.y)).toList
          dispatchTo(subscribersMap)(id,Protocol.FeedApples(foodlists))
          Behaviors.same

        case ReStart(id) =>
          log.info(s"RoomActor Restart Send!++++++++++++++")
//          timer.cancel(ReliveTimeOutKey)
          grid.addPlayer(id, userMap.getOrElse(id, ("Unknown",0l))._1)
          //这里的消息只是在重播背景音乐,真正是在addPlayer里面发送加入消息
          dispatchTo(subscribersMap)(id,Protocol.PlayerRestart(id))
          //复活时发送全量消息
          dispatchTo(subscribersMap)(id,grid.getAllGridData)
          Behavior.same

        case ReStartAck(id) =>
          //确认复活接收
          log.info(s"RoomActor Receive Relive Ack from $id *******************")
          grid.ReLiveMap -= id
//          timer.cancel(ReliveTimeOutKey(id))
          Behavior.same

        case UserActor.Left(id, name) =>
          log.info(s"got----RoomActor----Left $msg")
//          subscribersMap.get(id).foreach(r=>ctx.unwatch(r))
          //复活列表清除
          grid.ReLiveMap -= id
          grid.removePlayer(id)
          dispatch(subscribersMap)(Protocol.PlayerLeft(id, name))
          try{
            log.info("userMap:   "+ userMap)
            val leftballId = userMap(id)._2
            //添加离开信息
            log.info(s"user left fra ${grid.frameCount}  ${leftballId} ")
            val event = UserLeftRoom(id,name,leftballId,roomId,grid.frameCount)
            grid.AddGameEvent(event)
          }catch{
            case e:Exception =>
              log.error(s"Had something wrong in add Left event!! Caused by:${e.getMessage}")
          }
          //userMap里面只存玩家信息
          userMap.remove(id)
          //玩家离开or观战者离开
//          println(s"userlist$userList")


          var list=List[Int2]()
          var user = -1
          for(i<-0 until userList.length){
            //观战者离开
//            println(s"i=$i,u(i)=${userList(i)} ")
            for(j<-0 until userList(i).shareList.length){
              if(userList(i).shareList(j) == id){
//                println(s"share    i=$i,u(i)=${userList(i)} j=$j ")
                list :::= List(Int2(i,j))
              }
            }
            //玩家离开
            if(userList(i).id == id){
              user = i
            }
          }
          list.map{l=>
            userList(l.i).shareList.remove(l.j)
          }
          if(user != -1){
            userList.remove(user)
          }
          subscribersMap.remove(id)
          Behaviors.same

        case UserActor.Key(id, keyCode,frame,n) =>
          log.debug(s"got $msg")
          //dispatch(Protocol.TextMsg(s"Aha! $id click [$keyCode]")) //just for test
          if (keyCode == KeyEvent.VK_SPACE) {
            grid.addPlayer(id, userMap.getOrElse(id, ("Unknown",0l))._1)
            dispatchTo(subscribersMap)(id,Protocol.PlayerRestart(id))
//            grid.addSnake(id, userMap.getOrElse(id, ("Unknown",0l))._1)
//            dispatchTo(subscribersMap,id,Protocol.SnakeRestart(id),userList)
          } else {
            grid.addActionWithFrame(id, KeyCode(id,keyCode,math.max(grid.frameCount,frame),n))
            dispatch(subscribersMap)(KeyCode(id,keyCode,math.max(grid.frameCount,frame),n))
          }
          Behaviors.same

        case UserActor.Mouse(id,x,y,frame,n) =>
          log.debug(s"gor $msg")
          grid.addMouseActionWithFrame(id,MousePosition(id,x,y,math.max(grid.frameCount,frame),n))
          dispatch(subscribersMap)(MousePosition(id,x,y,math.max(grid.frameCount,frame),n))
          Behaviors.same

        case Sync =>
          grid.getSubscribersMap(subscribersMap)
          grid.getUserList(userList)
          grid.update()
          val feedapples = grid.getNewApples
          val eventList = grid.getEvents()
          if(AppSettings.gameRecordIsWork){
//            if(tickCount % 20 == 1){
              getGameRecorder(ctx,grid,roomId) ! GameRecorder.GameRecord(eventList, Some(GypsyGameSnapshot(grid.getSnapShot())))
//            }
          }

          if(grid.ReLiveMap.nonEmpty){
            val curTime = System.currentTimeMillis()
            val ToReLive = grid.ReLiveMap.filter(i=> (curTime - i._2) >AppSettings.reliveTime*1000)
            val newReLive = ToReLive.map{live =>
              ctx.self ! ReStart(live._1)
              (live._1,curTime)
            }
            grid.ReLiveMap ++= newReLive
          }

          if (tickCount % 20 == 5) {
            //remind 此处传输全局数据-同步数据
            val gridData = grid.getAllGridData
            dispatch(subscribersMap)(gridData)
          } else {
            if (feedapples.nonEmpty) {
              val foodlists = feedapples.map(s=>Food(s._2,s._1.x,s._1.y)).toList
              dispatch(subscribersMap)(Protocol.FeedApples(foodlists))
              grid.cleanNewApple
            }
          }
          if (tickCount % 20 == 1) {
//            val currentRankEvent = Protocol.CurrentRanks(grid.currentRank)
//            grid.AddGameEvent(currentRankEvent)
//            dispatch(subscribersMap)(Protocol.Ranks(grid.currentRank))

//            val PerfectRanks = grid.currentRank.splitAt(10)
//            val RestRanks = grid.currentRank.takeRight()

            val temp = grid.currentRank.zipWithIndex.splitAt(GameConfig.rankShowNum)
            val PerfectRanks = temp._1.map(r=>RankInfo(r._2+1,r._1))
            val RestRanks = temp._2.map(r=>(r._1.id,RankInfo(r._2+1,r._1)))
            dispatch(subscribersMap)(Protocol.Ranks(PerfectRanks))

            if(RestRanks.nonEmpty){
              RestRanks.foreach{rank=>
//                val index = rank._2 + 1 + GameConfig.rankShowNum
                dispatchTo(subscribersMap)(rank._1,Protocol.MyRank(rank._2))
              }
            }

          }
          if(tickCount==0){
            val gridData = grid.getAllGridData
            dispatch(subscribersMap)(gridData)
            val foodlists = grid.getApples.map(i=>Food(i._2,i._1.x,i._1.y)).toList
            dispatch(subscribersMap)(Protocol.FeedApples(foodlists))
          }
          idle(roomId,userList,userMap,subscribersMap,grid,tickCount+1)

        case UserActor.NetTest(id, createTime) =>
          //log.info(s"Net Test: createTime=$createTime")
          dispatchTo(subscribersMap)(id, Protocol.Pong(createTime))
          Behaviors.same

          //actor gameRecorder 死亡
        case ChildDead(name, childRef) =>
          log.debug(s"${ctx.self.path} recv a msg:${msg}")
          ctx.unwatch(childRef)
          Behaviors.same

        case TimeOut=>
          val overTime=System.currentTimeMillis()
          grid.playerMap.foreach{p=>
            dispatchTo(subscribersMap)(p._1,Protocol.GameOverMessage(p._1,p._2.kill,p._2.cells.map(_.mass).sum.toInt,overTime-p._2.startTime))
          }
          timer.cancel(SyncTimeKey)
          roomManager ! RemoveRoom(roomId)
          Behaviors.stopped

        case GetGamePlayerList(_ ,replyTo) =>
          val playerList=userMap.map{i=>PlayerInfo(i._1,i._2._1)}.toList
          if(playerList!=null){
            replyTo ! RoomPlayerInfoRsp(players(playerList),0,"ok")
          }
          else{
            replyTo ! RoomPlayerInfoRsp(players(playerList),404,"该房间内没有玩家")
          }
          Behaviors.same

        case GetRoomId(playerId,replyTo) =>
          val IsqueryUser = if(userMap.keySet.contains(playerId)) true else false
          if(IsqueryUser){
            replyTo ! RoomIdRsp(roomInfo(roomId.toLong),0,"ok")
          }
          else replyTo ! RoomIdRsp(roomInfo(-1L),1000,"该玩家不在游戏中")
          Behaviors.same

        case x =>
          log.warn(s"got unknown msg: $x")
          Behaviors.unhandled
      }
    }
  }

  def dispatch(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }

  def dispatchTo(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(id:String,msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.get(id).foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))

  }


  //暂未考虑下匹配的情况
//  private def getGameRecorder(ctx: ActorContext[Command],gameContainer:GameContainerServerImpl,roomId:Long):ActorRef[GameRecorder.Command] = {
  private def getGameRecorder(ctx: ActorContext[Command], grid:GameServer, roomId:Long):ActorRef[GameRecorder.Command] = {
    val childName = s"gameRecorder"
    ctx.child(childName).getOrElse{
      val curTime = System.currentTimeMillis()
      val fileName = s"gypsyGame_${curTime}"
//      val gameInformation = TankGameEvent.GameInformation(curTime,AppSettings.tankGameConfig.getTankGameConfigImpl())
      val gameInformation = GameInformation(curTime)
//      val gameInformation = ""
      val initStateOpt = Some(GypsyGameSnapshot(grid.getSnapShot()))
//      println(s"beginSnapShot  $initStateOpt  ================ ")
      val initFrame = grid.frameCount
      val actor = ctx.spawn(GameRecorder.create(fileName,gameInformation,curTime,initFrame,initStateOpt,roomId),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[GameRecorder.Command]
  }


}
