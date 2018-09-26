package com.neo.sk.gypsy.front.gypsyClient

import com.neo.sk.gypsy.front.scalajs.LoginPage
import org.scalajs.dom
import org.scalajs.dom.raw.Event

import scala.scalajs.js

/**
  * User: sky
  * Date: 2018/9/21
  * Time: 16:16
  */
object GypsyMain extends js.JSApp {
  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    dom.window.onload = {
      (_: Event) =>
        LoginPage.homePage()
    }
  }
}
