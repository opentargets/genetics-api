package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import clickhouse.ClickHouseProfile
import models.Entities._
import models.Functions._
import models.Entities.Prefs._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scala.concurrent._

class Backend @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[ClickHouseProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._

  def findAt(pos: DNAPosition) = {
    val founds = sql"""
      |select
      | feature,
      | round(avg(position)) as avg_v_position,
      | uniq(gene_id),
      | uniq(variant_id)
      |from #$v2gTName
      |where chr_id = ${pos.chrId} and
      | position >= ${pos.position - 1000000} and
      | position <= ${pos.position + 1000000}
      |group by feature
      |order by avg_v_position asc
     """.stripMargin.as[V2GRegionSummary]

    db.run(founds.asTry)
  }

  def summaryAt(pos: DNAPosition) = {
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
      | chr_id = ${pos.chrId} and
      | position >= ${pos.position - 1000000} and
      | position <= ${pos.position + 1000000}
      |group by index_variant_id
      |order by index_position asc
    """.stripMargin.as[D2V2GRegionSummary]

    db.run(founds.asTry)
  }

  def buildPheWASTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]) = {
    // select trait_reported, stid, any(pval), any(n_initial), any(n_replication)
    // from ot.v2d_by_chrpos prewhere chr_id = '6' and variant_id = '6_88310327_G_A'
    // group by trait_reported, stid
    val limitClause = parsePaginationTokens(pageIndex, pageSize)
    val variant = Variant(variantID)

    variant match {
      case Success(Some(v)) => {
        val query =
          sql"""
               |select
               | trait_reported,
               | stid,
               | any(pval),
               | any(n_initial),
               | any(n_replication)
               |from #$v2dByChrPosTName
               |prewhere chr_id = ${v.locus.chrId} and
               |  position = ${v.locus.position} and
               |  variant_id = ${v.id}
               |group by trait_reported, stid
               |order by trait_reported asc
               |#$limitClause
         """.stripMargin.as[V2DByVariantPheWAS]

        db.run(query.asTry).map {
          case Success(v) => PheWASTable(
            associations = v.map(el => {
              PheWASAssociation(el.stid, el.traitReported, Option.empty, el.pval, 0,
                el.nInitial + el.nRepeated, 0)
            })
          )
          case _ =>
            PheWASTable(associations = Vector.empty)
        }
      }
      case _ =>
        Future {
          PheWASTable(associations = Vector.empty)
        }
    }
  }

  def getStudyInfo(studyID: String) = {
    val studyQ = sql"""
                 |select
                 | stid,
                 | trait_code,
                 | trait_reported,
                 | trait_efos,
                 | pmid,
                 | pub_date,
                 | pub_journal,
                 | pub_title,
                 | pub_author
                 |from #$studiesFullTName
                 |where stid = $studyID
      """.stripMargin.as[Study]

    db.run(studyQ.asTry).map {
      case Success(v) => if (v.length > 0) Some(v(0)) else None
      case Failure(_) => None
    }
  }

  def buildManhattanTable(studyID: String, pageIndex: Option[Int], pageSize: Option[Int]) = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)

    val idxVariants = sql"""
      |SELECT
      |    index_variant_id,
      |    index_rs_id,
      |    pval,
      |    credibleSetSize,
      |    ldSetSize,
      |    uniq_variants,
      |    top_genes_ids,
      |    top_genes_scores
      |FROM
      |(
      |    SELECT
      |        index_variant_id,
      |        any(index_rs_id) AS index_rs_id,
      |        any(pval) AS pval,
      |        uniqIf(variant_id, posterior_prob > 0) AS credibleSetSize,
      |        uniqIf(variant_id, r2 > 0) AS ldSetSize,
      |        uniq(variant_id) AS uniq_variants
      |    FROM #$v2dFullByStTName
      |    PREWHERE stid = $studyID
      |    GROUP BY index_variant_id
      |)
      |ALL LEFT OUTER JOIN
      |(
      |    SELECT
      |        variant_id AS index_variant_id,
      |        groupArray(gene_id) AS top_genes_ids,
      |        groupArray(overall_score) AS top_genes_scores
      |    FROM ot.d2v2g_score_by_overall
      |    PREWHERE (variant_id = index_variant_id) AND (overall_score > 0.)
      |    GROUP BY variant_id
      |) USING (index_variant_id)
      |#$limitClause
      """.stripMargin.as[V2DByStudy]

    // map to proper manhattan association with needed fields
    db.run(idxVariants.asTry).map {
      case Success(v) => ManhattanTable(
        v.map(el => {
          // we got the line so correct variant must exist
          val variant: Try[Option[Variant]] = el.index_variant_id
          val completedV = variant.map(_.map(v => Variant(v.locus, v.refAllele, v.altAllele, el.index_rs_id)))

          ManhattanAssociation(completedV.get.get, el.pval, el.topGenes,
            el.credibleSetSize, el.ldSetSize, el.totalSetSize)
        })
      )
      case Failure(ex) => ManhattanTable(associations = Vector.empty)
    }
  }

  def buildIndexVariantAssocTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]) = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)
    val variant: Try[Option[Variant]] = variantID

    variant match {
      case Success(Some(v)) =>
        val assocs = sql"""
                       |select
                       | chr_id,
                       | position,
                       | ref_allele,
                       | alt_allele,
                       | rs_id,
                       | stid,
                       | trait_code,
                       | trait_reported,
                       | trait_efos,
                       | pmid,
                       | pub_date,
                       | pub_journal,
                       | pub_title,
                       | pub_author,
                       | pval,
                       | ifNull(n_initial,0) + ifNull(n_replication,0),
                       | ifNull(n_cases, 0),
                       | r2,
                       | afr_1000g_prop,
                       | amr_1000g_prop,
                       | eas_1000g_prop,
                       | eur_1000g_prop,
                       | sas_1000g_prop,
                       | log10_abf,
                       | posterior_prob
                       |from #$v2dByChrPosTName
                       |prewhere
                       |  chr_id = ${v.locus.chrId} and
                       |  index_position = ${v.locus.position} and
                       |  index_ref_allele = ${v.refAllele} and
                       |  index_alt_allele = ${v.altAllele}
                       |#$limitClause
          """.stripMargin.as[IndexVariantAssociation]

        // map to proper manhattan association with needed fields
        val ret = db.run(assocs.asTry).map {
          case Success(r) => Entities.IndexVariantTable(r)
          case Failure(ex) => Entities.IndexVariantTable(associations = Vector.empty)
        }
        ret
      case _ => Future.successful(Entities.IndexVariantTable(associations = Vector.empty))
    }
  }

  private val v2dByStTName: String = "v2d_by_stchr"
  private val v2dByChrPosTName: String = "v2d_by_chrpos"
  private val d2v2gTName: String = "d2v2g"
  private val v2gTName: String = "v2g"
  private val studiesTName: String = "studies"
  private val v2dFullByStTName: String = "v2df_by_stchr"
  private val studiesFullTName: String = "studiesf"
}