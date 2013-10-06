package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json.JsValue

import models.bgcolor.BackgroundColor

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def bgcolor = Action { implicit request =>
    Ok(views.html.bgcolor())
  }

  def bgcolorSocket = WebSocket.async[JsValue] { request =>
    BackgroundColor.join
  }
}
