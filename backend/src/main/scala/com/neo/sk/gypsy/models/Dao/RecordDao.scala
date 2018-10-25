package com.neo.sk.gypsy.models.Dao

import com.neo.sk.gypsy.models.SlickTables
import com.neo.sk.gypsy.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.gypsy.models.SlickTables._

import scala.concurrent.Future

object RecordDao {

  def insertGameRecord(g: rGameRecord)={
    db.run( tGameRecord.returning(tGameRecord.map(_.recordId)) += g)
  }

  def insertUserRecordList(list: List[rUserRecordMap])={
    db.run(tUserRecordMap ++= list)
  }

}
