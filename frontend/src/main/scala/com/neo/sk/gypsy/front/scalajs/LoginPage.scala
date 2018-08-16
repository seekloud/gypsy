package com.neo.sk.gypsy.front.scalajs

import com.neo.sk.gypsy.front.common.Routes.UserRoute
import com.neo.sk.gypsy.front.scalajs.NetGameHolder.{joinGame,isDead}
import com.neo.sk.gypsy.front.utils.{Http, LayuiJs}
import com.neo.sk.gypsy.front.utils.LayuiJs.layer
import com.neo.sk.gypsy.shared.ptcl.Protocol.MousePosition
import com.neo.sk.gypsy.shared.ptcl._
import com.neo.sk.gypsy.shared.ptcl.UserProtocol.{UserLoginInfo, UserLoginRsq, UserRegisterInfo}
import org.scalajs.dom
import org.scalajs.dom.html._
import org.scalajs.dom.raw._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import org.scalajs.dom.ext.KeyCode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scalatags.JsDom.all._
import scalatags.JsDom.short.*

object LoginPage {

  private def guestModel = LayuiJs.layer.open(new LayuiJs.open {
    override val `type`: UndefOr[Int] = 1
    override val title: UndefOr[Boolean] = false
    override val closeBtn: UndefOr[Int] = 0
    override val area: UndefOr[js.Array[String]] = js.Array("500px", "500px")
    override val shade: UndefOr[Float] = 0.8f
    //        override val btn: UndefOr[js.Array[String]] = js.Array("登陆","注册")
    override val id: UndefOr[String] = "guest-login"
    override val moveType: UndefOr[Int] = 1
    override val resize: UndefOr[Boolean] = false
    override val scrollbar: UndefOr[Boolean] = false
    override val content: UndefOr[HTMLElement] = div(
        //br(),
        div(style := "position:center;margin:50px 146px",
          h1(*.id := "BigTitle", style := "font-size:30px", "欢迎来到Gypsy")
        ),
        //br(),
        div(`class` := "nameInput",
          label(*.`for` := "name", style := "margin-left:130px", s"Name:"),
          input(*.id := "guestName", *.`type` := "text", placeholder := "请输入用户名", maxlength := "25"),
          div(*.id := "room",
            select(*.id := "roomId",name := "home",
              option(value := "", "房间"),
              optgroup(attr("label") := "普通模式",
                option(value := "11", "房间1"),
                option(value := "12", "房间2"),
                option(value := "21", "房间3"),
                option(value := "22", "房间4")),
              optgroup(attr("label") := "限时模式",
                option(value := "1", attr("disabled") := "", "房间1"),
                option(value := "2", attr("disabled") := "", "房间2")),
            )
          )
        ),
        br(),
        button(*.id := "guestLogin", `class` := "layui-btn layui-btn-warm",  style := "margin-top:30px;margin-left:210px",
          i( `class`:="layui-icon layui-icon-fire"), "游客登录"),
        br(),
        button(*.id := "userLogin", `class` := "layui-btn", style := "margin-top:30px;margin-left:210px",
          i( `class`:="layui-icon layui-icon-user"), "用户登陆"),
        br(),
        button(*.id := "userRegister", `class` := "layui-btn layui-btn-normal ", style := "margin-top:30px;margin-left:210px",
          i( `class`:="layui-icon layui-icon-tree"),"用户注册"),
        p(style := "margin-top:30px;margin-left:120px","小tips:F键分裂 E键吐出小球 Esc建呼出主菜单!")

    ).toString().asInstanceOf[HTMLElement]
  })

  private def loginModel(captchaImgPath:String) = LayuiJs.layer.open(new LayuiJs.open {
    override val `type`: UndefOr[Int] = 1
    override val title: UndefOr[Boolean] = false
    override val closeBtn: UndefOr[Int] = 0
    override val area: UndefOr[js.Array[String]] = js.Array("500px", "500px")
    override val shade: UndefOr[Float] = 0.8f
    override val id: UndefOr[String] = "user-Login"
    override val moveType: UndefOr[Int] = 1
    override val resize: UndefOr[Boolean] = false
    override val scrollbar: UndefOr[Boolean] = false
    override val content: UndefOr[HTMLElement] = div(
      `class` := "login-main",
      div(`class` := "user-login-box user-login-header",
        h2(style := "margin-bottom:10px;font-weight:300;font-size:30px;color:#000", "GypsyLogin")),
      div(`class` := "user-login-box user-login-body layui-form",
        div(`class` := "layui-form-item",
          label(`class` := "user-login-icon layui-icon layui-icon-username", `for` := "user-login-username"),
          input(*.`type` := "text", name := "username", *.id := "user-login-username", attr("lay-verify") := "required", placeholder := "用户名", `class` := "layui-input")),
        div(`class` := "layui-form-item",
          label(`class` := "user-login-icon layui-icon layui-icon-password", `for` := "user-login-password"),
          input(*.`type` := "password", name := "password", *.id := "user-login-password", attr("lay-verify") := "required", placeholder := "密码", `class` := "layui-input")),
        div(`class` := "layui-form-item",
          div(`class` := "row",
            div(`class` := "layui-col-xs7",
              label(`class` := "user-login-icon layui-icon layui-icon-vercode", `for` := "user-login-vercode"),
              input(*.`type` := "text", name := "vercode", *.id := "user-login-vercode", attr("lay-verify") := "required", placeholder := "图形验证码", `class` := "layui-input")),
            div(`class` := "layui-col-xs5",
              div(style := "margin-left: 10px;",
                img(src := captchaImgPath, `class` := "user-login-codeimg", *.id := "user-get-vercode"))
            )
          )
        ),
        div(`class` := "layui-form-item", style := "margin-bottom: 20px;",
          input(*.`type` := "checkbox", name := "remember", attr("lay-skin") := "primary", *.title := "记住密码"),
          div(`class` := "layui-unselect layui-form-checkbox layui-form-checked", attr("lay-skin") := "primary",
            span("记住密码"),
            i(`class` := "layui-icon layui-icon-ok")),
          a(`class` := "user-jump-change register-link", *.id := "login2register", style := "margin-top: 7px;", "注册账号")
        ),
        div(`class` := "layui-form-item",
          button(`class` := "layui-btn layui-btn-fluid", *.id := "login", "登陆")),
        div(`class` := "layui-form-item login-back",
          //i(`class`:="layui-icon layui-icon-return",style:="color:#1E9FFF;"),
          a(*.id := "back2guest", style := "color:#1E9FFF;", "返回登陆")
        )
      )
    ).toString().asInstanceOf[HTMLElement]
  })

  private def registerModel = LayuiJs.layer.open(new LayuiJs.open {
    override val `type`: UndefOr[Int] = 1
    override val title: UndefOr[Boolean] = false
    override val closeBtn: UndefOr[Int] = 0
    override val area: UndefOr[js.Array[String]] = js.Array("500px", "500px")
    override val shade: UndefOr[Float] = 0.8f
    override val id: UndefOr[String] = "user-Register"
    override val moveType: UndefOr[Int] = 1
    override val resize: UndefOr[Boolean] = false
    override val scrollbar: UndefOr[Boolean] = false
    override val content: UndefOr[HTMLElement] = div(
      `class` := "login-main",
      div(`class` := "user-login-box user-login-header",
        h2(style := "margin-bottom:10px;font-weight:300;font-size:30px;color:#000", "GypsyRegister")),
      div(`class` := "user-login-box user-login-body layui-form",
        div(`class` := "layui-form-item",
          label(`class` := "user-login-icon layui-icon layui-icon-username", `for` := "user-register-username"),
          input(*.`type` := "text", name := "username", *.id := "user-register-username", attr("lay-verify") := "required", placeholder := "昵称", `class` := "layui-input")),
        div(`class` := "layui-form-item",
          label(`class` := "user-login-icon layui-icon layui-icon-password", `for` := "user-register-password"),
          input(*.`type` := "password", name := "password", *.id := "user-register-password", attr("lay-verify") := "required", placeholder := "密码", `class` := "layui-input")),
        div(`class` := "layui-form-item",
          label(`class` := "user-login-icon layui-icon layui-icon-password", `for` := "user-register-password-check"),
          input(*.`type` := "password", name := "password-check", *.id := "user-register-password-check", attr("lay-verify") := "required", placeholder := "确认密码", `class` := "layui-input")),
        div(`class` := "layui-form-item", style := "margin-bottom: 20px;",
          input(*.`type` := "checkbox", name := "remember", attr("lay-skin") := "primary", *.title := "同意用户协议"),
          div(`class` := "layui-unselect layui-form-checkbox layui-form-checked", attr("lay-skin") := "primary",
            span("同意用户协议"),
            i(`class` := "layui-icon layui-icon-ok")),
          a(`class` := "user-jump-change register-link", *.id := "register2login", style := "margin-top: 7px;", "用已有账号登入")
        ),
        div(`class` := "layui-form-item",
          button(`class` := "layui-btn layui-btn-fluid", *.id := "register", "注册")),
        div(`class` := "layui-form-item login-back",
          a(*.id := "rback2guest", style := "color:#1E9FFF;", "返回登陆")
        )
      )

    ).toString().asInstanceOf[HTMLElement]

  })

  def homePage(): Unit = {

    val guestIndex = guestModel
    val nameField: Input = dom.document.getElementById("guestName").asInstanceOf[HTMLInputElement]
    val loginButton: Button = dom.document.getElementById("guestLogin").asInstanceOf[HTMLButtonElement]
    val userLoginButton: Button = dom.document.getElementById("userLogin").asInstanceOf[HTMLButtonElement]
    val userRegisterButton: Button = dom.document.getElementById("userRegister").asInstanceOf[HTMLButtonElement]
    val roomId:Input = dom.document.getElementById("roomId").asInstanceOf[HTMLInputElement]

    nameField.focus()
    loginButton.onclick = {
      (_: MouseEvent) =>
        if (nameField.value.trim.isEmpty) {
          LayuiJs.msg("用户名不能为空!", 0, 2000, 6)
        } else {
          //修改参数一为房间编号（简单版中为：11,12,21,22）
          if(roomId.value !=""){
            joinGame(roomId.value,nameField.value)
            LayuiJs.layer.close(guestIndex)
          }
          else{
            LayuiJs.msg("请选择房间", 5, 2000)
          }
        }
    }

    nameField.onkeypress = {
      (event: KeyboardEvent) =>
        if (event.keyCode == 13) {
          loginButton.click()
          event.preventDefault()
        }
    }

    userLoginButton.onclick = {
      (_: MouseEvent) =>
        val form:FormData =new FormData()
        form.append("showapi_appid","70696")
        form.append("showapi_sign","a2d029e3526f43cebc4321b60697054f")
        form.append("textproducer_char_string","0123456789qwertyuiopasdfghjklzxcvbnm")
        form.append("border","no")
        form.append("image_height","80")
        Http.postFormAndParse[Captcha](" http://route.showapi.com/26-4",form).map{
          case Right(rspc)=>
            if(rspc.showapi_res_code==0){
              val loginIndex = loginModel(rspc.showapi_res_body.img_path)
              LayuiJs.layer.close(guestIndex)
              val userName: Input = dom.document.getElementById("user-login-username").asInstanceOf[HTMLInputElement]
              val userPassword: Input = dom.document.getElementById("user-login-password").asInstanceOf[HTMLInputElement]
              val vercode: Input = dom.document.getElementById("user-login-vercode").asInstanceOf[HTMLInputElement]
              val userLoginButton: Button = dom.document.getElementById("login").asInstanceOf[HTMLButtonElement]
              val login2register = dom.document.getElementById("login2register").asInstanceOf[HTMLButtonElement]
              val back2guest = dom.document.getElementById("back2guest").asInstanceOf[HTMLButtonElement]

              userName.focus()
              userLoginButton.onclick = {
                (_: MouseEvent) =>
                  if (userName.value.trim.isEmpty) {
                    LayuiJs.msg("用户名不能为空!", 0, 2000, 6)
                  } else if (userPassword.value.trim.isEmpty) {
                    LayuiJs.msg("密码不能为空!", 0, 2000, 6)
                  } else if (vercode.value.trim.isEmpty||vercode.value!=rspc.showapi_res_body.text) {
                    LayuiJs.msg("验证码错误!", 0, 2000, 6)
                  } else {
                    val bodyStr = UserLoginInfo(userName.value, userPassword.value).asJson.noSpaces
                    Http.postJsonAndParse[UserLoginRsq](UserRoute.userLogin, bodyStr).map {
                      case Right(rsp) =>
                        if (rsp.errCode != 0) {
                          println(s"name or password error in login ${rsp.errCode} ")
                          LayuiJs.msg(rsp.msg, 5, 2000)
                        } else {
                          joinGame("11",userName.value, 1,rsp.data.get.score)
                          LayuiJs.layer.close(loginIndex)
                        }
                      case Left(e) =>
                        println(s"parse error in login $e ")
                        LayuiJs.msg(e.toString, 5, 2000)
                    }
                  }
              }

              back2guest.onclick = {
                (_: MouseEvent) =>
                  homePage()
                  LayuiJs.layer.close(loginIndex)
              }

              login2register.onclick={
                (_: MouseEvent) =>
                  userRegisterButton.click()
                  LayuiJs.layer.close(loginIndex)
              }

            }else{
              println(s"${rspc.showapi_res_error} ")
              val loginIndex = loginModel("https://www.oschina.net/action/user/captcha")
              LayuiJs.layer.close(guestIndex)
              val userName: Input = dom.document.getElementById("user-login-username").asInstanceOf[HTMLInputElement]
              val userPassword: Input = dom.document.getElementById("user-login-password").asInstanceOf[HTMLInputElement]
              val vercode: Input = dom.document.getElementById("user-login-vercode").asInstanceOf[HTMLInputElement]
              val userLoginButton: Button = dom.document.getElementById("login").asInstanceOf[HTMLButtonElement]
              val login2register = dom.document.getElementById("login2register").asInstanceOf[HTMLButtonElement]
              val back2guest = dom.document.getElementById("back2guest").asInstanceOf[HTMLButtonElement]
              userName.focus()
              userLoginButton.onclick = {
                (_: MouseEvent) =>
                  if (userName.value.trim.isEmpty) {
                    LayuiJs.msg("用户名不能为空!", 0, 2000, 6)
                  } else if (userPassword.value.trim.isEmpty) {
                    LayuiJs.msg("密码不能为空!", 0, 2000, 6)
                  } else if (vercode.value.trim.isEmpty) {
                    LayuiJs.msg("验证码错误!", 0, 2000, 6)
                  } else {
                    val bodyStr = UserLoginInfo(userName.value, userPassword.value).asJson.noSpaces
                    Http.postJsonAndParse[UserLoginRsq](UserRoute.userLogin, bodyStr).map {
                      case Right(rsp) =>
                        if (rsp.errCode != 0) {
                          println(s"name or password error in login ${rsp.errCode} ")
                          LayuiJs.msg(rsp.msg, 5, 2000)
                        } else {
                          joinGame("11",userName.value, 1,rsp.data.get.score)
                          LayuiJs.layer.close(loginIndex)
                        }
                      case Left(e) =>
                        println(s"parse error in login $e ")
                        LayuiJs.msg(e.toString, 5, 2000)
                    }
                  }
              }

              back2guest.onclick = {
                (_: MouseEvent) =>
                  homePage()
                  LayuiJs.layer.close(loginIndex)
              }

              login2register.onclick={
                (_: MouseEvent) =>
                  userRegisterButton.click()
                  LayuiJs.layer.close(loginIndex)
              }

            }
          case Left(e)=>
            println(s"parse error in login $e ")
            LayuiJs.msg(e.toString, 5, 2000)

        }
    }

    userRegisterButton.onclick = {
      (_: MouseEvent) =>
        val registerIndex = registerModel
        LayuiJs.layer.close(guestIndex)

        val userName = dom.document.getElementById("user-register-username").asInstanceOf[HTMLInputElement]
        val password= dom.document.getElementById("user-register-password").asInstanceOf[HTMLInputElement]
        val passwordCheck=dom.document.getElementById("user-register-password-check").asInstanceOf[HTMLInputElement]
        val registerButton=dom.document.getElementById("register").asInstanceOf[HTMLButtonElement]
        val back2guest = dom.document.getElementById("rback2guest").asInstanceOf[HTMLButtonElement]
        val register2login = dom.document.getElementById("register2login").asInstanceOf[HTMLButtonElement]

        registerButton.onclick={
          (_: MouseEvent) =>
            if(userName.value.trim.isEmpty){
              LayuiJs.msg("用户名不能为空!", 0, 2000, 6)
            }else if(password.value.trim.isEmpty || passwordCheck.value.trim.isEmpty){
              LayuiJs.msg("密码不能为空!", 0, 2000, 6)
            }else if(password.value!=passwordCheck.value){
              LayuiJs.msg("两次密码不一致!", 0, 2000, 6)
            }else{
              val bodyStr = UserRegisterInfo(userName.value, password.value,"").asJson.noSpaces
              Http.postJsonAndParse[SuccessRsp](UserRoute.userRegister,bodyStr).map{
                case Right(rsp) =>
                  if (rsp.errCode != 0) {
                    println(s"register error${rsp.errCode} ")
                    LayuiJs.msg(rsp.msg, 5, 2000)
                  } else {
                    userLoginButton.click()
                    LayuiJs.layer.close(registerIndex)
                    LayuiJs.msg("注册成功!",6,2000)
                  }
                case Left(e) =>
                  println(s"parse error in login $e ")
                  LayuiJs.msg(e.toString, 5, 2000)
              }
            }
        }

        back2guest.onclick = {
          (_: MouseEvent) =>
            homePage()
            LayuiJs.layer.close(registerIndex)
        }
        register2login.onclick={
          (_: MouseEvent) =>
            userLoginButton.click()
            LayuiJs.layer.close(registerIndex)
        }

    }


  }
}
