package board

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._

import reactivemongo.bson.BSONObjectID

trait EventSource {

  def streamEvents(): Enumerator[Domain.EventEnvelope]

  def streamEventsSince(lastId: BSONObjectID): Enumerator[Domain.EventEnvelope]

  def streamEventsSinceAndContinue(lastId: BSONObjectID): Enumerator[Domain.EventEnvelope] = {
    streamEventsSince(lastId).andThen(streamEvents)
  }
}

object EventSource {

  def apply(source: Enumerator[Domain.EventEnvelope], history: Journal): EventSource = new EventSource {

    def streamEvents(): Enumerator[Domain.EventEnvelope] = source

    def streamEventsSince(lastId: BSONObjectID): Enumerator[Domain.EventEnvelope] = history.getEventsSince(lastId)
  }

  def apply(parentEventSource: EventSource, translate: PartialFunction[Domain.EventEnvelope, Future[List[Domain.EventEnvelope]]]) = new EventSource {

    // unsafe, will ignore silently any missing case even
    // don't use in real world
    val safeTranslate = translate.orElse[Domain.EventEnvelope, Future[List[Domain.EventEnvelope]]] {
      case _ => Future.successful(Nil)
    }

    def streamEvents(): Enumerator[Domain.EventEnvelope] = {
      parentEventSource
        .streamEvents()
        .through(Enumeratee.mapM(safeTranslate))
        .through(Enumeratee.mapConcat(identity))
    }

    def streamEventsSince(lastId: BSONObjectID): Enumerator[Domain.EventEnvelope] = {
      parentEventSource
        .streamEventsSince(lastId)
        .through(Enumeratee.mapM(safeTranslate))
        .through(Enumeratee.mapConcat(identity))
    }

  }

}