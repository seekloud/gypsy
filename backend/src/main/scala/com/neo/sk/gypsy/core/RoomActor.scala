package com.neo.sk.gypsy.core

import java.awt.event.KeyEvent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.testkit.TestActor.Watch
import com.neo.sk.gypsy.Boot._
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.core.RoomManager.RemoveRoom
import com.neo.sk.gypsy.models.Dao.UserDao
import com.neo.sk.gypsy.shared.ptcl.GypsyGameEvent.{GameInformation, GypsyGameConfigImpl, GypsyGameSnapshot, UserJoinRoom}
import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.{Boundary, Point, WsMsgProtocol, WsSourceProtocol}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.CheckNameRsp
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.snake.GridOnServer
import com.neo.sk.gypsy.utils.byteObject.ByteObject
import io.circe.Decoder
import io.circe.parser._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import scala.concurrent.duration._
/**
  * User: sky
  * Date: 2018/8/11
  * Time: 9:22
  */
object RoomActor {
  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command
  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  private case object SyncTimeKey

  private case object Sync extends Command

  private case object TimeOutKey

  private case object TimeOut extends Command

  private case class Join(id: Long, name: String, subscriber: ActorRef[WsMsgProtocol.WsMsgFront],watchgame:Boolean) extends Command

  private case class ChangeWatch(id: Long, watchId: Long) extends Command

  private case class Left(id: Long, name: String) extends Command

  private case class Key(id: Long, keyCode: Int,frame:Long,n:Int) extends Command

  private case class Mouse(id: Long, clientX:Double,clientY:Double,frame:Long,n:Int) extends Command

  private case class NetTest(id: Long, createTime: Long) extends Command

  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  private case object UnKnowAction extends Command

  case class CheckName(name:String,replyTo:ActorRef[CheckNameRsp])extends Command
  case class getGamePlayerList(roomId:Long ,replyTo:ActorRef[RoomPlayerInfoRsp]) extends Command
  case class getRoomId(playerId:String,replyTo:ActorRef[RoomIdRsp]) extends Command

  case class UserInfo(id:Long, name:String, shareList:mutable.ListBuffer[Long]) extends Command

  val bounds = Point(Boundary.w, Boundary.h)

  def create(roomId:Long,matchRoom:Boolean):Behavior[Command] = {
    log.debug(s"RoomActor-$roomId start...")
    Behaviors.setup[Command] { ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]]()
            val userMap = mutable.HashMap[Long, String]()
            val userList = mutable.ListBuffer[UserInfo]()
//            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val grid = new GridOnServer(bounds)
            grid.setRoomId(roomId)
            if(matchRoom){
              timer.startSingleTimer(TimeOutKey,TimeOut,AppSettings.matchTime.seconds)
              wait(roomId,userList,userMap,subscribersMap,grid)
            }else{
              if(AppSettings.gameRecordIsWork){
               getGameRecorder(ctx,grid,roomId.toInt)
              }
              timer.startPeriodicTimer(SyncTimeKey,Sync,WsMsgProtocol.frameRate millis)
              idle(roomId,userList,userMap,subscribersMap,grid,0l)
            }
        }
    }
  }

  def idle(
            roomId:Long,
            userList:mutable.ListBuffer[UserInfo],
            userMap:mutable.HashMap[Long,String],
            subscribersMap:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]],
            grid:GridOnServer,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command]
//            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case CheckName(name,replyTo)=>
          log.info(s"$name check name")
          if(userMap.exists(_._2==name)){
           replyTo ! CheckNameRsp(roomId,10000,"UserName has existed!")
          }else{
            UserDao.getUserByName(name).map{
              case Some(_)=>
                replyTo ! CheckNameRsp(roomId,10000,"UserName has existed!")
              case None=>
                replyTo ! CheckNameRsp(roomId)
            }
          }
          Behavior.same

        case Join(id, name, subscriber,watchgame) =>
          log.info(s"got $msg")
          if(watchgame){
            val x = (new util.Random).nextInt(userList.length)
            userList(x).shareList.append(id)
            ctx.watchWith(subscriber,Left(id,name))
            subscribersMap.put(id,subscriber)
            //观察者前端的id是其观察对象的id
            dispatchTo(subscribersMap,id, WsMsgProtocol.Id(userList(x).id),userList)
            dispatchTo(subscribersMap,id,grid.getGridData(userList(x).id),userList)
          }else{
            userList.append(UserInfo(id, name, mutable.ListBuffer[Long]()))
            userMap.put(id,name)
            ctx.watchWith(subscriber,Left(id,name))
            subscribersMap.put(id,subscriber)
            grid.addSnake(id, name)
            dispatchTo(subscribersMap,id, WsMsgProtocol.Id(id),userList)
            dispatchTo(subscribersMap,id,grid.getGridData(id),userList)
          }
//          dispatchTo(subscribersMap,id, WsMsgProtocol.Id(id),userList)
//          dispatchTo(subscribersMap,id,grid.getGridData(id),userList)
          Behaviors.same

        case ChangeWatch(id, watchId) =>
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
              dispatchTo(subscribersMap,id, WsMsgProtocol.Id(watchId),userList)
              dispatchTo(subscribersMap,id,grid.getGridData(watchId),userList)
            }
          }
          Behaviors.same

        case Left(id, name) =>
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

        case Key(id, keyCode,frame,n) =>
          log.debug(s"got $msg")
          //dispatch(Protocol.TextMsg(s"Aha! $id click [$keyCode]")) //just for test
          if (keyCode == KeyEvent.VK_SPACE) {
            grid.addSnake(id, userMap.getOrElse(id, "Unknown"))
            dispatchTo(subscribersMap,id,WsMsgProtocol.SnakeRestart(id),userList)
          } else {
            grid.addActionWithFrame(id, KeyCode(id,keyCode,math.max(grid.frameCount,frame),n))
            dispatch(subscribersMap,KeyCode(id,keyCode,math.max(grid.frameCount,frame),n))
          }
          Behaviors.same

        case Mouse(id,x,y,frame,n) =>
          log.debug(s"gor $msg")
          //为什么一个动作要插入两次？
          grid.addMouseActionWithFrame(id,MousePosition(id,x,y,math.max(grid.frameCount,frame),n))
          dispatch(subscribersMap,MousePosition(id,x,y,math.max(grid.frameCount,frame),n))
          Behaviors.same

        case Sync =>
          grid.getSubscribersMap(subscribersMap)
          grid.getUserList(userList)
          grid.update()
          val feedApples = grid.getFeededApple
          val gridData = grid.getAllGridData
          val eventList = grid.getEvents()
          if(AppSettings.gameRecordIsWork){
            getGameRecorder(ctx,grid,roomId) ! GameRecorder.GameRecord(eventList, Some(GypsyGameSnapshot(grid.getSnapShot())))
          }

          if (tickCount % 20 == 5) {
            //remind 此处传输全局数据-同步数据
//            val gridData = grid.getAllGridData
            dispatch(subscribersMap,gridData)
          } else {
            if (feedApples.nonEmpty) {
              dispatch(subscribersMap,WsMsgProtocol.FeedApples(feedApples))
            }
          }
          if (tickCount % 20 == 1) {
            dispatch(subscribersMap,WsMsgProtocol.Ranks(grid.currentRank, grid.historyRankList))
          }
          if(tickCount==0){
//            dispatch(subscribersMap,grid.getAllGridData)
            dispatch(subscribersMap,gridData)
          }
          idle(roomId,userList,userMap,subscribersMap,grid,tickCount+1)

        case NetTest(id, createTime) =>
          //log.info(s"Net Test: createTime=$createTime")
          //log.info(s"Net Test: createTime=$createTime")
          dispatchTo(subscribersMap,id, WsMsgProtocol.Pong(createTime),userList)
          Behaviors.same

//          不明其意
        case ChildDead(name, childRef) =>
          log.debug(s"${ctx.self.path} recv a msg:${msg}")
          ctx.unwatch(childRef)
          Behaviors.same


        case TimeOut=>
          val overTime=System.currentTimeMillis()
          grid.playerMap.foreach{p=>
            dispatchTo(subscribersMap,p._1,WsMsgProtocol.GameOverMessage(p._1,p._2.kill,p._2.cells.map(_.mass).sum.toInt,overTime-p._2.startTime),userList)
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
          val IsqueryUser = if(userMap.keySet.contains(playerId.toLong)) true else false
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

  /**
    * 本状态为等待匹配创建房间*/
  def wait(
            roomId:Long,
            userList:mutable.ListBuffer[UserInfo],
            userMap:mutable.HashMap[Long,String],
            subscribersMap:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]],
            grid:GridOnServer)(implicit timer:TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case CheckName(name,replyTo)=>
          log.info(s"$name check name")
          if(userMap.exists(_._2 == name)){
            replyTo ! CheckNameRsp(roomId,10000,"UserName has existed!")
          }else{
            UserDao.getUserByName(name).map{
              case Some(_)=>
                replyTo ! CheckNameRsp(roomId,10000,"UserName has existed!")
              case None=>
                replyTo ! CheckNameRsp(roomId)
            }
          }
          Behavior.same

        case Join(id, name, subscriber,watchgame) =>
          log.info(s"got $msg")
          userMap.put(id,name)
          ctx.watchWith(subscriber,Left(id,name))
          subscribersMap.put(id,subscriber)
          grid.addSnake(id, name)
          if(userMap.size>1){
            timer.cancel(TimeOutKey)
            timer.startPeriodicTimer(SyncTimeKey,Sync,WsMsgProtocol.frameRate millis)
            timer.startSingleTimer(TimeOutKey,TimeOut,AppSettings.gameTime.minutes)
            userMap.keys.foreach{r=>
              dispatchTo(subscribersMap,r, WsMsgProtocol.Id(r),userList)
              dispatchTo(subscribersMap,r,grid.getGridData(r),userList)
            }
            idle(roomId,userList,userMap,subscribersMap,grid,0l)
          }else{
            Behaviors.same
          }

        case TimeOut=>
          log.info("matchRoom timeOut!!")
          roomManager ! RemoveRoom(roomId)
          dispatch(subscribersMap,MatchRoomError)
          Behaviors.stopped

        case Left(id, name) =>
          log.info(s"got $msg")
          subscribersMap.get(id).foreach(r=>ctx.unwatch(r))
          grid.removePlayer(id)
          userMap.remove(id)
          subscribersMap.remove(id)
          if (userMap.isEmpty){
            Behaviors.stopped
          }else{
            Behaviors.same
          }

        case x=>
          Behaviors.unhandled
      }
    }
  }

  def dispatch(subscribers:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]], msg: WsMsgProtocol.WsMsgFront) = {
    subscribers.values.foreach( _ ! msg)
  }

  def dispatchTo(subscribers:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]], id:Long, msg:WsMsgProtocol.WsMsgFront,userList:mutable.ListBuffer[UserInfo]) = {
    var shareList = mutable.ListBuffer[Long]()
    userList.foreach(user =>
      if(user.id == id){
        shareList = user.shareList
      }
    )
    subscribers.get(id).foreach( _ ! msg)
    shareList.foreach(shareId=>
      subscribers.get(shareId).foreach( _ ! msg)
    )
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def joinGame(actor:ActorRef[RoomActor.Command], id: Long, name: String,watchgame: Boolean)(implicit decoder: Decoder[MousePosition]): Flow[WsMsgProtocol.WsMsgServer, WsSourceProtocol.WsMsgSource, Any] = {
    val in = Flow[WsMsgProtocol.WsMsgServer]
      .map {
        case KeyCode(i,keyCode,f,n)=>
          log.debug(s"键盘事件$keyCode")
          Key(id,keyCode,f,n)
        case MousePosition(i,clientX,clientY,f,n)=>
          Mouse(id,clientX,clientY,f,n)
        case UserLeft=>
          Left(id,name)
        case WsMsgProtocol.Ping(timestamp)=>
          NetTest(id,timestamp)
        case WatchChange(id, watchId) =>
          log.debug(s"切换观察者: $watchId")
          ChangeWatch(id, watchId)
        case _=>
          UnKnowAction
      }
      .to(sink(actor))

    val out =
      ActorSource.actorRef[WsSourceProtocol.WsMsgSource](
        completionMatcher = {
          case WsSourceProtocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case WsSourceProtocol.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 64,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! Join(id, name, outActor,watchgame))
    Flow.fromSinkAndSource(in, out)
  }


  //暂未考虑下匹配的情况
//  private def getGameRecorder(ctx: ActorContext[Command],gameContainer:GameContainerServerImpl,roomId:Long):ActorRef[GameRecorder.Command] = {
  private def getGameRecorder(ctx: ActorContext[Command],grid:GridOnServer,roomId:Long):ActorRef[GameRecorder.Command] = {
    val childName = s"gameRecorder"
    ctx.child(childName).getOrElse{
      val curTime = System.currentTimeMillis()
      val fileName = s"gypsyGame_${curTime}"
//      val gameInformation = TankGameEvent.GameInformation(curTime,AppSettings.tankGameConfig.getTankGameConfigImpl())
//      val gameInformation = GameInformation(curTime,GypsyGameConfigImpl())
      val gameInformation = ""
      val initStateOpt = Some(GypsyGameSnapshot(grid.getSnapShot()))
      val actor = ctx.spawn(GameRecorder.create(fileName,curTime,initStateOpt,roomId),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[GameRecorder.Command]
  }


}
