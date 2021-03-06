package com.neo.sk.gypsy.gypsyServer

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import com.neo.sk.gypsy.shared.Grid
import akka.actor.typed.ActorRef
import com.neo.sk.gypsy.shared.ptcl.Protocol.UserJoinRoom
import com.neo.sk.gypsy.shared.util.Utils._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Random
import com.neo.sk.gypsy.core.{BotActor, EsheepSyncClient, RoomActor, UserActor}
import com.neo.sk.gypsy.core.RoomActor.{Victory, dispatch, dispatchTo}
import com.neo.sk.gypsy.Boot.esheepClient
import com.neo.sk.gypsy.common.AppSettings

import scala.math.{Pi, abs, acos, atan2, cbrt, cos, pow, sin, sqrt}
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.gypsy.shared.ptcl.Protocol._
import com.neo.sk.gypsy.shared.ptcl.{Game, Protocol}
import com.neo.sk.gypsy.shared.ptcl.Game._
import com.neo.sk.gypsy.shared.ptcl.GameConfig._

/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 9:55 PM
  */
class GameServer(
  override val boundary: Point,
  roomActor: ActorRef[RoomActor.Command]
) extends Grid {


  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)


  private[this] var waitingJoin = Map.empty[String, String]
  private[this] var newFoods = Map[Point, Short]() // p -> color
  private[this] var eatenFoods = Map[Point, Short]()
  private[this] var addedVirus:List[Virus] = Nil
  private [this] var subscriber=mutable.HashMap[String,ActorRef[UserActor.Command]]()
  private[this] var botSubscriber=mutable.HashMap[String,(String,ActorRef[BotActor.Command],Boolean)]()
  var currentRank = List.empty[Score]

  val playerId2ByteMap  = new mutable.HashMap[String, Byte]()
  val playerId2ByteQueue = new mutable.Queue[Byte]()
  for( i <- 0 to 126){
    playerId2ByteQueue.enqueue(i.toByte)
  }

  def addPlayer(id: String, name: String): Unit = waitingJoin += (id -> name)

  private var roomId = 0l

  /**只针对bot的待复活列表**/
  var ReLiveMap = mutable.Map.empty[String,Long]   //(BotId -> 时间)


  def setRoomId(id:Long)={
    roomId = id
  }
  var VirusId = new AtomicLong(1000L)

  init()  //初始化苹果以及病毒数据

  implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(81920)

  private[this] def genWaitingStar(): Unit = {

    // create new player
    def createNewPlayer(id: String, name: String): Game.Player ={
      val center = randomEmptyPoint()
      val color = new Random(System.nanoTime()).nextInt(24)
      Player(
        id,
        name,
        color.toShort,
        center.x.toShort,
        center.y.toShort,
        targetX = 0,
        targetY = 0,
        kill = 0,
        protect = true,
        System.currentTimeMillis(),
        width = 8 + sqrt(10) * 12,
        height = 8 + sqrt(10) * 12,
        List(Cell(cellIdgenerator.getAndIncrement().toLong, center.x.toShort, center.y.toShort)),
        System.currentTimeMillis()
      )
    }

    // add players when it isn't in playerMap,
    // "genWaitingStar" executes in logic frame, but user join room doesn't have event
    waitingJoin.filterNot( kv => playerMap.contains(kv._1)).foreach { case (id, name) =>

      val player = createNewPlayer(id, name)
      playerMap += id -> player
      if(playerId2ByteMap.get(id).isEmpty){
        /**新玩家/bot加入，而非复活**/
        val playerIdByte = playerId2ByteQueue.dequeue()
        playerId2ByteMap += id -> playerIdByte
      }

      /*
      *   8> dispatch message when join success
      *   not dispatch many messages, only dispatch three
      *   "PlayerJoin": send byte id
      *   "GridDataSync": game state
      * */
      dispatchTo(subscriber)(id, getAllGridData)
      dispatchTo(subscriber)(id, Protocol.PlayerIdBytes(playerId2ByteMap.toMap))
      dispatch(subscriber)(PlayerJoin(playerId2ByteMap(id),player))

      // "UserJoinRoom" used in record
      val event = UserJoinRoom(roomId,player,frameCount+2)
      AddGameEvent(event)
    }
    waitingJoin = Map.empty[String, String]
  }


  private[this] def checkVictory(): Unit ={
    if(currentRank.nonEmpty){
      val FirstPlayer = currentRank.head
      if(FirstPlayer.score > VictoryScore){
        val finalFrame =  frameCount
        roomActor ! Victory(FirstPlayer.id,FirstPlayer.n,FirstPlayer.score,finalFrame)
      }
    }
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


  //这里用不到id！！！
  //键盘事件后，按键动作加入action列表
  def addActionWithFrame(id: String, keyCode: KC) = {
    val map = actionMap.getOrElse(keyCode.f, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (keyCode.f -> tmp)
    val action = KeyPress(id,keyCode.kC,keyCode.f,keyCode.sN)
    AddActionEvent(action)
  }

  def addMouseActionWithFrame(id: String, mp:MP) = {
    val map = mouseActionMap.getOrElse(mp.f, Map.empty)
    val tmp = map + (id -> mp)
    mouseActionMap += (mp.f -> tmp)
    val direct = (mp.cX,mp.cY)
    val action = MouseMove(id,direct,mp.f,mp.sN)
    AddActionEvent(action)
  }

  def removeActionWithFrame(id: String, userAction: UserAction, frame: Int) = {
    userAction match {
      case k:KC=>
        val map = actionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && k.sN == t._2.sN)
        actionMap += (frame->actionQueue)
      case m:MP=>
        val map = mouseActionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && m.sN == t._2.sN)
        mouseActionMap += (frame->actionQueue)
    }
  }

  def AddActionEvent(action: GameEvent):Unit ={
    ActionEventMap.get(action.frame) match {
      case Some(actionEvents) => ActionEventMap.put(action.frame,action :: actionEvents)
      case None => ActionEventMap.put(action.frame,List(action))
    }
  }

  def AddGameEvent(event:GameEvent):Unit ={
    GameEventMap.get(event.frame) match {
      case Some(gameEvents) => GameEventMap.put(event.frame,event :: gameEvents)
      case None => GameEventMap.put(event.frame,List(event))
    }
  }

  /**cell融合事件检测**/
  override def checkCellMerge: Boolean = {
    var mergeInFlame = false
    var mergePlayer = Map[Byte,List[(Long,Long)]]()
    val newPlayerMap = playerMap.values.map {
      player =>
        val newSplitTime = player.lastSplit
        var mergeCells = List[Cell]()
        var mergeCell = List[(Long,Long)]()
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
                if ((newSplitTime > System.currentTimeMillis() - mergeInterval) && System.currentTimeMillis() > newSplitTime + splitInterval) {
                  //                  if (cell.x < cell2.x) cellX = (cellX - ((cell.radius+cell2.radius-distance)*cos(deg))/4).toShort
                  //                  else if (cell.x > cell2.x) cellX = (cellX + ((cell.radius+cell2.radius-distance)*cos(deg))/4).toShort
                  //                  if (cell.y < cell2.y) cellY = (cellY - ((cell.radius+cell2.radius-distance)*sin(deg))/4).toShort
                  //                  else if (cell.y > cell2.y) cellY = (cellY + ((cell.radius+cell2.radius-distance)*sin(deg))/4).toShort
                  if (cell.x < cell2.x) cellX -= 1
                  else if (cell.x > cell2.x) cellX += 1
                  if (cell.y < cell2.y) cellY -= 1
                  else if (cell.y > cell2.y) cellY += 1
                }
                else if ((distance < radiusTotal / 2)&&(newSplitTime <= System.currentTimeMillis() - mergeInterval) && frameCount %2 ==0) {
                  /**融合实质上是吃与被吃的关系：大球吃小球，同等大小没办法融合**/
                  if (cell.radius > cell2.radius) {
                    //被融合的细胞不能再被其他细胞融合
                    if (!mergeCells.exists(_.id == cell2.id) && !mergeCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell.id)) {
                      playerIsMerge=true
                      newMass = (newMass + cell2.newmass).toShort
                      newRadius = Mass2Radius(newMass)
                      mergeCells = cell2 :: mergeCells
                      mergeCell = (cell.id,cell2.id) :: mergeCell
                    }
                  }
                  else if (cell.radius < cell2.radius && !deleteCells.exists(_.id == cell.id) && !deleteCells.exists(_.id == cell2.id)) {
                    playerIsMerge=true
                    newMass = 0
                    newRadius = 0
                    deleteCells = cell :: deleteCells
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
        if(playerIsMerge && playerId2ByteMap.get(player.id).isDefined){
          mergeInFlame = true
          mergePlayer += (playerId2ByteMap(player.id) -> mergeCell)
        }
        player.copy(x = newX.toShort, y = newY.toShort, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells.sortBy(_.id))
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    if(mergeInFlame){
      dispatch(subscriber)(UserMerge(mergePlayer, frameCount))
      val event = PlayerInfoChange(playerMap,frameCount)
      AddGameEvent(event)
    }
    mergeInFlame
  }


  /**玩家病毒碰撞检测**/
  override def checkPlayerVirusCrash(mergeInFlame: Boolean): Unit = {
    var removeVirus = Map.empty[Long,Virus]
    var splitPlayer = Map.empty[Byte,Player]
    val newPlayerMap = playerMap.values.map {
      player =>
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
                      val speedx = (degX * cell.speed).toFloat * 3
                      val speedy = (degY * cell.speed).toFloat * 3
                      vSplitCells ::= Cell(cellIdgenerator.getAndIncrement().toLong, (cell.x + startLen * degX).toShort, (cell.y + startLen * degY).toShort, cellMass, cellMass, cellRadius, cell.speed, speedx, speedy)
                    }
                  }
                }
              }
            }
            //TODO 修改策略
            /**1+13**/
            List(Cell(cell.id, cell.x, cell.y, newMass, newMass, newRadius, cell.speed, cell.speedX, cell.speedY,cell.parallel,cell.isCorner)) ::: vSplitCells
        }
        val length = newCells.length
        val newX = newCells.map(_.x).sum / length
        val newY = newCells.map(_.y).sum / length
        val left = newCells.map(a => a.x - a.radius).min
        val right = newCells.map(a => a.x + a.radius).max
        val bottom = newCells.map(a => a.y - a.radius).min
        val top = newCells.map(a => a.y + a.radius).max
        val newplayer = player.copy(x = newX.toShort, y = newY.toShort, lastSplit = newSplitTime, width = right - left, height = top - bottom, cells = newCells)
        if(isRemoveVirus){
          val playerByteId = playerId2ByteMap(player.id)
          splitPlayer += (playerByteId -> newplayer)
        }
        newplayer
    }
    virusMap --= removeVirus.keySet.toList
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    if(removeVirus.nonEmpty && splitPlayer.nonEmpty){
      val event2 = PlayerInfoChange(playerMap,frameCount)
      AddGameEvent(event2)
      //只发送改变的玩家
      dispatch(subscriber)(PlayerSplit(splitPlayer, frameCount))
      dispatch(subscriber)(RemoveVirus(removeVirus))
    }
  }

  /**碰撞事件检测**/
  override def checkPlayer2PlayerCrash(): Unit = {
    var p2pCrash = false
    var changedPlayers = Map[Byte,List[Cell]]()
    val newPlayerMap = playerMap.values.map {
      player =>
        var playerChange = false
        var newProtected = player.protect
        var killerId = ""
        val score = player.cells.map(_.mass).sum
        var changedCells = List[Cell]()
        val newCells = player.cells.sortBy(_.radius).reverse.map {
          cell =>
            var cellChange = false
            var newMass = cell.newmass
            var newRadius = cell.radius
            //            playerMap.filterNot(a => a._1 == player.id || a._2.protect).foreach { p =>
            playerMap.filterNot(a => a._1 == player.id).foreach { p =>
              p._2.cells.foreach { otherCell =>
                if (cell.mass * 1.1 < otherCell.mass && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (otherCell.radius - cell.radius * 0.8) && !player.protect) {
                  //被吃了
                  playerChange = true
                  p2pCrash = true
                  newMass = 0
                  newRadius = 0
                  killerId = p._1
                  cellChange = true
                } else if (cell.mass > otherCell.mass * 1.1 && sqrt(pow(cell.x - otherCell.x, 2.0) + pow(cell.y - otherCell.y, 2.0)) < (cell.radius - otherCell.radius * 0.8) && !p._2.protect) {
                  //吃掉别人了
                  playerChange = true
                  p2pCrash = true
                  newMass = (newMass + otherCell.newmass).toShort
                  newRadius = Mass2Radius(newMass)
                  cellChange = true
                  if(newProtected)
                    newProtected = false
                }
              }
            }
            val newCell = cell.copy(newmass = newMass,radius = newRadius)
            if(cellChange){
              changedCells = newCell :: changedCells
            }
            newCell
        }.filterNot(c => c.newmass < 1 || c.radius < 1 )
        if (newCells.isEmpty) {
          /**陪玩机器人加入待复活列表,如果总人数过多则直接杀死该bot**/
          if(player.id.startsWith("bot_")){
            val playerNum = playerMap.keySet.size
            if(playerNum>0){
              botSubscriber.get(player.id) match {
                case Some(bot) =>
                  bot._2 ! BotActor.KillBot
                case None =>
              }
            }
          }else{
            esheepClient ! EsheepSyncClient.InputRecord(player.id.toString,player.name,player.kill,1,player.cells.map(_.mass).sum.toInt, player.startTime, System.currentTimeMillis())
          }

          dispatchTo(subscriber)(player.id,Protocol.UserDeadMessage(playerMap.get(killerId).get.name,player.id,player.kill,score,System.currentTimeMillis()-player.startTime))
          dispatch(subscriber)(Protocol.KillMessage(killerId, player.id))
          val event = KillMsg(killerId,playerMap.get(killerId).get.name,player,score,System.currentTimeMillis()-player.startTime,frameCount)
          AddGameEvent(event)
          Left(killerId)
        } else {
          if (playerChange){
            changedPlayers+=(playerId2ByteMap(player.id) ->changedCells)
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
    //先把死亡的玩家清除
    playerMap = newPlayerMap.map {
      case Right(s) => (s.id, s)
      case Left(_) => ("", Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
    }.filterNot(_._1 == "").toMap

    //再把杀人的玩家kill + 1
    newPlayerMap.foreach {
      case Left(killId) =>
        val a = playerMap.getOrElse(killId, Player("", "", 0.toShort, 0, 0, cells = List(Cell(0L, 0, 0))))
        playerMap += (killId -> a.copy(kill = (a.kill + 1).toShort ))
      case Right(_) =>
    }

    if(p2pCrash){
      dispatch(subscriber)(UserCrash(changedPlayers, frameCount))
      val event = PlayerInfoChange(playerMap,frameCount)
      AddGameEvent(event)
    }
  }

  override def checkPlayerFoodCrash(): Unit = {
    val newPlayerMap = playerMap.values.map {
      player =>
        var eaten  = Map[Point, Short]()
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
                  eaten += (p->color)
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
              case m: Mass =>
                if (checkCollision(Point(cell.x, cell.y), Point(m.x, m.y), cell.radius, Mass2Radius(shotMass), coverRate)) {
                  if(m.id == player.id && m.speed>0){
                    //小球刚从玩家体内发射出，此时不吃小球
                  }else {
                    newMass = (newMass + shotMass).toShort
                    newRadius = Mass2Radius(newMass)
                    massList = massList.filterNot(l => l == m)
                    if(newProtected)
                      newProtected = false
                  }
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
    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
  }

  override def checkVirusMassCrash(): Unit = {
   var newVirus = Map.empty[Long,Virus]
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
        case m: Mass =>
          if (checkCollision(Point(v.x, v.y), Point(m.x, m.y), v.radius, Mass2Radius(shotMass), coverRate)) {
            val (mx,my)=normalization(m.targetX, m.targetY)
            var vx = (nx * newMass * newSpeed + mx * shotMass * m.speed * initVirusRatio ) /(newMass + shotMass)
            var vy = (ny * newMass * newSpeed + my * shotMass * m.speed * initVirusRatio) /(newMass + shotMass)
            newSpeed = sqrt(pow(vx,2)+ pow(vy,2)).toFloat + initVirusSpeed
            val degree =atan2(vy,vx)
            vx = newSpeed * cos(degree)
            vy = newSpeed * sin(degree)
            hasMoved =true
            newMass = (newMass + shotMass).toShort
            newRadius = Mass2Radius(newMass)
            newTargetX = vx.toShort
            newTargetY = vy.toShort
            massList = massList.filterNot(l => l == m)
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

  override def checkPlayerSplit(actMap: Map[String,KC], mouseActMap: Map[String, MP]): Unit = {
    var SplitPlayerMap = Map[Byte,Player]()
    val newPlayerMap = playerMap.values.map {
      player =>
        var isSplit = false
        var newSplitTime = player.lastSplit
        val mouseAct = mouseActMap.getOrElse(player.id,MP(playerId2ByteMap.get(player.id),player.targetX, player.targetY,0,0))
        val split = actMap.get(player.id) match {
          case Some(keyEvent) => keyEvent.kC==KeyEvent.VK_F
          case _ => false
        }
        var splitIncrement = 0
        val newCells = player.cells.sortBy(_.radius).reverse.flatMap {
          cell =>
            var newMass = cell.newmass
            var newRadius = cell.radius
            val target = Position( (mouseAct.cX + player.x - cell.x).toShort , (mouseAct.cY + player.y - cell.y).toShort )
            val deg = atan2(target.clientY, target.clientX)
            val degX = if (cos(deg).isNaN) 0 else cos(deg)
            val degY = if (sin(deg).isNaN) 0 else sin(deg)
            var splitX:Short = 0
            var splitY:Short = 0
            var splitMass:Short = 0
            var splitRadius:Short = 0
            var splitSpeed = 0.0
            var cellId = 0L
            if (split && cell.newmass > splitLimit && player.cells.size < maxCellNum-splitIncrement) {
              splitIncrement += 1
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
            /**效果：大球：缩小，小球：从0增大，且从大球中滑出**/
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
          if(playerId2ByteMap.get(player.id).isDefined)
            SplitPlayerMap += (playerId2ByteMap(player.id) -> newPlayer)
          newPlayer
        }else{
          player
        }

    }
    playerMap = newPlayerMap.map(s => (s.id, s)).toMap
    if(SplitPlayerMap.nonEmpty){
      val msg = PlayerSplit(SplitPlayerMap, frameCount)
      dispatch(subscriber)(msg)
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
    newFoods.foreach{
      case (p,mass) =>
        newFoodDetails ::= Food(mass, p.x, p.y)
    }
    playerMap = playerMap.map{
      item =>
        val  newcells  = item._2.cells.filterNot(_.newmass==0).map(cell => cell.copy(mass = cell.newmass))
        val newplayer = item._2.copy(cells = newcells)
        playerDetails ::= newplayer
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
      newFoodDetails,
      eatenFoodDetails
    )
  }

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
      massList.filter(m=>checkScreenRange(Point(currentPlayer._1,currentPlayer._2),Point(m.x,m.y),Mass2Radius(shotMass),width,height)),
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
    checkVictory()
    genWaitingStar()  //新增
    updateRanks()  //排名
  }

  def getApples = food

  def getNewApples = newFoods

  def cleanNewApple = {
    newFoods = Map.empty
  }

  override def clearAllData: Unit ={
    super.clearAllData
    newFoods = Map.empty
    eatenFoods = Map.empty
    playerId2ByteMap.clear()
    currentRank = List.empty[Score]
  }

  // 生成随机点
  def randomEmptyPoint(): Point = {
    Point(random.nextInt(boundary.x), random.nextInt(boundary.y))
  }

  def getSubscribersMap(subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]],botMap:mutable.HashMap[String,(String,ActorRef[BotActor.Command],Boolean)]) ={
    subscriber=subscribersMap
    botSubscriber=botMap
  }

  override def getActionEventMap(frame:Int): List[GameEvent] = {
    ActionEventMap.getOrElse(frame,List.empty)
  }

  override def getGameEventMap(frame: Int): List[Protocol.GameEvent] = {
    GameEventMap.getOrElse(frame,List.empty)
  }


}
