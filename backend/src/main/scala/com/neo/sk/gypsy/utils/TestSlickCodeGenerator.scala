package com.neo.sk.gypsy.utils

import slick.codegen.SourceCodeGenerator
import slick.driver.{JdbcProfile, PostgresDriver}
import slick.jdbc.meta.MTable
import slick.model.Column

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * User: sky
  * Date: 18-6-25
  * Time: 下午4:32
  * 主要解决参数>22问题
  * 引用于 https://stackoverflow.com/questions/36618280/slick-codegen-tables-with-22-columns
  * 修改了生成文件名和table名
  */

object TestSlickCodeGenerator {
  val slickDriver = "slick.jdbc.PostgresProfile"
  val jdbcDriver = "org.postgresql.Driver"
//  val url = "jdbc:postgresql://10.1.29.247:5432/pyramid"
  val url = "jdbc:postgresql://10.1.29.250:5432/pyramid"
  // val url = "jdbc:postgresql://localhost:5432/pyramid"
  val outputFolder = "target/gencode/genTablesPsql"
  val pkg = "com.neo.sk.pyramid.models"
  val user = "pyramid"
  val password = "py20rAmI18d"

  val driver: JdbcProfile = Class.forName(slickDriver + "$").getField("MODULE$").get(null).asInstanceOf[JdbcProfile]
  val dbFactory = driver.api.Database
  val db = dbFactory.forURL(url, driver = jdbcDriver, user = user, password = password, keepAliveConnection = true)

  // The schema is generated using Liquibase, which creates these tables that I don't want to use
  def excludedTables = Array("databasechangelog", "databasechangeloglock")

  def tableFilter(table: MTable): Boolean = {
    !excludedTables.contains(table.name.name) && schemaFilter(table.name.schema)
  }

  // There's also an 'audit' schema in the database, I don't want to use that one
  def schemaFilter(schema: Option[String]): Boolean = {
    schema match {
      case Some("public") => true
      case None => true
      case _ => false
    }
  }

  // Fetch data model
  val modelAction = PostgresDriver.defaultTables
    .map(_.filter(tableFilter))
    .flatMap(PostgresDriver.createModelBuilder(_, ignoreInvalidDefaults = false).buildModel)

  val modelFuture = db.run(modelAction)

  // customize code generator
  val codegenFuture = modelFuture.map(model => new SourceCodeGenerator(model) {

    // add custom import for added data types
//    override def code = "import my.package.Java8DateTypes._" + "\n" + super.code

    // override mapped table and class name
    override def entityName =
      dbTableName => "r" + dbTableName.toCamelCase

    override def tableName =
      dbTableName => "t" + dbTableName.toCamelCase

    override def Table = new Table(_) {
      table =>

      // Use different factory and extractor functions for tables with > 22 columns
      override def factory   = if(columns.size == 1) TableClass.elementType else if(columns.size <= 22) s"${TableClass.elementType}.tupled" else s"${EntityType.name}.apply"
      override def extractor = if(columns.size <= 22) s"${TableClass.elementType}.unapply" else s"${EntityType.name}.unapply"



      override def EntityType = new EntityTypeDef {
        override def code = {
          val args = columns.map(c =>
            c.default.map( v =>
              s"${c.name}: ${c.exposedType} = $v"
            ).getOrElse(
              s"${c.name}: ${c.exposedType}"
            )
          )
          val callArgs = columns.map(c => s"${c.name}")
          val types = columns.map(c => c.exposedType)

          if(classEnabled){
            val prns = (parents.take(1).map(" extends "+_) ++ parents.drop(1).map(" with "+_)).mkString("")
            s"""case class $name(${args.mkString(", ")})$prns"""
          } else {
            s"""
/** Constructor for $name providing default values if available in the database schema. */
case class $name(${args.map(arg => {s"val $arg"}).mkString(", ")})
type ${name}List = ${compoundType(types)}
object $name {
  def apply(hList: ${name}List): $name = new $name(${callArgs.zipWithIndex.map(pair => s"hList${tails(pair._2)}.head").mkString(", ")})
  def unapply(row: $name) = Some(${compoundValue(callArgs.map(a => s"row.$a"))})
}
          """.trim
          }
        }
      }

      override def PlainSqlMapper = new PlainSqlMapperDef {
        override def code = {
          val positional = compoundValue(columnsPositional.map(c => if (c.fakeNullable || c.model.nullable) s"<<?[${c.rawType}]" else s"<<[${c.rawType}]"))
          val dependencies = columns.map(_.exposedType).distinct.zipWithIndex.map{ case (t,i) => s"""e$i: GR[$t]"""}.mkString(", ")
          val rearranged = compoundValue(desiredColumnOrder.map(i => if(columns.size > 22) s"r($i)" else tuple(i)))
          def result(args: String) = s"$factory($args)"
          val body =
            if(autoIncLastAsOption && columns.size > 1){
              s"""
val r = $positional
import r._
${result(rearranged)} // putting AutoInc last
              """.trim
            } else {
              result(positional)
            }

          s"""
implicit def $name(implicit $dependencies): GR[${TableClass.elementType}] = GR{
  prs => import prs._
  ${indent(body)}
}
          """.trim
        }
      }

      override def TableClass = new TableClassDef {
        override def star = {
          val struct = compoundValue(columns.map(c=>if(c.fakeNullable)s"Rep.Some(${c.name})" else s"${c.name}"))
          val rhs = s"$struct <> ($factory, $extractor)"
          s"def * = $rhs"
        }
      }

      def tails(n: Int) = {
        List.fill(n)(".tail").mkString("")
      }

      // override column generator to add additional types
      override def Column = new Column(_) {
        override def rawType = {
          typeMapper(model).getOrElse(super.rawType)
        }
      }
    }
  })

  def typeMapper(column: Column): Option[String] = {
    column.tpe match {
      case "java.sql.Date" => Some("java.time.LocalDate")
      case "java.sql.Timestamp" => Some("java.time.LocalDateTime")
      case _ => None
    }
  }

  def doCodeGen() = {
    def generator = Await.result(codegenFuture, Duration.Inf)
    generator.writeToFile(slickDriver, outputFolder, pkg, "SlickTables", "SlickTables.scala")
  }

  def main(args: Array[String]) {
    doCodeGen()
    db.close()
  }
}