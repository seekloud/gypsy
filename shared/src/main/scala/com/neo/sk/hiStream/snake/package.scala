package com.neo.sk.hiStream



/**
  * User: Taoz
  * Date: 8/29/2016
  * Time: 9:48 PM
  */
package object snake {
//grid上的一个网格
//  sealed trait Spot
//  case class Body(id: Long, life: Int) extends Spot
//  case class Header(id: Long, life: Int) extends Spot
//  case class Apple(score: Int, life: Int) extends Spot
//
//  case class Center(id:Long, radius:Double, score:Double) extends Spot
////排行榜信息
  case class Score(id: Long, n: String, k: Int, score: Double, t: Option[Long] = None)
//  //snake body 用户id，剩余存在时间，x坐标，y坐标
//  case class Bd(id: Long, life: Int, x: Int, y: Int)
//  //apple 分值，剩余存在时间，x坐标，y坐标
//  case class Ap(score: Int, life: Int, x: Int, y: Int)
//
//  //圆心detail，包括用户id，半径，积分，坐标
//  case class Cd(id:Long,radius:Double,score:Double,x:Int,y:Int)

  case class Food(score:Int, x:Int, y:Int)


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
                   x:Int,
                   y:Int,
                   targetX:Int = 0,
                   targetY:Int = 0,
                   kill:Int = 0,
                   protect:Boolean = true,//出生保护
                   cells:List[Cell]
                   )
  case class Cell(
                 x:Int,
                 y:Int,
                 mass:Double = 1,
                 radius:Double = 4,
                 speed:Double = 12
                 )

  object Boundary{
    val w = 1200
    val h = 600
  }





}
