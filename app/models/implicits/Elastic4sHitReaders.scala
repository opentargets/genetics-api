package models.implicits

import com.sksamuel.elastic4s.{Hit, HitReader}
import models.entities.DNA.{Gene, Variant}
import models.entities.Entities.Study
import play.api.{Logger, Logging}
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json, Reads}

import scala.util
import scala.util.{Failure, Success, Try}

trait ElasticSearchEntity

object EsHitReader extends HitReader[ElasticSearchEntity] with Logging {

  def convertHit[T](hit: Hit)(implicit r: Reads[T]): Try[T] =
    Json.parse(hit.sourceAsString).validate[T] match {
      case JsSuccess(value, _) => Success(value)
      case JsError(errors) =>
        logger.error(errors.mkString(" "))
        Failure(new RuntimeException(errors.mkString(" ")))
    }

  override def read(hit: Hit): Try[ElasticSearchEntity] = {
    import models.implicits.EsImplicits._

    hit.index match {
      case "genes"                            => convertHit[Gene](hit)
      case "studies"                          => convertHit[Study](hit)
      case s: String if s.contains("variant") => convertHit[Variant](hit)
      case _ =>
        Failure(
          new RuntimeException(
            s"Unknown index returned: ${hit.index}. Unable to match results to HitReader."
          )
        )
    }

  }

}
