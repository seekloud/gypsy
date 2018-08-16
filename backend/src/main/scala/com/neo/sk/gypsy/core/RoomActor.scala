package com.neo.sk.gypsy.core

import java.awt.event.KeyEvent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.gypsy.shared.ptcl.Protocol.MousePosition
import com.neo.sk.gypsy.shared.ptcl.{Boundary, Point, Protocol, WsServerSourceProtocol}
import com.neo.sk.gypsy.shared.ptcl.Protocol.MousePosition
import com.neo.sk.gypsy.snake.GridOnServer
import io.circe.Decoder
import io.circe.parser._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext
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

  private case class Join(id: Long, name: String, subscriber: ActorRef[Protocol.GameMessage]) extends Command

  private case class Left(id: Long, name: String) extends Command

  private case class Key(id: Long, keyCode: Int) extends Command

  private case class Mouse(id: Long, clientX:Double,clientY:Double) extends Command

  private case class NetTest(id: Long, createTime: Long) extends Command

//  case class TextInfo(msg: String) extends Command



  val bounds = Point(Boundary.w, Boundary.h)

  def create(room:String):Behavior[Command] = {
    log.debug(s"RoomActor-$room start...")
    Behaviors.setup[Command] { ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[Long,ActorRef[Protocol.GameMessage]]()
            val userMap = mutable.HashMap[Long, String]()
            val grid = new GridOnServer(bounds)
            timer.startPeriodicTimer(SyncTimeKey,Sync,Protocol.frameRate millis)
            idle(userMap,subscribersMap,grid,0l)
        }
    }
  }

  def idle(
            userMap:mutable.HashMap[Long,String],
            subscribersMap:mutable.HashMap[Long,ActorRef[Protocol.GameMessage]],
            grid:GridOnServer,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command]
          ):Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Join(id, name, subscriber) =>
          log.info(s"got $msg")
          userMap += (id -> name)
          ctx.watch(subscriber)
          subscribersMap.put(id,subscriber)
          grid.addSnake(id, name)
          dispatchTo(subscribersMap,id, Protocol.Id(id))
          dispatch(subscribersMap,Protocol.NewSnakeJoined(id, name))
          dispatch(subscribersMap,grid.getGridData)
          Behaviors.same
        case Left(id, name) =>
          log.info(s"got $msg")
          subscribersMap.get(id).foreach(ctx.unwatch)
          subscribersMap.remove(id)
          grid.removePlayer(id)
          dispatch(subscribersMap,Protocol.PlayerLeft(id, name))
          Behaviors.same
        case Key(id, keyCode) =>
          log.debug(s"got $msg")
          //dispatch(Protocol.TextMsg(s"Aha! $id click [$keyCode]")) //just for test
          if (keyCode == KeyEvent.VK_SPACE) {
            grid.addSnake(id, userMap.getOrElse(id, "Unknown"))
            dispatchTo(subscribersMap,id,Protocol.SnakeRestart(id))
          } else {
            grid.addAction(id, keyCode)
            dispatch(subscribersMap,Protocol.SnakeAction(id, keyCode, grid.frameCount))
          }
          Behaviors.same
        case Mouse(id,x,y) =>
          log.debug(s"gor $msg")
          //为什么一个动作要插入两次？
          grid.addMouseAction(id,x,y)
          dispatch(subscribersMap,Protocol.SnakeMouseAction(id,x,y,grid.frameCount))
          Behaviors.same

       /* case Terminated(actor) =>
          log.warn(s"got $r")
          subscribers.find(_._2.equals(actor)).foreach { case (id, _) =>
            log.debug(s"got Terminated id = $id")
            subscribers -= id
            grid.removePlayer(id).foreach(s => dispatch(Protocol.PlayerLeft(id, s.name)))
          }
          Behaviors.same*/

        case Sync =>
          grid.update()
          val feedApples = grid.getFeededApple
          if (tickCount % 20 == 5) {
            val gridData = grid.getGridData
            dispatch(subscribersMap,gridData)
          } else {
            if (feedApples.nonEmpty) {
              dispatch(subscribersMap,Protocol.FeedApples(feedApples))
            }
          }
          if (tickCount % 20 == 1) {
            dispatch(subscribersMap,Protocol.Ranks(grid.currentRank, grid.historyRankList))
          }
          idle(userMap,subscribersMap,grid,tickCount+1)
          Behaviors.same
        case NetTest(id, createTime) =>
          log.info(s"Net Test: createTime=$createTime")
          dispatchTo(subscribersMap,id, Protocol.NetDelayTest(createTime))
          Behaviors.same
        case x =>
          log.warn(s"got unknown msg: $x")
          Behaviors.unhandled
      }
    }
  }

  def dispatch(subscribers:mutable.HashMap[Long,ActorRef[Protocol.GameMessage]],msg: Protocol.GameMessage) = {
    subscribers.values.foreach( _ ! msg)
  }

  def dispatchTo(subscribers:mutable.HashMap[Long,ActorRef[Protocol.GameMessage]],id:Long,msg:Protocol.GameMessage) = {
    subscribers.get(id).foreach( _ ! msg)
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def joinGame(actor:ActorRef[RoomActor.Command], id: Long, name: String)(implicit decoder: Decoder[MousePosition]): Flow[String, WsServerSourceProtocol.WsMsgSource, Any] = {
    val in = Flow[String]
      .map { s =>
        if (s.startsWith("T")) {
          val timestamp = s.substring(1).toLong
          NetTest(id, timestamp)
        }else if(s.contains("LEFT")){
          Left(id,name)
        } else {
          decode[MousePosition](s) match{
            case Right(mouse)=>
              Mouse(id,mouse.clientX,mouse.clientY)
            case k=>
              log.debug(s"键盘事件$k")
              Key(id, s.toInt)
          }
        }
      }
      .to(sink(actor))

    val out =
      ActorSource.actorRef[WsServerSourceProtocol.WsMsgSource](
        completionMatcher = {
          case WsServerSourceProtocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case WsServerSourceProtocol.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 64,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! Join(id, name, outActor))
    Flow.fromSinkAndSource(in, out)
  }
}
