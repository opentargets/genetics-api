package models

import clickhouse.rep.SeqRep
import clickhouse.rep.SeqRep.LSeqRep
import com.sksamuel.elastic4s.{Hit, HitReader}
import models.DNA.{Gene, Variant}
import models.Entities.Study

import scala.util.{Failure, Try}

trait ElasticSearchEntity

object EsHitReader extends HitReader[ElasticSearchEntity] {

  private def createGeneFromMap(source: Map[String, AnyRef]): Gene = {
    implicit val converter = SeqRep.Implicits
    Gene(
      source("gene_id").toString,
      Some(source("gene_name").toString),
      Some(source("biotype").toString),
      Some(source("description").toString),
      Some(source("chr").toString),
      Some(source("tss").toString.toLong),
      Some(source("start").toString.toLong),
      Some(source("end").toString.toLong),
      Some(source("fwdstrand").toString.map {
        case '0' => false
        case '1' => true
        case _ => false
      }.head),
      converter.seqLong(LSeqRep(Option(source("exons").toString).getOrElse(""))))
  }

  def createStudyFromMap(source: Map[String, AnyRef]): ElasticSearchEntity = ???

  def createVariantFromMap(source: Map[String, AnyRef]): ElasticSearchEntity = ???

  override def read(hit: Hit): Try[ElasticSearchEntity] = {
    val source = hit.sourceAsMap
    hit.index match {
      case "genes" => Try(createGeneFromMap(source))
      case "study" => Try(createStudyFromMap(source))
      case s: String if s.contains("variant") => Try(createVariantFromMap(source))
      case _ => Failure(new RuntimeException(s"Unknown index returned: ${hit.index}"))
    }

  }
}
