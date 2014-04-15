package board

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._

trait Command {

  def eventSource: EventSource

  def apply(event: Domain.EventEnvelope): Future[Domain.EventEnvelope]

  def start(): Unit

}

object Command {

  def apply(journal: Journal) = new Command {

    lazy val eventSource = EventSource(eventOutEnumerator, journal)

    def apply(event: Domain.EventEnvelope): Future[Domain.EventEnvelope] = {
      val promise = Promise[Domain.EventEnvelope]

      eventInChannel.push((event, promise))

      promise.future
    }

    def start() {
      eventInEnumerator
        .through(Enumeratee.recover(handleFailure))
        .through(Enumeratee.mapM(processEvent))
        .apply(Iteratee.foreach(consumeEvent))
    }

    private val (eventInEnumerator, eventInChannel) = Concurrent.broadcast[(Domain.EventEnvelope, Promise[Domain.EventEnvelope])]

    private val (eventOutEnumerator, eventOutChannel) = Concurrent.broadcast[Domain.EventEnvelope]

    private val processEvent: ((Domain.EventEnvelope, Promise[Domain.EventEnvelope])) => Future[(Domain.EventEnvelope, Promise[Domain.EventEnvelope])] = { case (event, promise) =>
        journal.write(event).map { eventWithId =>
          (eventWithId, promise)
        }
      }

    private val consumeEvent: ((Domain.EventEnvelope, Promise[Domain.EventEnvelope])) => Unit = { case (event, promise) =>
      promise.success(event)
      eventOutChannel.push(event)
    }

    private val handleFailure: (Throwable, Input[(Domain.EventEnvelope, Promise[Domain.EventEnvelope])]) => Unit = {
      case (NonFatal(exception), Input.El((event, promise))) => promise.failure(exception)
      case (exception, _) => throw exception
    }
  }

}