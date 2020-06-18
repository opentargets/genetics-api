package models

import clickhouse.rep.SeqRep.Implicits._
import clickhouse.rep.SeqRep._
import models.DNA._
import models.Functions._

import scala.collection.{SeqView, breakOut}

object Entities {

  case class SumStatsGWASRow(
                              typeId: String,
                              studyId: String,
                              variant: SimpleVariant,
                              eaf: Option[Double],
                              mac: Option[Double],
                              macCases: Option[Double],
                              info: Option[Double],
                              beta: Option[Double],
                              se: Option[Double],
                              pval: Double,
                              nTotal: Option[Long],
                              nCases: Option[Long],
                              isCC: Boolean
                            ) {
    lazy val oddsRatio: Option[Double] = if (isCC) Some(math.exp(beta.get)) else None
  }

  case class SumStatsMolTraitsRow(
                                   typeId: String,
                                   studyId: String,
                                   variant: SimpleVariant,
                                   eaf: Option[Double],
                                   mac: Option[Double],
                                   numTests: Option[Double],
                                   info: Option[Double],
                                   beta: Option[Double],
                                   se: Option[Double],
                                   pval: Double,
                                   nTotal: Option[Long],
                                   isCC: Boolean,
                                   phenotypeId: String,
                                   geneId: String,
                                   bioFeature: String
                                 ) {
    lazy val oddsRatio: Option[Double] = if (isCC) Some(math.exp(beta.get)) else None
  }

  case class PhewFromSumstatsTable(associations: Seq[SumStatsGWASRow])

  // TODO refactor these entities about v2d and d2v2g
  case class V2DOdds(
                      oddsCI: Option[Double],
                      oddsCILower: Option[Double],
                      oddsCIUpper: Option[Double]
                    )

  case class V2DBeta(
                      direction: Option[String],
                      betaCI: Option[Double],
                      betaCILower: Option[Double],
                      betaCIUpper: Option[Double]
                    )

  case class V2DAssociation(
                             pval: Double,
                             pvalExponent: Long,
                             pvalMantissa: Double,
                             r2: Option[Double],
                             log10Abf: Option[Double],
                             posteriorProbability: Option[Double],
                             afr1000GProp: Option[Double],
                             amr1000GProp: Option[Double],
                             eas1000GProp: Option[Double],
                             eur1000GProp: Option[Double],
                             sas1000GProp: Option[Double]
                           )

  case class LeadRow(
                      studyId: String,
                      leadVariant: SimpleVariant,
                      odds: V2DOdds,
                      beta: V2DBeta,
                      pval: Double,
                      pvalExponent: Long,
                      pvalMantissa: Double
                    )

  case class V2DRow(
                     tag: SimpleVariant,
                     lead: SimpleVariant,
                     study: Study,
                     association: V2DAssociation,
                     odds: V2DOdds,
                     beta: V2DBeta
                   )

  case class V2DL2GRowByGene(
                              studyId: String,
                              variantId: String,
                              odds: V2DOdds,
                              beta: V2DBeta,
                              pval: Double,
                              pvalExponent: Long,
                              pvalMantissa: Double,
                              yProbaDistance: Double,
                              yProbaInteraction: Double,
                              yProbaMolecularQTL: Double,
                              yProbaPathogenicity: Double,
                              yProbaModel: Double
                            )

  case class OverlapRow(stid: String, numOverlapLoci: Int)

  case class OverlappedLociStudy(studyId: String, topOverlappedStudies: IndexedSeq[OverlapRow])

  case class VariantStudyOverlapsRow(
                                      chromA: String,
                                      posA: Long,
                                      refA: String,
                                      altA: String,
                                      studyIdA: String,
                                      studyIdB: String,
                                      chromB: String,
                                      posB: Long,
                                      refB: String,
                                      altB: String,
                                      overlapAB: Long,
                                      distinctA: Long,
                                      distinctB: Long
                                    ) {
    val variantA: Variant = Variant.fromSimpleVariant(chromA, posA, refA, altA)
    val variantB: Variant = Variant.fromSimpleVariant(chromB, posB, refB, altB)
  }

  case class OverlappedVariantsStudy(studyId: String, overlaps: Seq[OverlappedVariant])

  case class OverlappedVariant(
                                variantIdA: String,
                                variantIdB: String,
                                overlapAB: Long,
                                distinctA: Long,
                                distinctB: Long
                              )

  case class VariantToDiseaseTable(associations: Seq[V2DRow])

  case class ManhattanTable(studyId: String, associations: Vector[ManhattanAssociation])

  case class ManhattanAssociation(
                                   variantId: String,
                                   pval: Double,
                                   pvalMantissa: Double,
                                   pvalExponent: Long,
                                   v2dOdds: V2DOdds,
                                   v2dBeta: V2DBeta,
                                   bestGenes: Seq[(String, Double)],
                                   bestColocGenes: Seq[(String, Double)],
                                   bestL2Genes: Seq[(String, Double)],
                                   credibleSetSize: Option[Long],
                                   ldSetSize: Option[Long],
                                   totalSetSize: Long
                                 )

  case class V2DStructure(typeId: String, sourceId: String, bioFeatureSet: Seq[String])

  case class SLGRow(
                     geneId: String,
                     yProbaDistance: Double,
                     yProbaInteraction: Double,
                     yProbaMolecularQTL: Double,
                     yProbaPathogenicity: Double,
                     yProbaModel: Double,
                     hasColoc: Boolean,
                     distanceToLocus: Long
                   )

  case class SLGTable(studyId: String, variantId: String, rows: Seq[SLGRow])

  case class V2DByStudy(
                         studyId: String,
                         variantId: String,
                         pval: Double,
                         pval_mantissa: Double,
                         pval_exponent: Long,
                         v2dOdds: V2DOdds,
                         v2dBeta: V2DBeta,
                         credibleSetSize: Option[Long],
                         ldSetSize: Option[Long],
                         totalSetSize: Long,
                         topGenes: Seq[(String, Double)],
                         topColocGenes: Seq[(String, Double)],
                         topL2Genes: Seq[(String, Double)]
                       )

  case class StudyInfo(study: Option[Study])

  case class Study(
                    studyId: String,
                    traitReported: String,
                    traitEfos: Seq[String],
                    pubId: Option[String],
                    pubDate: Option[String],
                    pubJournal: Option[String],
                    pubTitle: Option[String],
                    pubAuthor: Option[String],
                    hasSumstats: Option[Boolean],
                    ancestryInitial: Seq[String],
                    ancestryReplication: Seq[String],
                    nInitial: Option[Long],
                    nReplication: Option[Long],
                    nCases: Option[Long],
                    traitCategory: Option[String],
                    numAssocLoci: Option[Long]
                  ) extends ElasticSearchEntity

  case class GeneTagVariant(geneId: String, tagVariantId: String, overallScore: Double)

  case class TagVariantIndexVariantStudy(
                                          tagVariantId: String,
                                          indexVariantId: String,
                                          studyId: String,
                                          v2DAssociation: V2DAssociation,
                                          odds: V2DOdds,
                                          beta: V2DBeta
                                        )

  case class Gecko(
                    geneIds: Seq[String],
                    tagVariants: Seq[String],
                    indexVariants: Seq[String],
                    studies: Seq[String],
                    geneTagVariants: Seq[GeneTagVariant],
                    tagVariantIndexVariantStudies: Seq[TagVariantIndexVariantStudy]
                  )

  case class GeckoRow(
                       geneId: String,
                       tagVariant: SimpleVariant,
                       indexVariant: SimpleVariant,
                       studyId: String,
                       v2dAssociation: V2DAssociation,
                       overallScore: Double,
                       odds: V2DOdds,
                       beta: V2DBeta
                     )

  object Gecko {
    def apply(
               geckoLines: SeqView[GeckoRow, Seq[_]],
               geneIdsInLoci: Set[String] = Set.empty
             ): Option[Gecko] = {
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
          tagVariantIndexVariantStudies += TagVariantIndexVariantStudy(
            tVID,
            lVID,
            line.studyId,
            line.v2dAssociation,
            line.odds,
            line.beta
          )
        })

        // breakOut could be a good way to map virtually to a other collection of a different type
        /* https://stackoverflow.com/questions/46509951/how-do-i-efficiently-count-distinct-fields-in-a-collection */
        // val genes = geckoLines.map(_.gene)(breakOut).toSet.toSeq
        Some(
          Gecko(
            (geneIds union geneIdsInLoci).toSeq,
            tagVariants.toSeq,
            indexVariants.toSeq,
            studies.toSeq,
            geneTagVariants.toSeq,
            tagVariantIndexVariantStudies.toSeq
          )
        )
      }
    }
  }

  case class VariantSearchResult(variant: Variant)

  case class SearchResultSet(
                              totalGenes: Long,
                              genes: Seq[Gene],
                              totalVariants: Long,
                              variants: Seq[Variant],
                              totalStudies: Long,
                              studies: Seq[Study]
                            )

  case class Tissue(id: String) {
    lazy val name: String = id.replace("_", " ").toLowerCase.capitalize
  }

  case class G2VSchemaElement(
                               id: String,
                               sourceId: String,
                               displayLabel: Option[String],
                               overviewTooltip: Option[String],
                               tagSubtitle: Option[String],
                               pmid: Option[String],
                               tissues: Seq[Tissue]
                             )

  case class G2VSchema(
                        qtls: Seq[G2VSchemaElement],
                        intervals: Seq[G2VSchemaElement],
                        functionalPredictions: Seq[G2VSchemaElement],
                        distances: Seq[G2VSchemaElement]
                      )

  case class G2VAssociation(
                             geneId: String,
                             variantId: String,
                             overallScore: Double,
                             qtls: Seq[G2VElement[QTLTissue]],
                             intervals: Seq[G2VElement[IntervalTissue]],
                             fpreds: Seq[G2VElement[FPredTissue]],
                             distances: Seq[G2VElement[DistancelTissue]]
                           )

  object G2VAssociation {
    def toQtlTissues(scoreds: Seq[ScoredG2VLine]): Seq[QTLTissue] =
      scoreds.map(el =>
        QTLTissue(Tissue(el.feature), el.qtlScoreQ.getOrElse(0d), el.qtlBeta, el.qtlPval))

    def toIntervalTissues(scoreds: Seq[ScoredG2VLine]): Seq[IntervalTissue] =
      scoreds.map(el =>
        Entities
          .IntervalTissue(Tissue(el.feature), el.intervalScoreQ.getOrElse(0d), el.intervalScore))

    def toFPredTissues(scoreds: Seq[ScoredG2VLine]): Seq[FPredTissue] =
      scoreds.map(el =>
        Entities.FPredTissue(Tissue(el.feature), el.fpredMaxLabel, el.fpredMaxScore))

    def toDistanceTissues(scoreds: Seq[ScoredG2VLine]): Seq[DistancelTissue] =
      scoreds.map(el =>
        Entities.DistancelTissue(
          Tissue(el.feature),
          el.distanceScoreQ.getOrElse(0d),
          el.distance,
          el.distanceScore
        ))

    def apply(groupedGene: SeqView[ScoredG2VLine, Seq[_]]): G2VAssociation = {
      val geneId = groupedGene.head.geneId
      val variantId = groupedGene.head.variantId
      val score = groupedGene.head.overallScore
      val grouped = groupedGene.groupBy(r => (r.typeId, r.sourceId))

      val qtls = grouped
        .filterKeys(k => defaultQtlTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[QTLTissue](
            p.head.typeId,
            p.head.sourceId,
            None,
            p.head.sourceScores(sc),
            toQtlTissues(p)
          )
        })
        .values
        .toStream

      val intervals = grouped
        .filterKeys(k => defaultIntervalTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[IntervalTissue](
            p.head.typeId,
            p.head.sourceId,
            None,
            p.head.sourceScores(sc),
            toIntervalTissues(p)
          )
        })
        .values
        .toStream

      val fpreds = grouped
        .filterKeys(k => defaultFPredTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[FPredTissue](
            p.head.typeId,
            p.head.sourceId,
            None,
            p.head.sourceScores(sc),
            toFPredTissues(p)
          )
        })
        .values
        .toStream

      val distances = grouped
        .filterKeys(k => defaultDistanceTypes.contains(k._1))
        .mapValues(p => {
          val sc = p.head.sourceId
          G2VElement[DistancelTissue](
            p.head.typeId,
            p.head.sourceId,
            None,
            p.head.sourceScores(sc),
            toDistanceTissues(p)
          )
        })
        .values
        .toStream

      G2VAssociation(
        geneId,
        variantId,
        score,
        qtls.force,
        intervals.force,
        fpreds.force,
        distances.force
      )
    }
  }

  case class G2VElement[T](
                            id: String,
                            sourceId: String,
                            name: Option[String],
                            aggregatedScore: Double,
                            tissues: Seq[T]
                          )

  case class QTLTissue(tissue: Tissue, quantile: Double, beta: Option[Double], pval: Option[Double])

  case class IntervalTissue(tissue: Tissue, quantile: Double, score: Option[Double])

  case class DistancelTissue(
                              tissue: Tissue,
                              quantile: Double,
                              distance: Option[Long],
                              score: Option[Double]
                            )

  case class FPredTissue(
                          tissue: Tissue,
                          maxEffectLabel: Option[String],
                          maxEffectScore: Option[Double]
                        )

  case class ScoredG2VLine(
                            geneId: String,
                            variantId: String,
                            overallScore: Double,
                            sourceScores: Map[String, Double],
                            typeId: String,
                            sourceId: String,
                            feature: String,
                            fpredMaxLabel: Option[String],
                            fpredMaxScore: Option[Double],
                            qtlBeta: Option[Double],
                            qtlSE: Option[Double],
                            qtlPval: Option[Double],
                            intervalScore: Option[Double],
                            qtlScoreQ: Option[Double],
                            intervalScoreQ: Option[Double],
                            distance: Option[Long],
                            distanceScore: Option[Double],
                            distanceScoreQ: Option[Double]
                          )

  case class FPredSection(
                           labels: Seq[String],
                           scores: Seq[Double],
                           maxLabel: Option[String],
                           maxScore: Option[Double]
                         )

  case class QTLSection(
                         beta: Option[Double],
                         se: Option[Double],
                         pval: Option[Double],
                         score: Option[Double],
                         scoreQ: Option[Double]
                       )

  case class IntervalSection(score: Option[Double], scoreQ: Option[Double])

  case class DistanceSection(distance: Option[Long], score: Option[Double], scoreQ: Option[Double])

  case class PureV2GRow(
                         geneId: String,
                         typeId: String,
                         sourceId: String,
                         feature: String,
                         fpred: FPredSection,
                         qtl: QTLSection,
                         interval: IntervalSection,
                         distance: DistanceSection
                       )

  case class V2GRow(
                     variant: SimpleVariant,
                     geneId: String,
                     typeId: String,
                     sourceId: String,
                     feature: String,
                     fpred: FPredSection,
                     qtl: QTLSection,
                     interval: IntervalSection,
                     distance: DistanceSection
                   )

  case class D2V2GRow(v2d: V2DRow, pureV2g: PureV2GRow)

  case class D2V2GScoreRow(
                            v2dRow: V2DRow,
                            pureV2gRow: PureV2GRow,
                            pureOverallScoreRow: PureOverallScoreRow
                          )

  case class PureOverallScoreRow(
                                  sources: Seq[String],
                                  sourceScores: Seq[Double],
                                  overallScore: Double
                                )

  case class OverallScoreRow(
                              variant: SimpleVariant,
                              geneId: String,
                              sources: Seq[String],
                              sourceScores: Seq[Double],
                              overallScore: Double
                            )

  case class ColocRowHs(
                         h0: Double,
                         h1: Double,
                         h2: Double,
                         h3: Double,
                         h4: Double,
                         h4h3: Double,
                         log2h4h3: Double,
                         nVars: Long,
                         lVariantRStudyBeta: Option[Double],
                         lVariantRStudySE: Option[Double],
                         lVariantRStudyPVal: Option[Double],
                         lVariantRIsCC: Option[Boolean]
                       )

  case class RightGWASColocRow(
                                hs: ColocRowHs,
                                isFlipped: Boolean,
                                rVariant: SimpleVariant,
                                rStudy: String
                              )

  case class RightQTLColocRow(
                               hs: ColocRowHs,
                               isFlipped: Boolean,
                               rVariant: SimpleVariant,
                               rStudy: String,
                               rType: String,
                               rGeneId: Option[String],
                               rBioFeature: Option[String],
                               rPhenotype: Option[String]
                             )

  case class ColocRow(
                       lVariant: SimpleVariant,
                       lStudy: String,
                       lType: String,
                       hs: ColocRowHs,
                       isFlipped: Boolean,
                       rVariant: SimpleVariant,
                       rStudy: String,
                       rType: String,
                       rGeneId: Option[String],
                       rBioFeature: Option[String],
                       rPhenotype: Option[String]
                     )

  case class CredSetRowStats(
                              postProb: Double,
                              tagBeta: Double,
                              tagPval: Double,
                              tagSE: Double,
                              is95: Boolean,
                              is99: Boolean,
                              logABF: Double,
                              multiSignalMethod: String
                            )

  case class CredSetRow(
                         studyId: String,
                         leadVariant: SimpleVariant,
                         tagVariant: SimpleVariant,
                         stats: CredSetRowStats,
                         bioFeature: Option[String],
                         pehotypeId: Option[String],
                         dataType: String
                       )

}
