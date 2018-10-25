package com.neo.sk.gypsy.front

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import com.neo.sk.gypsy.front.pages.MainPage
/**
  * @author zhaoyin
  * @date 2018/10/24  下午1:43
  */

@JSExportTopLevel("front.Main")
object Main {

  def main(args: Array[String]): Unit ={
    run()
  }

  @JSExport
  def run(): Unit = {
    MainPage.show()
  }
}
