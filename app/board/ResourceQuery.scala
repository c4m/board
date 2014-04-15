package board

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import play.modules.reactivemongo.json.collection.JSONCollection

trait ResourceQuery[Q, R] {

  def findByUri(uri: String): Future[R]

  def findAll(): Future[List[R]]

  def find(query: Q): Future[List[R]]

}

object ResourceQuery {

  def apply(collection: => JSONCollection): ResourceQuery[JsValue, JsValue] = new ResourceQuery[JsValue, JsValue] {
    def findByUri(uri: String): Future[JsValue] = collection.find(Json.obj(
      "_id" -> uri
    )).cursor[JsValue].headOption.map {
      case Some(result) => result
    }

    def findAll(): Future[List[JsValue]] =
      collection.find(Json.obj()).cursor[JsValue].toList()

    def find(query: JsValue): Future[List[JsValue]] = 
      collection.find(query).cursor[JsValue].toList()
  }

}