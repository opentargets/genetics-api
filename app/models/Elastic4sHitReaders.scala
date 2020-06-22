package models

import com.sksamuel.elastic4s.{Hit, HitReader}
import models.entities.DNA.{Gene, Variant}
import models.entities.Entities.Study
import play.api.libs.json.{Json, Reads}

import scala.util.{Failure, Try}

trait ElasticSearchEntity

object EsHitReader extends HitReader[ElasticSearchEntity] {

  def convertHit[T](hit: Hit)(implicit r: Reads[T]): T =
    Json.parse(hit.sourceAsString).validate[T].get

  override def read(hit: Hit): Try[ElasticSearchEntity] = {
    import EsImplicits._

    hit.index match {
      case "genes" => Try(convertHit[Gene](hit))
      case "studies" => Try(convertHit[Study](hit))
      case s: String if s.contains("variant") => Try(convertHit[Variant](hit))
      case _ =>
        Failure(
          new RuntimeException(
            s"Unknown index returned: ${hit.index}. Unable to match results to HitReader."))
    }

  }

}
