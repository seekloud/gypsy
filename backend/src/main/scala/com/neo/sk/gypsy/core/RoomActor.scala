package com.neo.sk.gypsy.core

import java.awt.event.KeyEvent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.gypsy.Boot._
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.core.RoomManager.RemoveRoom
import com.neo.sk.gypsy.core.UserActor.{Command, JoinRoomSuccess, JoinRoomSuccess4Watch}
import com.neo.sk.gypsy.gypsyServer.GameServer
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import com.neo.sk.gypsy.core.BotActor.InfoReply

import scala.collection.mutable
import scala.language.postfixOps
import scala.concurrent.duration._
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig
import com.neo.sk.gypsy.shared.ptcl.GameConfig._

import scala.util.Random


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

  case class botAction(botId:String,action:Protocol.UserAction) extends Command

  case class JoinRoom(playerInfo: PlayerInfo,roomId:Long,userActor:ActorRef[UserActor.Command]) extends Command

  case class JoinRoom4Watch(playerInfo: PlayerInfo,watchId: Option[String],userActor:ActorRef[UserActor.Command]) extends Command

  case class JoinRoom4Bot(playerInfo: PlayerInfo, botActor:ActorRef[BotActor.Command]) extends Command

  case class WebSocketMsg(uid:String,req:Protocol.UserAction) extends Command with RoomManager.Command

  case class UserReLive(id:String,frame:Int) extends Command

  case class GetBotInfo(id:String,botActor:ActorRef[BotActor.Command]) extends Command

  private case class ReStart(id: String) extends Command

  case class KeyR(id:String, keyCode: Int,frame:Int,n:Int) extends Command

  case class MouseR(id:String, clientX:Short,clientY:Short,frame:Int,n:Int) extends Command

//  case class ReStartAck(id: String) extends Command

  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  private case object UnKnowAction extends Command

  case class CheckName(name:String,replyTo:ActorRef[CheckNameRsp])extends Command
  case class GetGamePlayerList(roomId:Long ,replyTo:ActorRef[RoomPlayerInfoRsp]) extends Command
  case class GetRoomId(playerId:String,replyTo:ActorRef[RoomIdRsp]) extends Command

  case class UserInfo(id:String, name:String, shareList:mutable.ListBuffer[String]) extends Command

  val bounds = Point(Boundary.w, Boundary.h)

  val ballId = new AtomicLong(100000)

  val botId = new AtomicInteger(100)

  def create(roomId:Long):Behavior[Command] = {
    log.debug(s"RoomActor-$roomId start...")
    Behaviors.setup[Command] { ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[String,ActorRef[UserActor.Command]]()
            val userMap = mutable.HashMap[String, (String,Long,Long)]()
            val userSyncMap = mutable.HashMap[Long,Set[String]]()
//            val userList = mutable.ListBuffer[UserInfo]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val grid = new GameServer(bounds)
            grid.setRoomId(roomId)

            if (AppSettings.gameRecordIsWork) {
              getGameRecorder(ctx, grid, roomId.toInt)
            }

            if(AppSettings.addBotPlayer) {
              for(b <- 1 until AppSettings.botNum ){
                val id = "bot_"+roomId + "_100"+ b
                val botName = getStarName(new Random(System.nanoTime()).nextInt(AppSettings.starNames.size),b)
                getBotActor(ctx, id) ! BotActor.InitInfo(botName, grid, ctx.self)
              }

            }

//            if(AppSettings.addBotPlayer) {
//              AppSettings.botMap.foreach{b =>
//                val id = "bot_"+roomId + b._1
//                getBotActor(ctx, id) ! BotActor.InitInfo(b._2, grid, ctx.self)
//              }
//            }
            timer.startPeriodicTimer(SyncTimeKey, Sync, frameRate millis)
            idle(roomId, userMap, subscribersMap,userSyncMap ,grid, 0l)
        }
    }
  }

  def idle(
            roomId:Long,
            userMap:mutable.HashMap[String,(String,Long,Long)],//[Id, (name, ballId,group)]
            subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]],
            userSyncMap:mutable.HashMap[Long,Set[String]], //FrameCount Group => List(Id)
            grid:GameServer,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command],
            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case JoinRoom(playerInfo,roomId,userActor) =>
          val createBallId = ballId.incrementAndGet()
          println(s" ballId:${createBallId} id:${playerInfo.playerId} fra:${grid.frameCount}")
//          userList.append(UserInfo(playerInfo.playerId, playerInfo.nickname, mutable.ListBuffer[String]()))
          val group = tickCount % AppSettings.SyncCount
          userMap.put(playerInfo.playerId, (playerInfo.nickname, createBallId,group))
          subscribersMap.put(playerInfo.playerId, userActor)
          userSyncMap.get(group) match{
            case Some(s) =>userSyncMap.update(group,s + playerInfo.playerId)
            case None => userSyncMap.put(group,Set(playerInfo.playerId))
          }
          grid.addPlayer(playerInfo.playerId, playerInfo.nickname)
          userActor ! JoinRoomSuccess(roomId,ctx.self)

          dispatchTo(subscribersMap)(playerInfo.playerId, Protocol.Id(playerInfo.playerId))
//          dispatchTo(subscribersMap)(playerInfo.playerId, grid.getAllGridData)
          val foodlists = grid.getApples.map(i=>Food(i._2,i._1.x,i._1.y)).toList
          dispatchTo(subscribersMap)(playerInfo.playerId,Protocol.FeedApples(foodlists))

          val event = UserWsJoin(roomId, playerInfo.playerId, playerInfo.nickname, createBallId, grid.frameCount,-1)
          grid.AddGameEvent(event)
          Behaviors.same

        case JoinRoom4Bot(botInfo,botActor)=>
          val createBallId = ballId.incrementAndGet()
          //          userList.append(UserInfo(playerInfo.playerId, playerInfo.nickname, mutable.ListBuffer[String]()))
          val group = tickCount % AppSettings.SyncCount
          userMap.put(botInfo.playerId, (botInfo.nickname, createBallId, group))
          botActor ! BotActor.StartTimer
          userSyncMap.get(group) match{
            case Some(s) =>userSyncMap.update(group,s + botInfo.playerId)
            case None => userSyncMap.put(group,Set(botInfo.playerId))
          }
          grid.addPlayer(botInfo.playerId, botInfo.nickname)
          //userActor ! JoinRoomSuccess(roomId,ctx.self)

          dispatchTo(subscribersMap)(botInfo.playerId, Protocol.Id(botInfo.playerId))
//          dispatchTo(subscribersMap)(botInfo.playerId, grid.getAllGridData)
          val foodlists = grid.getApples.map(i=>Food(i._2,i._1.x,i._1.y)).toList
          dispatchTo(subscribersMap)(botInfo.playerId,Protocol.FeedApples(foodlists))

          val event = UserWsJoin(roomId, botInfo.playerId, botInfo.nickname, createBallId, grid.frameCount,-1)
          grid.AddGameEvent(event)
          Behaviors.same

        case JoinRoom4Watch(playerInfo,watchId,userActor) =>
          subscribersMap.put(playerInfo.playerId,userActor)
          userActor ! JoinRoomSuccess4Watch(ctx.self,roomId)
          watchId match{
            case Some(wid) =>
//              }
              dispatchTo(subscribersMap)(playerInfo.playerId,Protocol.Id(wid))
              dispatchTo(subscribersMap)(playerInfo.playerId,grid.getAllGridData)
            case None =>
//              val x = (new util.Random).nextInt(userList.length)
//              userList(x).shareList.append(playerInfo.playerId)
              //观察者前端的id是其观察对象的id
              dispatchTo(subscribersMap)(playerInfo.playerId,Protocol.Id(userMap.head._1))
              dispatchTo(subscribersMap)(playerInfo.playerId,grid.getAllGridData)
          }

          val group = grid.frameCount % AppSettings.SyncCount
          //TODO 没玩家怎么观看 ？？？ 先这样写
          userSyncMap.get(group) match{
            case Some(s) =>userSyncMap.update(group,s + playerInfo.playerId)
            case None => userSyncMap.put(group,Set(playerInfo.playerId))
          }

          val foodlists = grid.getApples.map(i=>Food(i._2,i._1.x,i._1.y)).toList
          dispatchTo(subscribersMap)(playerInfo.playerId,Protocol.FeedApples(foodlists))
          Behaviors.same

        case ReStart(id) =>
          log.info(s"RoomActor Restart Receive $id Relive Msg!++++++++++++++")
//          timer.cancel(ReliveTimeOutKey)
          grid.addPlayer(id, userMap.getOrElse(id, ("Unknown",0l,0l))._1)
          //这里的消息只是在重播背景音乐,真正是在addPlayer里面发送加入消息
          dispatchTo(subscribersMap)(id,Protocol.PlayerRestart(id))
          //复活时发送全量消息
//          dispatchTo(subscribersMap)(id,grid.getAllGridData)
          Behaviors.same


        case UserReLive(id,frame) =>
          log.info(s"RoomActor Receive Relive Ack from $id *******************")
          ctx.self ! ReStart(id)
          Behaviors.same

//        case ReStartAck(id) =>
//          //确认复活接收
//          log.info(s"RoomActor Receive Relive Ack from $id *******************")
//          grid.ReLiveMap -= id
//          Behaviors.same

        case UserActor.Left(playerInfo) =>
          log.info(s"got----RoomActor----Left $msg")
          log.info(s"bot$playerInfo die")

//          //复活列表清除(Bot感觉不用)
          grid.ReLiveMap -= playerInfo.playerId

          grid.removePlayer(playerInfo.playerId)
          /**移除playerId2ByteMap**/
          grid.playerId2ByteMap -= playerInfo.playerId
          dispatch(subscribersMap)(Protocol.PlayerLeft(playerInfo.playerId, playerInfo.nickname))
          try{
            // 添加离开事件
            val leftballId = userMap(playerInfo.playerId)._2
            //添加离开信息
            log.info(s"user left fra ${grid.frameCount}  ${leftballId} ")
            val event = UserLeftRoom(playerInfo.playerId,playerInfo.nickname,leftballId,roomId,grid.frameCount)
            grid.AddGameEvent(event)
          }catch{
            case e:Exception =>
              log.error(s"Had something wrong in add Left event!! Caused by:${e.getMessage}")
          }
          //userSyncMap 中删去数据
          userMap.get(playerInfo.playerId).foreach{u=>
            val group = u._3
            userSyncMap.get(group) match {
              case Some(s) => userSyncMap.update(group,s-playerInfo.playerId)
              case None => userSyncMap.put(group,Set.empty[String])
            }
          }

          //userMap里面只存玩家信息
          userMap.remove(playerInfo.playerId)
          //玩家离开or观战者离开
//          var list=List[Int2]()
//          var user = -1
//          for(i<-0 until userList.length){
//            //观战者离开
//            for(j<-0 until userList(i).shareList.length){
//              if(userList(i).shareList(j) == playerInfo.playerId){
//                list :::= List(Int2(i,j))
//              }
//            }
//            //玩家离开
//            if(userList(i).id == playerInfo.playerId){
//              user = i
//            }
//          }
//          list.map{l=>
//            userList(l.i).shareList.remove(l.j)
//          }
//          if(user != -1){
//            userList.remove(user)
//          }
          subscribersMap.remove(playerInfo.playerId)
          Behaviors.same

        case UserActor.Left4Watch(playerInfo) =>
          //userSyncMap 中删去数据
          userMap.get(playerInfo.playerId).foreach{u=>
            val group = u._3
            userSyncMap.get(group) match {
              case Some(s) => userSyncMap.update(group,s-playerInfo.playerId)
              case None => userSyncMap.put(group,Set.empty[String])
            }
          }

          subscribersMap.remove(playerInfo.playerId)
          Behaviors.same

        case RoomActor.KeyR(id, keyCode,frame,n) =>
          log.debug(s"got $msg")
          if (keyCode == KeyEvent.VK_SPACE) {
            grid.addPlayer(id, userMap.getOrElse(id, ("Unknown",0l,0l))._1)
            dispatchTo(subscribersMap)(id,Protocol.PlayerRestart(id))
          } else {
            grid.addActionWithFrame(id, KC(Some(grid.playerId2ByteMap(id)),keyCode,math.max(grid.frameCount,frame),n))
            dispatch(subscribersMap)(KC(Some(grid.playerId2ByteMap(id)),keyCode,math.max(grid.frameCount,frame),n))
          }
          Behaviors.same

        case RoomActor.MouseR(id,x,y,frame,n) =>
          log.debug(s"gor $msg")
          grid.addMouseActionWithFrame(id,MP(Some(grid.playerId2ByteMap(id)),x,y,math.max(grid.frameCount,frame),n))
          dispatch(subscribersMap)(MP(Some(grid.playerId2ByteMap(id)),x,y,math.max(grid.frameCount,frame),n))
          Behaviors.same

        case GetBotInfo(id,botActor)=>
          val data = grid.getDataForBot(id,1200,600)
          botActor ! InfoReply(data)
          Behaviors.same

        case botAction(botId,userAction)=>
        userAction match{
          case KC(id,keyCode,frame,n) =>
            if (keyCode == KeyEvent.VK_SPACE) {
              grid.addPlayer(botId, userMap.getOrElse(botId, ("Unknown",0l,0l))._1)
              dispatchTo(subscribersMap)(botId,Protocol.PlayerRestart(botId))
            } else {
//              println(s"get keyCode $keyCode")
              grid.addActionWithFrame(botId, KC(id,keyCode,math.max(grid.frameCount,frame),n))
              dispatch(subscribersMap)(KC(id,keyCode,math.max(grid.frameCount,frame),n))
            }

          case MP(id,x,y,frame,n) =>
            grid.addMouseActionWithFrame(botId,MP(id,x,y,math.max(grid.frameCount,frame),n))
            dispatch(subscribersMap)(MP(id,x,y,math.max(grid.frameCount,frame),n))

        }
          Behaviors.same

        case Sync =>
          grid.getSubscribersMap(subscribersMap)
//          grid.getUserList(userList)
          grid.update()
          val feedapples = grid.getNewApples
          val eventList = grid.getEvents()
          if(AppSettings.gameRecordIsWork){
              getGameRecorder(ctx,grid,roomId) ! GameRecorder.GameRecord(eventList, Some(GypsyGameSnapshot(grid.getSnapShot())))
          }

//          复活列表
//          if(grid.ReLiveMap.nonEmpty){
//            val curTime = System.currentTimeMillis()
//            val ToReLive = grid.ReLiveMap.filter(i=> (curTime - i._2) >AppSettings.reliveTime*1000)
//            val newReLive = ToReLive.map{live =>
//              ctx.self ! ReStart(live._1)
//              (live._1,curTime)
//            }
//            grid.ReLiveMap ++= newReLive
//          }

          if(grid.ReLiveMap.nonEmpty){
            grid.ReLiveMap.foreach{live =>
              ctx.self ! ReStart(live._1)
            }
            grid.ReLiveMap = Map.empty
          }

          if(tickCount % 10 ==0){
            var playerNum = 0
            var allPlayerNum = 0
            val PlayerMap = grid.playerMap.filterNot(id=>id._1.startsWith("bot_"))
            grid.playerMap.foreach(_=>allPlayerNum+=1)
            PlayerMap.foreach(_=>playerNum+=1)
            if(playerNum<AppSettings.botNum && allPlayerNum<AppSettings.botNum){
              for(b <- 1 to (AppSettings.botNum-allPlayerNum)){
                val id = "bot_"+roomId + "_200"+ botId.getAndIncrement()
                val botName = getStarName(new Random(System.nanoTime()).nextInt(AppSettings.starNames.size),b)
                getBotActor(ctx, id) ! BotActor.InitInfo(botName, grid, ctx.self)
              }
            }
          }


          //错峰发送全量数据 与 苹果数据
          val group = tickCount % AppSettings.SyncCount
          var syncUser = Set.empty[String]
          userSyncMap.get(group).foreach{users=>
            if(users.nonEmpty){
              syncUser = users
              val gridData = grid.getAllGridData
              dispatch(subscribersMap.filter(s=>users.contains(s._1)))(gridData)
            }

          }
          //发苹果数据
          if (feedapples.nonEmpty) {
            val foodlists = feedapples.map(s=>Food(s._2,s._1.x,s._1.y)).toList
            dispatch(subscribersMap.filterNot(s=>syncUser.contains(s._1)))(Protocol.FeedApples(foodlists))
            grid.cleanNewApple
          }


//          if (tickCount % 20 == 5) {
//            //remind 此处传输全局数据-同步数据
//            val gridData = grid.getAllGridData
//            dispatch(subscribersMap)(gridData)
//          } else {
//            if (feedapples.nonEmpty) {
//              val foodlists = feedapples.map(s=>Food(s._2,s._1.x,s._1.y)).toList
//              dispatch(subscribersMap)(Protocol.FeedApples(foodlists))
//              grid.cleanNewApple
//            }
//          }

          if (tickCount % 20 == 1) {

            val temp = grid.currentRank.zipWithIndex.splitAt(GameConfig.rankShowNum)
            val PerfectRanks = temp._1.map(r=>RankInfo(r._2+1,r._1))
            val RestRanks = temp._2.map(r=>(r._1.id,RankInfo(r._2+1,r._1)))
            dispatch(subscribersMap)(Protocol.Ranks(PerfectRanks))

            if(RestRanks.nonEmpty){
              RestRanks.foreach{rank=>
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
          idle(roomId,userMap,subscribersMap,userSyncMap,grid,tickCount+1)

        case UserActor.NetTest(id, createTime) =>
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
            dispatchTo(subscribersMap)(p._1,Protocol.GameOverMessage(p._1,p._2.kill,p._2.cells.map(_.mass).sum,overTime-p._2.startTime))
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
  private def getGameRecorder(ctx: ActorContext[Command], grid:GameServer, roomId:Long):ActorRef[GameRecorder.Command] = {
    val childName = s"gameRecorder"
    ctx.child(childName).getOrElse{
      val curTime = System.currentTimeMillis()
      val fileName = s"gypsyGame_${curTime}"
//      val gameInformation = TankGameEvent.GameInformation(curTime,AppSettings.tankGameConfig.getTankGameConfigImpl())
      val gameInformation = GameInformation(curTime)
      val initStateOpt = Some(GypsyGameSnapshot(grid.getSnapShot()))
      val initFrame = grid.frameCount
      val actor = ctx.spawn(GameRecorder.create(fileName,gameInformation,curTime,initFrame,initStateOpt,roomId),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[GameRecorder.Command]
  }

  private def getBotActor(ctx:ActorContext[Command],botId: String) = {
    val childName = botId
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(BotActor.create(botId), childName)
      actor
    }.upcast[BotActor.Command]
  }

  private def getStarName(nameNum:Int,index:Int) = {
    if(AppSettings.starNames.isEmpty){
      "Star"+"-"+index
    }else if(nameNum < AppSettings.starNames.length){
      AppSettings.starNames(nameNum)+"-"+index
    }else{
      AppSettings.starNames.head+"-"+index
    }

  }

}
