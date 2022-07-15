package models.implicits

import components.clickhouse.rep.SeqRep.{DSeqRep, StrSeqRep}
import models.entities.DNA.SimpleVariant
import models.entities.Entities._
import slick.jdbc.{GetResult, PositionedResult}

import scala.collection.breakOut

object DbImplicits {

  import components.clickhouse.rep.SeqRep.Implicits._

  implicit val getSLGRow: GetResult[SLGRow] =
    GetResult(r => SLGRow(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  implicit val getV2DL2GRowByGene: GetResult[V2DL2GRowByGene] =
    GetResult { r =>
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

    }

  implicit val GetV2DStructure: Any with GetResult[V2GStructureRow] =
    GetResult(r => V2GStructureRow(typeId = r.<<, sourceId = r.<<, bioFeatureSet = StrSeqRep(r.<<)))

}
