package board

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

object System extends System {

  private def eventCollection() = ReactiveMongoPlugin.db.collection[JSONCollection]("events")

  private def boardsCollection() = ReactiveMongoPlugin.db.collection[JSONCollection]("boards")

  private def columnsCollection() = ReactiveMongoPlugin.db.collection[JSONCollection]("columns")

  private def cardsCollection() = ReactiveMongoPlugin.db.collection[JSONCollection]("cards")

  lazy val command = Command(Journal(eventCollection()))

  lazy val query = new {
    lazy val boards = ResourceQuery(boardsCollection())
    lazy val columns = ResourceQuery(columnsCollection())
    lazy val cards = ResourceQuery(cardsCollection())
  }

  lazy val boards = Boards(command.eventSource, boardsCollection())

  lazy val columns = Columns(command.eventSource, columnsCollection())

  lazy val cards = Cards(command.eventSource, cardsCollection())

}

trait System {

  def command: Command

  def query: {
    def boards: ResourceQuery[JsValue, JsValue]
    def columns: ResourceQuery[JsValue, JsValue]
    def cards: ResourceQuery[JsValue, JsValue]
  }

  def boards: Boards

  def columns: Columns

  def cards: Cards

  def start(): Future[Unit] = {
    command.start()

    for {
      _ <- boards.start()
      _ <- columns.start()
      _ <- cards.start()
    } yield ()
  }

}