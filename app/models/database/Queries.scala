package models.database

import models.entities.DNA

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

}
