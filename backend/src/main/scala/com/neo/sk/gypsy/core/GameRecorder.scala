package com.neo.sk.gypsy.core

import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.gypsy.common.AppSettings
import com.neo.sk.gypsy.ptcl.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey}
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl
import org.seekloud.byteobject._
import org.seekloud.essf.io.FrameOutputStream
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration._
import com.neo.sk.gypsy.utils.ESSFSupport._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.encoder.BytesEncoder
import com.neo.sk.gypsy.models.SlickTables._
import com.neo.sk.gypsy.models.Dao._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import scala.language.implicitConversions
import com.neo.sk.gypsy.utils.ESSFSupport.userMapEncode
import com.neo.sk.gypsy.Boot.executor

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}
import scala.concurrent.{Await, Future}
import org.seekloud.byteobject.encoder.BytesEncoder

object GameRecorder {


  sealed trait Command

  final case class GameRecord(event:(List[GameEvent],Option[Protocol.GameSnapshot])) extends Command
  final case class SaveDate(left:Boolean) extends Command
  final case object Save extends Command
  final case object RoomClose extends Command
  final case object StopRecord extends Command


  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey
  private final case object SaveDateKey
  private final val saveTime = 2.minute

  final case class SwitchBehavior(
                                  name: String,
                                  behavior: Behavior[Command],
                                  durationOpt: Option[FiniteDuration] = None,
                                  timeOut: TimeOut = TimeOut("busy time error")
                                ) extends Command

  case class TimeOut(msg:String) extends Command

  final case class GameRecorderData(
                                     roomId: Long,
                                     fileName: String,
                                     fileIndex:Int,
                                     gameInformation: GameInformation,
                                     InitialTime: Long, //本房间内记录最最开始的事件
                                     StartTime:Long, //该记录开始时间
                                     StartFrame:Long,
                                     initStateOpt: Option[Protocol.GameSnapshot],
                                     recorder:FrameOutputStream,
                                     var gameRecordBuffer:List[GameRecord]
                                  )

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                    timer:TimerScheduler[Command]) = {
    //log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }


  private final val maxRecordNum = 100
  private final val fileMaxRecordNum = 100000000
  private final val log = LoggerFactory.getLogger(this.getClass)

  def create(fileName:String,gameInformation: GameInformation, InitialTime: Long, startFrame:Long,initStateOpt:Option[Protocol.GameSnapshot] = None, roomId: Long):Behavior[Command] = {
    Behaviors.setup{ ctx =>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val middleBuffer = new MiddleBufferInJvm(10 * 4096)
      Behaviors.withTimers[Command] { implicit timer =>
        val fileRecorder = initFileRecorder(fileName,0,gameInformation,initStateOpt)
        val gameRecordBuffer:List[GameRecord] = List[GameRecord]()
        val data = GameRecorderData(roomId,fileName,0,gameInformation,InitialTime,InitialTime,startFrame,initStateOpt,fileRecorder,gameRecordBuffer)
        timer.startSingleTimer(SaveDateKey, Save, saveTime)
        switchBehavior(ctx,"work",work(data,mutable.HashMap.empty[EssfMapKey,EssfMapJoinLeftInfo],mutable.HashMap.empty[String,(Long,String,Long)],mutable.HashMap.empty[String,(Long,String,Long)], startFrame, -1L))
      }
    }
  }


  private def work(gameRecordData: GameRecorderData,
    essfMap: mutable.HashMap[EssfMapKey,EssfMapJoinLeftInfo],
    userAllMap: mutable.HashMap[String,(Long,String,Long)],  //userId = > (roomId,name,ballId)
    userMap: mutable.HashMap[String,(Long,String,Long)],
    startF: Long,
    endF: Long,
  )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    middleBuffer: MiddleBufferInJvm
  ) : Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive[Command]{ (ctx,msg) =>
      msg match {
        case t:GameRecord =>
          //log.info(s"${ctx.self.path} work get msg gameRecord")
          val wsMsg = t.event._1
          wsMsg.foreach{
            case r@UserWsJoin(roomId ,userId,userName,ballId,frame) =>
              println(s"record: ${r}")
              userAllMap.put(userId,(roomId,userName,ballId))
              userMap.put(userId, (roomId,userName,ballId))
              essfMap.put(EssfMapKey(roomId,userId, userName,ballId), EssfMapJoinLeftInfo(frame, -1l))
 //             println(s"11111111111111111essfMap$essfMap")

            case r@UserLeftRoom(userId, name,ballId,roomId,frame) =>
              println(s"left ${r}  ")
              if (userMap.contains(userId)){
                userMap.remove(userId)
              }
  //            println(s"11111111111111111essfMap$essfMap")
              val startF = essfMap(EssfMapKey(roomId, userId, name,ballId)).joinF
              essfMap.put(EssfMapKey(roomId, userId,name,ballId), EssfMapJoinLeftInfo(startF,frame))

            case _ =>

          }
          gameRecordBuffer = t :: gameRecordBuffer
          val newEndF = t.event._2.get match {
            case syncdata:GypsyGameSnapshot =>
              syncdata.state.frameCount
          }
          //每100帧记录一次全量数据
          if(gameRecordBuffer.size > maxRecordNum){
            val rs = gameRecordBuffer.reverse
            rs.headOption.foreach{ e =>
              recorder.writeFrame(e.event._1.fillMiddleBuffer(middleBuffer).result(),e.event._2.map(_.fillMiddleBuffer(middleBuffer).result()))
              rs.tail.foreach{e =>
                if(e.event._1.nonEmpty){
                  recorder.writeFrame(e.event._1.fillMiddleBuffer(middleBuffer).result())
                }else{
                  recorder.writeEmptyFrame()
                }
              }
            }

            gameRecordBuffer = List[GameRecord]()
            switchBehavior(ctx,"work",work(gameRecordData,essfMap,userAllMap,userMap, startF, newEndF))
          }else{
            switchBehavior(ctx,"work",work(gameRecordData,essfMap,userAllMap,userMap, startF, newEndF))
          }

        case Save =>
          log.info(s"${ctx.self.path} work get msg save")
          timer.startSingleTimer(SaveDateKey, Save, saveTime)
          ctx.self ! SaveDate(false)
          switchBehavior(ctx,"save",save(gameRecordData,essfMap,userAllMap,userMap,startF,endF))

        case RoomClose =>
          log.info(s"${ctx.self.path} work get msg save, room close")
          ctx.self ! SaveDate(true)
          switchBehavior(ctx,"save",save(gameRecordData,essfMap,userAllMap,userMap,startF,endF))


        case unknow =>
          log.warn(s"${ctx.self.path} recv an unknown msg:${unknow}")
          Behaviors.same
      }


    }.receiveSignal{
      case (ctx,PostStop) =>
        timer.cancelAll()
        log.info(s"${ctx.self.path} stopping....")
        // todo  保存信息
        val mapInfo = essfMap.map{
          essf=>
            if(essf._2.leftF == -1L){
              (essf._1,EssfMapJoinLeftInfo(essf._2.joinF,endF))
            }else{
              essf
            }
        }
        log.info("bugbug^^^^^^^^^^^^^^^^^^^^")
        recorder.putMutableInfo(AppSettings.essfMapKeyName,userMapEncode(mapInfo))
        recorder.finish()
        val endTime = System.currentTimeMillis()
        val filePath = AppSettings.gameDataDirectoryPath + fileName + s"_$fileIndex"
//        val recordInfo = rGameRecord(-1L, roomId, gameRecordData.gameInformation.gameStartTime, endTime,filePath)
        val recordInfo = rGameRecord(-1L, gameRecordData.roomId,gameRecordData.StartTime, endTime,filePath,gameRecordData.InitialTime)
        val recordId =Await.result(RecordDao.insertGameRecord(recordInfo), 1.minute)
        val list = ListBuffer[rUserRecordMap]()
        userAllMap.foreach{
          userRecord =>
            list.append(rUserRecordMap(recordId, userRecord._1, roomId))
        }
        Await.result(RecordDao.insertUserRecordList(list.toList), 2.minute)
        Behaviors.stopped
    }
  }

  private def save(
    gameRecordData: GameRecorderData,
    essfMap: mutable.HashMap[EssfMapKey,EssfMapJoinLeftInfo],
    userAllMap: mutable.HashMap[String,(Long,String,Long)],
    userMap: mutable.HashMap[String,(Long,String,Long)],
    startF: Long,
    endF: Long
  )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    middleBuffer: MiddleBufferInJvm
  ): Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case date:SaveDate =>
          log.info(s"${ctx.self.path} save get msg saveDate")
          val mapInfo = essfMap.map{
            essf=>
              if(essf._2.leftF == -1L){
                (essf._1,EssfMapJoinLeftInfo(essf._2.joinF,endF))
              }else{
                essf
              }
          }
          recorder.putMutableInfo(AppSettings.essfMapKeyName,userMapEncode(mapInfo))

          recorder.finish()
          log.info(s"${ctx.self.path} has save game data to file=${fileName}_$fileIndex")
          val endTime = System.currentTimeMillis()
          val filePath = AppSettings.gameDataDirectoryPath + fileName + s"_$fileIndex"
          val recordInfo = rGameRecord(-1L, gameRecordData.roomId,gameRecordData.StartTime, endTime,filePath,gameRecordData.InitialTime)
          RecordDao.insertGameRecord(recordInfo).onComplete{
            case Success(recordId) =>
              val list = ListBuffer[rUserRecordMap]()
              //TODO user all ？
              userAllMap.foreach{
                userRecord =>
                  val userId = userRecord._1
                  val userName = userRecord._2._2
                  val ballId = userRecord._2._3
                  val essf= essfMap.get(EssfMapKey(roomId,userId,userName,ballId))
                  if(essf.isDefined){
                    list.append(rUserRecordMap(recordId,userId,roomId))
//                    list.append(rUserRecordMap(recordId,userId,roomId))
                  }else{
                    list.append(rUserRecordMap(recordId,userId,roomId))
                    //                    list.append(rUserRecordMap(recordId,userId,roomId))
                  }

              }
              RecordDao.insertUserRecordList(list.toList).onComplete{
                case Success(_) =>
                  log.info(s"insert user record success")
                  ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameRecordData.InitialTime, userMap))
                case Failure(e) =>
                  log.error(s"insert user record fail, error: $e")
                  ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameRecordData.InitialTime, userMap))
              }

            case Failure(e) =>
              log.error(s"insert geme record fail, error: $e")
              ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameRecordData.InitialTime, userMap))

          }

//          ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameRecordData.InitialTime, userMap))

        switchBehavior(ctx,"busy",busy())
        case unknow =>
          log.warn(s"${ctx} save got unknow msg ${unknow}")
          Behaviors.same
      }

    }

  }


  private def initRecorder(
    roomId: Long,
    fileName: String,
    fileIndex:Int,
    InitalTime: Long,
    userMap: mutable.HashMap[String,(Long,String,Long)]
  )(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    middleBuffer: MiddleBufferInJvm
  ):Behavior[Command] = {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case t:GameRecord =>
          log.info(s"${ctx.self.path} init get msg gameRecord")
          val startF = t.event._2.get match {
            case syncdata:GypsyGameSnapshot =>
              syncdata.state.frameCount
          }
          val startTime = System.currentTimeMillis()
          val newInitStateOpt =t.event._2
          val newGameInformation = GameInformation(startTime)
          val newRecorder = initFileRecorder(fileName,fileIndex + 1, newGameInformation, newInitStateOpt)
//          val newGameInformation = ""
          val newGameRecorderData = GameRecorderData(roomId, fileName, fileIndex + 1,newGameInformation, InitalTime,startTime, startF,newInitStateOpt, newRecorder, gameRecordBuffer = List[GameRecord]())
          val newEssfMap = mutable.HashMap.empty[EssfMapKey, EssfMapJoinLeftInfo]
          val newUserAllMap = mutable.HashMap.empty[String,(Long,String,Long)]
          userMap.foreach{
            user=>
              val userId = user._1
              val userName = user._2._2
              val ballId = user._2._3
              newEssfMap.put(EssfMapKey(user._2._1,userId,userName,ballId), EssfMapJoinLeftInfo( startF, -1L))
              newUserAllMap.put(user._1, user._2)
          }
          switchBehavior(ctx,"work",work(newGameRecorderData, newEssfMap, newUserAllMap, userMap, startF, -1L))

        case StopRecord=>
          log.info(s"${ctx.self.path} room close, stop record ")
          Behaviors.stopped

        case unknow =>
          log.warn(s"${ctx} initRecorder got unknow msg ${unknow}")
          Behaviors.same
      }
    }

  }


  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior,durationOpt,timeOut) =>
          switchBehavior(ctx,name,behavior,durationOpt,timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }



}
