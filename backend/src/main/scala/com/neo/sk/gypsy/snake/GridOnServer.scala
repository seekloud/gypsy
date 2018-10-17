package com.neo.sk.gypsy.snake

import com.neo.sk.gypsy.shared.Grid
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.core.RoomActor.{dispatch, dispatchTo}
import com.neo.sk.gypsy.shared._
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.UserMerge
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.util.utils.{checkCollision, normalization}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.math.{Pi, cos, pow, sin, sqrt,atan2,abs,acos}
import scala.util.Random

/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 9:55 PM
  */
class GridOnServer(override val boundary: Point) extends Grid {


  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)


  private[this] var waitingJoin = Map.empty[Long, String]
  private[this] var feededApples: List[Food] = Nil
  private[this] var newFoods = Map[Point, Int]()
  private[this] var eatenFoods = Map[Point, Int]()
  private[this] var addedVirus:List[Virus] = Nil
  private[this] var subscriber=mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]]()


  var currentRank = List.empty[Score]
  private[this] var historyRankMap = Map.empty[Long, Score]
  var historyRankList = historyRankMap.values.toList.sortBy(_.k).reverse

  private[this] var historyRankThreshold = if (historyRankList.isEmpty) -1 else historyRankList.map(_.k).min

  def addSnake(id: Long, name: String) = waitingJoin += (id -> name)


  private[this] def genWaitingStar() = {
    waitingJoin.filterNot(kv => playerMap.contains(kv._1)).foreach { case (id, name) =>
      val center = randomEmptyPoint()
      val color = new Random(System.nanoTime()).nextInt(7)
      playerMap += id -> Player(id,name,color.toString,center.x,center.y,0,0,0,true,System.currentTimeMillis(),"",8 + sqrt(10)*12,8 + sqrt(10)*12,List(Cell(cellIdgenerator.getAndIncrement().toLong,center.x,center.y)),System.currentTimeMillis())
    }
    waitingJoin = Map.empty[Long, String]
  }

  implicit val scoreOrdering = new Ordering[Score] {
    override def compare(x: Score, y: Score): Int = {
      var r = (y.score - x.score).toInt
      if (r == 0) {
        r = (x.id - y.id).toInt
      }
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
    feededApples = Nil
    var appleNeeded = appleCount
    while (appleNeeded > 0) {
      val p = randomEmptyPoint()
      val color = random.nextInt(7)
      feededApples ::= Food(color,p.x,p.y)
      newFoods+=(p->color)
      food += (p->color)
      appleNeeded -= 1
    }
  }

  override def addVirus(v: Int): Unit = {
    addedVirus = Nil
    var virusNeeded = v
    while(virusNeeded > 0){
      val p =randomEmptyPoint()
      val mass = 50 + random.nextInt(50)
      val radius = 4 + sqrt(mass) * mass2rRate
      virus ::= Virus(p.x,p.y,mass,radius)
      virusNeeded -= 1
    }
  }

  override def checkPlayer2PlayerCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var killer = 0l
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
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)
        }.filterNot(_.mass <= 0)
        if (newCells.isEmpty) {
          playerMap.get(killer) match {
            case Some(killerPlayer) =>
              player.killerName = killerPlayer.name
            case _ =>
              player.killerName = "unknown"
          }
          dispatchTo(subscriber,player.id,WsMsgProtocol.UserDeadMessage(player.id,killer,player.killerName,player.kill,score.toInt,System.currentTimeMillis()-player.startTime))
          dispatch(subscriber,WsMsgProtocol.KillMessage(killer,player))

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
      case Left(_) => (-2l, Player(0, "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
    }.filterNot(_._1 == -2l).toMap
    newPlayerMap.foreach {
      case Left(killId) =>
        val a = playerMap.getOrElse(killId, Player(0, "", "", 0, 0, cells = List(Cell(0L, 0, 0))))
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
            List(Cell(cell.id, cellX, cellY, newMass, newRadius, cell.speed, cell.speedX, cell.speedY))
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
    val newPlayerMap = playerMap.values.map {
      player =>
        var newSplitTime = player.lastSplit
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var vSplitCells = List[Cell]()
            var newMass = cell.mass
            var newRadius = cell.radius
            //病毒碰撞检测
            virus.foreach { v =>
              if ((sqrt(pow(v.x - cell.x, 2.0) + pow(v.y - cell.y, 2.0)) < cell.radius) && (cell.radius > v.radius * 1.2) && !mergeInFlame) {
                virus = virus.filterNot(_ == v)
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
            List(Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)) ::: vSplitCells
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
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)
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
                }
            }
            Cell(cell.id, cell.x, cell.y, newMass, newRadius, cell.speed, cell.speedX, cell.speedY)
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

 override def checkVirusMassCrash(): Unit = {
    val virus1 = virus.flatMap{v=>
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
            val borderCalc = 0
            if (newX > boundary.x - borderCalc) newX = boundary.x - borderCalc
            if (newY > boundary.y - borderCalc) newY = boundary.y - borderCalc
            if (newX < borderCalc) newX = borderCalc
            if (newY < borderCalc) newY = borderCalc
            newMass += p.mass
            newRadius = 4 + sqrt(newMass) * mass2rRate
            newSpeed = sqrt(pow(vx,2)+ pow(vy,2))
            newTargetX = vx
            newTargetY = vy
            massList = massList.filterNot(l => l == p)
          }
      }
      newSpeed -= 0.3
      if(newSpeed<0) newSpeed=0
      if(hasMoved==false && newSpeed!=0){
        newX += (nx*newSpeed).toInt
        newY += (ny*newSpeed).toInt
        val borderCalc = 0
        if (newX > boundary.x - borderCalc) newX = boundary.x - borderCalc
        if (newY > boundary.y - borderCalc) newY = boundary.y - borderCalc
        if (newX < borderCalc) newX = borderCalc
        if (newY < borderCalc) newY = borderCalc
      }
      if(newMass>virusMassLimit){
        newMass = newMass/2
        newRadius = 4 + sqrt(newMass) * mass2rRate
        val newX2 = newX + (nx*newRadius*2).toInt
        val newY2 = newY + (ny*newRadius*2).toInt
        List(v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed),v.copy(x = newX2,y=newY2,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed))
      }else{
        List(v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed))
      }
    }
    virus = virus1
  }

  override def getAllGridData: WsMsgProtocol.GridDataSync = {
    val foodDetails: List[Food] = Nil
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
    WsMsgProtocol.GridDataSync(
      frameCount,
      playerDetails,
      foodDetails,
      massList,
      virus,
      1.0,
      newFoodDetails,
      eatenFoodDetails
    )
  }

  override def update(): Unit = {
    super.update()
    genWaitingStar()  //新增
    updateRanks()  //排名
  }

  def getFeededApple = feededApples

  def randomEmptyPoint(): Point = {
    val p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
    //    while (grid.contains(p)) {
    //      p = Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
    //      //TODO 随机点的生成位置需要限制
    //    }
    p
  }

  def getSubscribersMap(subscribersMap:mutable.HashMap[Long,ActorRef[WsMsgProtocol.WsMsgFront]]) ={
    subscriber=subscribersMap
  }

}
