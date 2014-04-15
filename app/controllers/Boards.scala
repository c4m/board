package controllers

import scala.concurrent.Future

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.EventSource

import Secure._

object Boards extends Controller with board.QueryController {
  
  lazy val resourceQuery = board.System.query.boards

  lazy val eventSource = board.System.boards.eventSource

  def create = Authorized.async(board.BodyParsers.postEvent(board.Domain.JsonFormat.boardCreateFormat)) { implicit request =>
    board.System.command(
      board.Domain.EventEnvelope(None, request.user, request.body)
    ).map { eventEnvelope =>
      Ok(board.Domain.JsonFormat.eventEnvelopeWrites.writes(eventEnvelope))
    }
  }

}