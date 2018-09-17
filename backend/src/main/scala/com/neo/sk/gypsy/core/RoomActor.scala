package com.neo.sk.gypsy.core

import java.awt.event.KeyEvent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.gypsy.Boot._
import com.neo.sk.gypsy.models.Dao.UserDao
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.{Boundary, Point, WsMsgProtocol, WsSourceProtocol}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.CheckNameRsp
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{KeyCode, MousePosition, UserLeft}
import com.neo.sk.gypsy.snake.GridOnServer
import io.circe.Decoder
import io.circe.parser._
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

  private case class Join(id: Long, name: String, subscriber: ActorRef[WsMsgProtocol.WsMsgFront]) extends Command

  private case class Left(id: Long, name: String) extends Command

  private case class Key(id: Long, keyCode: Int,frame:Long,n:Int) extends Command

  private case class Mouse(id: Long, clientX:Double,clientY:Double,frame:Long,n:Int) extends Command

  private case class NetTest(id: Long, createTime: Long) extends Command

  private case object UnKnowAction extends Command

  case class CheckName(name:String,replyTo:ActorRef[CheckNameRsp])extends Command



  val bounds = Point(Boundary.w, Boundary.h)

  def create(room:String):Behavior[Command] = {
    log.debug(s"RoomActor-$room start...")
    Behaviors.setup[Command] { ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]]()
            val userMap = mutable.HashMap[Long, String]()
            val grid = new GridOnServer(bounds)
            timer.startPeriodicTimer(SyncTimeKey,Sync,WsMsgProtocol.frameRate millis)
            idle(userMap,subscribersMap,grid,0l)
        }
    }
  }

  def idle(
            userMap:mutable.HashMap[Long,String],
            subscribersMap:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]],
            grid:GridOnServer,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command]
          ):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {

        case CheckName(name,replyTo)=>
          log.info(s"$name check name")
          if(grid.playerMap.exists(_._2.name.trim==name)){
           replyTo ! CheckNameRsp(10000,"UserName has existed!")
          }else{
            UserDao.getUserByName(name).map{
              case Some(_)=>
                replyTo ! CheckNameRsp(10000,"UserName has existed!")
              case None=>
                replyTo ! CheckNameRsp()
            }
          }
          Behavior.same
        case Join(id, name, subscriber) =>
          log.info(s"got $msg")
          userMap.put(id,name)
          ctx.watchWith(subscriber,Left(id,name))
          subscribersMap.put(id,subscriber)
          grid.addSnake(id, name)
          dispatchTo(subscribersMap,id, WsMsgProtocol.Id(id))
          dispatchTo(subscribersMap,id,grid.getGridData(id))
          Behaviors.same
        case Left(id, name) =>
          log.info(s"got $msg")
          subscribersMap.get(id).foreach(r=>ctx.unwatch(r))
          grid.removePlayer(id)
         // dispatch(subscribersMap,Protocol.PlayerLeft(id, name))
          Behaviors.same
        case Key(id, keyCode,frame,n) =>
          log.debug(s"got $msg")
          //dispatch(Protocol.TextMsg(s"Aha! $id click [$keyCode]")) //just for test
          if (keyCode == KeyEvent.VK_SPACE) {
            grid.addSnake(id, userMap.getOrElse(id, "Unknown"))
            dispatchTo(subscribersMap,id,WsMsgProtocol.SnakeRestart(id))
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
          grid.update()
          val feedApples = grid.getFeededApple
          if (tickCount % 20 == 5) {
            //remind 此处传输全局数据-同步数据
            val gridData = grid.getAllGridData
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
            dispatch(subscribersMap,grid.getAllGridData)
          }
          idle(userMap,subscribersMap,grid,tickCount+1)
        case NetTest(id, createTime) =>
          //log.info(s"Net Test: createTime=$createTime")
          //log.info(s"Net Test: createTime=$createTime")
          dispatchTo(subscribersMap,id, WsMsgProtocol.Pong(createTime))
          Behaviors.same
        case x =>
          log.warn(s"got unknown msg: $x")
          Behaviors.unhandled
      }
    }
  }

  def dispatch(subscribers:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]], msg: WsMsgProtocol.WsMsgFront) = {
    subscribers.values.foreach( _ ! msg)
  }

  def dispatchTo(subscribers:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]], id:Long, msg:WsMsgProtocol.WsMsgFront) = {
    subscribers.get(id).foreach( _ ! msg)
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def joinGame(actor:ActorRef[RoomActor.Command], id: Long, name: String)(implicit decoder: Decoder[MousePosition]): Flow[WsMsgProtocol.WsMsgServer, WsSourceProtocol.WsMsgSource, Any] = {
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
      ).mapMaterializedValue(outActor => actor ! Join(id, name, outActor))
    Flow.fromSinkAndSource(in, out)
  }
}
