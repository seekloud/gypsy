package com.neo.sk.gypsy.shared.ptcl

import scala.math._

import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.util.utils._

/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:48 PM
  */
object Game {


////排行榜信息
  case class Score(id: String, n: String, k: Short, score: Short)

  case class RankInfo(
          index:Int, //排名
          score:Score //分数
  )

  case class Food(color:Short, x:Int, y:Int)


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
                   color:Short,
                   x:Int,
                   y:Int,
                   targetX:Short = 0,//运动方向，大部分做了归一化
                   targetY:Short = 0,
                   kill:Short = 0,
                   protect:Boolean = true,//出生保护
                   lastSplit:Long = System.currentTimeMillis(),
//                   var killerName:String= "",
                   width:Double =  initSize,
                   height:Double =  initSize,
                   cells:List[Cell],//分裂
                   startTime:Long=System.currentTimeMillis()
                   )

  case class PlayerPosition(
                             id:String,
                             x:Int,
                             y:Int,
                             //targetX:Int = 0,//运动方向，大部分做了归一化
                             //targetY:Int = 0,
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
                   mass:Short = initMass,  //小球体重
                   newmass:Short = initMass,
                   radius:Short = Mass2Radius(initMass),
                   speed:Float = 12,
                   speedX:Float = 0,
                   speedY:Float = 0,
                   parallel:Boolean = false,
                   isCorner:Boolean =false
                 )
//吐出的小球
  case class Mass(
                 x:Short,
                 y:Short,
                 targetX:Short,
                 targetY:Short,
                 color:Short,
                 mass:Short,
                 radius:Short,
                 speed:Float
                 )
  case class Virus(
                  vid:Long,
                  x:Short,
                  y:Short,
                  mass:Short,  //质量
                  radius:Short,
                  targetX:Short = 0,
                  targetY:Short = 0,
                  speed:Float = 0
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
                       clientX:Short,
                       clientY:Short
                     )


  object GameState{
//    val waiting:Int = 0
    val play:Int = 0
    val dead:Int = 1
    val allopatry:Int = 2
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


}
