package board

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._

trait Columns {

  def eventSource: EventSource

  def start(): Future[Unit]

}

object Columns {

  def apply(parentEventSource: EventSource, collection: => JSONCollection): Columns = new Columns {

    lazy val eventSource = EventSource(parentEventSource, translateEvents)

    def start() : Future[Unit] = {
      ViewHandler(eventSource, collection, snapshotEvents).start()
    }

    def findForCard(cardUri: String): Future[JsValue] = collection.find(Json.obj(
      "cards" -> cardUri
    )).cursor[JsValue].headOption.map {
      case Some(result) => result
    }

    val translateEvents: PartialFunction[Domain.EventEnvelope, Future[List[Domain.EventEnvelope]]] = {
      case ee@Domain.EventEnvelope(_, _, event:Domain.Column.Create) =>
        Future.successful(List(ee))
      case ee@Domain.EventEnvelope(_, _, event@Domain.Card.Create(cardUri, _, columnUri, _, _)) =>
        Future.successful(List(ee.copy(body=Domain.Column.CardAdded(columnUri, cardUri))))
      case ee@Domain.EventEnvelope(_, _, event@Domain.Card.Move(cardUri, columnUri)) =>
        val eventuallyOldColumn = findForCard(cardUri)
        val eventuallyOldColumnUri = eventuallyOldColumn.map { column =>
          (column \ "_id").as[String]
        }
        // inconsistent, even if the API allows you to return a list,
        // since both events will have the same id, it will break sequential consistency
        // don't use in real world, use different ids
        val eventuallyEvents = eventuallyOldColumnUri.map { oldColumnUri =>
          List(
            ee.copy(body = Domain.Column.CardRemoved(oldColumnUri, cardUri)),
            ee.copy(body = Domain.Column.CardAdded(columnUri, cardUri))
          )
        }
        eventuallyEvents
    }

    val snapshotEvents: PartialFunction[(JSONCollection, Domain.EventEnvelope), Future[Unit]] = {
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Column.Create(uri, projectUri, name))) => collection.insert(Json.obj(
        "_id" -> uri,
        "_author" -> author,
        "_lastUpdateByEvent" -> id,
        "projectUri" -> projectUri,
        "name" -> name
      )).map(_ => ())
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Column.CardAdded(columnUri, cardUri))) => collection.update(Json.obj(
        "_id" -> columnUri
      ), Json.obj(
        "$set" -> Json.obj(
          "_lastUpdateByEvent" -> id
        ),
        "$addToSet" -> Json.obj(
          "cards" -> cardUri
        )
      )).map(_ => ())
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Column.CardRemoved(columnUri, cardUri))) => collection.update(Json.obj(
        "_id" -> columnUri
      ), Json.obj(
        "$set" -> Json.obj(
          "_lastUpdateByEvent" -> id
        ),
        "$pull" -> Json.obj(
          "cards" -> cardUri
        )
      )).map(_ => ())
    }

  }

}