package board

import scala.concurrent.Future

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._

import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._

trait ViewHandler {

  def start(): Future[Unit]

}

object ViewHandler {

  def apply(eventSource: EventSource, collection: => JSONCollection, process: PartialFunction[(JSONCollection, Domain.EventEnvelope), Future[Unit]]): ViewHandler = new ViewHandler {

    val safeProcess = process.orElse[(JSONCollection, Domain.EventEnvelope), Future[Unit]] {
      case _ => Future.successful(())
    }

    def createPartialIDFromTime(timestamp: Int) = {
      val id = new Array[Byte](12)
      id(0) = (timestamp >>> 24).toByte
      id(1) = (timestamp >> 16 & 0xFF).toByte
      id(2) = (timestamp >> 8 & 0xFF).toByte
      id(3) = (timestamp & 0xFF).toByte

      BSONObjectID(id)
    }

    def lastEventId(): Future[BSONObjectID] = collection.find(
      Json.obj(), Json.obj("_lastUpdateByEvent" -> 1)).sort(Json.obj(
        "_lastUpdateByEvent" -> -1)).one[JsObject].map(_.flatMap { json =>
        (json \ "_lastUpdateByEvent").asOpt[BSONObjectID]
      }.getOrElse(createPartialIDFromTime(0)))

    def start(): Future[Unit] = {
      val ready = lastEventId().flatMap { lastEventId =>
        eventSource.streamEventsSince(lastEventId).through(Enumeratee.recover { (e, input) =>
          Logger.error(s"[start process] An error has occured with input : ${input}", e)
        }).run(Iteratee.foreach { e =>
          Logger.info(s"recovering $e")
          process(collection, e)
        })
      }
      ready.foreach { _ =>
        Logger.info(s"View ${this} ready")
        eventSource.streamEvents().run(Iteratee.foreach { e =>
          Logger.info(s"processing $e")
          process(collection, e)
        })
      }
      ready
    }
  }

}