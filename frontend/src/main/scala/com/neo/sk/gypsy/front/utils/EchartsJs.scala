package com.neo.sk.gypsy.front.utils


import io.circe.Json
import scala.scalajs.js
import org.scalajs.dom.raw.HTMLElement

/**
  * @author zhaoyin
  * 2018/12/14  2:32 PM
  */

object EchartsJs extends js.Object {


  @js.native
  trait Echart extends js.Object{
    def setOption(props:Json):Unit = js.native
  }

  @js.native
  object echarts extends js.Object{
    def init(props:HTMLElement):EchartsJs.Echart = js.native
  }



}

