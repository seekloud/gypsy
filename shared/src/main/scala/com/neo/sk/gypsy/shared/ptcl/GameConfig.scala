package com.neo.sk.gypsy.shared.ptcl

import com.neo.sk.gypsy.shared.util.Utils

import scala.math.sqrt


object GameConfig {

//  val historyRankLength = 5

  val version = "20190313"
  //  初始质量
  val initMass:Short = 10
  val slowBase = 4.5 //10
  val initMassLog = Utils.logSlowDown(initMass,slowBase)
  val initSpeed = 40
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
  val splitBaseSpeed = 50
  //食物质量
  val foodMass:Short = 2
  //食物池
  var foodPool = 150
  //病毒数量
  var virusNum:Int = 30
  //病毒质量上限
  var virusMassLimit:Int = 150
  val shotMass:Short = 10
  //最大分裂个数
  val maxCellNum = 16
  //质量衰减下限
  val decreaseLimit = 200
  //衰减率
  val decreaseRate = 0.995

  //吐出mass速度及衰减率
  val shotSpeed = 100  //100
  val massSpeedDecayRate = 10.5f//25

  // 排行版显示玩家数
  val rankShowNum = 10

  val frameRate = 150  //ms

  // 统计分数时候存的最大容量（按一分钟多少帧来记 现在是400）
  val ScoreListMax = 60*1000 / frameRate

  val advanceFrame = 2 //客户端提前的帧数 //0

  val delayFrame = 1 //延时帧数，抵消网络延时

  val maxDelayFrame = 3

  //病毒分裂个数
  val VirusSplitNumber = 13
//  玩家初始长宽
  val initSize = 8 + sqrt(10)*12

  //病毒速度系数
  val initVirusRatio = 3

  val initVirusSpeed = 40
  // 病毒衰减速度
  val virusSpeedDecayRate = 3

  //Level5
  val bigPlayerMass:Short = 500


  //canvas大小屏时候中间值，作为grid里面updateMove里面鼠标缩放的默认参数
  //小屏（900*450） 大屏(1200+++*760)
  val CanvasWidth = 1100
  val CanvasHeight = 600

//  胜利分数
  val VictoryScore = 1000

//  val KillBotScore = 20000
  val KillBotScore = 500

}
