package com.neo.sk.gypsy.gypsyServer

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicLong

import com.neo.sk.gypsy.shared.Grid
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.shared.ptcl.Protocol.UserJoinRoom
import com.neo.sk.gypsy.shared.util.utils._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Random
import com.neo.sk.gypsy.core.{EsheepSyncClient, UserActor}
import com.neo.sk.gypsy.core.RoomActor.{dispatch, dispatchTo}
import com.neo.sk.gypsy.Boot.esheepClient

import scala.math.{Pi, abs, acos, atan2, cbrt, cos, pow, sin, sqrt}
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.Protocol
import com.neo.sk.gypsy.shared.ptcl.Game._
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
  private[this] var newFoods = Map[Point, Short]() // p -> color
  private[this] var eatenFoods = Map[Point, Short]()
  private[this] var addedVirus:List[Virus] = Nil
  private [this] var subscriber=mutable.HashMap[String,ActorRef[UserActor.Command]]()
  var currentRank = List.empty[Score]
//  private[this] var historyRankMap = Map.empty[String, Score]
//  var historyRankList = historyRankMap.values.toList.sortBy(_.k).reverse

//  private[this] var historyRankThreshold = if (historyRankList.isEmpty) -1 else historyRankList.map(_.k).min

  def addPlayer(id: String, name: String) = waitingJoin += (id -> name)

  private var roomId = 0l

  var ReLiveMap = Map.empty[String,Long]   //(BotId -> 时间)


  def setRoomId(id:Long)={
    roomId = id
  }
  var VirusId = new AtomicLong(1000L)

  init()  //初始化苹果以及病毒数据

  implicit val sendBuffer = new MiddleBufferInJvm(81920)

  private[this] def genWaitingStar() = {
    waitingJoin.filterNot(kv => playerMap.contains(kv._1)).foreach { case (id, name) =>
      val center = randomEmptyPoint()
      val color = new Random(System.nanoTime()).nextInt(24)
      val player = Player(id,name,color.toShort,center.x.toShort,center.y.toShort,0,0,0,true,System.currentTimeMillis(),"",8 + sqrt(10)*12,8 + sqrt(10)*12,List(Cell(cellIdgenerator.getAndIncrement().toLong,center.x.toShort,center.y.toShort)),System.currentTimeMillis())
      playerMap += id -> player
      val event = UserJoinRoom(roomId,player,frameCount+2)
      AddGameEvent(event)
      println(s" ${id} 加入事件！！  ${frameCount+2}")
      //TODO 这里没带帧号 测试后记入和实际上看的帧号有差
      dispatch(subscriber)(PlayerJoin(id,player))
      dispatchTo(subscriber)(id,getAllGridData)
    }
    waitingJoin = Map.empty[String, String]
  }

  implicit val scoreOrdering = new Ordering[Score] {
    override def compare(x: Score, y: Score): Int = {
      var r = y.score - x.score
      if (r == 0) {
        r = y.k - x.k
      }
      r
    }
  }

  private[this] def updateRanks() = {
    currentRank = playerMap.values.map(s => Score(s.id, s.name, s.kill, s.cells.map(_.mass).sum)).toList.sorted
//    var historyChange = false
//    currentRank.foreach { cScore =>
//      historyRankMap.get(cScore.id) match {
//        case Some(oldScore) if cScore.score > oldScore.score =>
//          historyRankMap += (cScore.id -> cScore)
//          historyChange = true
//        case None if cScore.score > historyRankThreshold =>
//          historyRankMap += (cScore.id -> cScore)
//          historyChange = true
//        case _ => //do nothing.
//      }
//    }
//    if (historyChange) {
//      historyRankList = historyRankMap.values.toList.sorted.take(historyRankLength)
//      historyRankThreshold = historyRankList.lastOption.map(_.score.toInt).getOrElse(-1)
//      historyRankMap = historyRankList.map(s => s.id -> s).toMap
//    }
  }

  override def feedApple(appleCount: Int): Unit = {
    //TODO 考虑出生时的苹果列表
    var appleNeeded = appleCount
    while (appleNeeded > 0) {
      val p = randomEmptyPoint()
      val color = random.nextInt(7).toShort
      newFoods += (p->color)
      food += (p->color)
      appleNeeded -= 1
    }
    val event = GenerateApples(newFoods,frameCount)
    AddGameEvent(event)
  }

  override def addVirus(v: Int): Unit = {
    var virusNeeded = v
    var addNewVirus = Map.empty[Long,Virus]
    while(virusNeeded > 0){
      val p =randomEmptyPoint()
      val mass = (50 + random.nextInt(50)).toShort
      val radius = Mass2Radius(mass)
      val vid = VirusId.getAndIncrement()
      val newVirus = Virus(vid,p.x.toShort,p.y.toShort,mass,radius)
      addNewVirus += (vid->newVirus)
      virusNeeded -= 1
    }
    virusMap ++= addNewVirus
    if(addNewVirus.keySet.nonEmpty){
      dispatch(subscriber)(AddVirus(addNewVirus))
      val event = GenerateVirus(addNewVirus,frameCount)
      AddGameEvent(event)
    }
  }

  //初始化，记录数据时候由于增加苹果和病毒是加在updateSpot里面所以初始化的快照没有任何数据
  def init()={
    addVirus(virusNum)
    feedApple(foodPool)
  }



  override def checkPlayer2PlayerCrash(): Unit = {
//    println(s"===========BEGIN============${frameCount} ")
//    playerMap.values.foreach{p=>
//      println(s"&&&&& size: ${p.cells.size}")
//      p.cells.foreach{c=>
//        println(s"playId:${p.id} mass:${c.newmass}  ")
//      }
//    }

    var p2pCrash = false
    var changedPlayers = Map[String,List[Cell]]()
    val newPlayerMap = playerMap.values.map {
      player =>
        var playerChange = false
        var killer = ""
        val score = player.cells.map(_.mass).sum
        var changedCells = List[Cell]()
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var cellChange = false
            var newMass = cell.newmass
            var newRadius = cell.radius
            playerMap.filterNot(a => a._1 == player.id || a._2.protect).foreach { p =>
              p._2.cells.foreach { otherCell =>
                if (cell.mass * 1.1 < otherCell.mass && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (otherCell.radius - cell.radius * 0.8) && !player.protect) {
                  //被吃了
                  playerChange = true
                  p2pCrash = true
                  newMass = 0
                  newRadius = 0
                  killer = p._1
                  cellChange = true
                } else if (cell.mass > otherCell.mass * 1.1 && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (cell.radius - otherCell.radius * 0.8) && !p._2.protect) {
                  //吃掉别人了
                  playerChange = true
                  p2pCrash = true
                  newMass = (newMass + otherCell.newmass).toShort
                  newRadius = Mass2Radius(newMass)
                  cellChange = true
                }
              }
            }
//            val newCell = cell.copy(id = cell.id,newmass = newMass,radius = newRadius)
            val newCell = cell.copy(newmass = newMass,radius = newRadius)
//            println(s"frame:${frameCount} ${player.id} (newId,oldId): ${(newCell.id,cell.id)} mass：${(newCell.newmass,cell.newmass)}  ")
            if(cellChange){
//              changedCells = Cell(cell.id, cell.x, cell.y, newMass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY, cell.parallel,cell.isCorner) :: changedCells
              changedCells = newCell :: changedCells
            }
//            Cell(cell.id, cell.x, cell.y, newMass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY, cell.parallel,cell.isCorner)
            newCell
        }.filterNot(c => c.newmass < 1 || c.radius < 1 )
        if (newCells.isEmpty) {
          playerMap.get(killer) match {
            case Some(killerPlayer) =>
              player.killerName = killerPlayer.name
            case _ =>
              player.killerName = "unknown"
          }
//          加入待复活列表
          if(player.id.startsWith("bot_")){
            ReLiveMap += (player.id -> System.currentTimeMillis())
          }

          dispatchTo(subscriber)(player.id,Protocol.UserDeadMessage(player.id,killer,player.killerName,player.kill,score,System.currentTimeMillis()-player.startTime))
          dispatch(subscriber)(Protocol.KillMessage(killer,player))
          esheepClient ! EsheepSyncClient.InputRecord(player.id.toString,player.name,player.kill,1,player.cells.map(_.mass).sum.toInt, player.startTime, System.currentTimeMillis())
          val event = KillMsg(killer,player,score,System.currentTimeMillis()-player.startTime,frameCount)
          AddGameEvent(event)
          Left(killer)
        } else {
          if (playerChange){
            changedPlayers+=(player.id->changedCells)
          }
          val length = newCells.length
          val newX = newCells.map(_.x).sum / length
          val newY = newCells.map(_.y).sum / length
          val left = newCells.map(a => a.x - a.radius).min
          val right = newCells.map(a => a.x + a.radius).max
          val bottom = newCells.map(a => a.y - a.radius).min
          val top = newCells.map(a => a.y + a.radius).max
          Right(player.copy(x = newX.toShort, y = newY.toShort, width = right - left, height = top - bottom, cells = newCells))
        }
    }
    playerMap = newPlayerMap.map {
      case Right(s) => (s.id, s)
      case Left(_) => ("", Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
    }.filterNot(_._1 == "").toMap

//    println(s"===========AFTER============ ")
//    playerMap.values.foreach{p=>
//      println(s"&&&&& size: ${p.cells.size}")
//      p.cells.foreach{c=>
//        println(s"playId:${p.id} mass:${c.newmass}  ")
//      }
//    }

    newPlayerMap.foreach {
      case Left(killId) =>
        val a = playerMap.getOrElse(killId, Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
        playerMap += (killId -> a.copy(kill = (a.kill + 1).toShort ))
      case Right(_) =>
    }
    if(p2pCrash){
      val event = PlayerInfoChange(playerMap,frameCount)
      AddGameEvent(event)
      dispatch(subscriber)(UserCrash(changedPlayers))
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
            var newMass = cell.newmass
            var cellX = cell.x
            var cellY = cell.y
            //自身cell合并检测
            player.cells.filterNot(p => p == cell).sortBy(_.radius).reverse.foreach { cell2 =>
              val distance = sqrt(pow(cell.y - cell2.y, 2) + pow(cell.x - cell2.x, 2))
              val deg= acos(abs(cell.x-cell2.x)/distance)
              val radiusTotal = cell.radius + cell2.radius
              if (distance < radiusTotal) {
                if ((newSplitTime > System.currentTimeMillis() - mergeInterval) && System.currentTimeMillis()>newSplitTime + 2*1000) {
                    if (cell.x < cell2.x) cellX = (cellX - ((cell.radius+cell2.radius-distance)*cos(deg))/4).toShort
                    else if (cell.x > cell2.x) cellX = (cellX + ((cell.radius+cell2.radius-distance)*cos(deg))/4).toShort
                    if (cell.y < cell2.y) cellY = (cellY - ((cell.radius+cell2.radius-distance)*sin(deg))/4).toShort
                    else if (cell.y > cell2.y) cellY = (cellY + ((cell.radius+cell2.radius-distance)*sin(deg))/4).toShort

                }
                else if ((distance < radiusTotal / 2)&&(newSplitTime <= System.currentTimeMillis() - mergeInterval) ) {
                  /**融合实质上是吃与被吃的关系：大球吃小球，同等大小没办法融合**/
                  if (cell.radius > cell2.radius) {
                    //被融合的细胞不能再被其他细胞融合
                    if (!mergeCells.exists(_.id == cell2.id) && !mergeCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell.id)) {
                      playerIsMerge=true
                      newMass = (newMass + cell2.newmass).toShort
                      newRadius = Mass2Radius(newMass)
                      mergeCells = cell2 :: mergeCells
                    }
                  }
                  else if (cell.radius < cell2.radius && !deleteCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell2.id)) {
                    playerIsMerge=true
                    newMass = 0
                    newRadius = 0
                    deleteCells = cell :: deleteCells
//                    cellX = cell2.x
//                    cellY = cell2.y
                  }
                }
              }
            }
            List(Cell(cell.id, cellX, cellY, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner))
        }.filterNot(e=> e.newmass <= 0 && e.mass <= 0)
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        if(playerIsMerge){
          mergeInFlame = true
          //dispatch(subscriber)(UserMerge(player.id,player.copy(x = newX, y = newY, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells.sortBy(_.id))))
        }

        player.copy(x = newX.toShort, y = newY.toShort, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells.sortBy(_.id))
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    if(mergeInFlame){
      val event = PlayerInfoChange(playerMap,frameCount)
      AddGameEvent(event)
    }
    mergeInFlame
  }

  override def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit = {
    var removeVirus = Map.empty[Long,Virus]
    var splitPlayer = Map.empty[String,Player]
    val newPlayerMap = playerMap.values.map {
      player =>
        var split = false
        var isRemoveVirus = false
        var newSplitTime = player.lastSplit
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var vSplitCells = List[Cell]()
            var newMass = cell.newmass
            var newRadius = cell.radius
            //病毒碰撞检测
            if(!mergeInFlame && !isRemoveVirus){
              val newvirusMap = virusMap.filter(v => (sqrt(pow(v._2.x - cell.x, 2.0) + pow(v._2.y - cell.y, 2.0)) < cell.radius)).
                toList.sortBy(v => (sqrt(pow(v._2.x - cell.x, 2.0) + pow(v._2.y - cell.y, 2.0)))).reverse
              newvirusMap.foreach { vi =>
                val v = vi._2
                if ((sqrt(pow(v.x - cell.x, 2.0) + pow(v.y - cell.y, 2.0)) < cell.radius) && (cell.radius > v.radius * 1.2) ) {
                  isRemoveVirus = true
                  split = true
                  removeVirus += (vi._1->vi._2)
                  val splitNum = if(VirusSplitNumber>maxCellNum-player.cells.length) maxCellNum-player.cells.length else VirusSplitNumber
                  if(splitNum>0){
                    val cellMass = (newMass / (splitNum + 1)).toShort
                    val cellRadius = Mass2Radius(cellMass)
                    newMass = ( (newMass / (splitNum + 1)) + (v.mass * 0.5) ).toShort
                    newRadius = Mass2Radius(newMass)
                    newSplitTime = System.currentTimeMillis()
                    val baseAngle = 2 * Pi / splitNum
                    for (i <- 0 until splitNum) {
                      val degX = cos(baseAngle * i)
                      val degY = sin(baseAngle * i)
                      val startLen = (newRadius + cellRadius) * 1.2 * 3
                      val speedx = (cos(baseAngle * i) * cell.speed).toFloat*3
                      val speedy = (sin(baseAngle * i) * cell.speed).toFloat*3
                      vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong, (cell.x + startLen * degX).toShort, (cell.y + startLen * degY).toShort, 1, cellMass, cellRadius, cell.speed, speedx, speedy)
                    }
                  }
                }
              }
            }
            /**1+13**/
            List(Cell(cell.id, cell.x, cell.y, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)) ::: vSplitCells
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        val newplayer = player.copy(x = newX.toShort, y = newY.toShort, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
        if(split){
          splitPlayer += (player.id->newplayer)
        }
        newplayer
    }
    virusMap --= removeVirus.keySet.toList
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    if(removeVirus.nonEmpty && splitPlayer.nonEmpty){
      val event2 = PlayerInfoChange(playerMap,frameCount)
      AddGameEvent(event2)
      //只发送改变的玩家
      dispatch(subscriber)(PlayerSplit(splitPlayer))
    }
  }

  override def checkPlayerFoodCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var newProtected = player.protect
        val newCells = player.cells.map {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            food.foreach {
              case (p, color) =>
                if (checkCollision(Point(cell.x, cell.y), p, cell.radius, 4, -1)) {
                  //食物被吃掉
                  newMass = (newMass+foodMass).toShort
                  newRadius = Mass2Radius(newMass)
                  food -= p
                  eatenFoods+=(p->color)
                  if (newProtected)
                  //吃食物后取消保护
                    newProtected = false
                }
            }
            Cell(cell.id, cell.x, cell.y, cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX.toShort, y = newY.toShort, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
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
            var newMass = cell.newmass
            var newRadius = cell.radius
            massList.foreach {
              case p: Mass =>
                if (checkCollision(Point(cell.x, cell.y), Point(p.x, p.y), cell.radius, p.radius, coverRate)) {
                  newMass = (newMass + p.mass).toShort
                  newRadius = Mass2Radius(newMass)
                  massList = massList.filterNot(l => l == p)
                }
            }
            Cell(cell.id, cell.x, cell.y,cell.mass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        player.copy(x = newX.toShort, y = newY.toShort, protect = newProtected, width = right - left, height = top - bottom, cells = newCells)
      //Player(player.id,player.name,player.color,player.x,player.y,player.targetX,player.targetY,player.kill,newProtected,player.lastSplit,player.killerName,player.width,player.height,newCells)
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  override def checkVirusMassCrash(): Unit = {
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
            val vx = (nx*newMass*newSpeed + mx*p.mass*p.speed)/(newMass+p.mass)
            val vy = (ny*newMass*newSpeed + my*p.mass*p.speed)/(newMass+p.mass)
            hasMoved =true
            newMass = (newMass + p.mass).toShort
            newRadius = Mass2Radius(newMass)
            newSpeed = sqrt(pow(vx,2)+ pow(vy,2)).toFloat
            newTargetX = vx.toShort
            newTargetY = vy.toShort
            massList = massList.filterNot(l => l == p)
          }
      }
      if(newMass > virusMassLimit){
        newMass = (newMass/2).toShort
        newRadius = Mass2Radius(newMass)
        val newX2 = newX + (nx*newRadius*2).toInt
        val newY2 = newY + (ny*newRadius*2).toInt
        //分裂后新生成两个
        val v1 = vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
        val v2 = VirusId.getAndIncrement() -> v.copy(x = newX2.toShort,y=newY2.toShort,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
        newVirus += v2
        List(v1,v2)
      }else{
        val v1 =vi._1 -> v.copy(x = newX,y=newY,mass=newMass,radius = newRadius,targetX = newTargetX,targetY = newTargetY,speed = newSpeed)
        List(v1)
      }
    }
    virusMap ++= virus1
   if(newVirus.nonEmpty){
     //生成病毒发送给前端（仅发送前端无法生成的v2）
     dispatch(subscriber)(AddVirus(newVirus))
     val event = GenerateVirus(newVirus,frameCount)
     AddGameEvent(event)
   }
  }

  /**
    * method: getAllGridData
    * describe: 获取全量数据
    */
  override def getAllGridData: Protocol.GridDataSync = {
    var playerDetails: List[Player] = Nil
    var newFoodDetails: List[Food] = Nil
    var eatenFoodDetails:List[Food] = Nil
    //var playerPosition:List[PlayerPosition] = Nil
    newFoods.foreach{
      case (p,mass) =>
        newFoodDetails ::= Food(mass, p.x, p.y)
    }
    playerMap = playerMap.map{
      item =>
        val  newcells  = item._2.cells.filterNot(_.newmass==0).map(cell => cell.copy(mass = cell.newmass))
        val newplayer = item._2.copy(cells = newcells)
        playerDetails ::= newplayer
        //playerPosition ::= PlayerPosition(item._1,item._2.x,item._2.y,item._2.targetX,item._2.targetY)
        item.copy(_2 = newplayer)
    }
    eatenFoods.foreach{
      case (p,mass) =>
        eatenFoodDetails ::= Food(mass, p.x, p.y)
    }
    newFoods=Map[Point, Short]().empty
    eatenFoods = Map[Point, Short]().empty
    Protocol.GridDataSync(
      frameCount,
      playerDetails,
      massList,
      virusMap,
      1.0,
      //playerPosition,
      newFoodDetails,
      eatenFoodDetails
    )
  }


  override def checkPlayerSplit(actMap: Map[String,KeyCode], mouseActMap: Map[String, MousePosition]): Unit = {
    var SplitPlayerMap = Map[String,Player]()
    val newPlayerMap = playerMap.values.map {
      player =>
        var isSplit = false
        var newSplitTime = player.lastSplit
        val mouseAct = mouseActMap.getOrElse(player.id,MousePosition(Some(player.id),player.targetX, player.targetY,0,0))
        val split = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.keyCode==KeyEvent.VK_F
          case _ => false
        }
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            val target = Position( (mouseAct.clientX + player.x - cell.x).toShort , (mouseAct.clientY + player.y - cell.y).toShort )
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            var splitX:Short = 0
            var splitY:Short = 0
            var splitMass:Short = 0
            var splitRadius:Short = 0
            var splitSpeed = 0.0
            var cellId = 0L
            if (split && cell.newmass > splitLimit && player.cells.size < maxCellNum) {
              newSplitTime = System.currentTimeMillis()
              splitMass = (newMass / 2).toShort
              newMass = (newMass- splitMass).toShort
              splitRadius = Mass2Radius(splitMass)
              newRadius = Mass2Radius(newMass)
              splitSpeed = splitBaseSpeed + 2 * cbrt(cell.radius)
              splitX = (cell.x + (newRadius + splitRadius) * degX).toShort
              splitY = (cell.y + (newRadius + splitRadius) * degY).toShort
              cellId = cellIdgenerator.getAndIncrement().toLong
              isSplit = true
            }
            /**效果：大球：缩小，小球：从0碰撞，且从大球中滑出**/
            //            println(cell.mass + "   " + newMass)
//            println(s"cellId:${cellId} id:${cell.id} ")
            List(Cell(cell.id, cell.x, cell.y, newMass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner),
              Cell(cellId,  splitX, splitY, splitMass, splitMass, splitRadius, splitSpeed.toFloat, (splitSpeed * degX).toFloat, (splitSpeed * degY).toFloat))


        }.filterNot(e=> e.newmass <= 0 && e.mass <=0 )

        if(isSplit){
          val length = newCells.length
          val newX = newCells.map(_.x).sum / length
          val newY = newCells.map(_.y).sum / length
          val left = newCells.map(a => a.x - a.radius).min
          val right = newCells.map(a => a.x + a.radius).max
          val bottom = newCells.map(a => a.y - a.radius).min
          val top = newCells.map(a => a.y + a.radius).max
          val newPlayer = player.copy(x = newX.toShort , y = newY.toShort , lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
          SplitPlayerMap += (player.id -> newPlayer)
          newPlayer
        }else{
          player
        }

    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap

    if(SplitPlayerMap.nonEmpty){
      val msg = PlayerSplit(SplitPlayerMap)
      dispatch(subscriber)(msg)
    }

  }



  override def getGridData(id: String, winWidth: Int, winHeight: Int): GridDataSync = super.getGridData(id, winWidth, winHeight)

  def getDataForBot(id:String,winWidth:Int,winHeight:Int):GridData4Bot =  {
    val currentPlayer = playerMap.get(id).map(a=>(a.x,a.y)).getOrElse(( winWidth/2 ,winHeight/2 ))
    val zoom = playerMap.get(id).map(a=>(a.width,a.height)).getOrElse((30.0,30.0))
    if(getZoomRate(zoom._1,zoom._2,winWidth,winHeight)!=1){
      Scale = getZoomRate(zoom._1,zoom._2,winWidth,winHeight)
    }
    val width = winWidth / Scale / 2
    val height = winHeight / Scale / 2
    var playerDetails: List[Player] = Nil

    playerMap.foreach{
      case (id,player) =>
        if (checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(player.x,player.y),sqrt(pow(player.width/2,2.0)+pow(player.height/2,2.0)),width,height))
          playerDetails ::= player
    }
    val foodList = food.filter(m =>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m._1.x,m._1.y),4,width,height)).map{m=>
      Food(m._2,m._1.x,m._1.y)
    }.toList

    Protocol.GridData4Bot(
      frameCount,
      playerDetails,
      massList.filter(m=>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m.x,m.y),m.radius,width,height)),
      virusMap.filter(m =>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m._2.x,m._2.y),m._2.radius,width,height)),
      foodList
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

    val snapRank = currentRank.zipWithIndex.map(r=>RankInfo(r._2+1,r._1))

    Protocol.GypsyGameSnapInfo(
      frameCount,
      playerDetails,
      foodDetails,
      massList,
      virusMap,
      snapRank
    )
  }

  //获取事件
  def getEvents()= {
    val ge = GameEventMap.getOrElse(frameCount-1,List.empty)
    val ae = ActionEventMap.getOrElse(frameCount-1,List.empty)
    (ge:::ae).filter(_.isInstanceOf[GameEvent])

  }


  override def update(): Unit = {
    super.update()
    genWaitingStar()  //新增
    updateRanks()  //排名
  }

  def getApples = food

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

  def getSubscribersMap(subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]]) ={
    subscriber=subscribersMap
  }

  override def getActionEventMap(frame:Int): List[GameEvent] = {
    ActionEventMap.getOrElse(frame,List.empty)
  }

  override def getGameEventMap(frame: Int): List[Protocol.GameEvent] = {
    GameEventMap.getOrElse(frame,List.empty)
  }


}
