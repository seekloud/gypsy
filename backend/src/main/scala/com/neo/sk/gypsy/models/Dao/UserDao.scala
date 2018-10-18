package com.neo.sk.gypsy.models.Dao

import com.neo.sk.gypsy.models.SlickTables
import com.neo.sk.gypsy.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.gypsy.models.SlickTables._

import scala.concurrent.Future

object UserDao {

  def getUserById(id:Long)={
    db.run(tUser.filter(_.id===id).result.headOption)
  }
  def getUserByName(name:String):Future[Option[SlickTables.rUser]] ={

    //db.run(tUser.filter(_.name===name).result.headOption)
    Future.successful(None)
  }

  def addUser(name:String,password:String,headImg:String,registerTime:Long)={
    db.run(
      tUser.returning(tUser.map(_.id))+=rUser(-1l,name,password,Some(headImg),false,registerTime)
    )
  }

  def getScoreById(id:Long)={
    db.run(
      tUser.filter(_.id===id).map(_.score).result.headOption
    )
  }

  def updateScoreById(id:Long,score:Int)={
    db.run(
      tUser.filter(_.id===id ).map(_.score).update(score)
    )
  }

}
