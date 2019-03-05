package models

import clickhouse.ClickHouseProfile
import clickhouse.rep.SeqRep.{DSeqRep, ISeqRep, LSeqRep, StrSeqRep}
import clickhouse.rep.SeqRep.Implicits._
import DNA._
import Entities._

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

  trait V2DTableFields extends Table[V2DRow] {
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
    def study = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
      traitCategory, numAssocLoci).mapTo[Study]

    def tagChromosome = column[String]("tag_chrom")
    def tagPosition = column[Long]("tag_pos")
    def tagRefAllele = column[String]("tag_ref")
    def tagAltAllele = column[String]("tag_alt")
    def tagVariant =
      (tagChromosome, tagPosition, tagRefAllele, tagAltAllele).mapTo[SimpleVariant]

    def leadChromosome = column[String]("lead_chrom")
    def leadPosition = column[Long]("lead_pos")
    def leadRefAllele = column[String]("lead_ref")
    def leadAltAllele = column[String]("lead_alt")
    def leadVariant =
      (leadChromosome, leadPosition, leadRefAllele, leadAltAllele).mapTo[SimpleVariant]

    def direction = column[String]("direction")
    def betaCI = column[Double]("beta")
    def betaCILower = column[Double]("beta_ci_lower")
    def betaCIUpper = column[Double]("beta_ci_upper")
    def beta =
      (direction, betaCI, betaCILower, betaCIUpper).mapTo[V2DBeta]

    def oddsCI = column[Double]("odds_ratio")
    def oddsCILower = column[Double]("oddsr_ci_lower")
    def oddsCIUpper = column[Double]("oddsr_ci_upper")
    def odds =
      (oddsCI, oddsCILower, oddsCIUpper).mapTo[V2DOdds]

    def pval = column[Double]("pval")
    def pvalMantissa = column[Double]("pval_mantissa")
    def pvalExponent = column[Long]("pval_exponent")
    def r2 = column[Option[Double]]("overall_r2")
    def log10Abf = column[Option[Double]]("log10_ABF")
    def posteriorProbability = column[Option[Double]]("posterior_prob")

    def afr1000GProp = column[Option[Double]]("AFR_1000G_prop")
    def amr1000GProp = column[Option[Double]]("AMR_1000G_prop")
    def eas1000GProp = column[Option[Double]]("EAS_1000G_prop")
    def eur1000GProp = column[Option[Double]]("EUR_1000G_prop")
    def sas1000GProp = column[Option[Double]]("SAS_1000G_prop")

    def association =
      (pval, pvalExponent, pvalMantissa, r2, log10Abf, posteriorProbability, afr1000GProp,
      amr1000GProp, eas1000GProp, eur1000GProp, sas1000GProp).mapTo[V2DAssociation]
  }

  class V2DsByStudy(tag: Tag) extends Table[V2DRow](tag, "v2d_by_stchr") with V2DTableFields {
    def * = (tagVariant, leadVariant, study, association, odds, beta).mapTo[V2DRow]
  }

  class V2DsByChrPos(tag: Tag) extends Table[V2DRow](tag, "v2d_by_chrpos") with V2DTableFields {
    def * = (tagVariant, leadVariant, study, association, odds, beta).mapTo[V2DRow]
  }

  class V2G(tag: Tag) extends Table[V2GRow](tag, "v2g") {
    def chromosome = column[String]("chr_id")
    def position = column[Long]("position")
    def refAllele = column[String]("ref_allele")
    def altAllele = column[String]("alt_allele")

    def variant = (chromosome, position, refAllele, altAllele).mapTo[SimpleVariant]

    def geneId = column[String]("gene_id")
    def typeId = column[String]("type_id")
    def sourceId = column[String]("source_id")
    def feature = column[String]("feature")

    def fpredLabels = column[Seq[String]]("fpred_labels")
    def fpredScores = column[Seq[Double]]("fpred_scores")
    def fpredMaxLabel = column[Option[String]]("fpred_max_label")
    def fpredMaxScore = column[Option[Double]]("fpred_max_score")

    def fpredSection = (fpredLabels, fpredScores, fpredMaxLabel, fpredMaxScore).mapTo[FPredSection]

    def qtlBeta = column[Option[Double]]("qtl_beta")
    def qtlSE = column[Option[Double]]("qtl_se")
    def qtlPVal = column[Option[Double]]("qtl_pval")
    def qtlScore = column[Option[Double]]("qtl_score")
    def qtlScoreQ = column[Option[Double]]("qtl_score_q")

    def qtlSection = (qtlBeta, qtlSE, qtlPVal, qtlScore, qtlScoreQ).mapTo[QTLSection]

    def intervalScore = column[Option[Double]]("interval_score")
    def intervalScoreQ = column[Option[Double]]("interval_score_q")

    def intervalSection = (intervalScore, intervalScoreQ).mapTo[IntervalSection]

    def distance = column[Option[Long]]("d")
    def distanceScore = column[Option[Double]]("distance_score")
    def distanceScoreQ = column[Option[Double]]("distance_score_q")

    def distanceSection = (distance, distanceScore, distanceScoreQ).mapTo[DistanceSection]

    //  case class V2GRow(variant: SimpleVariant, geneId: String, typeId: String, sourceId: String, feature: String,
    //                    fpred: FPredSection, qtl: QTLSection, interval: IntervalSection, distance: DistanceSection)

    def * =
      (variant, geneId, typeId, sourceId, feature, fpredSection,
        qtlSection, intervalSection, distanceSection).mapTo[V2GRow]
  }

  class V2GScore(tag: Tag) extends Table[V2GScoreRow](tag, "v2g_score_by_overall") {
    def chromosome = column[String]("chr_id")
    def position = column[Long]("position")
    def refAllele = column[String]("ref_allele")
    def altAllele = column[String]("alt_allele")

    def variant = (chromosome, position, refAllele, altAllele).mapTo[SimpleVariant]

    def geneId = column[String]("gene_id")

    def sources = column[Seq[String]]("source_list")
    def sourceScores = column[Seq[Double]]("source_score_list")
    def overallScore = column[Double]("overall_score")
    def * =
      (variant, geneId, sources, sourceScores, overallScore).mapTo[V2GScoreRow]
  }
}
