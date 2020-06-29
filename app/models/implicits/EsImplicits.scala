package models.implicits

import clickhouse.rep.SeqRep.LSeqRep
import models.entities.DNA._
import models.entities.Entities.Study
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

object EsImplicits {

  private def nullableIntToBoolean(int: Int): Boolean =
    int match {
      case 1 => true
      case _ => false
    }

  implicit val geneHitReader: Reads[Gene] = (
    (JsPath \ "gene_id").read[String] and
      (JsPath \ "gene_name").readNullable[String] and
      (JsPath \ "biotype").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "chr").readNullable[String] and
      (JsPath \ "tss").readNullable[Long] and
      (JsPath \ "start").readNullable[Long] and
      (JsPath \ "end").readNullable[Long] and
      (JsPath \ "fwdstrand")
        .readNullable[Int]
        .map(_.map {
          nullableIntToBoolean
        }) and
      (JsPath \ "exons").readNullable[String].map(r => LSeqRep(r.getOrElse("")).rep)
    ) (Gene.apply _)

  implicit val annotation: Reads[Annotation] = (
    (JsPath \ "gene_id_any").readNullable[String] and
      (JsPath \ "gene_id_distance").readNullable[Long] and
      (JsPath \ "gene_id_prot_coding").readNullable[String] and
      (JsPath \ "gene_id_prot_coding_distance").readNullable[Long] and
      (JsPath \ "most_severe_consequence").readNullable[String]
    ) (Annotation.apply _)

  implicit val caddAnnotation: Reads[CaddAnnotation] = (
    (JsPath \ "raw").readNullable[Double] and
      (JsPath \ "phred").readNullable[Double]
    ) (CaddAnnotation.apply _)

  implicit val gnomadAnnotation: Reads[GnomadAnnotation] = (
    (JsPath \ "gnomad_afr").readNullable[Double] and
      (JsPath \ "gnomad_seu").readNullable[Double] and
      (JsPath \ "gnomad_amr").readNullable[Double] and
      (JsPath \ "gnomad_asj").readNullable[Double] and
      (JsPath \ "gnomad_eas").readNullable[Double] and
      (JsPath \ "gnomad_fin").readNullable[Double] and
      (JsPath \ "gnomad_nfe").readNullable[Double] and
      (JsPath \ "gnomad_nfe_est").readNullable[Double] and
      (JsPath \ "gnomad_nfe_seu").readNullable[Double] and
      (JsPath \ "gnomad_nfe_onf").readNullable[Double] and
      (JsPath \ "gnomad_nfe_nwe").readNullable[Double] and
      (JsPath \ "gnomad_oth").readNullable[Double]
    ) (GnomadAnnotation.apply _)

  // implicit val variantHitReader: Reads[Variant] = Json.reads[Variant]
  implicit val variantHitReader: Reads[Variant] = (
    (JsPath \ "chr_id").read[String] and
      (JsPath \ "position").read[Long] and
      (JsPath \ "ref_allele").read[String] and
      (JsPath \ "alt_allele").read[String] and
      (JsPath \ "rs_id").readNullable[String] and
      annotation and
      caddAnnotation and
      gnomadAnnotation and
      (JsPath \ "chr_id_b37").readNullable[String] and
      (JsPath \ "position_b37").readNullable[Long]
    ) (Variant.apply _)

  // implicit val studyHitReader: Reads[Study] = Json.reads[Study]
  implicit val studyHitReader: Reads[Study] = (
    (JsPath \ "study_id").read[String] and
      (JsPath \ "trait_reported").read[String] and
      (JsPath \ "trait_efos").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "pmid").readNullable[String] and
      (JsPath \ "pub_date").readNullable[String] and
      (JsPath \ "pub_journal").readNullable[String] and
      (JsPath \ "pub_title").readNullable[String] and
      (JsPath \ "pub_author").readNullable[String] and
      (JsPath \ "has_sumstats")
        .readNullable[Int]
        .map(_.map {
          nullableIntToBoolean
        }) and
      (JsPath \ "ancestry_initial").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "ancestry_replication").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "n_initial").readNullable[Long] and
      (JsPath \ "n_replication").readNullable[Long] and
      (JsPath \ "n_cases").readNullable[Long] and
      (JsPath \ "trait_category").readNullable[String] and
      (JsPath \ "num_assoc_loci").readNullable[Long]
    ) (Study.apply _)

}
