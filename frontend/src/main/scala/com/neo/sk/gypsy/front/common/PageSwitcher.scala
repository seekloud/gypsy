package com.neo.sk.gypsy.front.common

import mhtml.{Var, mount}
import org.scalajs.dom
import org.scalajs.dom.HashChangeEvent
/**
  * @author zhaoyin
  * @date 2018/10/24  下午1:50
  */
trait PageSwitcher {

  import scalatags.JsDom.short._

  protected var currentPageName: Var[String] = Var("首页")

  private val bodyContent = div(*.height := "100%").render

  def getCurrentHash: String = dom.window.location.hash


  private[this] var internalTargetHash = ""


  //init.
  {

    val func = {
      e: HashChangeEvent =>
        //only handler browser history hash changed.
        if (internalTargetHash != getCurrentHash) {
          println(s"hash changed, new hash: $getCurrentHash")
          switchPageByHash()
        }
    }
    dom.window.addEventListener("hashchange", func, useCapture = false)
  }


  protected def switchToPage(pageName: String): Unit = {
    currentPageName.update(_ => pageName)
  }

  def getCurrentPageName: Var[String] = currentPageName

  def switchPageByHash(): Unit
}
