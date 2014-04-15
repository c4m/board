package controllers

import play.api._
import play.api.mvc._
import Secure._

object Application extends Controller {
  
  def index = Authorized {
    Ok(views.html.index())
  }
  
}