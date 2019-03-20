package com.neo.sk.gypsy.shared

import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer
import com.neo.sk.gypsy.shared.ptcl.Protocol.{UserAction, KC, MP}
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.util.Utils._
import com.neo.sk.gypsy.shared.util.Utils
import scala.collection.mutable
import scala.math._
import scala.util.Random

import com.neo.sk.gypsy.shared.ptcl.ApiProtocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.GameConfig._
import com.neo.sk.gypsy.shared.ptcl.Game._


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  val random = new Random(System.nanoTime())

  val cellIdgenerator = new AtomicInteger(1000000)

  var frameCount = 0
  //食物列表
  var food = Map[Point, Short]()
//  //病毒列表
//  var virus = List[Virus]()
  //病毒map (ID->Virus)
  var virusMap = Map.empty[Long,Virus]
  //玩家列表
  var playerMap = Map.empty[String,Player]
  //喷出小球列表
  var massList = List[Mass]()
  //衰减周期计数
  var tick = 0
  //操作列表  帧数->(用户ID->操作)
  var actionMap = Map.empty[Int, Map[String, KC]]

  var mouseActionMap = Map.empty[Int, Map[String, MP]]

  var ActionEventMap = mutable.HashMap[Int,List[GameEvent]]() //frame -> List[GameEvent]

  var GameEventMap = mutable.HashMap[Int,List[GameEvent]]() //frame -> List[GameEvent]

  var deadPlayerMap=Map.empty[Long,Player]

  //统计分数
  var Compress_times = 1
  var ScoreList = List.empty[Double]
  var tempScoreList = ListBuffer.empty[Int]
  var Scale=1.0

  //  var quad = new Quadtree(0, new Rectangle(0,0,boundary.x,boundary.y))
  //用户离开，从列表中去掉
  def removePlayer(id: String): Option[Player] = {
    val r = playerMap.get(id)
    if (r.isDefined) {
      playerMap -= id
    }
    r
  }

  //这里用不到id！！！
  //键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: String, keyCode: KC) : Unit

  def addMouseActionWithFrame(id: String, mp:MP) : Unit

  def removeActionWithFrame(id: String, userAction: UserAction, frame: Int): Unit




  def update() = {
//    println(frameCount + "    start-----")
    updatePlayer()//    updateScoreList()
    updateSpots()
    actionMap = actionMap.filter(_._1 > frameCount - delayFrame)
    mouseActionMap = mouseActionMap.filter(_._1 > frameCount - delayFrame)
    ActionEventMap = ActionEventMap.filter(_._1 > frameCount - 5)
    GameEventMap = GameEventMap.filter(_._1 > frameCount - 5)
//    println(frameCount + "    end-----")
    frameCount += 1
  }


  //食物更新
  private[this] def updateSpots() = {
    //更新喷出小球的位置
    updateMass()
    //更新病毒位置
    updateVirus()
    feedApple(foodPool + playerMap.size * 3 - food.size)//增添食物（后端增添，前端不添）
    addVirus(virusNum - virusMap.size) //增添病毒(后端增添，前端不添）
  }
  //玩家更新
  private[this] def updatePlayer()={

    //TODO 确认下是不是frameCount
    /**碰撞检测要放在玩家移动之前**/
    val mouseAct = mouseActionMap.getOrElse(frameCount, Map.empty[String, MP])
//    println("mouseAct:    "+mouseAct)
    val keyAct = actionMap.getOrElse(frameCount, Map.empty[String, KC])
    tick = tick+1
    //碰撞检测
    checkCrash(keyAct,mouseAct)
//    println(s"updatePlayer-----------------$frameCount")

    //先移动到指定位置，同时进行质量衰减
    playerMap = if(tick%10==1){
      tick = 1
      playerMap.values.map(updatePlayerMap(_,mouseAct,true)).map(s=>(s.id,s)).toMap
    }else{
      playerMap.values.map(updatePlayerMap(_,mouseAct,false)).map(s=>(s.id,s)).toMap
    }

    def updatePlayerMap(player: Player, mouseActMap: Map[String, MP],decrease:Boolean)={
      if(decrease){
        updatePlayerMove(massDecrease(player),mouseActMap)
      }else{
        updatePlayerMove(player,mouseActMap)
      }
    }
//    println(s"updatePlayerMap-----------------$frameCount")
  }
    //碰撞检测
  def checkCrash(keyAct: Map[String,KC], mouseAct: Map[String, MP])={
    checkPlayerSplit(keyAct,mouseAct)
    val mergeInFlame=checkCellMerge()
    checkPlayerVirusCrash(mergeInFlame)
    checkPlayer2PlayerCrash()
    checkPlayerFoodCrash()
    checkPlayerMassCrash()
    checkVirusMassCrash()
    checkPlayerShotMass(keyAct,mouseAct)
  }

  //更新病毒的位置
  def updateVirus() :Unit ={
    val NewVirus = virusMap.map{ vi=>
      val v =vi._2
      val (nx,ny)= normalization(v.targetX,v.targetY)
      var newX = v.x
      var newY = v.y
      var newSpeed = v.speed
      var newMass = vi._2.mass
      if(v.speed!=0){
        newX = (v.x + (nx*v.speed)).toShort
        newY = (v.y + (ny*v.speed)).toShort
        newSpeed = if(v.speed-virusSpeedDecayRate<0) 0f else (v.speed-virusSpeedDecayRate).toFloat
        val newPoint = ExamBoundary(newX,newY)
        if(newPoint._3)
          newMass = 0.toShort
      }
      vi._1 -> v.copy(x = newX,y=newY,speed = newSpeed, mass = newMass)
    }
    virusMap = NewVirus.filterNot(_._2.mass == 0)
  }
  //更新喷出小球的位置
  def updateMass():Unit = {
    massList = massList.map { mass =>
      val deg = Math.atan2(mass.targetY, mass.targetX)
      val deltaY = mass.speed * Math.sin(deg)
      val deltaX = mass.speed * Math.cos(deg)

      var newSpeed = mass.speed
      newSpeed -= massSpeedDecayRate
      if (newSpeed < 0) newSpeed = 0
      var newX = mass.x
      var newY = mass.y
      if (!deltaY.isNaN) newY = (newY + deltaY).toShort
      if (!deltaX.isNaN) newX = (newX + deltaX).toShort

      val borderCalc = Mass2Radius(shotMass) + 5
      if (newX > boundary.x - borderCalc) newX = (boundary.x - borderCalc).toShort
      if (newY > boundary.y - borderCalc) newY = (boundary.y - borderCalc).toShort
      if (newX < borderCalc) newX = borderCalc.toShort
      if (newY < borderCalc) newY = borderCalc.toShort

      mass.copy(x = newX, y = newY, speed = newSpeed)
    }
  }
  //具体函数定义在GridOnServer
  def feedApple(appleCount: Int): Unit
  def addVirus(virus: Int): Unit

  //边界超越校验
  def ExamBoundary(newX:Short,newY:Short)={
    var disappear = false
    val x = if(newX>boundary.x){
      disappear = true
      boundary.x
    } else if(newX<0){
      disappear = true
      0
    }else{
      newX
    }
    val y = if(newY>boundary.y){
      disappear = true
      boundary.y
    } else if(newY<0){
      disappear = true
      0
    }else{
      newY
    }

    (x.toShort ,y.toShort,disappear)
  }

  private[this] def updatePlayerMove(player: Player, mouseActMap: Map[String, MP]) = {
    var MouseScale = getZoomRate(player.width,player.height,CanvasWidth,CanvasHeight)
    val mouseAct = mouseActMap.getOrElse(player.id,MP(None,player.targetX, player.targetY,0,0))
    //1> 对每个cell计算新的方向、速度和位置
    val newCells = player.cells.sortBy(_.radius).reverse.flatMap { cell =>
      /**1> 计算该帧cell移动的距离**/
      val deg1 = atan2(player.targetY + player.y - cell.y, player.targetX + player.x - cell.x)
      val degX1 = if (cos(deg1).isNaN) 0 else cos(deg1)
      val degY1 = if (sin(deg1).isNaN) 0 else sin(deg1)
      //3> 速度*方向==xy方向移动的距离
      val move = Point((cell.speed * degX1).toInt, (cell.speed * degY1).toInt)
      //cell移动+边界检测
      var newX = if ((cell.x + move.x) > boundary.x-15)  boundary.x-15 else if ((cell.x + move.x) <= 15) 15 else cell.x + move.x
      var newY = if ((cell.y + move.y) > boundary.y-15) boundary.y-15 else if ((cell.y + move.y) <= 15) 15 else cell.y + move.y

      var isCorner= false
      var isParallel =false
      if((newX<=15&&newY<=15)||
        (newX>=boundary.x-15&&newY<=15)||
        (newX<=15&&newY>=boundary.y-15)||
        (newX>=boundary.x-15&&newY>=boundary.y-15)){
        isCorner=true
      }

      /**2> 计算下一帧cell的速度**/
      var newSpeed = cell.speed
      //todo 平行的时候如何处理
      //var target = if(!cell.parallel) Position( (mouseAct.cX + player.x - cell.x).toShort , (mouseAct.cY + player.y - cell.y).toShort  ) else Position(mouseAct.cX , mouseAct.cY)
      var target = Position((mouseAct.cX + player.x - cell.x).toShort , (mouseAct.cY + player.y - cell.y).toShort)
      val distance = sqrt(pow(target.clientX, 2) + pow(target.clientY, 2)) / MouseScale
      val deg = atan2(target.clientY, target.clientX)
      val degX = if (cos(deg).isNaN) 0 else cos(deg)
      val degY = if (sin(deg).isNaN) 0 else sin(deg)
      val slowdown = Utils.logSlowDown(cell.newmass, slowBase) - initMassLog + 1
      //2.1> 鼠标位置距离cell的远近影响cell的速度
      if (distance < sqrt(pow((newSpeed * degX).toInt, 2) + pow((newSpeed * degY).toInt, 2))) {
        newSpeed = (target.clientX / degX).toFloat
      } else {
        if (cell.speed > initSpeed / slowdown) {
          newSpeed -= acceleration
        } else {
          if (distance < cell.radius) {
            if (cell.speed > 0) {
              newSpeed = cell.speed - acceleration
            } else newSpeed = 0
          } else {
            newSpeed = if (cell.speed < initSpeed / slowdown) {
              cell.speed + acceleration
            } else (initSpeed / slowdown).toFloat
          }
        }
      }
      //遍历计算每个cell的新速度
      player.cells.filterNot(p => p == cell).sortBy(_.isCorner).reverse.foreach { cell2 =>
        val distance = sqrt(pow(newY - cell2.y, 2) + pow(newX - cell2.x, 2))
        val deg= acos(abs(newX-cell2.x)/distance)
        val radiusTotal = cell.radius + cell2.radius+2
        if (distance < radiusTotal) {
          if (player.lastSplit > System.currentTimeMillis() - mergeInterval&&System.currentTimeMillis()-player.lastSplit>1000) {
            val mouseX=mouseAct.cX/MouseScale+player.x
            val mouseY=mouseAct.cY/MouseScale+player.y
            val cos1=((cell2.x-cell.x)*(mouseX-cell.x)+(cell2.y-cell.y)*(mouseY-cell.y))/sqrt((pow(newY - cell2.y, 2) + pow(newX - cell2.x, 2))*(pow(newY - mouseY, 2) + pow(newX - mouseX, 2)))
            val cos2=((cell.x-cell2.x)*(mouseX-cell2.x)+(cell.y-cell2.y)*(mouseY-cell2.y))/sqrt((pow(newY - cell2.y, 2) + pow(newX - cell2.x, 2))*(pow(cell2.y - mouseY, 2) + pow(cell2.x - mouseX, 2)))
            val cos3=((cell.x-mouseX)*(cell2.x-mouseX)+(cell.y-mouseY)*(cell2.y-mouseY))/sqrt((pow(newY - mouseY, 2) + pow(newX - mouseX, 2))*(pow(cell2.y - mouseY, 2) + pow(cell2.x - mouseX, 2)))
            if(cos1<=0){
              if(newX==15&&newY==15){}
              else if(newX==15&&newY==boundary.y-15){}
              else if(newX==boundary.x-15&&newY==15){}
              else if(newX==boundary.x-15&&newY==boundary.y-15){}
              else{
                newSpeed+=2
              }
            }else if(cos2<=0){
              if(!cell2.isCorner){
                if(newSpeed>cell2.speed){
                  newSpeed=if(cell2.speed-2>=0)cell2.speed-2 else 0
                }
                if((cell2.x<=15&&cell2.y<=15)||
                  (cell2.x>=boundary.x-15&&cell2.y<=15)||
                  (cell2.x<=15&&cell2.y>=boundary.y-15)||
                  (cell2.x>=boundary.x-15&&cell2.y>=boundary.y-15)){
                  newX=cell.x
                  newY=cell.y
                }
              }else{
                isCorner=true
                newSpeed = 0
                newX=cell.x
                newY=cell.y
              }
            }else if(cos3<=0){
              newSpeed=0
            }else{
              if (cell.x < cell2.x) newX -= ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
              else if (cell.x > cell2.x) newX += ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
              if (cell.y < cell2.y) newY -= ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
              else if (cell.y > cell2.y) newY += ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
              isParallel=true
            }
          }
        }
      }

      List(Cell(cell.id, newX.toShort , newY.toShort , cell.mass, cell.newmass, cell.radius, newSpeed, (newSpeed * degX).toFloat, (newSpeed * degY).toFloat,isParallel,isCorner))
    }
    val length = newCells.length
    val newX = newCells.map(_.x).sum / length
    val newY = newCells.map(_.y).sum / length
    val left = newCells.map(a => a.x - a.radius).min
    val right = newCells.map(a => a.x + a.radius).max
    val bottom = newCells.map(a => a.y - a.radius).min
    val top = newCells.map(a => a.y + a.radius).max
    player.copy(x = newX.toShort , y = newY.toShort , targetX = mouseAct.cX , targetY = mouseAct.cY , protect = player.protect, kill = player.kill, lastSplit = player.lastSplit, width = right - left, height = top - bottom, cells = newCells)
  }

  //TODO 前后
  //食物检测
  def checkPlayerFoodCrash(): Unit
  //TODO 前后
  //mass检测(前后端一样)
  def checkPlayerMassCrash(): Unit
  //TODO 前后
  //mass检测
  def checkVirusMassCrash(): Unit
  //TODO 只后台！！
  //与用户检测
  def checkPlayer2PlayerCrash(): Unit

  //TODO 前端做排斥后台判断
  //返回在这一帧是否融合过
  def checkCellMerge(): Boolean

  //TODO 前端后台
  //病毒碰撞检测
  def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit

  //TODO 前后
  //发射小球
  def checkPlayerShotMass(actMap: Map[String, KC], mouseActMap: Map[String, MP]): Unit = {
    //TODO 这里写下有哪些是分裂的
    val newPlayerMap = playerMap.values.map {
      player =>
        val mouseAct = mouseActMap.getOrElse(player.id, MP(None,player.targetX.toShort, player.targetY.toShort,0,0))
        val shot = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.kC==KeyEvent.VK_E
          case _ => false
        }
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            val target = Position( (mouseAct.cX + player.x - cell.x).toShort , (mouseAct.cY + player.y - cell.y).toShort )
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            var newMassList =  List.empty[Mass]
            if (shot && newMass > shotMass * 3) {
              newMass = (newMass - shotMass).toShort
              newRadius = Mass2Radius(newMass)
              val massX = (cell.x + (newRadius - 50) * degX).toInt
              val massY = (cell.y + (newRadius - 50) * degY).toInt
              newMassList ::= Game.Mass(player.id,massX.toShort , massY.toShort , player.targetX, player.targetY, player.color, shotSpeed)
            }
            massList :::=newMassList
            Cell(cell.id, cell.x, cell.y, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }.filterNot(e=>e.newmass <= 0 && e.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX.toShort , y = newY.toShort , width = right - left, height = top - bottom, cells = newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  //TODO 暂时前后不同 ，后台有广播哪些玩家分裂
  //分裂检测
  def checkPlayerSplit(actMap: Map[String,KC], mouseActMap: Map[String, MP]): Unit = {}

  def massDecrease(player:Player)={
    val newCells=player.cells.map{cell=>
      var newMass = cell.newmass
      if(cell.newmass > decreaseLimit)
        newMass = (cell.newmass * decreaseRate).toShort
      cell.copy(mass = newMass,newmass = newMass)
    }
    player.copy(cells = newCells)
  }

  /**
    * method: getGridData
    * describe: 获取自己视角中的全量数据
    * winWidth & winHeight:当前窗口的宽高
    * screenScale 屏幕缩放比例
    */
  def getGridData(id:String,winWidth:Int,winHeight:Int,screenScale:Double) = {
    //FIXME 编译时候有出现格式匹配出错的问题，一般是currentPlayer的getorelse里面toshort导致的
    val currentPlayerWH = playerMap.get(id).map(a=>(a.x,a.y)).getOrElse((winWidth/2,winHeight/2 ))
    val zoom = playerMap.get(id).map(a=>(a.width,a.height)).getOrElse((30.0,30.0))
    if(getZoomRate(zoom._1,zoom._2,winWidth,winHeight)!=1){
      Scale = getZoomRate(zoom._1,zoom._2,winWidth,winHeight)
    }
    val width = winWidth / Scale / screenScale
    val height = winHeight / Scale / screenScale

    var playerDetails: List[Player] = Nil

    playerMap.foreach{
      case (_,player) =>
        if(checkScreenRangeAll(Point(currentPlayerWH._1,currentPlayerWH._2),width,height,Point(player.x,player.y),player.width,player.height)){
          playerDetails ::= player
        }
    }

    Protocol.GridDataSync(
      frameCount,
      playerDetails,
      massList.filter(m=>checkScreenRange(Point(currentPlayerWH._1,currentPlayerWH._2),Point(m.x,m.y),Mass2Radius(shotMass),width,height)),
      virusMap.filter(m =>checkScreenRange(Point(currentPlayerWH._1,currentPlayerWH._2),Point(m._2.x,m._2.y),m._2.radius,width,height)),
      Scale
    )
  }


  def clearAllData = {
    //grid中数据清除
    food = Map[Point, Short]()
    playerMap = Map.empty[String,Player]
    virusMap = Map.empty[Long,Virus]
    massList = List[Mass]()
    deadPlayerMap=Map.empty[Long,Player]
    //动作清除
    actionMap = Map.empty[Int, Map[String, KC]]
    mouseActionMap = Map.empty[Int, Map[String, MP]]

    //event记录清除
    ActionEventMap.clear()
    GameEventMap.clear()

    tick = 0
    Scale = 1.0

  }

  def getAllGridData: Protocol.GridDataSync

  def getActionEventMap(frame:Int):List[GameEvent]

  def getGameEventMap(frame:Int):List[GameEvent]

}
