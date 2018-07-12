package com.neo.sk.gypsy.shared.ptcl

import java.awt.Rectangle

import com.neo.sk.gypsy.shared.ptcl.Protocol.CollisionObj
import com.sun.xml.internal.bind.v2.util.CollisionCheckStack

import scala.collection.mutable.ArrayBuffer


class Quadtree {
   val MAX_OBJECTS = 10
   val MAX_LEVELS = 5

   var level = 0
   var objects = ArrayBuffer[CollisionObj]()
   var bounds:Rectangle = new Rectangle(0,0,Boundary.w,Boundary.h)
   var nodes = new Array[Quadtree](4)

  /*
    * 构造函数
    */
  def this(pLevel: Int, pBounds: Rectangle) {
    this()
    this.level = pLevel
    this.objects = ArrayBuffer[CollisionObj]()
    bounds = pBounds
    nodes = new Array[Quadtree](4)
  }
//清空
  def clear(): Unit = {
    objects = ArrayBuffer[CollisionObj]()
    for (i <- 0 until nodes.length) {
      if (nodes(i) != null) {
        nodes(i).clear()
        nodes(i) = null
      }
    }
  }
  //划分
  def split() {
    val subWidth = (bounds.getWidth() / 2).toInt
    val subHeight = (bounds.getHeight() / 2).toInt
    val x = bounds.getX().toInt
    val y = bounds.getY().toInt
    nodes(0) = new Quadtree(level+1, new Rectangle(x + subWidth, y, subWidth, subHeight))
    nodes(1) = new Quadtree(level+1, new Rectangle(x, y, subWidth, subHeight))
    nodes(2) = new Quadtree(level+1, new Rectangle(x, y + subHeight, subWidth, subHeight))
    nodes(3) = new Quadtree(level+1, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight))
  }
  //节点判断
  def getIndex(obj:CollisionObj): Int = {
    var index = -1
    // 中线
    val verticalMidpoint = bounds.getX + (bounds.getWidth / 2)
    val horizontalMidpoint = bounds.getY + (bounds.getHeight / 2)
    // 物体完全位于上面两个节点所在区域
    val topQuadrant = obj.y + obj.radius < horizontalMidpoint
    // 物体完全位于下面两个节点所在区域
    val bottomQuadrant = obj.y - obj.radius > horizontalMidpoint
    // 物体完全位于左面两个节点所在区域
    if (obj.x + obj.radius < verticalMidpoint)
      if (topQuadrant) index = 1 // 处于左上节点
    else if (bottomQuadrant) index = 2 // 处于左下节点
    else { // 物体完全位于右面两个节点所在区域
      if (obj.x -obj.radius > verticalMidpoint)
        if (topQuadrant) index = 0 // 处于右上节点
      else if (bottomQuadrant) index = 3 // 处于右下节点
    }
    index
  }
//插入
  def insert(obj:CollisionObj) {
    // 插入到子节点
    if (nodes(0) != null) {
      val index = getIndex(obj)
      if (index != -1) {
        nodes(index).insert(obj)
        return
      }
    }
    // 还没分裂或者插入到子节点失败，只好留给父节点了
    objects += obj
    // 超容量后如果没有分裂则分裂
    if (objects.length > MAX_OBJECTS && level < MAX_LEVELS) {
      if (nodes(0) == null) {
        split()
      }
      // 分裂后要将父节点的物体分给子节点们
      var i = 0
      while (i < objects.size) {
        val index = getIndex(objects(i))
        if (index != -1) {
          nodes(index).insert(objects.remove(i))
        }
        else {
          i += 1
        }
      }
    }
  }
//搜索范围内碰撞体
  def retrieve(returnObjects: ArrayBuffer[CollisionObj], obj:CollisionObj): ArrayBuffer[CollisionObj] = {
    val index = getIndex(obj)
    if (index != -1 && nodes(0) != null) nodes(index).retrieve(returnObjects, obj)
    returnObjects ++= objects
    returnObjects
  }
 }

