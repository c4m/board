package board

import play.api.libs.json._
import play.api.libs.functional.syntax._

import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

object Domain {

  sealed trait Event

  case class EventEnvelope(id: Option[BSONObjectID], author: String, body: Event)

  object Board {
    sealed trait Event extends Domain.Event
    case class Create(uri: String, name: String) extends Event
    case class ColumnAdded(projectUri: String, columnUri: String) extends Event
  }

  object Column {
    sealed trait Event extends Domain.Event
    case class Create(uri: String, projectUri: String, name: String) extends Event
    case class CardAdded(columnUri: String, cardUri: String) extends Event
    case class CardRemoved(columnUri: String, cardUri: String) extends Event
  }

  object Card {
    sealed trait Event extends Domain.Event
    case class Create(uri: String, projectUri: String, columnUri: String, name: String, description: String) extends Event
    case class Move(cardUri: String, columnUri: String) extends Event
  }


  object JsonFormat {

    val (boardCreateFormat, boardColumnAdded) = {
      import Board._

      (Json.format[Create], Json.format[ColumnAdded])
    }

    val (columnsCreateFormat, columnsCardAdded, columnsCardRemoved) = {
      import Column._

      (Json.format[Create], Json.format[CardAdded], Json.format[CardRemoved])
    }


    val (cardCreateFormat, cardMoveFormat) = {
      import Card._

      (Json.format[Create], Json.format[Move])
    }

    val eventReadsRegistry = Map[String, JsValue => JsResult[Event]](
      classOf[Board.Create].getName -> (boardCreateFormat.reads(_: JsValue)),
      classOf[Board.ColumnAdded].getName -> (boardColumnAdded.reads(_: JsValue)),
      classOf[Column.Create].getName -> (columnsCreateFormat.reads(_: JsValue)),
      classOf[Column.CardAdded].getName -> (columnsCardAdded.reads(_: JsValue)),
      classOf[Column.CardRemoved].getName -> (columnsCardRemoved.reads(_: JsValue)),
      classOf[Card.Create].getName -> (cardCreateFormat.reads(_: JsValue)),
      classOf[Card.Move].getName -> (cardMoveFormat.reads(_: JsValue))
    )

    def eventReads(typeHint: String): Reads[Event] = new Reads[Event] {
      def reads(json: JsValue): JsResult[Event] = {
        eventReadsRegistry.get(typeHint).map { reader =>
          reader(json)
        } getOrElse {
          JsError(s"No reader registered for type $typeHint")
        }
      }
    }

    val eventWritesRegistry = Map[String, Event => JsValue](
      classOf[Board.Create].getName -> ( (evt: Event) => (boardCreateFormat.writes(evt.asInstanceOf[Board.Create]))),
      classOf[Board.ColumnAdded].getName -> ( (evt: Event) => (boardColumnAdded.writes(evt.asInstanceOf[Board.ColumnAdded]))),
      classOf[Column.Create].getName -> ( (evt: Event) => (columnsCreateFormat.writes(evt.asInstanceOf[Column.Create]))),
      classOf[Column.CardAdded].getName -> ( (evt: Event) => (columnsCardAdded.writes(evt.asInstanceOf[Column.CardAdded]))),
      classOf[Column.CardRemoved].getName -> ( (evt: Event) => (columnsCardRemoved.writes(evt.asInstanceOf[Column.CardRemoved]))),
      classOf[Card.Create].getName -> ( (evt: Event) => (cardCreateFormat.writes(evt.asInstanceOf[Card.Create]))),
      classOf[Card.Move].getName -> ( (evt: Event) => (cardMoveFormat.writes(evt.asInstanceOf[Card.Move])))
    )

    val eventWrites = new Writes[Event] {
      def writes(event: Event): JsValue = {
        //unsafe throws exceptions
        // don't use in real world
        eventWritesRegistry(event.getClass.getName)(event)
      }
    }

    val eventEnveloppeReads = (
      (__ \ "_id").readNullable[BSONObjectID] and
      (__ \ "author").read[String] and
      (__ \ "_typeHint").read[String].flatMap { typeHint =>
        (__ \ "body").read(eventReads(typeHint))
      }
    )(EventEnvelope.apply _)

    val eventEnvelopeWrites = new Writes[EventEnvelope] {
      def writes(ee: EventEnvelope): JsValue = Json.obj(
        "_id" -> ee.id,
        "author" -> ee.author,
        "_typeHint" -> ee.body.getClass.getName,
        "body" -> eventWrites.writes(ee.body)
      )
    }

  }

}