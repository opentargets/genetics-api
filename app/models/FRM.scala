package models

import clickhouse.ClickHouseProfile
import clickhouse.rep.SeqRep.{DSeqRep, LSeqRep, StrSeqRep}
import clickhouse.rep.SeqRep.Implicits._
import DNA.{Variant, Gene}
import Entities.Study

object FRM {
  import clickhouse.ClickHouseProfile.api._

  /** Clickhouse driver allows us to get serialised Arrays of all scalar types. But
    * jdbc does not allows to map to a seq of a scalar so this columns are defined here to
    * be able to interpret them implicitily. It is worth noting these functions can be
    * moved into the slick driver for clickhouse
    */
  implicit val seqLongType: ClickHouseProfile.BaseColumnType[Seq[Long]] =
    MappedColumnType.base[Seq[Long], String](_.mkString("[", ",", "]"), LSeqRep(_))
  implicit val seqDoubleType: ClickHouseProfile.BaseColumnType[Seq[Double]] =
    MappedColumnType.base[Seq[Double], String](_.mkString("[", ",", "]"), DSeqRep(_))
  implicit val seqStringType: ClickHouseProfile.BaseColumnType[Seq[String]] =
    MappedColumnType.base[Seq[String], String](_.map("'" + _ + "'").mkString("[", ",", "]"), StrSeqRep(_))

  // --------------------------------------------------------
  // GENE

  class Genes(tag: Tag) extends Table[Gene](tag, "gene") {
    def id = column[String]("gene_id")
    def symbol = column[Option[String]]("gene_name")
    def bioType = column[Option[String]]("biotype")
    def chromosome = column[Option[String]]("chr")
    def tss = column[Option[Long]]("tss")
    def start = column[Option[Long]]("start")
    def end = column[Option[Long]]("end")
    def fwd = column[Option[Boolean]]("fwdstrand")
    def exons = column[Seq[Long]]("exons")
    def * = (id, symbol, bioType, chromosome, tss, start, end, fwd, exons) <>
      (Gene.tupled, Gene.unapply)
  }

  // --------------------------------------------------------
  // VARIANT

  class Variants(tag: Tag) extends Table[Variant](tag, "variants") {
    def id = column[String]("variant_id")
    def chromosome = column[String]("chr_id")
    def position = column[Long]("position")
    def refAllele = column[String]("ref_allele")
    def altAllele = column[String]("alt_allele")
    def rsId = column[Option[String]]("rs_id")
    def nearestGeneId = column[Option[String]]("gene_id")
    def nearestCodingGeneId = column[Option[String]]("gene_id_prot_coding")
    def * = (id, chromosome, position, refAllele, altAllele, rsId, nearestGeneId, nearestCodingGeneId) <>
      (Variant.tupled, Variant.unapply)
  }

  // --------------------------------------------------------
  // STUDY

  class Studies(tag: Tag) extends Table[Study](tag, "studies") {
    def studyId = column[String]("study_id")
    def traitCode = column[String]("trait_code")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_date")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def * = (
      studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory
    ) <> (Study.tupled, Study.unapply)
  }

  //  // V2D (NOT CURRENTLY USED)
  //  // --------------------------------------------------------

//  case class V2D(tag: Variant, lead: Variant, study: Study, association: V2DAssociation)
//
//  // def tupleToVariant (t: Tuple6[_, String, Long, String, String, Option[String]]) = Variant(t._2, t._3, t._4, t._5, t._6)
//  // def variantToTuple (v: Variant) = Some(v.id, v.chromosome, v.position, v.refAllele, v.altAllele, v.rsId)
//
//  trait TagVariantFields extends Table[V2D] {
//    def tagId = column[String]("variant_id")
//    def tagChromosome = column[String]("chr_id")
//    def tagPosition = column[Long]("position")
//    def tagRefAllele = column[String]("ref_allele")
//    def tagAltAllele = column[String]("alt_allele")
//    def tagRsId = column[Option[String]]("rs_id")
//    def `*`: ProvenShape[Variant] = (tagId, tagChromosome, tagPosition, tagRefAllele, tagAltAllele, tagRsId) <>
//      (Variant.tupled, Variant.unapply)
//  }
//
//  trait LeadVariantFields extends Table[V2D] {
//    def leadId = column[String]("index_variant_id")
//    def leadChromosome = column[String]("index_chr_id")
//    def leadPosition = column[Long]("index_position")
//    def leadRefAllele = column[String]("index_ref_allele")
//    def leadAltAllele = column[String]("index_alt_allele")
//    def leadRsId = column[Option[String]]("index_rs_id")
//    def leadProjection = (leadId, leadChromosome, leadPosition, leadRefAllele, leadAltAllele, leadRsId) <> (tupleToVariant, variantToTuple)
//  }
//
//  trait StudyFields extends Table[V2D] {
//    def studyId = column[String]("stid")
//    def traitCode = column[String]("trait_code")
//    def traitReported = column[String]("trait_reported")
//    def traitEfos = column[Seq[String]]("trait_efos")
//    def pubId = column[Option[String]]("pmid")
//    def pubDate = column[Option[String]]("pub_date")
//    def pubJournal = column[Option[String]]("pub_journal")
//    def pubTitle = column[Option[String]]("pub_title")
//    def pubAuthor = column[Option[String]]("pub_author")
//    def ancestryInitial = column[Seq[String]]("ancestry_initial")
//    def ancestryReplication = column[Seq[String]]("ancestry_replication")
//    def nInitial = column[Option[Long]]("n_initial")
//    def nReplication = column[Option[Long]]("n_replication")
//    def nCases = column[Option[Long]]("n_cases")
//    def traitCategory = column[Option[String]]("trait_category")
//    def studyProjection = (studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle, pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory) <> (Study.tupled, Study.unapply)
//  }
//
//  trait V2DAssociationFields extends Table[V2D] {
//    def pval = column[Double]("pval")
//    def r2 = column[Option[Double]]("r2")
//    def log10Abf = column[Option[Double]]("log10_abf")
//    def posteriorProbability = column[Option[Double]]("posterior_prob")
//
//    def afr1000GProp = column[Option[Double]]("afr_1000g_prop")
//    def amr1000GProp = column[Option[Double]]("amr_1000g_prop")
//    def eas1000GProp = column[Option[Double]]("eas_1000g_prop")
//    def eur1000GProp = column[Option[Double]]("eur_1000g_prop")
//    def sas1000GProp = column[Option[Double]]("sas_1000g_prop")
//    def associationProjection = (pval, r2, log10Abf, posteriorProbability, afr1000GProp, amr1000GProp, eas1000GProp, eur1000GProp, sas1000GProp) <> (V2DAssociation.tupled, V2DAssociation.unapply)
//  }
//
//  class V2DsByChrPos(tag: Tag) extends Table[V2D](tag, "v2d_by_chrpos") with TagVariantFields with LeadVariantFields with StudyFields with V2DAssociationFields {
//    def * = (tagProjection, leadProjection, studyProjection, associationProjection) <> (V2D.tupled, V2D.unapply)
//  }
//
//  class V2DsByStudy(tag: Tag) extends Table[V2D](tag, "v2d_by_stchr") with TagVariantFields with LeadVariantFields with StudyFields with V2DAssociationFields {
//    def * = (tagProjection, leadProjection, studyProjection, associationProjection) <> (V2D.tupled, V2D.unapply)
//  }
}
