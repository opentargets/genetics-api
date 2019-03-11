package models

import com.sksamuel.elastic4s.{Hit, HitReader}
import slick.jdbc.GetResult
import models.Functions._
import models.DNA._
import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._

import scala.collection.SeqView

object Entities {

  case class V2DOdds(oddsCI: Double, oddsCILower: Double, oddsCIUpper: Double)
  case class V2DBeta(direction: String, betaCI: Double, betaCILower: Double, betaCIUpper: Double)
  case class V2DAssociation(pval: Double, pvalExponent: Long, pvalMantissa: Double,
                            r2: Option[Double], log10Abf: Option[Double],
                            posteriorProbability: Option[Double], afr1000GProp: Option[Double],
                            amr1000GProp: Option[Double], eas1000GProp: Option[Double],
                            eur1000GProp: Option[Double], sas1000GProp: Option[Double])

  case class V2DRow(tag: SimpleVariant, lead: SimpleVariant, study: Study, association: V2DAssociation,
                    odds: V2DOdds, beta: V2DBeta)

  case class OverlapRow(stid: String, numOverlapLoci: Int)

  case class OverlappedLociStudy(studyId: String, topOverlappedStudies: IndexedSeq[OverlapRow])

  case class VariantStudyOverlapsRow(chromA: String, posA: Long, refA: String,
                                     altA: String, studyIdA: String,
                                     studyIdB: String, chromB: String,
                                     posB: Long, refB: String,
                                     altB: String, overlapAB: Long,
                                     distinctA: Long, distinctB: Long) {
    val variantA: Variant = Variant(chromA, posA, refA, altA)
    val variantB: Variant = Variant(chromB, posB, refB, altB)
  }

  case class OverlappedVariantsStudy(studyId: String, overlaps: Seq[OverlappedVariant])

  case class OverlappedVariant(variantIdA: String, variantIdB: String, overlapAB: Long,
                               distinctA: Long, distinctB: Long)

  case class VariantToDiseaseTable(associations: Seq[V2DRow])

  case class ManhattanTable(studyId: String, associations: Vector[ManhattanAssociation])

  case class ManhattanAssociation(variantId: String, pval: Double, pvalMantissa: Double, pvalExponent: Long,
                                  bestGenes: Seq[(String, Double)], crediblbeSetSize: Option[Long],
                                  ldSetSize: Option[Long], totalSetSize: Long)

  case class V2DStructure(typeId: String,
                           sourceId: String,
                          bioFeatureSet: Seq[String])

  case class V2DByStudy(lead_chrom: String, lead_pos: Long, lead_ref: String, lead_alt: String, pval: Double,
                        pval_mantissa: Double, pval_exponent: Long,
                        credibleSetSize: Option[Long], ldSetSize: Option[Long], totalSetSize: Long, topGenes: Seq[(String, Double)])

  case class StudyInfo(study: Option[Study])

  case class Study(studyId: String, traitReported: String, traitEfos: Seq[String],
                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
                   pubAuthor: Option[String], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
                   nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
                   traitCategory: Option[String], numAssocLoci: Option[Long])

  case class PheWASTable(associations: Vector[VariantPheWAS])
  case class VariantPheWAS(stid: String, pval: Double, beta: Double, se: Double, eaf: Double, maf: Double,
                           nSamplesVariant: Option[Long], nSamplesStudy: Option[Long], nCasesStudy: Option[Long],
                           nCasesVariant: Option[Long], oddRatio: Option[Double], chip: String, info: Option[Double])
  case class GeneTagVariant(geneId: String, tagVariantId: String, overallScore: Double)
  case class TagVariantIndexVariantStudy(tagVariantId: String, indexVariantId: String, studyId: String,
                                         v2DAssociation: V2DAssociation)
  case class Gecko(geneIds: Seq[String], tagVariants: Seq[String], indexVariants: Seq[String],
                   studies: Seq[String], geneTagVariants: Seq[GeneTagVariant],
                   tagVariantIndexVariantStudies: Seq[TagVariantIndexVariantStudy])
  case class GeckoRow(geneId: String, tagVariant: SimpleVariant, indexVariant: SimpleVariant, studyId: String,
                      v2dAssociation: V2DAssociation, overallScore: Double)

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
            line.studyId, line.v2dAssociation)
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
    lazy val name: Option[String] = Option(id.replace("_", " ").toLowerCase.capitalize)
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

  object ESImplicits {
    implicit object GeneHitReader extends HitReader[Gene] {
      override def read(hit: Hit): Either[Throwable, Gene] = {
        if (hit.isSourceEmpty) Left(new NoSuchFieldError("source object is empty"))
        else {
          val mv = hit.sourceAsMap

          Right(Gene(mv("gene_id").toString,
            mv.get("gene_name").map(_.toString),
            mv.get("biotype").map(_.toString),
            mv.get("chr").map(_.toString),
            mv.get("tss").map(_.toString.toLong),
            mv.get("start").map(_.toString.toLong),
            mv.get("end").map(_.toString.toLong),
            mv.get("fwdstrand").map(_.toString.toInt match {
              case 0 => false
              case 1 => true
              case _ => false
            }),
            LSeqRep(mv.get("exons").map(_.toString).getOrElse(""))
          ))
        }
      }
    }

    implicit object VariantHitReader extends HitReader[Variant] {
      override def read(hit: Hit): Either[Throwable, Variant] = {
        if (hit.isSourceEmpty) Left(new NoSuchFieldError("source object is empty"))
        else {
          val mv = hit.sourceAsMap
          Right(Variant(mv("chr_id").toString,
            mv("position").toString.toLong,
            mv("ref_allele").toString,
            mv("alt_allele").toString,
            mv.get("rs_id").map(_.toString),
            Annotation(mv.get("gene_id").map(_.toString),
              mv.get("gene_id_distance").map(_.toString.toLong),
              mv.get("gene_id_prot_coding").map(_.toString),
              mv.get("gene_id_prot_coding_distance").map(_.toString.toLong),
              mv.get("most_severe_consequence").map(_.toString)),
            CaddAnnotation(mv.get("raw").map(_.toString.toDouble),
              mv.get("phred").map(_.toString.toDouble)),
            GnomadAnnotation(mv.get("gnomad_afr").map(_.toString.toDouble),
              mv.get("gnomad_seu").map(_.toString.toDouble),
              mv.get("gnomad_amr").map(_.toString.toDouble),
              mv.get("gnomad_asj").map(_.toString.toDouble),
              mv.get("gnomad_eas").map(_.toString.toDouble),
              mv.get("gnomad_fin").map(_.toString.toDouble),
              mv.get("gnomad_nfe").map(_.toString.toDouble),
              mv.get("gnomad_nfe_est").map(_.toString.toDouble),
              mv.get("gnomad_nfe_seu").map(_.toString.toDouble),
              mv.get("gnomad_nfe_onf").map(_.toString.toDouble),
              mv.get("gnomad_nfe_nwe").map(_.toString.toDouble),
              mv.get("gnomad_oth").map(_.toString.toDouble))))
        }
      }
    }

    implicit object StudyHitReader extends HitReader[Study] {
      override def read(hit: Hit): Either[Throwable, Study] = {
        if (hit.isSourceEmpty) Left(new NoSuchFieldError("source object is empty"))
        else {
          val mv = hit.sourceAsMap

          Right(Study(mv("study_id").toString,
            mv.get("trait_reported").map(_.toString).get,
            mv.get("trait_efos").map(_.asInstanceOf[Seq[String]]).getOrElse(Seq.empty),
            mv.get("pmid").map(_.asInstanceOf[String]),
            mv.get("pub_date").map(_.asInstanceOf[String]),
            mv.get("pub_journal").map(_.asInstanceOf[String]),
            mv.get("pub_title").map(_.asInstanceOf[String]),
            mv.get("pub_author").map(_.asInstanceOf[String]),
            mv.get("ancestry_initial").map(_.asInstanceOf[Seq[String]]).getOrElse(Seq.empty),
            mv.get("ancestry_replication").map(_.asInstanceOf[Seq[String]]).getOrElse(Seq.empty),
            mv.get("n_initial").map(_.asInstanceOf[Int].toLong),
            mv.get("n_replication").map(_.asInstanceOf[Int].toLong),
            mv.get("n_cases").map(_.asInstanceOf[Int].toLong),
            mv.get("trait_category").map(_.asInstanceOf[String]),
            mv.get("num_assoc_loci").map(_.asInstanceOf[Int]))
          )
        }
      }
    }
  }

  object DBImplicits {
    implicit val getV2DByStudy: GetResult[V2DByStudy] = {
      def toGeneScoreTuple(geneIds: Seq[String], geneScores: Seq[Double]): Seq[(String, Double)] = {
        val ordScored = (geneIds zip geneScores)
          .sortBy(_._2)(Ordering[Double].reverse)

        if (ordScored.isEmpty) ordScored
        else {
          ordScored.takeWhile(_._2 == ordScored.head._2)
        }
      }

      GetResult(r => V2DByStudy(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<,
        toGeneScoreTuple(StrSeqRep(r.<<), DSeqRep(r.<<))))
    }

    implicit val getSumStatsByVariantPheWAS: GetResult[VariantPheWAS] =
      GetResult(r => VariantPheWAS(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<,
        r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<, r.<<?))

  }
}

