package board

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._

import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import play.modules.reactivemongo.json.collection.JSONCollection

trait Journal {

  def write(eventEnvelope: Domain.EventEnvelope): Future[Domain.EventEnvelope]

  def getEventsSince(lastId: BSONObjectID): Enumerator[Domain.EventEnvelope]

}

object Journal {

  def apply(collection: => JSONCollection): Journal = new Journal {

    def write(eventEnvelope: Domain.EventEnvelope): Future[Domain.EventEnvelope] = {
      val eventEnvelopeWithId = eventEnvelope.copy(id = Some(BSONObjectID.generate))
      collection.insert(Domain.JsonFormat.eventEnvelopeWrites.writes(eventEnvelopeWithId)).map { _ =>
        eventEnvelopeWithId
      }
    }

    def getEventsSince(lastId: BSONObjectID): Enumerator[Domain.EventEnvelope] = {
      collection.find(Json.obj(
        "_id" -> Json.obj(
          "$gt" -> lastId
        )
      )).sort(Json.obj(
        "_id" -> 1
      )).cursor[JsValue].enumerate().map { json =>
        Domain.JsonFormat.eventEnveloppeReads.reads(json).get
      }
    }

  }

}