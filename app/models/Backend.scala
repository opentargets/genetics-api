package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import clickhouse.ClickHouseProfile
import models.Entities._
import sangria.ast.Field
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax.HNil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class Backend @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[ClickHouseProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._

  def findAt(pos: Position) = {
    val founds = sql"""
      select
        feature,
        round(avg(position)) as avg_v_position,
        uniq(gene_id),
        uniq(variant_id)
      from #$v2gTName
      where chr_id = ${pos.chr_id} and
        position >= ${pos.position - 1000000} and
        position <= ${pos.position + 1000000}
      group by feature
      order by avg_v_position asc
     """.as[V2GRegionSummary]

    db.run(founds.asTry)
  }

  def summaryAt(pos: Position) = {
    val founds = sql"""
     select
       any(index_chr_id) as index_chr_id,
       any(index_position) as index_position,
       any(index_ref_allele) as index_ref_allele,
       any(index_alt_allele) as index_alt_allele,
       uniq(gene_id) as uniq_genes,
       uniq(variant_id) as uniq_tag_variants,
       count() as count_evs
     from #$d2v2gTName
     where
       chr_id = ${pos.chr_id} and
       position >= ${pos.position - 1000000} and
       position <= ${pos.position + 1000000}
     group by index_variant_id
     order by index_position asc
    """.as[D2V2GRegionSummary]

    db.run(founds.asTry)
  }

  def manhattanTable(studyID: String, fields: Vector[Field]) = {
    // TODO use fields to optimise the requested query
    val idxVariants = sql"""
      |select index_variant_id, any(index_rs_id), any(index_chr_id), any(index_position), any(index_ref_allele),
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

  // private[models] val queryV2G = TableQuery[V2G]
  // private[models] val queryV2DChrPos = TableQuery[V2DChrPos]
  // private[models] val queryV2DStudyChr = TableQuery[V2DStudyChr]
  // private[models] val queryD2V2G = TableQuery[V2G]


//  private[models] class V2DChrPos(tag: Tag) extends V2D(tag, "v2d_by_chrpos")
//  private[models] class V2DStudyChr(tag: Tag) extends V2D(tag, "v2d_by_stid")
//
//  private[models] class V2D(tag: Tag, tName: String) extends Table[V2DEv](tag, tName) {
//    def chr_id = column[String]("chr_id")
//    def position = column[Long]("position")
//    def segment = column[Int]("segment")
//    def ref_allele = column[String]("ref_allele")
//    def alt_allele = column[String]("alt_allele")
//    def variant_id = column[String]("variant_id")
//    def rs_id = column[String]("rs_id")
//    def index_chr_id = column[String]("chr_id")
//    def index_position = column[Long]("position")
//    def index_ref_allele = column[String]("ref_allele")
//    def index_alt_allele = column[String]("alt_allele")
//    def index_variant_id = column[String]("variant_id")
//    def index_rs_id = column[String]("rs_id")
//    def efo_code = column[String]("efo_code")
//    def efo_label = column[String]("efo_label")
//    def r2 = column[Double]("r2")
//    def afr = column[Double]("afr_1000g_prop")
//    def mar = column[Double]("mar_1000g_prop")
//    def eas = column[Double]("eas_1000g_prop")
//    def eur = column[Double]("eur_1000g_prop")
//    def sas = column[Double]("sas_1000g_prop")
//    def log10_abf = column[Double]("log10_abf")
//    def posterior_prob = column[Double]("posterior_prob")
//    def pval = column[Double]("pval")
//    def n_initial = column[Int]("n_initial")
//    def n_replication = column[Int]("n_replication")
//    def trait_reported = column[String]("trait_reported")
//    def ancestry_initial = column[String]("ancestry_initial")
//    def ancestry_replication = column[String]("ancestry_replication")
//    def pmid = column[String]("pmid")
//    def pub_date = column[String]("pub_date")
//    def pub_journal = column[String]("pub_journal")
//    def pub_title = column[String]("pub_title")
//    def pub_author = column[String]("pub_author")
//
//    def projection = (chr_id :: position :: segment :: ref_allele :: alt_allele :: rs_id ::
//      index_chr_id :: index_position :: index_ref_allele :: index_alt_allele :: index_rs_id ::
//      efo_code :: efo_label :: r2.? :: afr.? :: mar.? :: eas.? :: eur.? :: sas.? :: log10_abf.? ::
//      posterior_prob.? :: pval.? :: n_initial.? :: n_replication.? :: trait_reported :: ancestry_initial.? ::
//      ancestry_replication.? :: pmid.? :: pub_date.? :: pub_journal.? ::
//      pub_title.? :: pub_author.? :: HList).toMap[V2DEv]
//
//    def * = projection
//  }

  private[models] class V2G(tag: Tag) extends Table[V2GEv](tag, "v2g") {
    def chr_id = column[String]("chr_id")
    def position = column[Long]("position")
    def segment = column[Int]("segment")
    def ref_allele = column[String]("ref_allele")
    def alt_allele = column[String]("alt_allele")
    def variant_id = column[String]("variant_id")
    def rs_id = column[String]("rs_id")
    def gene_chr = column[String]("gene_chr")
    def gene_id = column[String]("gene_id")
    def gene_start = column[Long]("gene_start")
    def gene_end = column[Long]("gene_end")
    def gene_name = column[String]("gene_name")
    def feature = column[String]("feature")
    def type_id = column[String]("type_id")
    def source_id = column[String]("source_id")
    def csq_counts = column[Long]("csq_counts")
    def qtl_beta = column[Double]("qtl_beta")
    def qtl_se = column[Double]("qtl_se")
    def qtl_pval = column[Double]("qtl_pval")
    def interval_score = column[Double]("interval_score")

    def projection = (chr_id, position, segment, ref_allele, alt_allele, rs_id,
      gene_chr, gene_id, gene_start, gene_end, gene_name, feature, type_id, source_id,
      csq_counts.?, qtl_beta.?, qtl_se.?, qtl_pval.?, interval_score.?) <> (V2GEv.tupled, V2GEv.unapply)

    def * = projection
  }
}

//object V2G extends TableQuery(new V2G(_, "v2g")) {
//  val groupToFeature = this.groupBy(t => (t.type_id, t.source_id, t.feature))
//}