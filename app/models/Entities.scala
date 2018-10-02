package models

import com.sksamuel.elastic4s.{Hit, HitReader}
import slick.jdbc.GetResult
import models.Violations._
import models.Functions._
import models.DNA._
import sangria.execution.deferred.HasId
import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._

object Entities {


  case class OverlapRow(stid: String, numOverlapLoci: Int)
  case class OverlappedLociStudy(studyId: String, topOverlappedStudies: IndexedSeq[OverlapRow])

  case class OverlappedVariantsStudy(studyId: String, overlaps: Seq[OverlappedVariant])
  case class OverlappedVariant(variantIdA: String, variantIdB: String, overlapAB: Int,
                               distinctA: Int, distinctB: Int)



  case class TagVariantTable(associations: Vector[TagVariantAssociation])
  case class TagVariantAssociation(indexVariant: Variant,
                                     studyId: String,
                                     pval: Double,
                                     nTotal: Int, // n_initial + n_replication which could be null as well both fields
                                     nCases: Int,
                                     r2: Option[Double],
                                     afr1000GProp: Option[Double],
                                     amr1000GProp: Option[Double],
                                     eas1000GProp: Option[Double],
                                     eur1000GProp: Option[Double],
                                     sas1000GProp: Option[Double],
                                     log10Abf: Option[Double],
                                     posteriorProbability: Option[Double])


  case class IndexVariantTable(associations: Vector[IndexVariantAssociation])
  case class IndexVariantAssociation(tagVariant: Variant,
                                     studyId: String,
                                     pval: Double,
                                     nTotal: Int, // n_initial + n_replication which could be null as well both fields
                                     nCases: Int,
                                     r2: Option[Double],
                                     afr1000GProp: Option[Double],
                                     amr1000GProp: Option[Double],
                                     eas1000GProp: Option[Double],
                                     eur1000GProp: Option[Double],
                                     sas1000GProp: Option[Double],
                                     log10Abf: Option[Double],
                                     posteriorProbability: Option[Double])

  case class ManhattanTable(studyId: String, associations: Vector[ManhattanAssociation])
  case class ManhattanAssociation(variantId: String, pval: Double,
                                  bestGenes: Seq[(String, Double)], crediblbeSetSize: Option[Long],
                                  ldSetSize: Option[Long], totalSetSize: Long)

  case class V2DByStudy(index_variant_id: String, pval: Double,
                        credibleSetSize: Option[Long], ldSetSize: Option[Long], totalSetSize: Long, topGenes: Seq[(String, Double)])

  case class StudyInfo(study: Option[Study])

  object Study {
    implicit val hasId = HasId[Study, String](_.studyId)
  }

  case class Study(studyId: String, traitCode: String, traitReported: String, traitEfos: Seq[String],
                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
                   pubAuthor: Option[String], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
                   nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
                   traitCategory: Option[String])

  case class PheWASTable(associations: Vector[VariantPheWAS])
  case class VariantPheWAS(stid: String, pval: Double, beta: Double, se: Double, eaf: Double, maf: Double,
                           nSamplesVariant: Option[Long], nSamplesStudy: Option[Long], nCasesStudy: Option[Long],
                           nCasesVariant: Option[Long], oddRatio: Option[Double], chip: String, info: Option[Double])

  case class GeneTagVariant(geneId: String, tagVariantId: String, overallScore: Double)
  case class TagVariantIndexVariantStudy(tagVariantId: String, indexVariantId: String, studyId: String,
                                         r2: Option[Double], pval: Double, posteriorProb: Option[Double])
  case class Gecko(geneIds: Seq[String], tagVariants: Seq[Variant], indexVariants: Seq[Variant],
                   studies: Seq[String], geneTagVariants: Seq[GeneTagVariant],
                   tagVariantIndexVariantStudies: Seq[TagVariantIndexVariantStudy])
  case class GeckoLine(geneId: String, tagVariant: Variant, indexVariant: Variant, studyId: String,
                       geneTagVariant: GeneTagVariant, tagVariantIndexVariantStudy: TagVariantIndexVariantStudy)

  object Gecko {
    def apply(geckoLines: Seq[GeckoLine], geneIdsInLoci: Set[String] = Set.empty): Option[Gecko] = {
      if (geckoLines.isEmpty)
        Some(Gecko(Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty))
      else {
        var geneIds: Set[String] = Set.empty
        var tagVariants: Set[Variant] = Set.empty
        var indexVariants: Set[Variant] = Set.empty
        var studies: Set[String] = Set.empty
        var tagVariantIndexVariantStudies: Set[TagVariantIndexVariantStudy] = Set.empty
        var geneTagVariants: Set[GeneTagVariant] = Set.empty

        geckoLines.foreach(line => {
          geneIds += line.geneId
          tagVariants += line.tagVariant
          indexVariants += line.indexVariant
          studies += line.studyId
          geneTagVariants += line.geneTagVariant
          tagVariantIndexVariantStudies += line.tagVariantIndexVariantStudy
        })

        // breakOut could be a good way to map virtually to a other collection of a different type
        // https://stackoverflow.com/questions/46509951/how-do-i-efficiently-count-distinct-fields-in-a-collection
        // val genes = geckoLines.map(_.gene)(breakOut).toSet.toSeq
         Some(Gecko((geneIds union geneIdsInLoci).toSeq, tagVariants.toSeq, indexVariants.toSeq, studies.toSeq,
                  geneTagVariants.toSeq, tagVariantIndexVariantStudies.toSeq))
      }
    }
  }

  case class VariantSearchResult (variant: Variant, relatedGenes: Seq[String])

  case class SearchResultSet(totalGenes: Long, genes: Seq[Gene],
                             totalVariants: Long, variants: Seq[VariantSearchResult],
                             totalStudies: Long, studies: Seq[Study])

  case class Tissue(id: String) {
    lazy val name: Option[String] = Option(id.replace("_", " ").toLowerCase.capitalize)
  }

  case class G2VSchemaElement(id: String, sourceId: String, tissues: Seq[Tissue])

  case class G2VSchema(qtls: Seq[G2VSchemaElement], intervals: Seq[G2VSchemaElement],
                       functionalPredictions: Seq[G2VSchemaElement])

  case class G2VAssociation(geneId: String, variantId: String, overallScore: Double, qtls: Seq[G2VElement[QTLTissue]],
                            intervals: Seq[G2VElement[IntervalTissue]], fpreds: Seq[G2VElement[FPredTissue]])

  object G2VAssociation {
    def toQtlTissues(scoreds: Seq[ScoredG2VLine]): Seq[QTLTissue] =
      scoreds.map(el =>
        QTLTissue(Tissue(el.feature), el.qtlScoreQ, el.qtlBeta, el.qtlPval))

    def toIntervalTissues(scoreds: Seq[ScoredG2VLine]): Seq[IntervalTissue] =
      scoreds.map(el =>
        Entities.IntervalTissue(Tissue(el.feature), el.intervalScoreQ, el.intervalScore))

    def toFPredTissues(scoreds: Seq[ScoredG2VLine]): Seq[FPredTissue] =
      scoreds.map(el =>
        Entities.FPredTissue(Tissue(el.feature), el.fpredMaxLabel, el.fpredMaxScore))

    def apply(groupedGene: Seq[ScoredG2VLine]): G2VAssociation = {
      val geneId = groupedGene.head.geneId
      val variantId = groupedGene.head.variantId
      val score = groupedGene.head.overallScore
      val grouped = groupedGene.view.groupBy(r => (r.typeId, r.sourceId))

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

      G2VAssociation(geneId, variantId, score, qtls, intervals, fpreds)
    }
  }

  case class G2VElement[T](id: String, sourceId: String, name: Option[String], aggregatedScore: Double,
                           tissues: Seq[T])

  case class QTLTissue(tissue: Tissue, quantile: Double, beta: Option[Double], pval: Option[Double])
  case class IntervalTissue(tissue: Tissue, quantile: Double, score: Option[Double])
  case class FPredTissue(tissue: Tissue, maxEffectLabel: Option[String], maxEffectScore: Option[Double])

  case class ScoredG2VLine(geneId: String, variantId: String, overallScore: Double, sourceScores: Map[String, Double],
                           typeId: String, sourceId: String, feature: String, fpredMaxLabel: Option[String],
                           fpredMaxScore: Option[Double], qtlBeta: Option[Double], qtlSE: Option[Double],
                           qtlPval: Option[Double], intervalScore: Option[Double], qtlScoreQ: Double,
                           intervalScoreQ: Double)

  object ESImplicits {
    implicit object GeneHitReader extends HitReader[Gene] {
      override def read(hit: Hit): Either[Throwable, Gene] = {
        if (hit.isSourceEmpty) Left(new NoSuchFieldError("source object is empty"))
        else {
          val mv = hit.sourceAsMap

          Right(Gene(mv("gene_id").toString,
            Option(mv("gene_name").asInstanceOf[String]),
            Option(mv("start").asInstanceOf[Int]),
            Option(mv("end").asInstanceOf[Int]),
            Option(mv("chr").toString),
            Option(mv("tss").asInstanceOf[Int]),
            Option(mv("biotype").asInstanceOf[String]),
            Option(mv("fwdstrand").asInstanceOf[Int] match {
              case 0 => false
              case 1 => true
              case _ => false
            })
            )
          )
        }
      }
    }

    implicit object VariantHitReader extends HitReader[VariantSearchResult] {
      override def read(hit: Hit): Either[Throwable, VariantSearchResult] = {
        if (hit.isSourceEmpty) Left(new NoSuchFieldError("source object is empty"))
        else {
          val mv = hit.sourceAsMap

          val variant = Variant(Position(mv("chr_id").toString, mv("position").asInstanceOf[Int]),
            mv("ref_allele").toString, mv("alt_allele").toString, Option(mv("rs_id").toString))
          val relatedGenes = mv("gene_set_ids").asInstanceOf[Seq[String]]

          Right(VariantSearchResult(variant, relatedGenes))
        }
      }
    }

    implicit object StudyHitReader extends HitReader[Study] {
      override def read(hit: Hit): Either[Throwable, Study] = {
        if (hit.isSourceEmpty) Left(new NoSuchFieldError("source object is empty"))
        else {
          val mv = hit.sourceAsMap

          Right(Study(mv("study_id").toString,
            mv.get("trait_code").map(_.toString).get,
            mv.get("trait_reported").map(_.toString).get,
            mv.get("trait_efos").map(_.asInstanceOf[Seq[String]]).get,
            mv.get("pmid").map(_.asInstanceOf[String]),
            mv.get("pub_date").map(_.asInstanceOf[String]),
            mv.get("pub_journal").map(_.asInstanceOf[String]),
            mv.get("pub_title").map(_.asInstanceOf[String]),
            mv.get("pub_author").map(_.asInstanceOf[String]),
            mv.get("ancestry_initial").map(_.asInstanceOf[Seq[String]]).get,
            mv.get("ancestry_replication").map(_.asInstanceOf[Seq[String]]).get,
            mv.get("n_initial").map(_.asInstanceOf[Int].toLong),
            mv.get("n_replication").map(_.asInstanceOf[Int].toLong),
            mv.get("n_cases").map(_.asInstanceOf[Int].toLong),
            mv.get("trait_category").map(_.asInstanceOf[String]))
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

      GetResult(r => V2DByStudy(r.<<, r.<<, r.<<?, r.<<?, r.<<,
        toGeneScoreTuple(StrSeqRep(r.<<), DSeqRep(r.<<))))
    }

    implicit val getSumStatsByVariantPheWAS: GetResult[VariantPheWAS] =
      GetResult(r => VariantPheWAS(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<,
        r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<, r.<<?))

    implicit val getStudy: GetResult[Study] =
      GetResult(r => Study(r.<<, r.<<, r.<<, StrSeqRep(r.<<), r.<<?, r.<<?, r.<<?, r.<<?, r.<<?,
        StrSeqRep(r.<<), StrSeqRep(r.<<), r.<<?, r.<<?, r.<<?, r.<<?))

    implicit val getIndexVariantAssoc: GetResult[IndexVariantAssociation] = GetResult(
      r => {
        val variant = Variant(r.<<, r.<<?)
        IndexVariantAssociation(variant.right.get, r.<<,
          r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?)
      }
    )

    implicit val getTagVariantAssoc: GetResult[TagVariantAssociation] = GetResult(
      r => {
        val variant = Variant(r.<<, r.<<?)
        TagVariantAssociation(variant.right.get, r.<<,
          r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?)
      }
    )

    implicit val getGeckoLine: GetResult[GeckoLine] = GetResult(
      r => {
        val tagVariant = Variant(r.<<, r.<<?).right.get
        val indexVariant = Variant(r.<<, r.<<?).right.get

        val geneId = r.nextString()
        val studyId: String = r.<<

        val r2 = r.nextDoubleOption()
        val posteriorProb = r.nextDoubleOption()
        val pval = r.nextDouble()
        val overallScore = r.nextDouble()

        val geneTagVariant = GeneTagVariant(geneId, tagVariant.id, overallScore)
        val tagVariantIndexVariantStudy = TagVariantIndexVariantStudy(tagVariant.id, indexVariant.id,
          studyId, r2, pval, posteriorProb)

        GeckoLine(geneId, tagVariant, indexVariant, studyId, geneTagVariant, tagVariantIndexVariantStudy)
      }
    )

    implicit val getScoredG2VLine: GetResult[ScoredG2VLine] = GetResult(
      r => {
        ScoredG2VLine(r.<<, r.<<, r.<<,
          (StrSeqRep(r.nextString()).rep zip DSeqRep(r.nextString()).rep).toMap,
          r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<, r.<<)
      }
    )
  }
}
