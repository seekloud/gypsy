package com.neo.sk.gypsy.shared

import scala.math._

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:48 PM
  */
package object ptcl {


  trait CommonRsp {
    val errCode: Int
    val msg: String
  }


  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp

////排行榜信息
  case class Score(id: Long, n: String, k: Int, score: Double, t: Option[Long] = None)


  case class Food(color:Int, x:Int, y:Int)


//网格上的一个点
  //边界检测，超过边界后从另一边界出来
  case class Point(x: Int, y: Int) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def *(n: Int) = Point(x * n, y * n)

    def %(other: Point) = Point(x % other.x, y % other.y)
  }


  case class Player(
                   id:Long,
                   name:String,
                   color:String,
                   x:Int,
                   y:Int,
                   targetX:Int = 0,
                   targetY:Int = 0,
                   kill:Int = 0,
                   protect:Boolean = true,//出生保护
                   lastSplit:Long = System.currentTimeMillis(),
                   var killerName:String= "",
                   width:Double =  8 + sqrt(10)*12,
                   height:Double =  8 + sqrt(10)*12,
                   cells:List[Cell],
                   startTime:Long=System.currentTimeMillis()
                   )
  case class Cell(
                 id:Long,
                 x:Int,
                 y:Int,
                 mass:Double = 200,  //小球体重
                 radius:Double = 4 + sqrt(10)*6,
                 speed:Double = 12,
                 speedX:Float = 0,
                 speedY:Float = 0
                 )

  case class Mass(
                 x:Int,
                 y:Int,
                 targetX:Int,
                 targetY:Int,
                 color:Int,
                 mass:Double,
                 radius:Double,
                 speed:Double
                 )
  case class Virus(
                  x:Int,
                  y:Int,
                  mass:Double,
                  radius:Double,
                  splitNumber:Int = 13
                  )
  object Boundary{
    val w = 4800
    val h = 2400
  }

  object Window{
    val w = 1200
    val h = 600
  }


  case class Captcha(
                      showapi_res_error:String="",
                      showapi_res_code:Int,
                      showapi_res_body:CaptchaBody
  )
  case class CaptchaBody(
                          img_path_https:String,
                          ret_code:Int,
                          img_path:String,
                          text:String
                        )

  case class Position(
                       clientX:Double,
                       clientY:Double
                     )
}
