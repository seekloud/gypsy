package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.util.utils

import scala.math.sqrt


object GameConfig {

//  val historyRankLength = 5

  val version = "20190114"

  val slowBase = 10
  val initMassLog = utils.logSlowDown(10,slowBase)
  val acceleration  = 2
  //质量转半径率
  val mass2rRate = 6
  //吞噬覆盖率  (-1,1) 刚接触->完全覆盖
  val coverRate = 0
  //合并时间间隔
  val mergeInterval = 6 * 1000 // 12 * 1000
  //分裂时间间隔
  val splitInterval = 2 * 1000
  //最小分裂大小
  val splitLimit = 30
  //分裂初始速度
  val splitBaseSpeed = 40
  //食物质量
  val foodMass:Short = 1
  //食物池
  var foodPool = 300
  //病毒数量
  var virusNum:Int = 16
  //病毒质量上限
  var virusMassLimit:Int = 200
  val shotMass:Short = 10
  val shotSpeed = 100
  //最大分裂个数
  val maxCellNum = 16
  //质量衰减下限
  val decreaseLimit = 200
  //衰减率
  val decreaseRate = 0.995
  //小球速度衰减率
  val massSpeedDecayRate = 25
//  病毒衰减速度
  val virusSpeedDecayRate = 0.3
  // 排行版显示玩家数
  val rankShowNum = 10

  val frameRate = 150  //ms

  // 统计分数时候存的最大容量（按一分钟多少帧来记 现在是400）
  val ScoreListMax = 60*1000 / frameRate

  val advanceFrame = 0 //客户端提前的帧数

  val delayFrame = 1 //延时帧数，抵消网络延时

  val maxDelayFrame = 3

//  初始质量
  val initMass:Short = 200
//病毒分裂个数
  val VirusSplitNumber = 13
//  玩家初始长宽
  val initSize = 8 + sqrt(10)*12

  //Level5
  val bigPlayerMass:Short = 500

}
