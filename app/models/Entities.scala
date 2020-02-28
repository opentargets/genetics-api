package models

import slick.jdbc.GetResult
import models.Functions._
import models.DNA._
import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.{SeqView, breakOut}

object Entities {
  case class SumStatsGWASRow(typeId: String, studyId: String, variant: SimpleVariant,
                             eaf: Option[Double], mac: Option[Double], macCases: Option[Double], info: Option[Double], beta: Option[Double],
                             se: Option[Double], pval: Double, nTotal: Option[Long], nCases: Option[Long], isCC: Boolean) {
    lazy val oddsRatio: Option[Double] = if (isCC) Some(math.exp(beta.get)) else None
  }

  case class SumStatsMolTraitsRow(typeId: String, studyId: String, variant: SimpleVariant,
                             eaf: Option[Double], mac: Option[Double], numTests: Option[Double], info: Option[Double], beta: Option[Double],
                             se: Option[Double], pval: Double, nTotal: Option[Long], isCC: Boolean, phenotypeId: String, geneId: String,
                                  bioFeature: String) {
    lazy val oddsRatio: Option[Double] = if (isCC) Some(math.exp(beta.get)) else None
  }

  case class PhewFromSumstatsTable(associations: Seq[SumStatsGWASRow])

  // TODO refactor these entities about v2d and d2v2g
  case class V2DOdds(oddsCI: Option[Double], oddsCILower: Option[Double], oddsCIUpper: Option[Double])
  case class V2DBeta(direction: Option[String], betaCI: Option[Double], betaCILower: Option[Double],
                     betaCIUpper: Option[Double])
  case class V2DAssociation(pval: Double, pvalExponent: Long, pvalMantissa: Double,
                            r2: Option[Double], log10Abf: Option[Double],
                            posteriorProbability: Option[Double], afr1000GProp: Option[Double],
                            amr1000GProp: Option[Double], eas1000GProp: Option[Double],
                            eur1000GProp: Option[Double], sas1000GProp: Option[Double])

  case class LeadRow(studyId: String, leadVariant: SimpleVariant,
                    odds: V2DOdds, beta: V2DBeta, pval: Double,
                     pvalExponent: Long, pvalMantissa: Double)

  case class V2DRow(tag: SimpleVariant, lead: SimpleVariant, study: Study, association: V2DAssociation,
                    odds: V2DOdds, beta: V2DBeta)

  case class V2DL2GRowByGene(studyId: String, variantId: String, odds: V2DOdds, beta: V2DBeta, pval: Double,
                             pvalExponent: Long, pvalMantissa: Double, yProbaDistance: Double,
                             yProbaInteraction: Double, yProbaMolecularQTL: Double,
                             yProbaPathogenicity: Double, yProbaModel: Double)

  case class OverlapRow(stid: String, numOverlapLoci: Int)

  case class OverlappedLociStudy(studyId: String, topOverlappedStudies: IndexedSeq[OverlapRow])

  case class VariantStudyOverlapsRow(chromA: String, posA: Long, refA: String,
                                     altA: String, studyIdA: String,
                                     studyIdB: String, chromB: String,
                                     posB: Long, refB: String,
                                     altB: String, overlapAB: Long,
                                     distinctA: Long, distinctB: Long) {
    val variantA: Variant = Variant.fromSimpleVariant(chromA, posA, refA, altA)
    val variantB: Variant = Variant.fromSimpleVariant(chromB, posB, refB, altB)
  }

  case class OverlappedVariantsStudy(studyId: String, overlaps: Seq[OverlappedVariant])

  case class OverlappedVariant(variantIdA: String, variantIdB: String, overlapAB: Long,
                               distinctA: Long, distinctB: Long)

  case class VariantToDiseaseTable(associations: Seq[V2DRow])

  case class ManhattanTable(studyId: String, associations: Vector[ManhattanAssociation])

  case class ManhattanAssociation(variantId: String, pval: Double, pvalMantissa: Double, pvalExponent: Long,
                                  v2dOdds: V2DOdds, v2dBeta: V2DBeta,
                                  bestGenes: Seq[(String, Double)],
                                  bestColocGenes: Seq[(String, Double)],
                                  bestL2Genes: Seq[(String, Double)],
                                  crediblbeSetSize: Option[Long],
                                  ldSetSize: Option[Long], totalSetSize: Long)

  case class V2DStructure(typeId: String,
                           sourceId: String,
                          bioFeatureSet: Seq[String])

  case class SLGRow(geneId: String,
                    yProbaDistance: Double,
                    yProbaInteraction: Double,
                    yProbaMolecularQTL: Double,
                    yProbaPathogenicity: Double,
                    yProbaModel: Double,
                    hasColoc: Boolean,
                    distanceToLocus: Long)
  case class SLGTable(studyId: String, variantId: String, rows: Seq[SLGRow])
  case class V2DByStudy(studyId: String, variantId: String, pval: Double,
                        pval_mantissa: Double, pval_exponent: Long, v2dOdds: V2DOdds, v2dBeta: V2DBeta,
                        credibleSetSize: Option[Long], ldSetSize: Option[Long], totalSetSize: Long,
                        topGenes: Seq[(String, Double)], topColocGenes: Seq[(String, Double)],
                        topL2Genes: Seq[(String, Double)])

  case class StudyInfo(study: Option[Study])

  case class Study(studyId: String, traitReported: String, traitEfos: Seq[String],
                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
                   pubAuthor: Option[String], hasSumstats: Option[Boolean], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
                   nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
                   traitCategory: Option[String], numAssocLoci: Option[Long])

  case class GeneTagVariant(geneId: String, tagVariantId: String, overallScore: Double)
  case class TagVariantIndexVariantStudy(tagVariantId: String, indexVariantId: String, studyId: String,
                                         v2DAssociation: V2DAssociation, odds: V2DOdds, beta: V2DBeta)
  case class Gecko(geneIds: Seq[String], tagVariants: Seq[String], indexVariants: Seq[String],
                   studies: Seq[String], geneTagVariants: Seq[GeneTagVariant],
                   tagVariantIndexVariantStudies: Seq[TagVariantIndexVariantStudy])
  case class GeckoRow(geneId: String, tagVariant: SimpleVariant, indexVariant: SimpleVariant, studyId: String,
                      v2dAssociation: V2DAssociation, overallScore: Double,
                      odds: V2DOdds, beta: V2DBeta)

  object Gecko {
    def apply(geckoLines: SeqView[GeckoRow, Seq[_]], geneIdsInLoci: Set[String] = Set.empty): Option[Gecko] = {
      if (geckoLines.isEmpty)
        Some(Gecko(Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty))
      else {
        var geneIds: Set[String] = Set.empty
        var tagVariants: Set[String] = Set.empty
        var indexVariants: Set[String] = Set.empty
        var studies: Set[String] = Set.empty
        var tagVariantIndexVariantStudies: Set[TagVariantIndexVariantStudy] = Set.empty
        var geneTagVariants: Set[GeneTagVariant] = Set.empty

        geckoLines.foreach(line => {
          val lVID = line.indexVariant.id
          val tVID = line.tagVariant.id
          geneIds += line.geneId
          tagVariants += tVID
          indexVariants += lVID
          studies += line.studyId
          geneTagVariants += GeneTagVariant(line.geneId, tVID, line.overallScore)
          tagVariantIndexVariantStudies += TagVariantIndexVariantStudy(tVID, lVID,
            line.studyId, line.v2dAssociation, line.odds, line.beta)
        })

        // breakOut could be a good way to map virtually to a other collection of a different type
        // https://stackoverflow.com/questions/46509951/how-do-i-efficiently-count-distinct-fields-in-a-collection
        // val genes = geckoLines.map(_.gene)(breakOut).toSet.toSeq
         Some(Gecko((geneIds union geneIdsInLoci).toSeq, tagVariants.toSeq, indexVariants.toSeq, studies.toSeq,
                  geneTagVariants.toSeq, tagVariantIndexVariantStudies.toSeq))
      }
    }
  }

  case class VariantSearchResult (variant: Variant)

  case class SearchResultSet(totalGenes: Long, genes: Seq[Gene],
                             totalVariants: Long, variants: Seq[Variant],
                             totalStudies: Long, studies: Seq[Study])

  case class Tissue(id: String) {
    lazy val name: String = id.replace("_", " ").toLowerCase.capitalize
  }

  case class G2VSchemaElement(id: String, sourceId: String, displayLabel: Option[String],
                              overviewTooltip: Option[String], tagSubtitle: Option[String],
                              pmid: Option[String], tissues: Seq[Tissue])

  case class G2VSchema(qtls: Seq[G2VSchemaElement], intervals: Seq[G2VSchemaElement],
                       functionalPredictions: Seq[G2VSchemaElement], distances: Seq[G2VSchemaElement])

  case class G2VAssociation(geneId: String, variantId: String, overallScore: Double, qtls: Seq[G2VElement[QTLTissue]],
                            intervals: Seq[G2VElement[IntervalTissue]], fpreds: Seq[G2VElement[FPredTissue]],
                            distances: Seq[G2VElement[DistancelTissue]])

  object G2VAssociation {
    def toQtlTissues(scoreds: Seq[ScoredG2VLine]): Seq[QTLTissue] =
      scoreds.map(el =>
        QTLTissue(Tissue(el.feature), el.qtlScoreQ.getOrElse(0D), el.qtlBeta, el.qtlPval))

    def toIntervalTissues(scoreds: Seq[ScoredG2VLine]): Seq[IntervalTissue] =
      scoreds.map(el =>
        Entities.IntervalTissue(Tissue(el.feature), el.intervalScoreQ.getOrElse(0D), el.intervalScore))

    def toFPredTissues(scoreds: Seq[ScoredG2VLine]): Seq[FPredTissue] =
      scoreds.map(el =>
        Entities.FPredTissue(Tissue(el.feature), el.fpredMaxLabel, el.fpredMaxScore))

    def toDistanceTissues(scoreds: Seq[ScoredG2VLine]): Seq[DistancelTissue] =
      scoreds.map(el =>
        Entities.DistancelTissue(Tissue(el.feature), el.distanceScoreQ.getOrElse(0D), el.distance, el.distanceScore))

    def apply(groupedGene: SeqView[ScoredG2VLine, Seq[_]]): G2VAssociation = {
      val geneId = groupedGene.head.geneId
      val variantId = groupedGene.head.variantId
      val score = groupedGene.head.overallScore
      val grouped = groupedGene.groupBy(r => (r.typeId, r.sourceId))

      val qtls = grouped.filterKeys(k => defaultQtlTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[QTLTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toQtlTissues(p))
        }).values.toStream

      val intervals = grouped.filterKeys(k => defaultIntervalTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[IntervalTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toIntervalTissues(p))
        }).values.toStream

      val fpreds = grouped.filterKeys(k => defaultFPredTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[FPredTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toFPredTissues(p))
        }).values.toStream

      val distances = grouped.filterKeys(k => defaultDistanceTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[DistancelTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toDistanceTissues(p))
        }).values.toStream


      G2VAssociation(geneId, variantId, score,
        qtls.force, intervals.force, fpreds.force, distances.force)
    }
  }

  case class G2VElement[T](id: String, sourceId: String, name: Option[String], aggregatedScore: Double,
                           tissues: Seq[T])

  case class QTLTissue(tissue: Tissue, quantile: Double, beta: Option[Double], pval: Option[Double])

  case class IntervalTissue(tissue: Tissue, quantile: Double, score: Option[Double])

  case class DistancelTissue(tissue: Tissue, quantile: Double, distance: Option[Long], score: Option[Double])

  case class FPredTissue(tissue: Tissue, maxEffectLabel: Option[String], maxEffectScore: Option[Double])

  case class ScoredG2VLine(geneId: String, variantId: String, overallScore: Double, sourceScores: Map[String, Double],
                           typeId: String, sourceId: String, feature: String, fpredMaxLabel: Option[String],
                           fpredMaxScore: Option[Double], qtlBeta: Option[Double], qtlSE: Option[Double],
                           qtlPval: Option[Double], intervalScore: Option[Double], qtlScoreQ: Option[Double],
                           intervalScoreQ: Option[Double], distance: Option[Long], distanceScore: Option[Double],
                           distanceScoreQ: Option[Double])

  case class FPredSection(labels: Seq[String], scores: Seq[Double], maxLabel: Option[String], maxScore: Option[Double])
  case class QTLSection(beta: Option[Double], se: Option[Double], pval: Option[Double], score: Option[Double], scoreQ: Option[Double])
  case class IntervalSection(score: Option[Double], scoreQ: Option[Double])
  case class DistanceSection(distance: Option[Long], score: Option[Double], scoreQ: Option[Double])
  case class PureV2GRow(geneId: String, typeId: String, sourceId: String, feature: String,
                        fpred: FPredSection, qtl: QTLSection, interval: IntervalSection, distance: DistanceSection)
  case class V2GRow(variant: SimpleVariant, geneId: String, typeId: String, sourceId: String, feature: String,
                    fpred: FPredSection, qtl: QTLSection, interval: IntervalSection, distance: DistanceSection)

  case class D2V2GRow(v2d: V2DRow, pureV2g: PureV2GRow)
  case class D2V2GScoreRow(v2dRow: V2DRow, pureV2gRow: PureV2GRow, pureOverallScoreRow: PureOverallScoreRow)

  case class PureOverallScoreRow(sources: Seq[String], sourceScores: Seq[Double], overallScore: Double)

  case class OverallScoreRow(variant: SimpleVariant, geneId: String, sources: Seq[String], sourceScores: Seq[Double],
                             overallScore: Double)

  case class ColocRowHs(h0: Double, h1: Double, h2: Double, h3: Double, h4: Double,
                        h4h3: Double, log2h4h3: Double, nVars: Long,
                        lVariantRStudyBeta: Option[Double],
                        lVariantRStudySE: Option[Double],
                        lVariantRStudyPVal: Option[Double],
                        lVariantRIsCC: Option[Boolean])

  case class RightGWASColocRow(hs: ColocRowHs, isFlipped: Boolean,
                               rVariant: SimpleVariant, rStudy: String)

  case class RightQTLColocRow(hs: ColocRowHs, isFlipped: Boolean,
                              rVariant: SimpleVariant, rStudy: String, rType: String,
                              rGeneId: Option[String], rBioFeature: Option[String],
                              rPhenotype: Option[String])

  case class ColocRow(lVariant: SimpleVariant,
                      lStudy: String, lType: String,
                      hs: ColocRowHs, isFlipped: Boolean,
                      rVariant: SimpleVariant, rStudy: String, rType: String,
                      rGeneId: Option[String], rBioFeature: Option[String], rPhenotype: Option[String])

  case class CredSetRowStats(postProb: Double, tagBeta: Double,
                             tagPval: Double, tagSE: Double, is95: Boolean, is99: Boolean,
                             logABF: Double, multiSignalMethod: String)

  case class CredSetRow(studyId: String, leadVariant: SimpleVariant, tagVariant: SimpleVariant,
                        stats: CredSetRowStats, bioFeature: Option[String], pehotypeId: Option[String], dataType: String)

  object ESImplicits {
    implicit val geneHitReader: Reads[Gene] = (
      (JsPath \ "gene_id").read[String] and
        (JsPath \ "gene_name").readNullable[String] and
        (JsPath \ "biotype").readNullable[String] and
        (JsPath \ "description").readNullable[String] and
        (JsPath \ "chr").readNullable[String] and
        (JsPath \ "tss").readNullable[Long] and
        (JsPath \ "start").readNullable[Long] and
        (JsPath \ "end").readNullable[Long] and
        (JsPath \ "fwdstrand").readNullable[Int].map(_.map {
          case 0 => false
          case 1 => true
          case _ => false
        }) and
        (JsPath \ "exons").readNullable[String].map(r => LSeqRep(r.getOrElse("")).rep)
      )(Gene.apply _)

    implicit val annotation: Reads[Annotation] = (
      (JsPath \ "gene_id_any").readNullable[String] and
        (JsPath \ "gene_id_distance").readNullable[Long] and
        (JsPath \ "gene_id_prot_coding").readNullable[String] and
        (JsPath \ "gene_id_prot_coding_distance").readNullable[Long] and
        (JsPath \ "most_severe_consequence").readNullable[String]
      )(Annotation.apply _)

    implicit val caddAnnotation: Reads[CaddAnnotation] = (
      (JsPath \ "raw").readNullable[Double] and
        (JsPath \ "phred").readNullable[Double]
      )(CaddAnnotation.apply _)

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
      )(GnomadAnnotation.apply _)

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
      )(Variant.apply _)

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
        (JsPath \ "has_sumstats").readNullable[Int].map(_.map{
          case 1 => true
          case _ => false
        }) and
        (JsPath \ "ancestry_initial").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "ancestry_replication").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "n_initial").readNullable[Long] and
        (JsPath \ "n_replication").readNullable[Long] and
        (JsPath \ "n_cases").readNullable[Long] and
        (JsPath \ "trait_category").readNullable[String] and
        (JsPath \ "num_assoc_loci").readNullable[Long]
      )(Study.apply _)
  }

  object DBImplicits {
    implicit val getSLGRow: GetResult[SLGRow] = {
      GetResult(r => SLGRow(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
    }

    implicit val getV2DL2GRowByGene: GetResult[V2DL2GRowByGene] = {
      GetResult(r => {
        val studyId: String = r.<<
        val svID: String = SimpleVariant(r.<<, r.<<, r.<<, r.<<).id

        val yProbaLogiDistance: Double = r.<<
        val yProbaLogiInteraction: Double = r.<<
        val yProbaLogiMolecularQTL: Double = r.<<
        val yProbaLogiPathogenicity: Double = r.<<
        val yProbaFullModel: Double = r.<<

        val odds = V2DOdds(r.<<?, r.<<?, r.<<?)
        val beta = V2DBeta(r.<<?, r.<<?, r.<<?, r.<<?)
        val pval: Double = r.<<
        val pvalExponent: Long = r.<<
        val pvalMantissa: Double = r.<<

        V2DL2GRowByGene(studyId, svID, odds, beta, pval, pvalExponent, pvalMantissa,
          yProbaLogiDistance, yProbaLogiInteraction, yProbaLogiMolecularQTL,
          yProbaLogiPathogenicity, yProbaFullModel)

      })
    }

    implicit val getV2DByStudy: GetResult[V2DByStudy] = {
      def toGeneScoreTupleWithDuplicates(geneIds: Seq[String], geneScores: Seq[Double]): Seq[(String, Double)] = {
        val ordScored = geneIds zip geneScores

        if (ordScored.isEmpty)
          ordScored
        else
          ordScored.takeWhile(_._2 == ordScored.head._2)
      }

      def toGeneScoreTuple(geneIds: Seq[String], geneScores: Seq[Double]): Seq[(String, Double)] = {
        val ordScored = geneIds zip geneScores

        if (ordScored.isEmpty)
          ordScored
        else
          ordScored.groupBy(_._1).map(_._2.head)(breakOut).sortBy(_._2)(Ordering[Double].reverse)
      }

      GetResult(r => {
        val studyId: String = r.<<
        val svID: String = SimpleVariant(r.<<, r.<<, r.<<, r.<<).id
        val pval: Double = r.<<
        val pvalMantissa: Double = r.<<
        val pvalExponent: Long = r.<<
        val odds = V2DOdds(r.<<?, r.<<?, r.<<?)
        val beta = V2DBeta(r.<<?, r.<<?, r.<<?, r.<<?)
        val credSetSize: Option[Long] = r.<<?
        val ldSetSize: Option[Long] = r.<<?
        val totalSize: Long= r.<<
        val aggTop10RawIds = StrSeqRep(r.<<)
        val aggTop10RawScores = DSeqRep(r.<<)
        val aggTop10ColocIds = StrSeqRep(r.<<)
        val aggTop10ColocScores = DSeqRep(r.<<)
        val aggTop10L2GIds = StrSeqRep(r.<<)
        val aggTop10L2GScores = DSeqRep(r.<<)

        V2DByStudy(studyId, svID, pval, pvalMantissa, pvalExponent, odds, beta,
          credSetSize, ldSetSize, totalSize,
          toGeneScoreTupleWithDuplicates(aggTop10RawIds, aggTop10RawScores),
          toGeneScoreTuple(aggTop10ColocIds, aggTop10ColocScores),
          toGeneScoreTuple(aggTop10L2GIds, aggTop10L2GScores))
      })
    }
  }
}

