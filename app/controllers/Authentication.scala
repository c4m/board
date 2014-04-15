package controllers

import scala.util.control.NonFatal

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.openid._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._

object Authentication extends Controller {

  val OPENID =  "https://www.google.com/accounts/o8/site-xrds?hd="+Play.configuration.getString("openid.domain").get


  def authenticate = Action.async { implicit request =>
      OpenID.redirectURL(
        OPENID,
        routes.Authentication.openIDCallback.absoluteURL(request.headers.get("X-Forwarded-Proto") == Some("https")),
        List(
          ("email", "http://axschema.org/contact/email"),
          ("firstname", "http://axschema.org/namePerson/first"),
          ("lastname", "http://axschema.org/namePerson/last")
        )
      ).map { url =>
        Redirect(url).flashing(
          "uri" -> request.flash.get("uri").getOrElse("/")
        )
      }.recover {
        case NonFatal(e) =>
          Logger.error("error", e)
          Forbidden
      }
  }

  def openIDCallback = Action { implicit request =>
    val info = UserInfo(request.queryString)
    val email = info.attributes("email")

    Redirect(request.flash.get("uri").getOrElse("/")).withSession(
      "user" -> info.attributes("email"),
      "firstname" -> info.attributes("firstname"),
      "lastname" -> info.attributes("lastname")
    )
  }

}