package com.neo.sk.gypsy.snake

import com.neo.sk.gypsy.shared.Grid
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.core.RoomActor.{dispatch, dispatchTo}
import com.neo.sk.gypsy.shared._
import com.neo.sk.gypsy.shared.ptcl._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.math.{pow, sqrt}
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
  private[this] var addedVirus:List[Virus] = Nil
  private [this] var subscriber=mutable.HashMap[Long,ActorRef[WsFrontProtocol.WsMsgFront]]()


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
          dispatchTo(subscriber,player.id,WsFrontProtocol.UserDeadMessage(player.id,killer,player.killerName,player.kill,score.toInt,System.currentTimeMillis()-player.startTime))
          dispatch(subscriber,WsFrontProtocol.KillMessage(killer,player.id))

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

  def getSubscribersMap(subscribersMap:mutable.HashMap[Long,ActorRef[WsFrontProtocol.WsMsgFront]]) ={
    subscriber=subscribersMap
  }

}
