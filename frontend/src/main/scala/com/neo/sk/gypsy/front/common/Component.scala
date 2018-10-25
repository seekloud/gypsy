package com.neo.sk.gypsy.front.common


import scala.language.implicitConversions
import scala.xml.Elem
/**
  * @author zhaoyin
  * @date 2018/10/24  下午2:04
  */
trait Component {

  def render: Elem

}

object Component {
  implicit def component2Element(comp: Component): Elem = comp.render
}