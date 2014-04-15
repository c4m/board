package board

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._

import reactivemongo.bson.BSONObjectID

object BodyParsers {

  def postEvent[T <: Domain.Event](implicit reader: Reads[T]): BodyParser[T] = new BodyParser[T] {
    def apply(rh: RequestHeader): Iteratee[Array[Byte], Either[SimpleResult, T]] = {
      play.api.mvc.BodyParsers.parse.json(rh).flatMap {
        case Right(json: JsObject) =>
          val basePath = rh.path.stripSuffix("/")
          val jsonWithId = json ++ Json.obj(
            "uri" -> s"${basePath}/${BSONObjectID.generate.stringify}"
          )
          reader.reads(jsonWithId).map { event =>
            Done[Array[Byte], Either[SimpleResult, T]](Right(event), Input.Empty)
          } recoverTotal { errors =>
            Done[Array[Byte], Either[SimpleResult, T]](Left(BadRequest(JsError.toFlatJson(errors))), Input.Empty)
          }
        case _ => Done[Array[Byte], Either[SimpleResult, T]](
          Left(BadRequest(JsError.toFlatJson(JsError("json boject expected")))),
          Input.Empty
        )
      }
    }
  }

}