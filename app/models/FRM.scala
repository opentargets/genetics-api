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

    def variantA = (chromA, posA, refA, altA).mapTo[SimpleVariant]
    def variantB = (chromB, posB, refB, altB).mapTo[SimpleVariant]

    def * =
      (chromA, posA, refA, altA, studyIdA, studyIdB,
      chromB, posB, refB, altB,
      overlapsAB, distinctA, distinctB).mapTo[VariantStudyOverlapsRow]
  }

  class Genes(tag: Tag) extends Table[Gene](tag, "genes") {
    def id = column[String]("gene_id")
    def symbol = column[String]("gene_name")
    def bioType = column[String]("biotype")
    def description = column[String]("description")
    def chromosome = column[String]("chr")
    def tss = column[Long]("tss")
    def start = column[Long]("start")
    def end = column[Long]("end")
    def fwd = column[Boolean]("fwdstrand")
    def exons = column[Seq[Long]]("exons")
    def * =
      (id, symbol.?, bioType.?, description.?, chromosome.?, tss.?, start.?, end.?, fwd.?, exons)
        .mapTo[Gene]
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
    def hasSumstats = column[Option[Boolean]]("has_sumstats")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def numAssocLoci = column[Option[Long]]("num_assoc_loci")
    def * = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, hasSumstats, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
      traitCategory, numAssocLoci).mapTo[Study]
  }

  class V2GStructure(tag: Tag) extends Table[V2DStructure](tag, "v2g_structure") {
    def typeId = column[String]("type_id")
    def sourceId = column[String]("source_id")
    def bioFeatureSet = column[Seq[String]]("feature_set")

    def * = (typeId, sourceId, bioFeatureSet).mapTo[V2DStructure]
  }

  class V2DsByStudy(tag: Tag) extends Table[V2DRow](tag, "v2d_by_stchr") {
    def studyId = column[String]("study_id")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_date")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def hasSumstats = column[Option[Boolean]]("has_sumstats")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def numAssocLoci = column[Option[Long]]("num_assoc_loci")
    def study = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, hasSumstats, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
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

    def direction = column[Option[String]]("direction")
    def betaCI = column[Option[Double]]("beta")
    def betaCILower = column[Option[Double]]("beta_ci_lower")
    def betaCIUpper = column[Option[Double]]("beta_ci_upper")
    def beta =
      (direction, betaCI, betaCILower, betaCIUpper).mapTo[V2DBeta]

    def oddsCI = column[Option[Double]]("odds_ratio")
    def oddsCILower = column[Option[Double]]("oddsr_ci_lower")
    def oddsCIUpper = column[Option[Double]]("oddsr_ci_upper")
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

    def * = (tagVariant, leadVariant, study, association, odds, beta).mapTo[V2DRow]
  }

  class V2DsByChrPos(tag: Tag) extends Table[V2DRow](tag, "v2d_by_chrpos") {
    def studyId = column[String]("study_id")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_date")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def hasSumstats = column[Option[Boolean]]("has_sumstats")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def numAssocLoci = column[Option[Long]]("num_assoc_loci")
    def study = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, hasSumstats, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
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

    def direction = column[Option[String]]("direction")
    def betaCI = column[Option[Double]]("beta")
    def betaCILower = column[Option[Double]]("beta_ci_lower")
    def betaCIUpper = column[Option[Double]]("beta_ci_upper")
    def beta =
      (direction, betaCI, betaCILower, betaCIUpper).mapTo[V2DBeta]

    def oddsCI = column[Option[Double]]("odds_ratio")
    def oddsCILower = column[Option[Double]]("oddsr_ci_lower")
    def oddsCIUpper = column[Option[Double]]("oddsr_ci_upper")
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

    def * = (tagVariant, leadVariant, study, association, odds, beta).mapTo[V2DRow]
  }

  trait PureV2GTableFields extends Table[PureV2GRow] {
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

    def pureV2gRow =
      (geneId, typeId, sourceId, feature, fpredSection,
        qtlSection, intervalSection, distanceSection).mapTo[PureV2GRow]
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

    def pureV2gRow =
      (geneId, typeId, sourceId, feature, fpredSection,
        qtlSection, intervalSection, distanceSection).mapTo[PureV2GRow]

    def * =
      (variant, geneId, typeId, sourceId, feature, fpredSection,
        qtlSection, intervalSection, distanceSection).mapTo[V2GRow]
  }

  class V2GOverallScore(tag: Tag) extends Table[OverallScoreRow](tag, "v2g_score_by_overall") {
    def chromosome = column[String]("chr_id")
    def position = column[Long]("position")
    def refAllele = column[String]("ref_allele")
    def altAllele = column[String]("alt_allele")

    def variant = (chromosome, position, refAllele, altAllele).mapTo[SimpleVariant]

    def geneId = column[String]("gene_id")
    def sources = column[Seq[String]]("source_list")
    def sourceScores = column[Seq[Double]]("source_score_list")
    def overallScore = column[Double]("overall_score")
    def pureOverallScoreRow =
      (sources, sourceScores, overallScore).mapTo[PureOverallScoreRow]

    def * =
      (variant, geneId, sources, sourceScores, overallScore).mapTo[OverallScoreRow]
  }

  class D2V2G(tag: Tag) extends Table[D2V2GRow](tag, "d2v2g") {
    def studyId = column[String]("study_id")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_date")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def hasSumstats = column[Option[Boolean]]("has_sumstats")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def numAssocLoci = column[Option[Long]]("num_assoc_loci")
    def study = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, hasSumstats, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
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

    def direction = column[Option[String]]("direction")
    def betaCI = column[Option[Double]]("beta")
    def betaCILower = column[Option[Double]]("beta_ci_lower")
    def betaCIUpper = column[Option[Double]]("beta_ci_upper")
    def beta =
      (direction, betaCI, betaCILower, betaCIUpper).mapTo[V2DBeta]

    def oddsCI = column[Option[Double]]("odds_ratio")
    def oddsCILower = column[Option[Double]]("oddsr_ci_lower")
    def oddsCIUpper = column[Option[Double]]("oddsr_ci_upper")
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

    def v2dRow = (tagVariant, leadVariant, study, association, odds, beta).mapTo[V2DRow]

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

    def pureV2gRow =
      (geneId, typeId, sourceId, feature, fpredSection,
        qtlSection, intervalSection, distanceSection).mapTo[PureV2GRow]

    def * = (v2dRow, pureV2gRow).mapTo[D2V2GRow]

  }

  class D2V2GScored(tag: Tag) extends Table[D2V2GScoreRow](tag, "d2v2g_scored") {
    def studyId = column[String]("study_id")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_date")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def hasSumstats = column[Option[Boolean]]("has_sumstats")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def numAssocLoci = column[Option[Long]]("num_assoc_loci")
    def study = (
      studyId, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, hasSumstats, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases,
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

    def direction = column[Option[String]]("direction")
    def betaCI = column[Option[Double]]("beta")
    def betaCILower = column[Option[Double]]("beta_ci_lower")
    def betaCIUpper = column[Option[Double]]("beta_ci_upper")
    def beta =
      (direction, betaCI, betaCILower, betaCIUpper).mapTo[V2DBeta]

    def oddsCI = column[Option[Double]]("odds_ratio")
    def oddsCILower = column[Option[Double]]("oddsr_ci_lower")
    def oddsCIUpper = column[Option[Double]]("oddsr_ci_upper")
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

    def sources = column[Seq[String]]("source_list")
    def sourceScores = column[Seq[Double]]("source_score_list")
    def overallScore = column[Double]("overall_score")
    def pureOverallScoreRow =
      (sources, sourceScores, overallScore).mapTo[PureOverallScoreRow]

    def leadRow =
      (studyId, leadVariant, odds, beta, pval, pvalExponent, pvalMantissa).mapTo[LeadRow]

    def v2dRow = (tagVariant, leadVariant, study, association, odds, beta).mapTo[V2DRow]

    def geckoRow =
      (geneId, tagVariant, leadVariant, studyId, association,
        overallScore, odds, beta).mapTo[GeckoRow]

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

    def pureV2gRow =
      (geneId, typeId, sourceId, feature, fpredSection,
        qtlSection, intervalSection, distanceSection).mapTo[PureV2GRow]

    def * = (v2dRow, pureV2gRow, pureOverallScoreRow).mapTo[D2V2GScoreRow]

  }

  class D2V2GOverallScore(tag: Tag) extends Table[OverallScoreRow](tag, "d2v2g_score_by_overall") {
    def tagChrom = column[String]("tag_chrom")
    def tagPos = column[Long]("tag_pos")
    def tagRef = column[String]("tag_ref")
    def tagAlt = column[String]("tag_alt")

    def variant = (tagChrom, tagPos, tagRef, tagAlt).mapTo[SimpleVariant]

    def geneId = column[String]("gene_id")

    def sources = column[Seq[String]]("source_list")
    def sourceScores = column[Seq[Double]]("source_score_list")
    def overallScore = column[Double]("overall_score")
    def pureOverallScoreRow =
      (sources, sourceScores, overallScore).mapTo[PureOverallScoreRow]

    def * =
      (variant, geneId, sources, sourceScores, overallScore).mapTo[OverallScoreRow]
  }

  class Coloc(tag: Tag) extends Table[ColocRow](tag, "v2d_coloc") {
    def h0 = column[Double]("coloc_h0")
    def h1 = column[Double]("coloc_h1")
    def h2 = column[Double]("coloc_h2")
    def h3 = column[Double]("coloc_h3")
    def h4 = column[Double]("coloc_h4")
    def h4h3 = column[Double]("coloc_h4_h3")
    def log2h4h3 = column[Double]("coloc_log2_h4_h3")
    def nVars = column[Long]("coloc_n_vars")

    def hs = (h0, h1, h2, h3, h4, h4h3, log2h4h3, nVars).mapTo[ColocRowHs]

    def isFlipped = column[Boolean]("is_flipped")

    def lStudy = column[String]("left_study")
    def lType = column[String]("left_type")
    def lChrom = column[String]("left_chrom")
    def lPos = column[Long]("left_pos")
    def lRef = column[String]("left_ref")
    def lAlt = column[String]("left_alt")

    def lVariant = (lChrom, lPos, lRef, lAlt).mapTo[SimpleVariant]

    def rStudy = column[String]("right_study")
    def rType = column[String]("right_type")
    def rChrom = column[String]("right_chrom")
    def rPos = column[Long]("right_pos")
    def rRef = column[String]("right_ref")
    def rAlt = column[String]("right_alt")

    def rVariant = (lChrom, lPos, lRef, lAlt).mapTo[SimpleVariant]

    def rGeneId = column[String]("right_gene_id")
    def rBioFeature = column[String]("right_bio_feature")
    def rPhenotype = column[String]("right_phenotype")

    def * = (lVariant, lStudy, lType, hs, isFlipped,
      rVariant, rStudy, rType, rGeneId, rBioFeature, rPhenotype).mapTo[ColocRow]
  }

  class CredSet(tag: Tag) extends Table[CredSetRow](tag, "v2d_credset") {
    def studyId = column[String]("study_id")
    def bioFeature = column[String]("bio_feature")
    def phenotypeId = column[String]("pehnotype_id")
    def dataType = column[String]("data_type")

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

    def postProb = column[Double]("posterior_prob")
    def postProbCumSum = column[Double]("posterior_prob_cumsum")
    def beta = column[Double]("beta")
    def betaCond = column[Double]("beta_cond")
    def pval = column[Double]("pval")
    def pvalCond = column[Double]("pval_condition")
    def se = column[Double]("se")
    def seCond = column[Double]("se_cond")
    def is95 = column[Boolean]("is95")
    def is99 = column[Boolean]("is99")
    def logABF = column[Double]("log10_ABF")
    def multiSignalMethod = column[String]("multisignalmethod")

    def stats = (postProb, postProbCumSum, beta, betaCond, pval, pvalCond,
      se, seCond, is95, is99, logABF, multiSignalMethod).mapTo[CredSetRowStats]

    def * = (studyId, leadVariant, tagVariant, stats,
      bioFeature, phenotypeId, dataType).mapTo[CredSetRow]
  }

  class SumStatsGWAS(tag: Tag) extends Table[SumStatsGWASRow](tag, "v2d_sa_gwas") {
    def typeId = column[String]("type_id")
    def studyId = column[String]("study_id")
    def chrom = column[String]("chrom")
    def pos = column[Long]("pos")
    def ref = column[String]("ref")
    def alt = column[String]("alt")
    def eaf = column[Option[Double]]("eaf")
    def mac = column[Option[Double]]("mac")
    def macCases = column[Option[Double]]("mac_cases")
    def info = column[Option[Double]]("info")
    def beta = column[Option[Double]]("beta")
    def se = column[Option[Double]]("se")
    def pval = column[Double]("pval")
    def nTotal = column[Option[Long]]("n_total")
    def nCases = column[Option[Long]]("n_cases")
    def isCC = column[Boolean]("is_cc")

    def variant = (chrom, pos, ref, alt).mapTo[SimpleVariant]

    def * = (typeId, studyId, variant, eaf, mac, macCases, info, beta, se,
      pval, nTotal, nCases, isCC).mapTo[SumStatsGWASRow]
  }
}
