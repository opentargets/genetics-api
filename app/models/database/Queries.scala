package models.database

import models.entities.{DNA, Entities}
import models.entities.DNA.Variant

object Queries extends GeneticsDbTables {

  import components.clickhouse.ClickHouseProfile.api._

  def geneInRegion(chromosome: String, start: Long, end: Long): Query[FRM.Genes, DNA.Gene, Seq] =
    genes
      .filter(
        r =>
          (r.chromosome === chromosome) &&
            (r.start >= start) && (r.end <= end))

  def geneIdByRegion(chromosome: String, start: Long, end: Long): Query[Rep[String], String, Seq] =
    geneInRegion(chromosome, start, end).map(_.id)

  def credibleSetByChrPosRefAltStdId(v: Variant, studyId: String): Query[FRM.CredSet, Entities.CredSetRow, Seq] = credsets.filter(r =>
      (r.leadChromosome === v.chromosome) &&
      (r.leadPosition === v.position) &&
      (r.leadRefAllele === v.refAllele) &&
      (r.leadAltAllele === v.altAllele) &&
      (r.studyId === studyId)
  )

  def colocOnVariantAndStudy(v: Variant, studyId: String): Query[FRM.Coloc, Entities.ColocRow, Seq] = colocs
      .filter(
        r =>
          (r.lChrom === v.chromosome) &&
            (r.lPos === v.position) &&
            (r.lRef === v.refAllele) &&
            (r.lAlt === v.altAllele) &&
            (r.lStudy === studyId))

}
