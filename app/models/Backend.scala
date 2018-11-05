package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import play.api.Configuration
import clickhouse.ClickHouseProfile
import com.sksamuel.elastic4s.{ElasticsearchClientUri, analyzers}
import models.Entities._
import models.Functions._
import models.DNA._
import models.DNA.Implicits._
import models.Entities.DBImplicits._
import models.Entities.ESImplicits._
import models.Violations.{InputParameterCheckError, RegionViolation, SearchStringViolation}
import clickhouse.rep.SeqRep.StrSeqRep
import com.sksamuel.elastic4s.analyzers._
import sangria.validation.Violation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent._
import com.sksamuel.elastic4s.http._
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import play.db.NamedDatabase
import play.api.Logger
import play.api.Environment
import java.nio.file.{Path, Paths}

import slick.jdbc.GetResult


class Backend @Inject()(@NamedDatabase("default") protected val dbConfigProvider: DatabaseConfigProvider,
                        @NamedDatabase("sumstats") protected val dbConfigProviderSumStats: DatabaseConfigProvider,
                        config: Configuration,
                        env: Environment) {
  val dbConfig = dbConfigProvider.get[ClickHouseProfile]
  val dbConfigSumStats = dbConfigProviderSumStats.get[ClickHouseProfile]
  val db = dbConfig.db
  val dbSS = dbConfigSumStats.db
  val logger = Logger(this.getClass)

  val denseRegionsPath: Path = Paths.get(env.rootPath.getAbsolutePath, "resources", "dense_regions.tsv")
  val denseRegionChecker: DenseRegionChecker = DenseRegionChecker(denseRegionsPath.toString)

  import dbConfig.profile.api._

  // you must import the DSL to use the syntax helpers
  import com.sksamuel.elastic4s.http.ElasticDsl._
  val esUri = ElasticsearchClientUri(config.get[String]("ot.elasticsearch.host"),
    config.get[Int]("ot.elasticsearch.port"))
  val esQ = HttpClient(esUri)

//  def buildPheWASTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]):
//  Future[Entities.PheWASTable] = {
//    val limitClause = parsePaginationTokens(pageIndex, pageSize)
//    val variant = Variant(variantID)
//
//    variant match {
//      case Right(v) =>
//        val segment = toSumStatsSegment(v.position.position)
//        val tableName = gwasSumStatsTName format v.position.chrId
//        val query =
//          sql"""
//               |select
//               | study_id,
//               | pval,
//               | beta,
//               | se,
//               | eaf,
//               | maf,
//               | n_samples_variant_level,
//               | n_samples_study_level,
//               | n_cases_study_level,
//               | n_cases_variant_level,
//               | if(is_cc,exp(beta),NULL) as odds_ratio,
//               | chip,
//               | info
//               |from #$tableName
//               |prewhere chrom = ${v.position.chrId} and
//               |  pos_b37 = ${v.position.position} and
//               |  segment = $segment and
//               |  variant_id_b37 = ${v.id}
//               |#$limitClause
//         """.stripMargin.as[VariantPheWAS]
//
//        dbSS.run(query.asTry).map {
//          case Success(traitVector) => PheWASTable(traitVector)
//          case Failure(ex) =>
//            logger.error(ex.getMessage)
//            PheWASTable(associations = Vector.empty)
//        }
//
//      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
//    }
//  }

//  def getG2VSchema: Future[Entities.G2VSchema] = {
//    def toSeqStruct(elems: Map[String, Map[String, String]]) = {
//      (for {
//        triple <- elems
//        tuple <- triple._2
//
//      } yield Entities.G2VSchemaElement(triple._1, tuple._1,
//        StrSeqRep(tuple._2).rep.map(el => Tissue(el)))).toSeq
//    }
//    val studyQ = sql"""
//                      |select
//                      | type_id,
//                      | source_id,
//                      | feature_set
//                      |from #$v2gStructureTName
//      """.stripMargin.as[(String, String, String)]
//
//    db.run(studyQ.asTry).map {
//      case Success(v) =>
//        val mappedRows = v.groupBy(_._1).mapValues(_.groupBy(_._2).mapValues(_.head._3))
//        val qtlElems = toSeqStruct(mappedRows.filterKeys(defaultQtlTypes.contains(_)))
//        val intervalElems = toSeqStruct(mappedRows.filterKeys(defaultIntervalTypes.contains(_)))
//        val fpredElems = toSeqStruct(mappedRows.filterKeys(defaultFPredTypes.contains(_)))
//
//        G2VSchema(qtlElems, intervalElems, fpredElems)
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        G2VSchema(Seq.empty, Seq.empty, Seq.empty)
//    }
//  }

//  def getSearchResultSet(qString: String, pageIndex: Option[Int], pageSize: Option[Int]):
//  Future[Entities.SearchResultSet] = {
//    val limitClause = parsePaginationTokensForES(pageIndex, pageSize)
//    val stoken = qString.toLowerCase
//    // val stoken = qString
//    val cleanedTokens = stoken.replaceAll("-", " ")
//
//    if (stoken.length > 0) {
//      esQ.execute {
//          search("studies") query boolQuery.should(matchQuery("study_id", stoken),
//            matchQuery("pmid", stoken),
//            multiMatchQuery(cleanedTokens)
//              .matchType(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
//              .lenient(true)
//              .slop(10)
//              .prefixLength(2)
//              .maxExpansions(50)
//              .operator("OR")
//              .analyzer(WhitespaceAnalyzer)
//              .fields(Map("trait_reported" -> 1.5F,
//                "pub_author" -> 1.2F,
//                "_all" -> 1.0F)),
//            simpleStringQuery(cleanedTokens)
//              .defaultOperator("AND")
//            ) start limitClause._1 limit limitClause._2 sortByFieldDesc "n_initial"
//      }.zip {
//        esQ.execute {
//          search("variant_*") query boolQuery.should(matchQuery("variant_id", stoken),
//            matchQuery("rs_id", stoken)) start limitClause._1 limit limitClause._2
//        }
//      }.zip {
//        esQ.execute {
//          search("genes") query boolQuery.should(matchQuery("gene_id", stoken),
//            matchQuery("gene_name", stoken),
//            multiMatchQuery(cleanedTokens)
//              .matchType(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
//              .lenient(true)
//              .slop(10)
//              .prefixLength(2)
//              .maxExpansions(50)
//              .operator("OR")
//              .analyzer(WhitespaceAnalyzer)
//              .fields("gene_name")) start limitClause._1 limit limitClause._2
//        }
//      }.map{
//        case ((studiesRS, variantsRS), genesRS) =>
//          SearchResultSet(genesRS.totalHits, genesRS.to[FRM.Gene],
//            variantsRS.totalHits, variantsRS.to[VariantSearchResult],
//            studiesRS.totalHits, studiesRS.to[FRM.Study])
//      }
//    } else {
//      Future.failed(InputParameterCheckError(Vector(SearchStringViolation())))
//    }
//  }

  /** get top Functions.defaultTopOverlapStudiesSize studies sorted desc by
    * the number of overlapped loci
    *
    * @param stid given a study ID
    * @return a Entities.OverlappedLociStudy which could contain empty list of ovelapped studies
    */
//  def getTopOverlappedStudies(stid: String, pageIndex: Option[Int] = Some(0), pageSize: Option[Int] = Some(defaultTopOverlapStudiesSize)):
//  Future[Entities.OverlappedLociStudy] = {
//    val limitClause = parsePaginationTokens(pageIndex, pageSize)
//
//    val topOverlappedsSQL = sql"""
//                          |SELECT
//                          |  study_id_b,
//                          |  uniq(index_variant_id_a) AS num_overlap_loci
//                          |FROM #$studiesOverlapTName
//                          |PREWHERE (study_id_a = $stid) and
//                          |  set_type = 'combined'
//                          |GROUP BY
//                          |  study_id_a,
//                          |  study_id_b
//                          |ORDER BY num_overlap_loci desc
//                          |#$limitClause
//      """.stripMargin.as[(String, Int)]
//
//    db.run(topOverlappedsSQL.asTry).map {
//      case Success(v) =>
//        if (v.nonEmpty) {
//          OverlappedLociStudy(stid, v.map(t => OverlapRow(t._1, t._2)))
//        } else {
//          OverlappedLociStudy(stid, Vector.empty)
//        }
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        OverlappedLociStudy(stid, Vector.empty)
//    }
//  }

//  def getOverlapVariantsIntersectionForStudies(stid: String, stids: Seq[String]): Future[Vector[String]] = {
//    val stidListString = stids.map("'" + _ + "'").mkString(",")
//    val numStids = if (stids.nonEmpty) stids.length else 0
//    val overlapSQL = sql"""
//                          |SELECT index_variant_id_a
//                          |FROM
//                          |(
//                          |    SELECT
//                          |        index_variant_id_a,
//                          |        uniq(study_id_b) AS num_studies
//                          |    FROM #$studiesOverlapTName
//                          |    PREWHERE (study_id_a = $stid) AND
//                          |        (study_id_b IN (#${stidListString})) AND
//                          |        (set_type = 'combined')
//                          |    GROUP BY index_variant_id_a
//                          |    HAVING num_studies = ${numStids}
//                          |)
//      """.stripMargin.as[String]
//
//    db.run(overlapSQL.asTry).map {
//      case Success(v) => v
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        Vector.empty
//    }
//  }

//  def getOverlapVariantsForStudies(stid: String, stids: Seq[String]): Future[Vector[Entities.OverlappedVariantsStudy]] = {
//    val stidListString = stids.map("'" + _ + "'").mkString(",")
//    val overlapSQL = sql"""
//                          |SELECT
//                          |  study_id_b,
//                          |  index_variant_id_a,
//                          |  index_variant_id_b,
//                          |  any(overlap_AB) AS overlap_AB,
//                          |  any(distinct_A) AS distinct_A,
//                          |  any(distinct_B) AS distinct_B
//                          |FROM #$studiesOverlapTName
//                          |PREWHERE (study_id_a = $stid) AND
//                          |  (study_id_b IN (#${stidListString})) AND
//                          |  (set_type = 'combined')
//                          |GROUP BY
//                          |  study_id_a,
//                          |  study_id_b,
//                          |  index_variant_id_a,
//                          |  index_variant_id_b
//      """.stripMargin.as[(String, String, String, Int, Int, Int)]
//
//    db.run(overlapSQL.asTry).map {
//      case Success(v) =>
//        if (v.nonEmpty) {
//          v.view.groupBy(_._1).map(pair =>
//            OverlappedVariantsStudy(pair._1,
//              pair._2.map(t => OverlappedVariant(t._2, t._3, t._4, t._5, t._6)))).toVector
//        } else {
//          Vector.empty
//        }
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        Vector.empty
//    }
//  }

//  def getStudiesForGene(geneId: String): Future[Vector[String]] = {
//    val studiesSQL = sql"""
//                           |SELECT DISTINCT stid
//                           |FROM #$d2v2gTName
//                           |PREWHERE
//                           |  (gene_id = $geneId) AND
//                           |  (chr_id = dictGetString('gene','chr',tuple($geneId)))
//      """.stripMargin.as[String]
//
//    db.run(studiesSQL.asTry).map {
//      case Success(v) => v
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        Vector.empty
//    }
//  }

  def getGenes(geneIds: Seq[String]): Future[Seq[FRM.Gene]] = {
    if (geneIds.nonEmpty) {
      val q = for {
        g <- FRM.genes
        if g.id inSetBind geneIds
      } yield g

      db.run(q.result.asTry).map {
        case Success(v) => v
        case Failure(ex) =>
          logger.error(ex.getMessage)
          Seq.empty
      }
    } else {
      Future.successful(Seq.empty)
    }
  }

  /** query variants table with a list of variant ids and get all related information */
  def getVariants(variantIds: Seq[String]): Future[Seq[FRM.Variant]] = {
    if (variantIds.nonEmpty) {
      val q = for {
        v <- FRM.variants
        if v.id inSetBind variantIds
      } yield v

      db.run(q.result.asTry).map {
        case Success(v) => v
        case Failure(ex) =>
          logger.error(ex.getMessage)
          Seq.empty
      }
    } else {
      Future.successful(Seq.empty)
    }
  }

  def getStudies(stids: Seq[String]): Future[Seq[FRM.Study]] = {
    if (stids.nonEmpty) {
      val q = for {
        v <- FRM.studies
        if v.studyId inSetBind stids
      } yield v

      db.run(q.result.asTry).map {
        case Success(v) => v
        case Failure(ex) =>
          logger.error(ex.getMessage)
          Seq.empty
      }
    } else {
      Future.successful(Seq.empty)
    }
  }

//  def buildManhattanTable(studyId: String, pageIndex: Option[Int], pageSize: Option[Int]):
//  Future[Entities.ManhattanTable] = {
//    val limitClause = parsePaginationTokens(pageIndex, pageSize)
//
//    val idxVariants = sql"""
//      |SELECT
//      |    index_variant_id,
//      |    pval,
//      |    credibleSetSize,
//      |    ldSetSize,
//      |    uniq_variants,
//      |    top_genes_ids,
//      |    top_genes_scores
//      |FROM
//      |(
//      |    SELECT
//      |        index_variant_id,
//      |        any(pval) AS pval,
//      |        uniqIf(variant_id, posterior_prob > 0) AS credibleSetSize,
//      |        uniqIf(variant_id, r2 > 0) AS ldSetSize,
//      |        uniq(variant_id) AS uniq_variants
//      |    FROM #$v2dByStTName
//      |    PREWHERE stid = $studyId
//      |    GROUP BY index_variant_id
//      |)
//      |ALL LEFT OUTER JOIN
//      |(
//      |    SELECT
//      |        variant_id AS index_variant_id,
//      |        groupArray(gene_id) AS top_genes_ids,
//      |        groupArray(overall_score) AS top_genes_scores
//      |    FROM ot.d2v2g_score_by_overall
//      |    PREWHERE (variant_id = index_variant_id) AND (overall_score > 0.)
//      |    GROUP BY variant_id
//      |) USING (index_variant_id)
//      |#$limitClause
//      """.stripMargin.as[V2DByStudy]
//
//    // map to proper manhattan association with needed fields
//    db.run(idxVariants.asTry).map {
//      case Success(v) => ManhattanTable(studyId,
//        v.map(el => {
//          ManhattanAssociation(el.index_variant_id, el.pval, el.topGenes,
//            el.credibleSetSize, el.ldSetSize, el.totalSetSize)
//        })
//      )
//      case Failure(ex) =>
//        logger.error(ex.getMessage)
//        ManhattanTable(studyId, associations = Vector.empty)
//    }
//  }

//  def buildIndexVariantAssocTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]):
//  Future[Entities.IndexVariantTable] = {
//    val limitClause = parsePaginationTokens(pageIndex, pageSize)
//    val variant = Variant(variantID)
//
//    variant match {
//      case Right(v) =>
//        val assocs = sql"""
//                       |select
//                       | variant_id,
//                       | rs_id,
//                       | stid,
//                       | pval,
//                       | ifNull(n_initial,0) + ifNull(n_replication,0),
//                       | ifNull(n_cases, 0),
//                       | r2,
//                       | afr_1000g_prop,
//                       | amr_1000g_prop,
//                       | eas_1000g_prop,
//                       | eur_1000g_prop,
//                       | sas_1000g_prop,
//                       | log10_abf,
//                       | posterior_prob
//                       |from #$v2dByChrPosTName
//                       |prewhere
//                       |  chr_id = ${v.position.chrId} and
//                       |  index_position = ${v.position.position} and
//                       |  index_ref_allele = ${v.refAllele} and
//                       |  index_alt_allele = ${v.altAllele}
//                       |#$limitClause
//          """.stripMargin.as[IndexVariantAssociation]
//
//        db.run(assocs.asTry).map {
//          case Success(r) => Entities.IndexVariantTable(r)
//          case Failure(ex) =>
//            logger.error(ex.getMessage)
//            Entities.IndexVariantTable(associations = Vector.empty)
//        }
//      case Left(violation) =>
//        Future.failed(InputParameterCheckError(Vector(violation)))
//    }
//  }

//  def buildTagVariantAssocTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]):
//  Future[TagVariantTable] = {
//    val limitClause = parsePaginationTokens(pageIndex, pageSize)
//    val variant = Variant(variantID)
//
//    variant match {
//      case Right(v) =>
//        val assocs = sql"""
//                          |select
//                          | index_variant_id,
//                          | index_rs_id,
//                          | stid,
//                          | pval,
//                          | ifNull(n_initial,0) + ifNull(n_replication,0),
//                          | ifNull(n_cases, 0),
//                          | r2,
//                          | afr_1000g_prop,
//                          | amr_1000g_prop,
//                          | eas_1000g_prop,
//                          | eur_1000g_prop,
//                          | sas_1000g_prop,
//                          | log10_abf,
//                          | posterior_prob
//                          |from #$v2dByChrPosTName
//                          |prewhere
//                          |  chr_id = ${v.position.chrId} and
//                          |  position = ${v.position.position} and
//                          |  ref_allele = ${v.refAllele} and
//                          |  alt_allele = ${v.altAllele}
//                          |#$limitClause
//          """.stripMargin.as[TagVariantAssociation]
//
//        // map to proper manhattan association with needed fields
//        db.run(assocs.asTry).map {
//          case Success(r) => Entities.TagVariantTable(r)
//          case Failure(ex) =>
//            logger.error(ex.getMessage)
//            Entities.TagVariantTable(associations = Vector.empty)
//        }
//      case Left(violation) =>
//        Future.failed(InputParameterCheckError(Vector(violation)))
//    }
//  }

//  def buildGecko(chromosome: String, posStart: Long, posEnd: Long): Future[Option[Entities.Gecko]] = {
//    (parseChromosome(chromosome), parseRegion(posStart, posEnd)) match {
//      case (Right(chr), Right((start, end))) =>
//        val inRegion = Region(chr, start, end)
//        if (denseRegionChecker.matchRegion(inRegion)) {
//          Future.failed(InputParameterCheckError(Vector(RegionViolation(inRegion))))
//        } else {
//            val geneIdsInLoci = sql"""
//                                     |SELECT
//                                     | gene_id
//                                     |FROM ot.gene
//                                     |WHERE
//                                     | chr = $chr and (
//                                     | (start >= $start and start <= $end) or
//                                     | (end >= $start and end <= $end))
//                """.stripMargin.as[String]
//
//            val assocs = sql"""
//                              |SELECT
//                              |  variant_id,
//                              |  rs_id,
//                              |  index_variant_id,
//                              |  index_variant_rsid,
//                              |  gene_id,
//                              |  stid,
//                              |  r2,
//                              |  posterior_prob,
//                              |  pval,
//                              |  overall_score
//                              |FROM (
//                              | SELECT
//                              |  stid,
//                              |  variant_id,
//                              |  any(rs_id) as rs_id,
//                              |  index_variant_id,
//                              |  any(index_variant_rsid) as index_variant_rsid,
//                              |  gene_id,
//                              |  any(r2) as r2,
//                              |  any(posterior_prob) as posterior_prob,
//                              |  any(pval) as pval
//                              | FROM #$d2v2gTName
//                              | PREWHERE
//                              |   chr_id = $chr and (
//                              |   (position >= $start and position <= $end) or
//                              |   (index_position >= $start and index_position <= $end) or
//                              |   (dictGetUInt32('gene','start',tuple(gene_id)) >= $start and
//                              |     dictGetUInt32('gene','start',tuple(gene_id)) <= $end) or
//                              |   (dictGetUInt32('gene','end',tuple(gene_id)) >= $start and
//                              |     dictGetUInt32('gene','end',tuple(gene_id)) <= $end))
//                              | group by stid, index_variant_id, variant_id, gene_id
//                              |) all inner join (
//                              | SELECT
//                              |   variant_id,
//                              |   gene_id,
//                              |   overall_score
//                              | FROM #$d2v2gOScoresTName
//                              | PREWHERE chr_id = $chr and
//                              | overall_score > 0.
//                              |) USING (variant_id, gene_id)
//                """.stripMargin
//              .as[GeckoLine]
//
//              db.run(
//                geneIdsInLoci.asTry zip assocs.asTry).map {
//                case (Success(geneIds), Success(assocs
//                )) => Entities.
//                  Gecko(assocs.view, geneIds.toSet)
//                case (Success(geneIds), Failure(
//                asscsEx)) =>
//                  logger.
//                    error(asscsEx.getMessage)
//                  Entities.Gecko(Seq.
//                    empty, geneIds.toSet)
//                case (_, _) =>
//                  logger.error("Something really wrong happened while getting geneIds from gene " +
//                    "dictionary and also from d2v2g table")
//                  Entities.Gecko(Seq.empty, Set.empty)
//              }
//            }
//
//      case (chrEither, rangeEither) =>
//        Future.failed(InputParameterCheckError(
//          Vector(chrEither, rangeEither).filter(_.isLeft).map(_.left.get).asInstanceOf[Vector[Violation]]))
//    }
//  }

//  def buildG2VByVariant(variantId: String): Future[Seq[Entities.G2VAssociation]] = {
//    val variant = Variant(variantId)
//
//    variant match {
//      case Right(v) =>
//        val assocs = sql"""
//                          |SELECT
//                          |    gene_id,
//                          |    variant_id,
//                          |    overall_score,
//                          |    source_list,
//                          |    source_score_list,
//                          |    type_id,
//                          |    source_id,
//                          |    feature,
//                          |    fpred_max_label,
//                          |    fpred_max_score,
//                          |    qtl_beta,
//                          |    qtl_se,
//                          |    qtl_pval,
//                          |    interval_score,
//                          |    qtl_score_q,
//                          |    interval_score_q
//                          |FROM
//                          |(
//                          |    SELECT
//                          |        gene_id,
//                          |        type_id,
//                          |        source_id,
//                          |        feature,
//                          |        fpred_max_label,
//                          |        fpred_max_score,
//                          |        qtl_beta,
//                          |        qtl_se,
//                          |        qtl_pval,
//                          |        interval_score,
//                          |        qtl_score_q,
//                          |        interval_score_q
//                          |    FROM #$v2gTName
//                          |    PREWHERE
//                          |       (chr_id = ${v.position.chrId}) AND
//                          |       (position = ${v.position.position}) AND
//                          |       (variant_id = ${v.id}) AND
//                          |       (isNull(fpred_max_score) OR fpred_max_score > 0.)
//                          |)
//                          |ALL INNER JOIN
//                          |(
//                          |    SELECT
//                          |        variant_id,
//                          |        gene_id,
//                          |        source_list,
//                          |        source_score_list,
//                          |        overall_score
//                          |    FROM #$v2gOScoresTName
//                          |    PREWHERE (chr_id = ${v.position.chrId}) AND
//                          |       (variant_id = ${v.id})
//                          |) USING (gene_id)
//                          |ORDER BY gene_id ASC
//          """.stripMargin.as[ScoredG2VLine]
//
//        db.run(assocs.asTry).map {
//          case Success(r) => r.view.groupBy(_.geneId).mapValues(G2VAssociation(_)).values.toSeq
//          case Failure(ex) =>
//            logger.error(ex.getMessage)
//            Seq.empty
//        }
//      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
//    }
//  }

  private val v2dByStTName: String = "v2d_by_stchr"
  private val v2dByChrPosTName: String = "v2d_by_chrpos"
  private val d2v2gTName: String = "d2v2g"
  private val d2v2gOScoresTName: String = "d2v2g_score_by_overall"
  private val v2gTName: String = "v2g"
  private val v2gOScoresTName: String = "v2g_score_by_overall"
  private val v2gStructureTName: String = "v2g_structure"
  private val studiesTName: String = "studies"
  private val variantsTName: String = "variants"
  private val studiesOverlapTName: String = "studies_overlap"
  private val gwasSumStatsTName: String = "gwas_chr_%s"
  private val genesTName: String = "gene"
}