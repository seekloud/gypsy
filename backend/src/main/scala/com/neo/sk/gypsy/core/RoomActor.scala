package com.neo.sk.gypsy.core

import java.awt.event.KeyEvent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.gypsy.Boot._
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.core.RoomManager.RemoveRoom
import com.neo.sk.gypsy.core.UserActor.JoinRoomSuccess
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.{Boundary, Point, WsMsgProtocol}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.CheckNameRsp
import com.neo.sk.gypsy.gypsyServer.GameServer
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

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

  private case object TimeOut extends Command

 // private case class Join(id: String, name: String, subscriber: ActorRef[UserActor.Command],watchgame:Boolean) extends Command

  case class JoinRoom(uid:String,name:String,startTime:Long,userActor:ActorRef[UserActor.Command],watchId:Option[String],watchgame:Boolean) extends Command

  case class WebSocketMsg(uid:String,req:Protocol.UserAction) extends Command with RoomManager.Command

//  private case class ChangeWatch(id: String, watchId: String) extends Command

//  private case class Left(id: String, name: String) extends Command
//
//  private case class Key(id: String, keyCode: Int,frame:Long,n:Int) extends Command
//
//  private case class Mouse(id: String, clientX:Double,clientY:Double,frame:Long,n:Int) extends Command
//
//  private case class NetTest(id: String, createTime: Long) extends Command
//
  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  private case object UnKnowAction extends Command

  case class CheckName(name:String,replyTo:ActorRef[CheckNameRsp])extends Command
  case class getGamePlayerList(roomId:Long ,replyTo:ActorRef[RoomPlayerInfoRsp]) extends Command
  case class getRoomId(playerId:String,replyTo:ActorRef[RoomIdRsp]) extends Command

  case class UserInfo(id:String, name:String, shareList:mutable.ListBuffer[String]) extends Command

  val bounds = Point(Boundary.w, Boundary.h)

  def create(roomId:Long,matchRoom:Boolean):Behavior[Command] = {
    log.debug(s"RoomActor-$roomId start...")
    Behaviors.setup[Command] { ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[String,ActorRef[UserActor.Command]]()
            val userMap = mutable.HashMap[String, String]()
            val userList = mutable.ListBuffer[UserInfo]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val grid = new GameServer(bounds)
            grid.setRoomId(roomId)
//            if(matchRoom){
//              timer.startSingleTimer(TimeOutKey,TimeOut,AppSettings.matchTime.seconds)
//              wait(roomId,userList,userMap,subscribersMap,grid)
//            }else{
              if(AppSettings.gameRecordIsWork){
               getGameRecorder(ctx,grid,roomId.toInt)
              }
              timer.startPeriodicTimer(SyncTimeKey,Sync,WsMsgProtocol.frameRate millis)
              idle(roomId,userList,userMap,subscribersMap,grid,0l)

        }
    }
  }

  def idle(
            roomId:Long,
            userList:mutable.ListBuffer[UserInfo],
            userMap:mutable.HashMap[String,String],
            subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]],
            grid:GameServer,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command],
            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case JoinRoom(id, name, startTime,subscriber,watchIdOpt,watchgame) =>
          log.info(s"got $msg")
          if(watchgame){
            val x = (new util.Random).nextInt(userList.length)
            userList(x).shareList.append(id)
            ctx.watchWith(subscriber,UserActor.Left(id,name))
            subscribersMap.put(id,subscriber)
            subscriber ! JoinRoomSuccess(id,ctx.self)
            //观察者前端的id是其观察对象的id
            dispatchTo(subscribersMap)(id,Protocol.Id(userList(x).id))
            dispatchTo(subscribersMap)(id,grid.getGridData(userList(x).id))
          }else{
            userList.append(UserInfo(id, name, mutable.ListBuffer[String]()))
            userMap.put(id,name)
            ctx.watchWith(subscriber,UserActor.Left(id,name))
            subscribersMap.put(id,subscriber)
            grid.addPlayer(id, name)
            subscriber ! JoinRoomSuccess(id,ctx.self)
            dispatchTo(subscribersMap)(id, Protocol.Id(id))
            dispatchTo(subscribersMap)(id,grid.getGridData(id))
          }
          //          dispatchTo(subscribersMap,id, WsMsgProtocol.Id(id),userList)
          //          dispatchTo(subscribersMap,id,grid.getGridData(id),userList)
          Behaviors.same

        case UserActor.ChangeWatch(id, watchId) =>
          log.info(s"get $msg")
          for(i<- 0 until userList.length){
            for(j<-0 until userList(i).shareList.length){
              if(userList(i).shareList(j) == id){
                userList(i).shareList.remove(j)
              }
            }
            if(userList(i).id == watchId){
              userList(i).shareList.append(id)
              //切换视角
              dispatchTo(subscribersMap)(id, Protocol.Id(watchId))
              dispatchTo(subscribersMap)(id,grid.getGridData(watchId))
            }
          }
          Behaviors.same

        case UserActor.Left(id, name) =>
          log.info(s"got $msg")
          subscribersMap.get(id).foreach(r=>ctx.unwatch(r))
          grid.removePlayer(id)
         // dispatch(subscribersMap,Protocol.PlayerLeft(id, name))
          userMap.remove(id)
          //玩家离开or观战者离开
          for(i<-0 until userList.length){
            //观战者离开
            for(j<-0 until userList(i).shareList.length){
              if(userList(i).shareList(j) == id){
                userList(i).shareList.remove(j)
              }
            }
            //玩家离开
            if(userList(i).id == id){
              userList.remove(i)
            }
          }
          subscribersMap.remove(id)
          Behaviors.same

        case UserActor.Key(id, keyCode,frame,n) =>
          log.debug(s"got $msg")
          //dispatch(Protocol.TextMsg(s"Aha! $id click [$keyCode]")) //just for test
          if (keyCode == KeyEvent.VK_SPACE) {
            grid.addPlayer(id, userMap.getOrElse(id, "Unknown"))
            dispatchTo(subscribersMap)(id,Protocol.SnakeRestart(id))
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

          val gridData = grid.getAllGridData

          val feedapples = gridData.newFoodDetails
          val eventList = grid.getEvents()
//          println(s"fra : ${grid.frameCount} ${eventList}")
          if(AppSettings.gameRecordIsWork){
            getGameRecorder(ctx,grid,roomId) ! GameRecorder.GameRecord(eventList, Some(GypsyGameSnapshot(grid.getSnapShot())))
          }

          if (tickCount % 20 == 5) {
            //remind 此处传输全局数据-同步数据
//            val gridData = grid.getAllGridData
            dispatch(subscribersMap)(gridData)
          } else {
            if (feedapples.nonEmpty) {
//              dispatch(subscribersMap,WsMsgProtocol.FeedApples(newApples))
              dispatch(subscribersMap)(Protocol.FeedApples(feedapples))
              grid.cleanNewApple
            }
          }
          if (tickCount % 20 == 1) {
            dispatch(subscribersMap)(Protocol.Ranks(grid.currentRank, grid.historyRankList))
          }
          if(tickCount==0){
//            dispatch(subscribersMap,grid.getAllGridData)
            dispatch(subscribersMap)(gridData)
          }
          idle(roomId,userList,userMap,subscribersMap,grid,tickCount+1)

        case UserActor.NetTest(id, createTime) =>
          //log.info(s"Net Test: createTime=$createTime")
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

        case getGamePlayerList(_ ,replyTo) =>
          val playerList=userMap.map{i=>PlayerInfo(i._1.toString,i._2)}.toList
          if(playerList!=null){
            replyTo ! RoomPlayerInfoRsp(players(playerList),0,"ok")
          }
          else{
            replyTo ! RoomPlayerInfoRsp(players(playerList),404,"该房间内没有玩家")
          }
          Behaviors.same

        case getRoomId(playerId,replyTo) =>
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

//  /**
//    * 本状态为等待匹配创建房间*/
//  def wait(
//            roomId:Long,
//            userList:mutable.ListBuffer[UserInfo],
//            userMap:mutable.HashMap[String,String],
//            subscribersMap:mutable.HashMap[String,ActorRef[WsMsgSource]],
//            grid:GameServer)(implicit timer:TimerScheduler[Command]):Behavior[Command] = {
//    Behaviors.receive { (ctx, msg) =>
//      msg match {
////        case CheckName(name,replyTo)=>
////          log.info(s"$name check name")
////          if(userMap.exists(_._2 == name)){
////            replyTo ! CheckNameRsp(roomId,10000,"UserName has existed!")
////          }else{
////            UserDao.getUserByName(name).map{
////              case Some(_)=>
////                replyTo ! CheckNameRsp(roomId,10000,"UserName has existed!")
////              case None=>
////                replyTo ! CheckNameRsp(roomId)
////            }
////          }
////          Behavior.same
//
//        case Join(id, name, subscriber,watchgame) =>
//          log.info(s"got $msg")
//          userMap.put(id,name)
//          ctx.watchWith(subscriber,Left(id,name))
//          subscribersMap.put(id,subscriber)
//          grid.addSnake(id, name)
//          if(userMap.size>1){
//            timer.cancel(TimeOutKey)
//            timer.startPeriodicTimer(SyncTimeKey,Sync,WsMsgProtocol.frameRate millis)
//            timer.startSingleTimer(TimeOutKey,TimeOut,AppSettings.gameTime.minutes)
//            userMap.keys.foreach{r=>
//              dispatchTo(subscribersMap,r, Protocol.Id(r),userList)
//              dispatchTo(subscribersMap,r,grid.getGridData(r),userList)
//            }
//            idle(roomId,userList,userMap,subscribersMap,grid,0l)
//          }else{
//            Behaviors.same
//          }
//
//        case TimeOut=>
//          log.info("matchRoom timeOut!!")
//          roomManager ! RemoveRoom(roomId)
//          dispatch(subscribersMap,MatchRoomError())
//          Behaviors.stopped
//
//        case Left(id, name) =>
//          log.info(s"got $msg")
//          subscribersMap.get(id).foreach(r=>ctx.unwatch(r))
//          grid.removePlayer(id)
//          userMap.remove(id)
//          subscribersMap.remove(id)
//          if (userMap.isEmpty){
//            Behaviors.stopped
//          }else{
//            Behaviors.same
//          }
//
//        case x=>
//          Behaviors.unhandled
//      }
//    }
//  }

//  def dispatch(subscribers:mutable.HashMap[String,ActorRef[WsMsgSource]], msg: WsMsgSource) = {
//    subscribers.values.foreach( _ ! msg)
//  }
//
//  def dispatchTo(subscribers:mutable.HashMap[String,ActorRef[WsMsgSource]], id:String, msg:WsMsgSource, userList:mutable.ListBuffer[UserInfo]) = {
//    var shareList = mutable.ListBuffer[String]()
//    userList.foreach(user =>
//      if(user.id == id){
//        shareList = user.shareList
//      }
//    )
//    subscribers.get(id).foreach( _ ! msg)
//    shareList.foreach(shareId=>
//      subscribers.get(shareId).foreach( _ ! msg)
//    )

  def dispatch(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }

  def dispatchTo(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(id:String,msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {

    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.get(id).foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))

  }

//  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
//    ref = actor,
//    onCompleteMessage = CompleteMsgFront,
//    onFailureMessage = FailMsgFront.apply
//  )

//  def joinGame(actor:ActorRef[RoomActor.Command], id: String, name: String,watchgame: Boolean)(implicit decoder: Decoder[UserAction]): Flow[UserAction,WsMsgSource, Any] = {
//    val in = Flow[UserAction]
//      .map {
//        case KeyCode(i,keyCode,f,n)=>
//          log.debug(s"键盘事件$keyCode")
//          Key(id,keyCode,f,n)
//        case MousePosition(i,clientX,clientY,f,n)=>
//          Mouse(id,clientX,clientY,f,n)
//        case UserLeft()=>
//          Left(id,name)
//        case Ping(timestamp)=>
//          NetTest(id,timestamp)
//        case WatchChange(id, watchId) =>
//          log.debug(s"切换观察者: $watchId")
//          ChangeWatch(id, watchId)
//        case _=>
//          UnKnowAction
//      }
//      .to(sink(actor))
//
//    val out =
//      ActorSource.actorRef[WsMsgSource](
//        completionMatcher = {
//          case CompleteMsgServer ⇒
//        },
//        failureMatcher = {
//          case FailMsgServer(e)  ⇒ e
//        },
//        bufferSize = 64,
//        overflowStrategy = OverflowStrategy.dropHead
//      ).mapMaterializedValue(outActor => actor ! Join(id, name, outActor,watchgame))
//    Flow.fromSinkAndSource(in, out)
//  }


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
      val initFrame = grid.frameCount
      val actor = ctx.spawn(GameRecorder.create(fileName,gameInformation,curTime,initFrame,initStateOpt,roomId),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[GameRecorder.Command]
  }


}
