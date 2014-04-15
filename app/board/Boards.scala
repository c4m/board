package board

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._

trait Boards {

  def eventSource: EventSource

  def start(): Future[Unit]

}

object Boards {

  def apply(parentEventSource: EventSource, collection: => JSONCollection): Boards = new Boards {

    lazy val eventSource = EventSource(parentEventSource, translateEvents)

    def start() : Future[Unit] = {
      ViewHandler(eventSource, collection, snapshotEvents).start()
    }

    val translateEvents: PartialFunction[Domain.EventEnvelope, Future[List[Domain.EventEnvelope]]] = {
      case ee@Domain.EventEnvelope(_, _, Domain.Board.Create(_, _)) =>
        Future.successful(List(ee))
      case ee@Domain.EventEnvelope(id, author, Domain.Column.Create(columnUri, projectUri, _)) =>
        Future.successful(List(ee.copy(body = Domain.Board.ColumnAdded(projectUri, columnUri))))
    }

    val snapshotEvents: PartialFunction[(JSONCollection, Domain.EventEnvelope), Future[Unit]] = {
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Board.Create(uri, name))) => collection.insert(Json.obj(
        "_id" -> uri,
        "_author" -> author,
        "_lastUpdateByEvent" -> id,
        "name" -> name
      )).map(_ => ())
      case (collection, ee@Domain.EventEnvelope(id, author, Domain.Board.ColumnAdded(projectUri, uri))) => collection.update(Json.obj(
        "_id" -> projectUri
      ), Json.obj(
        "$set" -> Json.obj(
          "_lastUpdateByEvent" -> id
        ),
        "$addToSet" -> Json.obj(
          "columns" -> uri
        )
      )).map(_ => ())
    }

  }

}