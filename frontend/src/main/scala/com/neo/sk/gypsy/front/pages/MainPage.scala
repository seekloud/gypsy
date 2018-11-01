package com.neo.sk.gypsy.front.pages

import com.neo.sk.gypsy.front.common.PageSwitcher
import mhtml._
import org.scalajs.dom
import scala.xml.Elem



/**
  * @author zhaoyin
  * @date 2018/10/24  下午1:48
  */
object MainPage extends PageSwitcher{

  private val currentPage: Rx[Elem] = currentHashVar.map{
    case "playGame" :: playerId :: playerName :: roomId :: accessCode :: Nil => new GamePage(playerId, playerName, roomId.toLong, accessCode).render
    case "watchGame" :: roomId :: accessCode :: Nil=> <div>WatchGame Page</div>//TODO new WatchGame().render
    case "watchGame" :: roomId :: playerId :: Nil => <div>WatchGame Page</div>//TODO new WatchGame().render
    case "watchRecord" :: recordId :: playerId :: frame :: accessCode :: Nil => new WatchRecord(recordId.toLong,playerId,frame.toInt,accessCode).render
    case x =>
      println(s"unknown hash: $x")
      <div>Error Page</div>
  }

  def show():Cancelable = {
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body,page)
  }
}