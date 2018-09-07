package models

import slick.jdbc.GetResult

import scala.util.Try
import scala.collection.breakOut
import models.Functions._

object Entities {

  case class DNAPosition(chrId: String, position: Long)
  case class Variant(locus: DNAPosition, refAllele: String, altAllele: String, rsId: Option[String]) {
    lazy val id: String = List(locus.chrId, locus.position.toString, refAllele, altAllele).map(_.toUpperCase).mkString("_")
  }

  object Variant {
    def apply(variantId: String, rsId: Option[String] = None): Try[Option[Variant]] = {
      Try {
        variantId.toUpperCase.split("_").toList match {
          case List(chr: String, pos: String, ref: String, alt: String) =>
            Some(Variant(DNAPosition(chr, pos.toLong), ref, alt, rsId))
          case _ => None
        }
      }
    }
  }

  case class Gene(id: String, symbol: Option[String] = None, start: Option[Long] = None, end: Option[Long] = None,
                  chromosome: Option[String] = None, tss: Option[Long] = None,
                  bioType: Option[String] = None, fwd: Option[Boolean] = None, exons: Seq[Long] = Seq.empty)

  case class PheWASTable(associations: Vector[PheWASAssociation])
  case class PheWASAssociation(studyId: String, traitReported: String, traitId: Option[String],
                               pval: Double, beta: Double, nTotal: Long, nCases: Long)

  case class TagVariantTable(associations: Vector[TagVariantAssociation])
  case class TagVariantAssociation(indexVariant: Variant,
                                     study: Study,
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
                                     study: Study,
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

  case class ManhattanTable(associations: Vector[ManhattanAssociation])
  case class ManhattanAssociation(variant: Variant, pval: Double,
                                  bestGenes: Seq[(Gene, Double)], crediblbeSetSize: Long,
                                  ldSetSize: Long, totalSetSize: Long)

  case class V2GRegionSummary(feature: String, avg_position: Long, uniq_genes: Long, uniq_variants: Long)


  case class D2V2GRegionSummary(index_chr_id: String, index_position: Long, index_ref_allele: String,
                                index_alt_allele: String, uniq_genes: Long, uniq_tag_variants: Long,
                                count_evs: Long)

  case class V2DByStudy(index_variant_id: String, index_rs_id: Option[String], pval: Double,
                        credibleSetSize: Long, ldSetSize: Long, totalSetSize: Long, topGenes: Seq[(Gene, Double)])

  case class StudyInfo(study: Option[Study])
  case class Study(studyId: String, traitCode: String, traitReported: String, traitEfos: Seq[String],
                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
                   pubAuthor: Option[String])

  case class V2DByVariantPheWAS(traitReported: String, stid: String, pval: Double, nInitial: Long, nRepeated: Long)

  case class GeneTagVariant(geneId: String, tagVariantId: String, overallScore: Double)
  case class TagVariantIndexVariantStudy(tagVariantId: String, indexVariantId: String, studyId: String,
                                         r2: Option[Double], pval: Double, posteriorProb: Option[Double])
  case class Gecko(genes: Seq[Gene], tagVariants: Seq[Variant], indexVariants: Seq[Variant],
                   studies: Seq[Study], geneTagVariants: Seq[GeneTagVariant],
                   tagVariantIndexVariantStudies: Seq[TagVariantIndexVariantStudy])
  case class GeckoLine(gene: Gene, tagVariant: Variant, indexVariant: Variant, study: Study,
                       geneTagVariant: GeneTagVariant, tagVariantIndexVariantStudy: TagVariantIndexVariantStudy)

  object Gecko {
    def apply(geckoLines: Seq[GeckoLine]): Option[Gecko] = {
      if (geckoLines.isEmpty)
        Some(Gecko(Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty))
      else {
        var genes: Set[Gene] = Set.empty
        var tagVariants: Set[Variant] = Set.empty
        var indexVariants: Set[Variant] = Set.empty
        var studies: Set[Study] = Set.empty
        var tagVariantIndexVariantStudies: Set[TagVariantIndexVariantStudy] = Set.empty

        var geneTagVariants: Set[GeneTagVariant] = Set.empty
        geckoLines.foreach(line => {
          genes += line.gene
          tagVariants += line.tagVariant
          indexVariants += line.indexVariant
          studies += line.study
          tagVariantIndexVariantStudies += line.tagVariantIndexVariantStudy
        })

        // breakOut could be a good way to map virtually to a other collection of a different type
        // https://stackoverflow.com/questions/46509951/how-do-i-efficiently-count-distinct-fields-in-a-collection
        // val genes = geckoLines.map(_.gene)(breakOut).toSet.toSeq
         Some(Gecko(genes.toStream, tagVariants.toStream, indexVariants.toStream, studies.toStream,
                  geneTagVariants.toStream, tagVariantIndexVariantStudies.toStream))
      }
    }
  }

  case class Tissue(id: String) {
    val name: Option[String] = Option(id.replace("_", " ").toLowerCase.capitalize)
  }

  case class G2VSchemaElement(id: String, sourceId: String, tissues: Seq[Tissue])

  case class G2VSchema(qtls: Seq[G2VSchemaElement], intervals: Seq[G2VSchemaElement],
                       functionalPredictions: Seq[G2VSchemaElement])

  case class G2VAssociation(gene: Gene, overallScore: Double, qtls: Seq[G2VElement[QTLTissue]],
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
      val gene = groupedGene.head.gene
      val score = groupedGene.head.overallScore
      val grouped = groupedGene.view.groupBy(r => (r.typeId, r.sourceId))

      val qtls = grouped.filterKeys(k => defaultQtlTypes.contains(k._1))
        .mapValues(p => {
          val tp = p.head.typeId
          val sc = p.head.sourceId
          G2VElement[QTLTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toQtlTissues(p))
        }).values.toStream

      val intervals = grouped.filterKeys(k => defaultIntervalTypes.contains(k._1))
        .mapValues(p => {
          val tp = p.head.typeId
          val sc = p.head.sourceId
          G2VElement[IntervalTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toIntervalTissues(p))
        }).values.toStream

      val fpreds = grouped.filterKeys(k => defaultFPredTypes.contains(k._1))
        .mapValues(p => {
          val tp = p.head.typeId
          val sc = p.head.sourceId
          G2VElement[FPredTissue](p.head.typeId, p.head.sourceId, None,
            p.head.sourceScores(sc), toFPredTissues(p))
        }).values.toStream

      G2VAssociation(gene, score, qtls, intervals, fpreds)
    }
  }

  case class G2VElement[T](id: String, sourceId: String, name: Option[String], aggregatedScore: Double,
                           tissues: Seq[T])

  case class QTLTissue(tissue: Tissue, quantile: Double, beta: Option[Double], pval: Option[Double])
  case class IntervalTissue(tissue: Tissue, quantile: Double, score: Option[Double])
  case class FPredTissue(tissue: Tissue, maxEffectLabel: Option[String], maxEffectScore: Option[Double])

  case class ScoredG2VLine(gene: Gene, overallScore: Double, sourceScores: Map[String, Double],
                           typeId: String, sourceId: String, feature: String, fpredMaxLabel: Option[String],
                           fpredMaxScore: Option[Double], qtlBeta: Option[Double], qtlSE: Option[Double],
                           qtlPval: Option[Double], intervalScore: Option[Double], qtlScoreQ: Double,
                           intervalScoreQ: Double)

  object Prefs {
    implicit def stringToVariant(variantID: String): Try[Option[Variant]] = Variant.apply(variantID)

    implicit val getV2GRegionSummary: GetResult[V2GRegionSummary] =
      GetResult(r => V2GRegionSummary(r.<<, r.<<, r.<<, r.<<))

    implicit val getV2DByStudy: GetResult[V2DByStudy] = {
      def toGeneScoreTuple(geneIds: Seq[String], geneNames: Seq[String],
                           geneScores: Seq[Double]) = {
        val ordScored = ((geneIds zip geneNames).map(t => Gene(id = t._1, symbol = Some(t._2))) zip geneScores)
          .sortBy(_._2)(Ordering[Double].reverse)

        if (ordScored.isEmpty) ordScored
        else {
          ordScored.takeWhile(_._2 == ordScored.head._2)
        }
      }

      GetResult(r => V2DByStudy(r.<<, r.<<?, r.<<, r.<<, r.<<, r.<<,
        toGeneScoreTuple(toSeqString(r.<<), toSeqString(r.<<), toSeqDouble(r.<<))))
    }

    implicit val getV2DByVariantPheWAS: GetResult[V2DByVariantPheWAS] =
      GetResult(r => V2DByVariantPheWAS(r.<<, r.<<, r.<<, r.<<, r.<<))

    implicit val getD2V2GRegionSummary: GetResult[D2V2GRegionSummary] =
      GetResult(r => D2V2GRegionSummary(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

    implicit val getStudy: GetResult[Study] =
      GetResult(r => Study(r.<<, r.<<, r.<<, toSeqString(r.<<), r.<<?, r.<<?, r.<<?, r.<<?, r.<<?))

    implicit val getIndexVariantAssoc: GetResult[IndexVariantAssociation] = GetResult(
      r => {
        val variant = Variant(r.<<, r.<<?)
        val study = Study(r.<<, r.<<, r.<<, toSeqString(r.<<), r.<<?, r.<<?, r.<<?, r.<<?, r.<<?)
        IndexVariantAssociation(variant.get.get, study,
          r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?)
      }
    )

    implicit val getTagVariantAssoc: GetResult[TagVariantAssociation] = GetResult(
      r => {
        val variant = Variant(r.<<, r.<<?)
        val study = Study(r.<<, r.<<, r.<<, toSeqString(r.<<), r.<<?, r.<<?, r.<<?, r.<<?, r.<<?)
        TagVariantAssociation(variant.get.get, study,
          r.<<, r.<<, r.<<, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?)
      }
    )

    implicit val getGeckoLine: GetResult[GeckoLine] = GetResult(
      r => {
        val tagVariant = Variant(r.<<, r.<<?).get.get
        val indexVariant = Variant(r.<<, r.<<?).get.get

        val gene = Gene(id = r.nextString(), symbol = r.nextStringOption(), bioType = r.nextStringOption(),
          chromosome = r.nextStringOption(), tss = r.nextLongOption(),
          start = r.nextLongOption(), end = r.nextLongOption(), fwd = r.nextBooleanOption(),
          exons = toSeqLong(r.nextString()))

        val study = Study(studyId = r.<<, pubId = r.<<?, pubDate = r.<<?, pubJournal = r.<<?,
          pubTitle = r.<<?, pubAuthor = r.<<?, traitReported = r.<<, traitEfos = toSeqString(r.<<),
          traitCode = r.<<)

        val r2 = r.nextDoubleOption()
        val posteriorProb = r.nextDoubleOption()
        val pval = r.nextDouble()
        val overallScore = r.nextDouble()

        val geneTagVariant = GeneTagVariant(gene.id, tagVariant.id, overallScore)
        val tagVariantIndexVariantStudy = TagVariantIndexVariantStudy(tagVariant.id, indexVariant.id,
          study.studyId, r2, pval, posteriorProb)

        GeckoLine(gene, tagVariant, indexVariant, study, geneTagVariant, tagVariantIndexVariantStudy)
      }
    )

    implicit val getScoredG2VLine: GetResult[ScoredG2VLine] = GetResult(
      r => {
        val gene = Gene(r.<<)
        ScoredG2VLine(gene, r.<<, (
          toSeqString(r.nextString()) zip toSeqDouble(r.nextString())).toMap, r.<<, r.<<, r.<<,
          r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<?, r.<<, r.<<)
      }
    )
  }
}
