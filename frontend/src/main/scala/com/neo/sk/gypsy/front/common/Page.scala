package com.neo.sk.gypsy.front.common

import scala.language.implicitConversions
import scala.xml.Elem
/**
  * @author zhaoyin
  * @date 2018/10/24  下午2:03
  */
trait Page extends Component {

  def render: Elem

}

object Page{
  implicit def page2Element(page: Page): Elem = page.render
}
