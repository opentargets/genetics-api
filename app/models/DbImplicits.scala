package models

import clickhouse.rep.SeqRep.{DSeqRep, StrSeqRep}
import models.DNA.SimpleVariant
import models.Entities.{SLGRow, V2DBeta, V2DByStudy, V2DL2GRowByGene, V2DOdds}
import slick.jdbc.GetResult

import scala.collection.breakOut

object DbImplicits {

  import clickhouse.rep.SeqRep.Implicits._

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

      V2DL2GRowByGene(
        studyId,
        svID,
        odds,
        beta,
        pval,
        pvalExponent,
        pvalMantissa,
        yProbaLogiDistance,
        yProbaLogiInteraction,
        yProbaLogiMolecularQTL,
        yProbaLogiPathogenicity,
        yProbaFullModel
      )

    })
  }

  implicit val getV2DByStudy: GetResult[V2DByStudy] = {
    def toGeneScoreTupleWithDuplicates(
                                        geneIds: Seq[String],
                                        geneScores: Seq[Double]
                                      ): Seq[(String, Double)] = {
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
      val totalSize: Long = r.<<
      val aggTop10RawIds = StrSeqRep(r.<<)
      val aggTop10RawScores = DSeqRep(r.<<)
      val aggTop10ColocIds = StrSeqRep(r.<<)
      val aggTop10ColocScores = DSeqRep(r.<<)
      val aggTop10L2GIds = StrSeqRep(r.<<)
      val aggTop10L2GScores = DSeqRep(r.<<)

      V2DByStudy(
        studyId,
        svID,
        pval,
        pvalMantissa,
        pvalExponent,
        odds,
        beta,
        credSetSize,
        ldSetSize,
        totalSize,
        toGeneScoreTupleWithDuplicates(aggTop10RawIds, aggTop10RawScores),
        toGeneScoreTuple(aggTop10ColocIds, aggTop10ColocScores),
        toGeneScoreTuple(aggTop10L2GIds, aggTop10L2GScores)
      )
    })
  }
}
