package models.database

object Queries extends GeneticsDbTables {

  import components.clickhouse.ClickHouseProfile.api._

  def geneInRegion(chromosome: String, start: Long, end: Long) =
    genes
      .filter(
        r =>
          (r.chromosome === chromosome) &&
            (r.start >= start) && (r.end <= end))

  def geneIdByRegion(chromosome: String, start: Long, end: Long) =
    geneInRegion(chromosome, start, end).map(_.id)

}
