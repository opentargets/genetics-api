package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import clickhouse.ClickHouseProfile
import models.Entities._
import sangria.ast.Field

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class Backend @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[ClickHouseProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._

  def findAt(pos: Position) = {
    val founds = sql"""
      |select
      | feature,
      | round(avg(position)) as avg_v_position,
      | uniq(gene_id),
      | uniq(variant_id)
      |from #$v2gTName
      |where chr_id = ${pos.chr_id} and
      | position >= ${pos.position - 1000000} and
      | position <= ${pos.position + 1000000}
      |group by feature
      |order by avg_v_position asc
     """.stripMargin.as[V2GRegionSummary]

    db.run(founds.asTry)
  }

  def summaryAt(pos: Position) = {
    val founds = sql"""
      |select
      | any(index_chr_id) as index_chr_id,
      | any(index_position) as index_position,
      | any(index_ref_allele) as index_ref_allele,
      | any(index_alt_allele) as index_alt_allele,
      | uniq(gene_id) as uniq_genes,
      | uniq(variant_id) as uniq_tag_variants,
      | count() as count_evs
      |from #$d2v2gTName
      |where
      | chr_id = ${pos.chr_id} and
      | position >= ${pos.position - 1000000} and
      | position <= ${pos.position + 1000000}
      |group by index_variant_id
      |order by index_position asc
    """.stripMargin.as[D2V2GRegionSummary]

    db.run(founds.asTry)
  }

  def manhattanTable(studyID: String, fields: Vector[Field]) = {
    // TODO use fields to optimise the requested query
    val idxVariants = sql"""
      |select
      | index_variant_id,
      | any(index_rs_id),
      | any(index_chr_id),
      | any(index_position),
      | any(index_ref_allele),
      | any(index_alt_allele),
      | any(pval)
      |from #$v2dByStTName
      |where stid = $studyID
      |group by index_variant_id
      """.stripMargin.as[ManhattanRow]

    // map to proper manhattan association with needed fields
    db.run(idxVariants.asTry).map {
      case Success(v) => v.map(el => ManhattanAssoc(el.index_variant_id, el.index_rs_id,
        el.pval, el.index_chr_id, el.index_position, List.empty, None, None))
      case Failure(ex) => Vector.empty
    }
  }

  private val v2dByStTName: String = "v2d_by_stchr"
  private val d2v2gTName: String = "d2v2g"
  private val v2gTName: String = "v2g"
}