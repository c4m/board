package board

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._

trait Cards {

  def eventSource: EventSource

  def start(): Future[Unit]

}

object Cards {

  def apply(parentEventSource: EventSource, collection: => JSONCollection): Cards = new Cards {

    lazy val eventSource = EventSource(parentEventSource, translateEvents)

    def start() : Future[Unit] = {
      ViewHandler(eventSource, collection, snapshotEvents).start()
    }

    val translateEvents: PartialFunction[Domain.EventEnvelope, Future[List[Domain.EventEnvelope]]] = {
      case ee@Domain.EventEnvelope(_, _, event:Domain.Card.Create) =>
        Future.successful(List(ee))
      case ee@Domain.EventEnvelope(_, _, event:Domain.Card.Move) =>
        Future.successful(List(ee))
    }

    val snapshotEvents: PartialFunction[(JSONCollection, Domain.EventEnvelope), Future[Unit]] = {
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Card.Create(uri, projectUri, columnUri, name, description))) =>
        collection.insert(Json.obj(
          "_id" -> uri,
          "_author" -> author,
          "_lastUpdateByEvent" -> id,
          "projectUri" -> projectUri,
          "columnUri" -> columnUri,
          "name" -> name,
          "description" -> description
        )).map(_ => ())
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Card.Move(cardUri, columnUri))) => collection.update(Json.obj(
        "_id" -> cardUri
      ), Json.obj(
        "$set" -> Json.obj(
          "_lastUpdateByEvent" -> id,
          "columnUri" -> columnUri
        )
      )).map(_ => ())
    }

  }

}