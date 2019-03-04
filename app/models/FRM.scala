package models

import clickhouse.ClickHouseProfile
import clickhouse.rep.SeqRep.{DSeqRep, ISeqRep, LSeqRep, StrSeqRep}
import clickhouse.rep.SeqRep.Implicits._
import DNA._
import Entities.{Study, V2DStructure, VariantStudyOverlapsRow}

object FRM {
  import clickhouse.ClickHouseProfile.api._

  /** ClickHouse driver allows us to get serialised Arrays of all scalar types. But
    * jdbc does not allow to map to a seq of a scalar so these columns are defined here to
    * be able to interpret them implicitly. It is worth noting these functions can be
    * moved into the slick driver for clickhouse
    */
  implicit val seqIntType: ClickHouseProfile.BaseColumnType[Seq[Int]] =
    MappedColumnType.base[Seq[Int], String](_.mkString("[", ",", "]"), ISeqRep(_))
  implicit val seqLongType: ClickHouseProfile.BaseColumnType[Seq[Long]] =
    MappedColumnType.base[Seq[Long], String](_.mkString("[", ",", "]"), LSeqRep(_))
  implicit val seqDoubleType: ClickHouseProfile.BaseColumnType[Seq[Double]] =
    MappedColumnType.base[Seq[Double], String](_.mkString("[", ",", "]"), DSeqRep(_))
  implicit val seqStringType: ClickHouseProfile.BaseColumnType[Seq[String]] =
    MappedColumnType.base[Seq[String], String](_.map("'" + _ + "'").mkString("[", ",", "]"), StrSeqRep(_))

  class Overlaps(tag: Tag) extends Table[VariantStudyOverlapsRow](tag, "studies_overlap_exploded") {
    def chromA  = column[String]("A_chrom")
    def posA = column[Long]("A_pos")
    def refA = column[String]("A_ref")
    def altA  = column[String]("A_alt")
    def studyIdA = column[String]("A_study_id")

    def studyIdB  = column[String]("B_study_id")
    def chromB = column[String]("B_chrom")
    def posB = column[Long]("B_pos")
    def refB = column[String]("B_ref")
    def altB = column[String]("B_alt")
    def overlapsAB = column[Long]("AB_overlap")
    def distinctA = column[Long]("A_distinct")
    def distinctB = column[Long]("B_distinct")

    def * =
      (chromA, posA, refA, altA, studyIdA, studyIdB,
      chromB, posB, refB, altB,
      overlapsAB, distinctA, distinctB).mapTo[VariantStudyOverlapsRow]
  }
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
    def * = (id, symbol, bioType, chromosome, tss, start, end, fwd, exons).mapTo[Gene]
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
    def nearestGeneId = column[Option[String]]("gene_id_any")
    def nearestCodingGeneId = column[Option[String]]("gene_id_prot_coding")
    def nearestGeneDistance = column[Option[Long]]("gene_id_any_distance")
    def nearestCodingGeneDistance = column[Option[Long]]("gene_id_prot_coding_distance")
    def mostSevereConsequence = column[Option[String]]("most_severe_consequence")
    def caddRaw = column[Option[Double]]("raw")
    def caddPhred = column[Option[Double]]("phred")
    def gnomadAFR = column[Option[Double]]("gnomad_afr")
    def gnomadSEU = column[Option[Double]]("gnomad_seu")
    def gnomadAMR = column[Option[Double]]("gnomad_amr")
    def gnomadASJ = column[Option[Double]]("gnomad_asj")
    def gnomadEAS = column[Option[Double]]("gnomad_eas")
    def gnomadFIN = column[Option[Double]]("gnomad_fin")
    def gnomadNFE = column[Option[Double]]("gnomad_nfe")
    def gnomadNFEEST = column[Option[Double]]("gnomad_nfe_est")
    def gnomadNFESEU = column[Option[Double]]("gnomad_nfe_seu")
    def gnomadNFEONF = column[Option[Double]]("gnomad_nfe_onf")
    def gnomadNFENWE = column[Option[Double]]("gnomad_nfe_nwe")
    def gnomadOTH = column[Option[Double]]("gnomad_oth")

    def annotations =
      (nearestGeneId, nearestGeneDistance, nearestCodingGeneId,
        nearestCodingGeneDistance, mostSevereConsequence).mapTo[Annotation]

    def caddAnnotations =
      (caddRaw, caddPhred).mapTo[CaddAnnotation]

    def gnomadAnnotations =
      (gnomadAFR, gnomadSEU, gnomadAMR, gnomadASJ, gnomadEAS, gnomadFIN,
        gnomadNFE, gnomadNFEEST, gnomadNFESEU, gnomadNFEONF, gnomadNFENWE, gnomadOTH)
        .mapTo[GnomadAnnotation]

    def * =
      (chromosome, position, refAllele, altAllele, rsId,
      annotations, caddAnnotations, gnomadAnnotations).mapTo[Variant]
  }

  // --------------------------------------------------------
  // STUDY

  class Studies(tag: Tag) extends Table[Study](tag, "studies") {
    def studyId = column[String]("study_id")
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
    def numAssocLoci = column[Option[Long]]("num_assoc_loci")
    def * = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
      traitCategory, numAssocLoci).mapTo[Study]
  }

  class V2GStructure(tag: Tag) extends Table[V2DStructure](tag, "v2g_structure") {
    def typeId = column[String]("type_id")
    def sourceId = column[String]("source_id")
    def bioFeatureSet = column[Seq[String]]("feature_set")

    def * = (typeId, sourceId, bioFeatureSet).mapTo[V2DStructure]
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
