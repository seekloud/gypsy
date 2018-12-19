package com.neo.sk.gypsy.front.utils


import io.circe.Json

import scala.scalajs.js
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js.annotation.{JSGlobal, ScalaJSDefined}

/**
  * @author zhaoyin
  * 2018/12/14  2:32 PM
  */

object EchartsJs{


  @js.native
  trait Echart extends js.Object{
    def setOption(props:Json):Unit = js.native
  }

  @js.native
  @JSGlobal("echarts")
  object echarts extends js.Object{
    //todo 这里的类型不太对 EchartsJs.Echart
    def init(props:HTMLElement):EchartsJs.Echart = js.native
  }

}

