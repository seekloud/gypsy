package com.neo.sk.gypsy.gypsyServer

import java.util.concurrent.atomic.AtomicLong

import com.neo.sk.gypsy.shared.Grid
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.core.RoomActor.{dispatch, dispatchTo}
import com.neo.sk.gypsy.shared._
import com.neo.sk.gypsy.shared.ptcl.Protocol.UserJoinRoom
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl
import com.neo.sk.gypsy.shared.util.utils.{checkCollision, normalization}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.math.{Pi, abs, acos, atan2, cos, pow, sin, sqrt}
import scala.util.Random
import com.neo.sk.gypsy.core.EsheepSyncClient
import com.neo.sk.gypsy.core.RoomActor.{UserInfo, dispatch, dispatchTo}
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.Boot.esheepClient

import scala.math.{Pi, abs, acos, atan2, cos, pow, sin, sqrt}
import com.neo.sk.gypsy.shared.ptcl.GameConfig._


/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 9:55 PM
  */
class GameServer(override val boundary: Point) extends Grid {


  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)


  private[this] var waitingJoin = Map.empty[String, String]
//  private[this] var feededApples: List[Food] = Nil
  private[this] var newFoods = Map[Point, Int]() // p -> color
  private[this] var eatenFoods = Map[Point, Int]()
  private[this] var addedVirus:List[Virus] = Nil
  private [this] var subscriber=mutable.HashMap[String,ActorRef[WsMsgSource]]()
  private [this] var userLists = mutable.ListBuffer[UserInfo]()


  var currentRank = List.empty[Score]
  private[this] var historyRankMap = Map.empty[String, Score]
  var historyRankList = historyRankMap.values.toList.sortBy(_.k).reverse

  private[this] var historyRankThreshold = if (historyRankList.isEmpty) -1 else historyRankList.map(_.k).min

  def addSnake(id: String, name: String) = waitingJoin += (id -> name)

  private var roomId = 0l

  def setRoomId(id:Long)={
    roomId = id
  }


  var VirusId = new AtomicLong(1000L)

  private[this] def genWaitingStar() = {
    waitingJoin.filterNot(kv => playerMap.contains(kv._1)).foreach { case (id, name) =>
      val center = randomEmptyPoint()
      val color = new Random(System.nanoTime()).nextInt(7)
      val player = Player(id,name,color.toString,center.x,center.y,0,0,0,true,System.currentTimeMillis(),"",8 + sqrt(10)*12,8 + sqrt(10)*12,List(Cell(cellIdgenerator.getAndIncrement().toLong,center.x,center.y)),System.currentTimeMillis())
      playerMap += id -> player
      val event = UserJoinRoom(roomId,player,frameCount)
      AddGameEvent(event)
    }
    waitingJoin = Map.empty[String, String]
  }

  implicit val scoreOrdering = new Ordering[Score] {
    override def compare(x: Score, y: Score): Int = {
      var r = (y.score - x.score).toInt
//      if (r == 0) {
//        r = y.id
//      }
      r
    }
  }

  private[this] def updateRanks() = {
    currentRank = playerMap.values.map(s => Score(s.id, s.name, s.kill, s.cells.map(_.mass).sum)).toList.sorted
    var historyChange = false
    currentRank.foreach { cScore =>
      historyRankMap.get(cScore.id) match {
        case Some(oldScore) if cScore.score > oldScore.score =>
          historyRankMap += (cScore.id -> cScore)
          historyChange = true
        case None if cScore.score > historyRankThreshold =>
          historyRankMap += (cScore.id -> cScore)
          historyChange = true
        case _ => //do nothing.
      }
    }

    if (historyChange) {
      historyRankList = historyRankMap.values.toList.sorted.take(historyRankLength)
      historyRankThreshold = historyRankList.lastOption.map(_.score.toInt).getOrElse(-1)
      historyRankMap = historyRankList.map(s => s.id -> s).toMap
    }

  }

  override def feedApple(appleCount: Int): Unit = {
    //TODO 考虑出生时的苹果列表
//    feededApples = Nil
    var appleNeeded = appleCount
    while (appleNeeded > 0) {
      val p = randomEmptyPoint()
      val color = random.nextInt(7)
//      feededApples ::= Food(color,p.x,p.y)
      newFoods+=(p->color)
      food += (p->color)
      appleNeeded -= 1
    }
    val event = GenerateApples(newFoods,frameCount)
    AddGameEvent(event)
  }

  override def addVirus(v: Int): Unit = {
    addedVirus = Nil
    var virusNeeded = v
    var addVirus = Map.empty[Long,Virus]
    while(virusNeeded > 0){
      val p =randomEmptyPoint()
      val mass = 50 + random.nextInt(50)
      val radius = 4 + sqrt(mass) * mass2rRate
      val vid = VirusId.getAndIncrement()
      val newVirus = Virus(vid,p.x,p.y,mass,radius)
      addVirus += (vid->newVirus)
      addedVirus ::= newVirus
//      virus ::= newVirus
      virusNeeded -= 1
    }
    virusMap ++= addVirus
    val event = GenerateVirus(addVirus,frameCount)
    AddGameEvent(event)
  }

  override def checkPlayer2PlayerCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var killer = ""
        val score=player.cells.map(_.mass).sum
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            playerMap.filterNot(a => a._1 == player.id || a._2.protect).foreach { p =>
              p._2.cells.foreach { otherCell =>
                if (cell.radius * 1.1 < otherCell.radius && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (otherCell.radius - cell.radius * 0.8) && !player.protect) {
                  //被吃了
                  newMass = 0
                  killer = p._1
                } else if (cell.radius > otherCell.radius * 1.1 && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (cell.radius - otherCell.radius * 0.8)) {
                  //吃掉别人了
                  newMass += otherCell.mass
                  newRadius = 4 + sqrt(newMass) * 6
                }
              }
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY, cell.parallel,cell.isCorner)
        }.filterNot(_.mass <= 0)
        if (newCells.isEmpty) {
          playerMap.get(killer) match {
            case Some(killerPlayer) =>
              player.killerName = killerPlayer.name
            case _ =>
              player.killerName = "unknown"
          }
          dispatchTo(subscriber,player.id,Protocol.UserDeadMessage(player.id,killer,player.killerName,player.kill,score.toInt,System.currentTimeMillis()-player.startTime),userLists)
          dispatch(subscriber,Protocol.KillMessage(killer,player))
          //添加死亡信息
          val event = UserLeftRoom(player.id,player.name,roomId,frameCount)
          AddGameEvent(event)
          esheepClient ! EsheepSyncClient.InputRecord(player.id.toString,player.name,player.kill,1,player.cells.map(_.mass).sum.toInt, player.startTime, System.currentTimeMillis())

          Left(killer)
        } else {
          val length = newCells.length
          val newX = newCells.map(_.x).sum / length
          val newY = newCells.map(_.y).sum / length
          val left = newCells.map(a => a.x - a.radius).min
          val right = newCells.map(a => a.x + a.radius).max
          val bottom = newCells.map(a => a.y - a.radius).min
          val top = newCells.map(a => a.y + a.radius).max
          Right(player.copy(x = newX, y = newY, width = right - left, height = top - bottom, cells = newCells))
        }
    }
    playerMap = newPlayerMap.map {
      case Right(s) => (s.id, s)
      case Left(_) => ("", Player("", "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
    }.filterNot(_._1 == "").toMap
    newPlayerMap.foreach {
      case Left(killId) =>
        val a = playerMap.getOrElse(killId, Player("", "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
        playerMap += (killId -> a.copy(kill = a.kill + 1))
      case Right(_) =>
    }
  }

  override def checkCellMerge: Boolean = {
    var mergeInFlame = false
    val newPlayerMap = playerMap.values.map {
      player =>
        val newSplitTime = player.lastSplit
        var mergeCells = List[Cell]()
        //已经被本体其他cell融合的cell
        var deleteCells = List[Cell]()
        var playerIsMerge=false
        //依据距离判断被删去的cell
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var newRadius = cell.radius
            var newMass = cell.mass
            var cellX = cell.x
            var cellY = cell.y
            //自身cell合并检测
            player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
              val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
              val deg= acos(abs(cell.x-cell2.x)/distance)
              val radiusTotal = cell.radius + cell2.radius
              if (distance < radiusTotal) {
                if (newSplitTime > System.currentTimeMillis() - mergeInterval) {
                    if (cell.x < cell2.x) cellX -= ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
                    else if (cell.x > cell2.x) cellX += ((cell.radius+cell2.radius-distance)*cos(deg)).toInt/4
                    if (cell.y < cell2.y) cellY -= ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4
                    else if (cell.y > cell2.y) cellY += ((cell.radius+cell2.radius-distance)*sin(deg)).toInt/4

                }
                else if (distance < radiusTotal / 2) {
                  if (cell.radius > cell2.radius) {
                    //被融合的细胞不能再被其他细胞融合
                    if (!mergeCells.exists(_.id == cell2.id) && !mergeCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell.id)) {
                      mergeInFlame = true
                      playerIsMerge=true
                      newMass += cell2.mass
                      newRadius = 4 + sqrt(newMass) * mass2rRate
                      mergeCells = cell2 :: mergeCells
                    }
                  }
                  else if (cell.radius < cell2.radius && !deleteCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell2.id)) {
                    mergeInFlame = true
                    playerIsMerge=true
                    newMass = 0
                    newRadius = 0
                    deleteCells = cell :: deleteCells
                  }
                }
              }
            }
            List(Cell(cell.id, cellX, cellY, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner))
        }.filterNot(_.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        if(playerIsMerge){
          dispatch(subscriber,UserMerge(player.id,player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells.sortBy(_.id))))
        }

        player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells.sortBy(_.id))
      //Player(player.id, player.name, player.color, player.x, player.y, player.targetX, player.targetY, player.kill, player.protect, player.lastSplit, player.killerName, player.width, player.height, newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    mergeInFlame
  }

  override def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit = {
    var removeVirus = Map.empty[Long,Virus]
//    var removeVirus = List.empty[Virus]
    val newPlayerMap = playerMap.values.map {
      player =>
        var newSplitTime = player.lastSplit
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var vSplitCells = List[Cell]()
            var newMass = cell.mass
            var newRadius = cell.radius
            //病毒碰撞检测
            virusMap.foreach { vi =>
              val v = vi._2
              if ((sqrt(pow(v.x - cell.x, 2.0) + pow(v.y - cell.y, 2.0)) < cell.radius) && (cell.radius > v.radius * 1.2) && !mergeInFlame) {
                removeVirus += (vi._1->vi._2)
//                virus = virus.filterNot(_ == v)
                val cellMass = (newMass / (v.splitNumber + 1)).toInt
                val cellRadius = 4 + sqrt(cellMass) * mass2rRate
                newMass = (newMass / (v.splitNumber + 1)).toInt + (v.mass * 0.5).toInt
                newRadius = 4 + sqrt(newMass) * mass2rRate
                newSplitTime = System.currentTimeMillis()
                val baseAngle = 2 * Pi / v.splitNumber
                for (i <- 0 until v.splitNumber) {
                  val degX = cos(baseAngle * i)
                  val degY = sin(baseAngle * i)
                  val startLen = (newRadius + cellRadius) * 1.2*3
                  // vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong,(cell.x + startLen * degX).toInt,(cell.y + startLen * degY).toInt,cellMass,cellRadius,cell.speed)
                  val speedx = (cos(baseAngle * i) * cell.speed).toFloat*3
                  val speedy = (sin(baseAngle * i) * cell.speed).toFloat*3
                  vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong, (cell.x + startLen * degX).toInt, (cell.y + startLen * degY).toInt, cellMass, cellRadius, cell.speed, speedx, speedy)
                }
              }
            }
            List(Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)) ::: vSplitCells
        }

        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id, player.name, player.color, player.x, player.y, player.targetX, player.targetY, player.kill, player.protect, newSplitTime, player.killerName, player.width, player.height, newCells)
    }

    val event = RemoveVirus(removeVirus,frameCount)
    AddGameEvent(event)
    virusMap --= removeVirus.keySet.toList
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  override def checkPlayerFoodCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            food.foreach {
              case (p, color) =>
                if (checkCollision(Point(cell.x, cell.y), p, cell.radius, 4, -1)) {
                  //食物被吃掉
                  newMass += foodMass
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  food -= p
                  eatenFoods+=(p->color)
                  if (newProtected)
                  //吃食物后取消保护
                    newProtected = false
                }
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,newProtected,player.lastSplit,player.killerName,player.width,player.height,newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  override def checkPlayerMassCrash(): Unit = {
    var removeMass = List.empty[Mass]
    val newPlayerMap = playerMap.values.map {
      player =>
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.mass
            var newRadius = cell.radius
            massList.foreach {
              case p: Mass =>
                if (checkCollision(Point(cell.x, cell.y), Point(p.x, p.y), cell.radius, p.radius, coverRate)) {
                  newMass += p.mass
                  newRadius = 4 + sqrt(newMass) * mass2rRate
                  massList = massList.filterNot(l => l == p)
                  removeMass ::= p
                }
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX, y = newY, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,newProtected,player.lastSplit,player.killerName,player.width,player.height,newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    val event = RemoveMass(removeMass,frameCount)
    AddGameEvent(event)
  }

 override def checkVirusMassCrash(): Unit = {
   var removeMass = List.empty[Mass]
   var newVirus = Map.empty[Long,Virus]
   //TODO 这边病毒的运动有待商榷
    val virus1 = virusMap.flatMap{vi=>
      val v = vi._2
      var newMass = v.mass
      var newRadius = v.radius
      var newSpeed = v.speed
      var newTargetX = v.targetX
      var newTargetY = v.targetY
      var newX = v.x
      var newY = v.y
      var hasMoved = false
      val (nx,ny)= normalization(newTargetX,newTargetY)
      massList.foreach {
        case p: Mass =>
          if (checkCollision(Point(v.x, v.y), Point(p.x, p.y), v.radius, p.radius, coverRate)) {
            val (mx,my)=normalization(p.targetX,p.targetY)
            // println(s"mx$mx,my$my")
            val vx = (nx*newMass*newSpeed + mx*p.mass*p.speed)/(newMass+p.mass)
            val vy = (ny*newMass*newSpeed + my*p.mass*p.speed)/(newMass+p.mass)

            newX += vx.toInt
            newY += vy.toInt
            hasMoved =true
            val newPoint =ExamBoundary(newX,newY)
            newX = newPoint._1
            newY = newPoint._2
           /* val borderCalc = 0
            if (newX > boundary.x - borderCalc) newX = boundary.x - borderCalc
            if (newY > boundary.y - borderCalc) newY = boundary.y - borderCalc
            if (newX < borderCalc) newX = borderCalc
            if (newY < borderCalc) newY = borderCalc*/
            newMass += p.mass
            newRadius = 4 + sqrt(newMass) * mass2rRate
            newSpeed = sqrt(pow(vx,2)+ pow(vy,2))
            newTargetX = vx
            newTargetY = vy
            massList = massList.filterNot(l => l == p)
            removeMass ::= p
            newVirus += vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
          }
      }
//      newSpeed -= virusSpeedDecayRate
//      if(newSpeed<0) newSpeed=0
//      if(hasMoved==false && newSpeed!=0){
//        newX += (nx*newSpeed).toInt
//        newY += (ny*newSpeed).toInt
//        val borderCalc = 0
//        if (newX > boundary.x - borderCalc) newX = boundary.x - borderCalc
//        if (newY > boundary.y - borderCalc) newY = boundary.y - borderCalc
//        if (newX < borderCalc) newX = borderCalc
//        if (newY < borderCalc) newY = borderCalc
//      }
      if(newMass>virusMassLimit){
        newMass = newMass/2
        newRadius = 4 + sqrt(newMass) * mass2rRate
        val newX2 = newX + (nx*newRadius*2).toInt
        val newY2 = newY + (ny*newRadius*2).toInt
        //分裂后新生成两个
        val v1 = vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
        val v2 = VirusId.getAndIncrement() -> v.copy(x = newX2,y=newY2,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
        newVirus ++= List(v2)
        List(v1,v2)
      }else{

        val v1 =vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
//        newVirus += v1
        List(v1)
      }
    }
//    virus = virus1
    virusMap ++= virus1
   if(removeMass.nonEmpty){
     val event = RemoveMass(removeMass,frameCount)
//     dispatch(subscriber,event)
     AddGameEvent(event)
   }
   if(newVirus.nonEmpty){
     val event = GenerateVirus(newVirus,frameCount)
//     dispatch(subscriber,event)
     AddGameEvent(event)
   }

  }

  override def getAllGridData: Protocol.GridDataSync = {
//    val foodDetails: List[Food] = Nil
    var playerDetails: List[Player] = Nil
    var newFoodDetails: List[Food] = Nil
    var eatenFoodDetails:List[Food] = Nil

    if(frameCount<100){
      food.foreach{
        case (p,mass) =>
          newFoodDetails ::= Food(mass, p.x, p.y)
      }
    }else{
      newFoods.foreach{
        case (p,mass) =>
          newFoodDetails ::= Food(mass, p.x, p.y)
      }
    }
    playerMap.foreach{
      case (id,player) =>
        playerDetails ::= player
    }
    eatenFoods.foreach{
      case (p,mass) =>
        eatenFoodDetails ::= Food(mass, p.x, p.y)
    }
    newFoods=Map[Point, Int]().empty
    eatenFoods = Map[Point, Int]().empty
    Protocol.GridDataSync(
      frameCount,
      playerDetails,
//      foodDetails,
      massList,
//      virus,
      virusMap,
      1.0,
      newFoodDetails,
      eatenFoodDetails
    )
  }

//获取快照
  def getSnapShot()={

    val playerDetails =  playerMap.map{
      case (id,player) => player
    }.toList

    val foodDetails = food.map{f=>
      Food(f._2,f._1.x,f._1.y)
    }.toList

    Protocol.GypsyGameSnapInfo(
      frameCount,
      playerDetails,
      foodDetails,
      massList,
      virusMap
    )
  }

//  获取事件
  def getEvents()={
    (GameEventMap.getOrElse(frameCount-1,Nil) ::: ActionEventMap.getOrElse(frameCount-1,Nil))
      .filter(_.isInstanceOf[GameEvent]).map(_.asInstanceOf[GameEvent])
  }


  override def update(): Unit = {
    super.update()
    genWaitingStar()  //新增
    updateRanks()  //排名
  }

  def getNewApples = newFoods
  def cleanNewApple = {
    newFoods = Map.empty
  }


  def randomEmptyPoint(): Point = {
    val p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
    //    while (grid.contains(p)) {
    //      p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
    //      //TODO 随机点的生成位置需要限制
    //    }
    p
  }

  def getSubscribersMap(subscribersMap:mutable.HashMap[String,ActorRef[WsMsgSource]]) ={
    subscriber=subscribersMap
  }


  override def getActionEventMap(frame:Long): List[GameEvent] = {
    ActionEventMap.getOrElse(frame,List.empty)
  }

  override def getGameEventMap(frame: Long): List[Protocol.GameEvent] = {
    GameEventMap.getOrElse(frame,List.empty)
  }

  def getUserList(userList:mutable.ListBuffer[UserInfo])={
    userLists = userList
  }

}
