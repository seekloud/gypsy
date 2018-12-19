package com.neo.sk.gypsy.shared

import scala.math._

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:48 PM
  */
package object ptcl {

//  /**
//    * WsMsgServer、WsMsgFront、WsMsgSource
//    * */
//
//
//  trait WsMsgSource
//  case class CompleteMsgServer() extends WsMsgSource
//  case class FailMsgServer(ex: Exception) extends WsMsgSource
//
//  trait WsMsgFront extends WsMsgSource
//  trait WsMsgServer extends WsMsgSource
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
  case class Score(id: String, n: String, k: Int, score: Double, t: Option[Long] = None)

  case class RankInfo(
          index:Int, //排名
          score:Score //分数
  )

  case class Food(color:Int, x:Int, y:Int)


//网格上的一个点
  //边界检测，超过边界后从另一边界出来
  case class Point(x: Int, y: Int) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def *(n: Int) = Point(x * n, y * n)

    def %(other: Point) = Point(x % other.x, y % other.y)
  }

//TODO 可以把里面的X，y改成Double
  case class Player(
                   id:String,
                   name:String,
                   color:String,
                   x:Int,
                   y:Int,
                   targetX:Int = 0,//运动方向，大部分做了归一化
                   targetY:Int = 0,
                   kill:Int = 0,
                   protect:Boolean = true,//出生保护
                   lastSplit:Long = System.currentTimeMillis(),
                   var killerName:String= "",
                   width:Double =  8 + sqrt(10)*12,
                   height:Double =  8 + sqrt(10)*12,
                   cells:List[Cell],//分裂
                   startTime:Long=System.currentTimeMillis()
                   )

  case class PlayerPosition(
                             id:String,
                             x:Int,
                             y:Int,
                             targetX:Int = 0,//运动方向，大部分做了归一化
                             targetY:Int = 0,
                           )
//  case class Cell(
//                 id:Long,
//                 x:Int,
//                 y:Int,
//                 mass:Double = 10,  //小球体重
//                 radius:Double = 4 + sqrt(10)*6,
//                 speed:Double = 12,
//                 speedX:Float = 0,
//                 speedY:Float = 0,
//                 parallel:Boolean = false,
//                 isCorner:Boolean =false
//                 )
  case class Cell(
                   id:Long,
                   x:Int,
                   y:Int,
                   mass:Double = 50,  //小球体重
                   newmass:Double = 50,
                   radius:Double = 4 + sqrt(50)*6,
                   speed:Double = 12,
                   speedX:Float = 0,
                   speedY:Float = 0,
                   parallel:Boolean = false,
                   isCorner:Boolean =false
                 )
//吐出的小球
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
                  vid:Long,
                  x:Int,
                  y:Int,
                  mass:Double,  //质量
                  radius:Double,
                  splitNumber:Int = 13,
                  targetX:Double = 0.0,
                  targetY:Double = 0.0,
                  speed:Double = 0
                  )
  case class Int2(
                 i:Int,
                 j:Int
                 )
  object Boundary{
    val w = 4800
    val h = 2400
  }

  object Window{
    val w = 1200.0 //1200
    val h = 600.0 //600
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


  object GameState{
    val waiting:Int = 0
    val play:Int = 1
    val dead:Int = 2
    val allopatry:Int = 3
  }

  case class XAxis(
                    `type`:String,
                    boundaryGap:Boolean,
                    data:List[String]
                  )
  case class YAxis(
                    `type`:String
                  )
  case class AreaStyle()

  case class SeriesItem(
                       data:List[Int],
                       `type`: String,
                       areaStyle: AreaStyle
                       )
  case class EchartOption(
                               xAxis:XAxis,
                               yAxis:YAxis,
                               series:List[SeriesItem]
                             )

//  object UserState{
//    val waiting = 0
//    val play = 1
//    val watch = 2
//    val replay = 3
//  }

}
