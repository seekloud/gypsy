package com.neo.sk.gypsy.front.utils

import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSGlobal, ScalaJSDefined}

object LayuiJs {

  @js.native
  trait wxData extends js.Object


  @ScalaJSDefined
  trait icon extends js.Object{
    val icon: js.UndefOr[Int] = js.undefined
    val time: js.UndefOr[Int] = js.undefined
    val anim: js.UndefOr[Int] = js.undefined
  }

  @ScalaJSDefined
  trait open extends js.Object{
    val `type` :js.UndefOr[Int] = js.undefined
    val title : js.UndefOr[Boolean]=js.undefined
    val closeBtn :js.UndefOr[Int] = js.undefined
    val area :js.UndefOr[js.Array[String]] = js.undefined
    val skin :js.UndefOr[String] = js.undefined
    //阴影
    val shade:js.UndefOr[Float]=js.undefined
    val id:js.UndefOr[String]=js.undefined
    //点击阴影关闭
    val shadeClose : js.UndefOr[Boolean]=js.undefined
    //是否允许浏览器出现滚动条
    val scrollbar:js.UndefOr[Boolean]=js.undefined
    //是否允许拉伸
    val resize:js.UndefOr[Boolean]=js.undefined
    val btn:js.UndefOr[js.Array[String]] = js.undefined
    val moveType:js.UndefOr[Int] = js.undefined
    val content : js.UndefOr[HTMLElement]=js.undefined
  }

  @ScalaJSDefined
  trait photos extends js.Object{
    val photos:js.UndefOr[String]=js.undefined
    val anim:js.UndefOr[Int]=js.undefined  //图片出现动画
  }
  @ScalaJSDefined
  trait fixbar extends js.Object{
    val bar1: js.UndefOr[Boolean]=js.undefined
    val showHeight:js.UndefOr[Int] = js.undefined
    def click: js.UndefOr[js.Function0[Any]] = js.undefined
  }
  @ScalaJSDefined
  trait ready extends js.Object{
    def ready:js.UndefOr[js.Function0[Any]] = js.undefined
  }

  @ScalaJSDefined
  trait prompt extends js.Object{
    val title:js.UndefOr[String] = js.undefined
    val formType:js.UndefOr[Int]=js.undefined
  }

  @ScalaJSDefined
  trait shareMessage extends js.Object{
    val title: js.UndefOr[String] // 分享标题
    val desc: js.UndefOr[String] = js.undefined // 分享描述
    val link: js.UndefOr[String] // 分享链接
    val imgUrl: js.UndefOr[String] = js.undefined  // 分享图标
    val `type`:js.UndefOr[String] = js.undefined   // 分享类型,music、video或link，不填默认为link
    val dataUrl:js.UndefOr[String] =  js.undefined // 如果type是music或video，则要提供数据链接，默认为空
    def success: js.UndefOr[js.Function0[Any]] = js.undefined
    def cancel: js.UndefOr[js.Function0[Any]] = js.undefined// 用户取消分享后执行的回调函数

  }

  @ScalaJSDefined
  trait shareLine extends js.Object{
    val title:js.UndefOr[String] =  js.undefined// 分享标题
    val link: js.UndefOr[String]=  js.undefined // 分享链接
    val imgUrl: js.UndefOr[String] =  js.undefined // 分享图标
    def success: js.UndefOr[js.Function0[Any]] = js.undefined
    def cancel: js.UndefOr[js.Function0[Any]] = js.undefined// 用户取消分享后执行的回调函数
  }



  @js.native
  @JSGlobal("layer")
  object layer extends js.Object{
    def msg(msg:js.UndefOr[String],props:icon,func:js.Function0[Any]):Unit=js.native
    def open(props:open):js.UndefOr[Any]=js.native
    def photos(props:photos): Unit =js.native
    def ready(props:ready):Unit=js.native
    def prompt(props:prompt,func:js.Function2[Any,Any,Any]):Unit=js.native
    def close(index:js.UndefOr[Any]):Unit=js.native
    def closeAll():Unit=js.native
  }


  @js.native
  @JSGlobal("util")
  object util extends js.Object{
    def fixbar(props:fixbar): Unit =js.native
  }

  def msg(msg:String="",icon1:Int= -1,time1:Int=3000,anim1:Int=0,func:js.Function0[Any]=()=>()): Unit ={
    layer.msg(msg,new icon{
      override val icon: UndefOr[Int] = icon1
      override val time: UndefOr[Int] = time1
      override val anim: UndefOr[Int] = anim1
    },func)
  }

  def photos(photos1:String,anim1:Int= -1): Unit ={
    layer.photos(new photos {
      override val photos: UndefOr[String] = photos1
      override val anim: UndefOr[Int] = anim1
    })
  }
}