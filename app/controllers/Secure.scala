package controllers

import scala.collection.JavaConverters._
import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play
import Play.current

object Secure {
  case class AuthorizedRequest[A](user: String, request: Request[A]) extends WrappedRequest(request)

  object Authorized extends ActionBuilder[AuthorizedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthorizedRequest[A]) => Future[SimpleResult]) = {
      request.session.get("user").map { user =>
        block(AuthorizedRequest(user, request))
      }.getOrElse(Future(Results.Redirect(routes.Authentication.authenticate)))
    }
  }
}