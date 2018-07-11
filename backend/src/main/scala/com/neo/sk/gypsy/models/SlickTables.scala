package com.neo.sk.gypsy.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.PostgresProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tAdmin.schema ++ tRoom.schema ++ tUser.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tAdmin
   *  @param adminName Database column admin_name SqlType(varchar), PrimaryKey, Length(255,true)
   *  @param pwdMd5 Database column pwd_md5 SqlType(varchar), Length(255,true)
   *  @param registerTime Database column register_time SqlType(int8) */
  case class rAdmin(adminName: String, pwdMd5: String, registerTime: Long)
  /** GetResult implicit for fetching rAdmin objects using plain SQL queries */
  implicit def GetResultrAdmin(implicit e0: GR[String], e1: GR[Long]): GR[rAdmin] = GR{
    prs => import prs._
    rAdmin.tupled((<<[String], <<[String], <<[Long]))
  }
  /** Table description of table admin. Objects of this class serve as prototypes for rows in queries. */
  class tAdmin(_tableTag: Tag) extends profile.api.Table[rAdmin](_tableTag, "admin") {
    def * = (adminName, pwdMd5, registerTime) <> (rAdmin.tupled, rAdmin.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(adminName), Rep.Some(pwdMd5), Rep.Some(registerTime)).shaped.<>({r=>import r._; _1.map(_=> rAdmin.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column admin_name SqlType(varchar), PrimaryKey, Length(255,true) */
    val adminName: Rep[String] = column[String]("admin_name", O.PrimaryKey, O.Length(255,varying=true))
    /** Database column pwd_md5 SqlType(varchar), Length(255,true) */
    val pwdMd5: Rep[String] = column[String]("pwd_md5", O.Length(255,varying=true))
    /** Database column register_time SqlType(int8) */
    val registerTime: Rep[Long] = column[Long]("register_time")
  }
  /** Collection-like TableQuery object for table tAdmin */
  lazy val tAdmin = new TableQuery(tag => new tAdmin(tag))

  /** Entity class storing rows of table tRoom
   *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
   *  @param roomName Database column room_name SqlType(varchar), Length(255,true)
   *  @param creater Database column creater SqlType(varchar), Length(255,true), Default(admin)
   *  @param createTime Database column create_time SqlType(int8)
   *  @param roomType Database column room_type SqlType(int4), Default(0)
   *  @param isClose Database column is_close SqlType(int4), Default(0)
   *  @param limitNumber Database column limit_number SqlType(int4), Default(30) */
  case class rRoom(id: Long, roomName: String, creater: String = "admin", createTime: Long, roomType: Int = 0, isClose: Int = 0, limitNumber: Int = 30)
  /** GetResult implicit for fetching rRoom objects using plain SQL queries */
  implicit def GetResultrRoom(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[rRoom] = GR{
    prs => import prs._
    rRoom.tupled((<<[Long], <<[String], <<[String], <<[Long], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table room. Objects of this class serve as prototypes for rows in queries. */
  class tRoom(_tableTag: Tag) extends profile.api.Table[rRoom](_tableTag, "room") {
    def * = (id, roomName, creater, createTime, roomType, isClose, limitNumber) <> (rRoom.tupled, rRoom.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(roomName), Rep.Some(creater), Rep.Some(createTime), Rep.Some(roomType), Rep.Some(isClose), Rep.Some(limitNumber)).shaped.<>({r=>import r._; _1.map(_=> rRoom.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column room_name SqlType(varchar), Length(255,true) */
    val roomName: Rep[String] = column[String]("room_name", O.Length(255,varying=true))
    /** Database column creater SqlType(varchar), Length(255,true), Default(admin) */
    val creater: Rep[String] = column[String]("creater", O.Length(255,varying=true), O.Default("admin"))
    /** Database column create_time SqlType(int8) */
    val createTime: Rep[Long] = column[Long]("create_time")
    /** Database column room_type SqlType(int4), Default(0) */
    val roomType: Rep[Int] = column[Int]("room_type", O.Default(0))
    /** Database column is_close SqlType(int4), Default(0) */
    val isClose: Rep[Int] = column[Int]("is_close", O.Default(0))
    /** Database column limit_number SqlType(int4), Default(30) */
    val limitNumber: Rep[Int] = column[Int]("limit_number", O.Default(30))
  }
  /** Collection-like TableQuery object for table tRoom */
  lazy val tRoom = new TableQuery(tag => new tRoom(tag))

  /** Entity class storing rows of table tUser
   *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Length(255,true)
   *  @param password Database column password SqlType(varchar), Length(255,true)
   *  @param headImg Database column head_img SqlType(varchar), Length(255,true), Default(None)
   *  @param isBan Database column is_ban SqlType(bool), Default(false)
   *  @param registerTime Database column register_time SqlType(int8)
   *  @param score Database column score SqlType(int4), Default(0) */
  case class rUser(id: Long, name: String, password: String, headImg: Option[String] = None, isBan: Boolean = false, registerTime: Long, score: Int = 0)
  /** GetResult implicit for fetching rUser objects using plain SQL queries */
  implicit def GetResultrUser(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[Boolean], e4: GR[Int]): GR[rUser] = GR{
    prs => import prs._
    rUser.tupled((<<[Long], <<[String], <<[String], <<?[String], <<[Boolean], <<[Long], <<[Int]))
  }
  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class tUser(_tableTag: Tag) extends profile.api.Table[rUser](_tableTag, "user") {
    def * = (id, name, password, headImg, isBan, registerTime, score) <> (rUser.tupled, rUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(name), Rep.Some(password), headImg, Rep.Some(isBan), Rep.Some(registerTime), Rep.Some(score)).shaped.<>({r=>import r._; _1.map(_=> rUser.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=true))
    /** Database column password SqlType(varchar), Length(255,true) */
    val password: Rep[String] = column[String]("password", O.Length(255,varying=true))
    /** Database column head_img SqlType(varchar), Length(255,true), Default(None) */
    val headImg: Rep[Option[String]] = column[Option[String]]("head_img", O.Length(255,varying=true), O.Default(None))
    /** Database column is_ban SqlType(bool), Default(false) */
    val isBan: Rep[Boolean] = column[Boolean]("is_ban", O.Default(false))
    /** Database column register_time SqlType(int8) */
    val registerTime: Rep[Long] = column[Long]("register_time")
    /** Database column score SqlType(int4), Default(0) */
    val score: Rep[Int] = column[Int]("score", O.Default(0))
  }
  /** Collection-like TableQuery object for table tUser */
  lazy val tUser = new TableQuery(tag => new tUser(tag))
}
