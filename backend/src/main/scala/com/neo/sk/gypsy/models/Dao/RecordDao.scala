package com.neo.sk.gypsy.models.Dao

import com.neo.sk.gypsy.models.SlickTables
import com.neo.sk.gypsy.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.gypsy.models.SlickTables._
//import scala.concurrent.ExecutionContext.Implicits.global
import com.neo.sk.gypsy.Boot.executor
import scala.concurrent.Future

object RecordDao {

  def insertGameRecord(g: rGameRecord) = {
    db.run(tGameRecord.returning(tGameRecord.map(_.recordId)) += g)
  }

  def insertUserRecordList(list: List[rUserRecordMap]) = {
    db.run(tUserRecordMap ++= list)
  }

  def getAllRecord(lastRecordId: Long, count: Int) = {
    if(lastRecordId!=999999L){
      val action1 = for {
        /*
         *RecordId 写入GameRecord表时，有的时候可能因为那段录像内没有用户所以不会记录到UserRecordMap表中
         * 所以查找的时候拿GameRecord表先查50个，UserRecordMap里面过滤后个数是《=50的
         *
        */
        r2 <- tUserRecordMap.sortBy(_.recordId.desc).take(count).result
        r1 <- tGameRecord.filter(i=>i.recordId.inSet(r2.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
        //        r1 <- tGameRecord.sortBy(_.recordId.desc).take(count).result
//        r2 <- tUserRecordMap.filter(i => i.recordId.inSet(r1.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
      } yield {
        (r1,r2)
      }
      db.run(action1.transactionally)
    }
    else{
      val action2 = for {
        r1 <- tGameRecord.filter(_.recordId<lastRecordId).sortBy(_.recordId.desc).take(count).result
        r2 <- tUserRecordMap.filter(i => i.recordId.inSet(r1.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
      } yield {
        (r1,r2)
      }
      db.run(action2.transactionally)
    }
    }

  def getRecordByTime(lastRecordId:Long,count:Int,startTime:Long,endTime:Long)={
    if(lastRecordId==0L){
      val action1 = for {
        r1 <- tGameRecord.filter(i=>i.startTime>=startTime && i.endTime<=endTime ).sortBy(_.recordId.desc).take(count).result
        r2 <- tUserRecordMap.filter(i => i.recordId.inSet(r1.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
      } yield {
        (r1,r2)
      }
      db.run(action1.transactionally)
    }
    else{
      val action2 = for {
        r1 <- tGameRecord.filter(i=>i.recordId<lastRecordId && i.startTime>=startTime && i.endTime<=endTime).sortBy(_.recordId.desc).take(count).result
        r2 <- tUserRecordMap.filter(i => i.recordId.inSet(r1.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
      } yield {
        (r1,r2)
      }
      db.run(action2.transactionally)
    }
  }

  def getRecordByPlayer(lastRecordId:Long,count:Int,playerId:String)={
    if(lastRecordId==0L){
      val action1 = for {
        r1 <- tUserRecordMap.filter(_.userId===playerId).sortBy(_.recordId.desc).take(count).result
        r2 <- tGameRecord.filter(i=>i.recordId.inSet(r1.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
        r3 <- tUserRecordMap.filter(i=> i.recordId.inSet(r2.map(_.recordId).toSet)).result
      } yield {
        (r2,r3)
      }
      db.run(action1.transactionally)
    }
    else{
      val action2 = for {
        r1 <- tUserRecordMap.filter(i=> i.recordId<lastRecordId && i.userId===playerId).sortBy(_.recordId.desc).take(count).result
        r2 <- tGameRecord.filter(i=>i.recordId.inSet(r1.map(_.recordId).toSet)).sortBy(_.recordId.desc).result
        r3 <- tUserRecordMap.filter(i=> i.recordId.inSet(r2.map(_.recordId).toSet)).result
      } yield {
        (r2,r3)
      }
      db.run(action2.transactionally)
    }
  }

  def getRecordById(id:Long)={
    db.run(tGameRecord.filter(_.recordId === id).result.headOption)
  }

  def getFilePath(recordId:Long) = {
    val q = tGameRecord.filter(_.recordId === recordId).map(_.filePath).result
    db.run(q)
  }
}

