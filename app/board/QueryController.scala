package board

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._

trait QueryController {
  self: Controller =>

  val AcceptEventStream = Accepting("text/event-stream")

  def resourceQuery: ResourceQuery[JsValue, JsValue]

  def eventSource: EventSource

  def findOne(id: String) = Action.async { implicit request =>
    resourceQuery.findByUri(request.path).map { resource =>
      Ok(Json.toJson(resource))
    }
  }

  def findAll() = Action.async { implicit request =>
    render.async {
      case Accepts.Json() =>
        resourceQuery.findAll().map { resources =>
          Ok(Json.toJson(resources))
        }
      case AcceptEventStream() =>
        Future.successful(Ok.feed(
          eventSource
            .streamEvents()
            .through(Enumeratee.map { ee =>
              Domain.JsonFormat.eventEnvelopeWrites.writes(ee)
            })
            .through(play.api.libs.EventSource[JsValue]())
        ).as("text/event-stream"))
    }
  }

}