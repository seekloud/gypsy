package com.neo.sk.gypsy.snake

import com.neo.sk.gypsy.shared.ptcl._
import org.slf4j.LoggerFactory

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


  var currentRank = List.empty[Score]
  private[this] var historyRankMap = Map.empty[Long, Score]
  var historyRankList = historyRankMap.values.toList.sortBy(_.k).reverse

  private[this] var historyRankThreshold = if (historyRankList.isEmpty) -1 else historyRankList.map(_.k).min

  def addSnake(id: Long, name: String) = waitingJoin += (id -> name)


  private[this] def genWaitingStar() = {
    waitingJoin.filterNot(kv => playerMap.contains(kv._1)).foreach { case (id, name) =>
      val center = randomEmptyPoint()
      val color = new Random(System.nanoTime()).nextInt(7)
      playerMap += id -> Player(id,name,color.toString,center.x,center.y,0,0,0,true,System.currentTimeMillis(),List(Cell(center.x,center.y)))
    }
    waitingJoin = Map.empty[Long, String]
  }

  implicit val scoreOrdering = new Ordering[Score] {
    override def compare(x: Score, y: Score): Int = {
      var r = y.k - x.k
      if (r == 0) {
        r = (y.score - x.score).toInt
      }
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
        case Some(oldScore) if cScore.k > oldScore.k =>
          historyRankMap += (cScore.id -> cScore)
          historyChange = true
        case None if cScore.k > historyRankThreshold =>
          historyRankMap += (cScore.id -> cScore)
          historyChange = true
        case _ => //do nothing.
      }
    }

    if (historyChange) {
      historyRankList = historyRankMap.values.toList.sorted.take(historyRankLength)
      historyRankThreshold = historyRankList.lastOption.map(_.k).getOrElse(-1)
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

  override def update(): Unit = {
    super.update()
    genWaitingStar()
    updateRanks()
  }

  def getFeededApple = feededApples

}
