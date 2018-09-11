package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.front.scalajs.NetGameHolder._
import com.neo.sk.gypsy.front.utils.{Http, LayuiJs}
import com.neo.sk.gypsy.front.utils.LayuiJs.layer
import com.neo.sk.gypsy.shared.ptcl.WsMsgProtocol.{MousePosition, UserLeft}
import com.neo.sk.gypsy.shared.ptcl.{Captcha, Point, WsMsgProtocol, SuccessRsp}
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.{UserLoginInfo, UserLoginRsq, UserMaxScore, UserRegisterInfo}
import org.scalajs.dom
import org.scalajs.dom.html._
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import org.scalajs.dom.ext.KeyCode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scalatags.JsDom.all._
import scalatags.JsDom.short.*

object DeadPage {

  def deadModel(id:Long,killerName:String,killNum:Int,score:Int,survivalTime:Long,maxScore:Int,gameStream:WebSocket)={
    isDead=true
    LayuiJs.layer.open(new LayuiJs.open {
      override val `type`: UndefOr[Int] = 1
      override val title: UndefOr[Boolean] = false
      override val closeBtn: UndefOr[Int] = 0
      override val area: UndefOr[js.Array[String]] = js.Array("500px", "500px")
      override val shade: UndefOr[Float] = 0.2f
      override val id: UndefOr[String] = "user-Dead"
      override val btn: UndefOr[js.Array[String]] = js.Array("重新开始","退出游戏")
      override val btnAlign: UndefOr[String] = "c"
      override val moveType: UndefOr[Int] = 1
      override val resize: UndefOr[Boolean] = false
      override val scrollbar: UndefOr[Boolean] = false
      override def yes() = {
        println(KeyCode.Space.toString)
        isDead=false
        sendMsg(WsMsgProtocol.KeyCode(myId,KeyCode.Space,grid.frameCount,getActionSerialNum),gameStream)
        layer.closeAll()
      } .asInstanceOf[js.Function0[Any]]

      override def btn2(): UndefOr[js.Function0[Any]] = {
        sendMsg(UserLeft,gameStream)
        gameStream.close()
        wsSetup=false
        layer.closeAll()
        LoginPage.homePage()
      }.asInstanceOf[js.Function0[ Any]]
      override val content: UndefOr[HTMLElement] = div(
        `class`:="dead-main",
        div(`class`:="user-login-box user-login-header",
          h2(style := "margin-bottom:10px;font-weight:300;font-size:30px;color:#fff", "YouDead!!")),
        div(`class` := "user-login-box user-login-body layui-form",
          div( *.id:="user-score",
            span(*.id:="statsText",`class`:="user-food-score",s"分数:$score"),
           // span(*.id:="statsSubtext","分数")
            ),
          div(*.id:="user-max-score",
            span(*.id:="statsText",`class`:="user-highest-score",s"历史最高分:$maxScore"),
           // span(*.id:="statsSubtext","历史最高分")
            ),
          div(*.id:="user-kill-num",
            span(*.id:="statsText",`class`:="user-kill",s"干掉小球数:$killNum"),
            //span(*.id:="statsSubtext","干掉小球数")
            ),
          div(*.id:="user-killerName",
            span(*.id:="statsText",`class`:="user-killer-name",s"凶手:$killerName"),
           // span(*.id:="statsSubtext","凶手")
            ),
          div(*.id:="user-survival-time",
            span(*.id:="statsText",`class`:="user-live-time",s"存活时间:${survivalTime/1000/60}min${survivalTime/1000%60}sec"),
            //span(*.id:="statsSubtext","存活时间")
            ))
      ).toString().asInstanceOf[HTMLElement]

      //override def btn2: UndefOr[js.Function2[Any, Any, Any]] =
    })
    if(id<1000000&&score>maxScore){
      val bodyStr=UserMaxScore(id,score).asJson.noSpaces
      Http.postJsonAndParse[SuccessRsp](UserRoute.updateUserScore,bodyStr).map{
        case Left(e) =>
          println(s"parse error in login $e ")
          LayuiJs.msg(e.toString, 5, 2000)
        case Right(rsp)=>
          if(rsp.errCode!=0){
            LayuiJs.msg(rsp.msg, 5, 2000)
          }
      }
    }

  }


}
