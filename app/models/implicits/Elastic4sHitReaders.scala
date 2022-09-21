package models.implicits

import com.sksamuel.elastic4s.{Hit, HitReader}
import play.api.Logging
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json, Reads}

import scala.util.{Failure, Success, Try}

sealed trait ElasticSearchEntity
case class GeneSearchResult(id: String) extends ElasticSearchEntity
case class VariantSearchResult(id: String) extends ElasticSearchEntity
case class StudySearchResult(id: String) extends ElasticSearchEntity

object EsHitReader extends HitReader[ElasticSearchEntity] with Logging {

  implicit val gsrReader: Reads[GeneSearchResult] =
    (JsPath \ "gene_id").read[String].map(GeneSearchResult)
  implicit val vsrReader: Reads[VariantSearchResult] =
    (JsPath \ "variant_id").read[String].map(VariantSearchResult)
  implicit val ssrReader: Reads[StudySearchResult] =
    (JsPath \ "study_id").read[String].map(StudySearchResult)

  def convertHit[T](hit: Hit)(implicit r: Reads[T]): Try[T] =
    Json.parse(hit.sourceAsString).validate[T] match {
      case JsSuccess(value, _) => Success(value)
      case JsError(errors) =>
        logger.error(errors.mkString(" "))
        Failure(new RuntimeException(errors.mkString(" ")))
    }

  override def read(hit: Hit): Try[ElasticSearchEntity] =
    hit.index match {
      case "genes"                            => convertHit[GeneSearchResult](hit)
      case "studies"                          => convertHit[StudySearchResult](hit)
      case s: String if s.contains("variant") => convertHit[VariantSearchResult](hit)
      case _ =>
        Failure(
          new RuntimeException(
            s"Unknown index returned: ${hit.index}. Unable to match results to HitReader."
          )
        )
    }

}
