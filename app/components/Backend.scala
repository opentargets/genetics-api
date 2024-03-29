package components

import java.nio.file.{Path, Paths}
import components.clickhouse.ClickHouseProfile
import components.elasticsearch.{ElasticsearchComponent, Pagination}
import configuration.{Metadata, MetadataConfiguration}

import javax.inject.{Inject, Singleton}
import models.Functions._
import models.database.FRM._
import models.database.Queries.geneIdByRegion
import models.database.{GeneticsDbTables, Queries}
import models.entities.DNA._
import models.entities.Entities._
import models.entities.Violations._
import models.entities.{DNA, Entities}
import models.implicits.DbImplicits._
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.{Configuration, Environment, Logger}
import play.db.NamedDatabase
import sangria.validation.{BaseViolation, Violation}
import slick.dbio.DBIOAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

@Singleton
class Backend @Inject() (
    @NamedDatabase("default") protected val dbConfigProvider: DatabaseConfigProvider,
    elasticsearch: ElasticsearchComponent,
    config: Configuration,
    env: Environment
) extends GeneticsDbTables {

  // Import config and settings files
  private val dbConfig = dbConfigProvider.get[ClickHouseProfile]
  private val db = dbConfig.db
  private val logger: Logger = Logger(this.getClass)

  import dbConfig.profile.api._

  def executeQuery[T](query: DBIO[T], defaultOnFail: T): Future[T] =
    db.run(query.asTry).map {
      case Success(s) => s
      case Failure(ex) =>
        logger.error(s"executeQuery error: ${ex.getMessage}")
        logger.info(s"Using default fallback: $defaultOnFail")
        defaultOnFail
    }

  def executeQueryForSeq[T](query: DBIO[Seq[T]]): Future[Seq[T]] =
    executeQuery(query, Seq.empty)

  def executeQueryForVec[T](query: DBIO[Vector[T]]): Future[Vector[T]] =
    executeQuery(query, Vector.empty).map(_.toVector)

  def getSequenceWithChromosomeAndRegionCheck[A, B >: Nothing](
      chromosome: String,
      start: Long,
      end: Long,
      q: => (String, Long, Long) => Query[A, B, Seq]
  ): Future[Seq[B]] =
    // 0 validate
    (parseChromosome(chromosome), parseRegion(start, end, defaultMaxRegionSize)) match {
      // right - right: all good
      case (Right(_), Right(_)) =>
        val query = q(chromosome, start, end)
        executeQueryForSeq(query.result)
      // one or more left
      case (chrEither, regionEither) =>
        Future.failed(Backend.inputParameterCheckErrorGenerator(chrEither, regionEither))
    }

  def getSequenceWithVariantCheck[A, B >: Nothing](
      variantID: String,
      query: => Variant => Query[A, B, Seq]
  ): Future[Seq[B]] =
    Variant.fromString(variantID) match {
      case Right(v) =>
        executeQueryForSeq(query(v).result)
      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }

  val denseRegionsPath: Path =
    Paths.get(env.rootPath.getAbsolutePath, "resources", "dense_regions.tsv")

  val denseRegionChecker: DenseRegionChecker = DenseRegionChecker(denseRegionsPath.toString)

  val v2gLabelsPath: Path =
    Paths.get(env.rootPath.getAbsolutePath, "resources", "v2g_display_labels.json")

  val v2gLabels: Map[String, JsValue] =
    loadJSONLinesIntoMap[String, JsValue](v2gLabelsPath.toString)(l =>
      (l \ "key").as[String] -> (l \ "value").get
    )

  val v2gBiofeatureLabelsPath: Path =
    Paths.get(env.rootPath.getAbsolutePath, "resources", "v2g_biofeature_labels.json")

  val v2gBiofeatureLabels: Map[String, String] =
    loadJSONLinesIntoMap(v2gBiofeatureLabelsPath.toString)(l =>
      (l \ "biofeature_code").as[String] -> (l \ "label").as[String]
    )

  def buildPhewFromSumstats(
      variantID: String,
      pageIndex: Option[Int],
      pageSize: Option[Int]
  ): Future[Seq[SumStatsGWASRow]] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val q = (v: Variant) =>
      sumstatsGWAS
        .filter(r =>
          (r.chrom === v.chromosome) &&
            (r.pos === v.position) &&
            (r.ref === v.refAllele) &&
            (r.alt === v.altAllele)
        )
        .drop(limitPair._1)
        .take(limitPair._2)
    getSequenceWithVariantCheck(variantID, q)
  }

  def colocalisationsForGene(geneId: String): Future[Seq[ColocRow]] = {
    val q1 = genes
      .filter(_.id === geneId)
      .take(1)
      .result
      .headOption
      .flatMap {
        case Some(g) =>
          colocs
            .filter(r =>
              (r.lChrom === g.chromosome) &&
                (r.rGeneId === g.id) &&
                (r.lType === GWASLiteral) &&
                (r.rType =!= GWASLiteral)
            )
            .result

        case None =>
          DBIOAction.successful(Seq.empty)
      }

    executeQueryForSeq(q1)
  }

  def gwasColocalisationForRegion(
      chromosome: String,
      startPos: Long,
      endPos: Long
  ): Future[Seq[ColocRow]] = {

    val q = (chromosome: String, startPos: Long, endPos: Long) =>
      colocs
        .filter(r =>
          (r.lChrom === chromosome) &&
            (r.lPos >= startPos) &&
            (r.lPos <= endPos) &&
            (r.rPos >= startPos) &&
            (r.rPos <= endPos) &&
            (r.rType === GWASLiteral)
        )
    getSequenceWithChromosomeAndRegionCheck(chromosome, startPos, endPos, q)

  }

  def gwasColocalisation(studyId: String, variantId: String): Future[Seq[ColocRow]] = {
    val q = (v: Variant) =>
      Queries.colocOnVariantAndStudy(v, studyId).filter(r => r.rType === GWASLiteral)
    getSequenceWithVariantCheck(variantId, q)
  }

  def qtlColocalisation(studyId: String, variantId: String): Future[Seq[ColocRow]] = {
    val q = (v: Variant) =>
      Queries.colocOnVariantAndStudy(v, studyId).filter(r => r.rType =!= GWASLiteral)
    getSequenceWithVariantCheck(variantId, q)
  }

  def gwasCredibleSet(studyId: String,
                      variantId: String
  ): Future[Seq[(SimpleVariant, CredSetRowStats)]] = {
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = credsets
          .filter(r =>
            (r.leadChromosome === v.chromosome) &&
              (r.leadPosition === v.position) &&
              (r.leadRefAllele === v.refAllele) &&
              (r.leadAltAllele === v.altAllele) &&
              (r.studyId === studyId) &&
              (r.dataType === GWASLiteral)
          )
          .map(_.tagVariantWithStats)

        executeQueryForSeq(q.result)

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def qtlCredibleSet(studyId: String,
                     variantId: String,
                     geneId: String,
                     bioFeatureId: String
  ): Future[Seq[(SimpleVariant, CredSetRowStats)]] = {
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = credsets
          .filter(r =>
            (r.leadChromosome === v.chromosome) &&
              (r.leadPosition === v.position) &&
              (r.leadRefAllele === v.refAllele) &&
              (r.leadAltAllele === v.altAllele) &&
              (r.studyId === studyId) &&
              (r.dataType =!= GWASLiteral) &&
              (r.geneId === geneId) &&
              (r.bioFeature === bioFeatureId)
          )
          .map(_.tagVariantWithStats)

        executeQueryForSeq(q.result)

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def gwasRegionalFromSumstats(
      studyId: String,
      chromosome: String,
      startPos: Long,
      endPos: Long
  ): Future[Seq[(SimpleVariant, Double)]] = {

    val q = (chromosome: String, startPos: Long, endPos: Long) => {
      val q = sumstatsGWAS
        .filter(r =>
          (r.chrom === chromosome) &&
            (r.pos >= startPos) &&
            (r.pos <= endPos) &&
            (r.studyId === studyId)
        )
        .map(_.variantAndPVal)

      logger.debug("gwasRegionalFromSumstats " + q.result.statements.mkString("\n"))

      q
    }

    getSequenceWithChromosomeAndRegionCheck(chromosome, startPos, endPos, q)

  }

  def qtlRegionalFromSumstats(
      studyId: String,
      bioFeature: String,
      geneId: String,
      chromosome: String,
      startPos: Long,
      endPos: Long
  ): Future[Seq[(SimpleVariant, Double)]] = {

    val q = (chr: String, startPos: Long, endPos: Long) =>
      sumstatsMolTraits
        .filter(r =>
          (r.chrom === chr) &&
            (r.pos >= startPos) &&
            (r.pos <= endPos) &&
            (r.bioFeature === bioFeature) &&
            (r.geneId === geneId) &&
            (r.studyId === studyId)
        )
        .map(_.variantAndPVal)

    getSequenceWithChromosomeAndRegionCheck(chromosome, startPos, endPos, q)
  }

  def getMetadata: Future[Metadata] = {
    import MetadataConfiguration._
    Future(config.get[Metadata]("ot.meta"))
  }

  def getG2VSchema: Future[Entities.G2VSchema] = {
    // converts V2DStructure to G2VSchemaElement
    def toSeqStruct(elems: Seq[V2GStructureRow]): Seq[G2VSchemaElement] =
      for {
        entry <- elems
      } yield Entities.G2VSchemaElement(
        entry.typeId,
        entry.sourceId,
        (v2gLabels(entry.sourceId) \ "display_label").asOpt[String],
        (v2gLabels(entry.sourceId) \ "overview_tooltip").asOpt[String],
        (v2gLabels(entry.sourceId) \ "tab_subtitle").asOpt[String],
        (v2gLabels(entry.sourceId) \ "pmid").asOpt[String],
        entry.bioFeatureSet.map(str => Tissue(str))
      )
    def filterDefaultType(v: Vector[V2GStructureRow], f: List[String]) =
      v.filter(it => f.contains(it.typeId))

    val plainSqlQuery =
      sql"""
           |select type_id, source_id, flatten(groupArray(feature_set)) from v2g_structure
           |group by (type_id, source_id)
           |
        """.stripMargin.as[V2GStructureRow]

    // 1. Get raw schema
    val res = db.run(plainSqlQuery.asTry)
    res.map {
      case Success(v) =>
        // 2. Map to types
        val distTypes = filterDefaultType(v, defaultDistanceTypes)
        val intTypes = filterDefaultType(v, defaultIntervalTypes)
        val fpredTypes = filterDefaultType(v, defaultFPredTypes)
        val qltTypes = filterDefaultType(v, defaultQtlTypes)
        // 3. Compose schema
        G2VSchema(toSeqStruct(qltTypes),
                  toSeqStruct(intTypes),
                  toSeqStruct(fpredTypes),
                  toSeqStruct(distTypes)
        )
      case Failure(ex) =>
        logger.error(s"Error creating G2V Schema: ${ex.getMessage} -- ${ex.toString}")
        G2VSchema(Seq.empty, Seq.empty, Seq.empty, Seq.empty)
    }
  }

  def search(query: String, pagination: Option[Pagination]): Future[SearchResultSet] =
    elasticsearch.search(query, pagination.getOrElse(Pagination.mkDefault))

  /** get top Functions.defaultTopOverlapStudiesSize studies sorted desc by the number of overlapped
    * loci
    *
    * @param stid
    *   given a study ID
    * @return
    *   a Entities.OverlappedLociStudy which could contain empty list of ovelapped studies
    */
  def getTopOverlappedStudies(stid: String,
                              pageIndex: Option[Int] = Some(0),
                              pageSize: Option[Int] = Some(defaultTopOverlapStudiesSize)
  ): Future[OverlappedLociStudy] = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)

    val plainQ =
      sql"""
           |select
           | B_study_id,
           | uniq(A_chrom, A_pos, A_ref, A_alt) as num_overlaps
           |from studies_overlap
           |prewhere A_study_id = $stid
           |group by B_study_id
           |order by num_overlaps desc
           |#$limitClause
         """.stripMargin.as[(String, Int)]

    db.run(plainQ.asTry).map {
      case Success(v) =>
        if (v.nonEmpty) {
          OverlappedLociStudy(stid, v.map(t => OverlapRow(t._1, t._2)))
        } else {
          OverlappedLociStudy(stid, Vector.empty)
        }
      case Failure(ex) =>
        logger.error(s"getTopOverlappedStudies failed with " + ex.getMessage)
        OverlappedLociStudy(stid, Vector.empty)
    }
  }

  def getOverlapVariantsIntersectionForStudies(stid: String,
                                               stids: Seq[String]
  ): Future[Vector[String]] =
    if (stids.nonEmpty) {
      val numStudies = stids.length.toLong

      val q = overlaps
        .filter(r => (r.studyIdA === stid) && (r.studyIdB inSetBind stids))
        .groupBy(_.variantA)
        .map { case (vA, g) => vA -> g.map(_.studyIdB).uniq }
        .filter(_._2 === numStudies)
        .map(_._1)

      db.run(q.result.asTry).map {
        case Success(v) =>
          v.view.map(_.id).toVector
        case Failure(ex) =>
          logger.error(ex.getMessage)
          Vector.empty
      }
    } else {
      Future.successful(Vector.empty)
    }

  def getOverlapVariantsForStudies(stid: String,
                                   stids: Seq[String]
  ): Future[Vector[Entities.OverlappedVariantsStudy]] = {
    val q =
      overlaps
        .filter(r => (r.studyIdA === stid) && (r.studyIdB inSetBind stids))
        .groupBy(r => (r.studyIdB, r.variantA, r.variantB))
        .map { case (l, g) =>
          (l._1, l._2, l._3) -> (g.map(_.overlapsAB).any, g.map(_.distinctA).any, g
            .map(_.distinctB)
            .any)
        }

    db.run(q.result.asTry).map {
      case Success(v) =>
        if (v.nonEmpty) {
          v.view
            .groupBy(_._1._1)
            .map(pair =>
              OverlappedVariantsStudy(
                pair._1,
                pair._2.map(t =>
                  OverlappedVariant(t._1._2.id, t._1._3.id, t._2._1.get, t._2._2.get, t._2._3.get)
                )
              )
            )
            .toVector
        } else {
          Vector.empty
        }
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }

  def getGeneChromosome(geneId: String): Future[String] = {
    val geneQ = genes.filter(_.id === geneId).map(g => g.chromosome)
    executeQuery(geneQ.result.head, "")
  }

  def getStudiesForGene(geneId: String): Future[Seq[String]] = {
    val studiesQ = genes
      .filter(_.id === geneId)
      .map(_.chromosome)
      .result
      .headOption
      .flatMap {
        case Some(chr) =>
          d2v2gScored
            .filter(r => (r.geneId === geneId) && (r.leadChromosome === chr))
            .groupBy(_.studyId)
            .map(_._1)
            .result
        case None =>
          DBIOAction.successful(Seq.empty)
      }

    executeQueryForSeq(studiesQ)
  }

  def getStudiesAndLeadVariantsForGeneByL2G(geneId: String,
                                            pageIndex: Option[Int],
                                            pageSize: Option[Int]
  ): Future[Vector[V2DL2GRowByGene]] = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)

    val topLociEnrich =
      sql"""
           |SELECT study_id,
           |       chrom,
           |       pos,
           |       ref,
           |       alt,
           |       y_proba_logi_distance,
           |       y_proba_logi_interaction,
           |       y_proba_logi_molecularQTL,
           |       y_proba_logi_pathogenicity,
           |       y_proba_full_model,
           |       odds_ratio,
           |       oddsr_ci_lower,
           |       oddsr_ci_upper,
           |       direction,
           |       beta,
           |       beta_ci_lower,
           |       beta_ci_upper,
           |       pval,
           |       pval_exponent,
           |       pval_mantissa
           |FROM ot.l2g_by_gsl l
           |ANY INNER JOIN (
           |    SELECT *,
           |           CAST(lead_chrom, 'String') as stringChrom
           |    FROM ot.v2d_by_stchr
           |    ) v on (v.study_id = l.study_id and
           |            v.stringChrom = l.chrom and
           |            v.lead_pos = l.pos and
           |            v.lead_ref = l.ref and
           |            v.lead_alt = l.alt)
           |PREWHERE gene_id = $geneId
           |#$limitClause
         """.stripMargin.as[V2DL2GRowByGene]

    executeQueryForVec(topLociEnrich)
  }

  def getGenesByRegion(chromosome: String, startPos: Long, endPos: Long): Future[Seq[Gene]] =
    getSequenceWithChromosomeAndRegionCheck(chromosome, startPos, endPos, Queries.geneInRegion)

  def getGenes(geneIds: Seq[String]): Future[Seq[Gene]] =
    if (geneIds.nonEmpty) {
      val q = genes.filter(r => r.id inSetBind geneIds)

      executeQueryForSeq(q.result)
    } else {
      Future.successful(Seq.empty)
    }

  /** query variants table with a list of variant ids and get all related information
    *
    * NOTE! WARNING! AT THE MOMENT, THE WAY TO DO IS USING THE VARIANT APPLY CONSTRUCTOR FROM A
    * STRING TO GET A WHITE-LABEL VARIANT WITH NO OTHER REFERENCES FROM RSID OR NEAREST GENES
    * (NONCODING AND PROTCODING)
    */
  def getVariants(variantIds: Seq[String]): Future[Seq[DNA.Variant]] =
    if (variantIds.nonEmpty) {
      val q = variants.filter(_.id inSetBind variantIds)

      db.run(q.result.asTry).map {
        case Success(v) =>
          val missingVIds = variantIds diff v.map(_.id)

          if (missingVIds.nonEmpty) {
            v ++ missingVIds.map(DNA.Variant.fromString).withFilter(_.isRight).map(_.right.get)
          } else v
        case Failure(ex) =>
          logger.error("BDIOAction failed with " + ex.getMessage)
          Seq.empty
      }
    } else {
      Future.successful(Seq.empty)
    }

  def getStudies(stids: Seq[String]): Future[Seq[Study]] =
    if (stids.nonEmpty) {
      val q = studies.filter(_.studyId inSetBind stids)
      executeQueryForSeq(q.result)
    } else {
      Future.successful(Seq.empty)
    }

  def buildManhattanTable(studyId: String,
                          pageIndex: Option[Int],
                          pageSize: Option[Int]
  ): Future[Entities.ManhattanTable] = {
    val limitClause = parsePaginationTokensForSlick(pageIndex, pageSize)

    val topLociEnrichQ = manhattan
      .filter(_.studyId === studyId)
      .drop(limitClause._1)
      .take(limitClause._2)

    db.run(topLociEnrichQ.result.asTry).map {
      case Success(v) =>
        ManhattanTable(
          studyId,
          v.map { el =>
            ManhattanAssociation(
              el.variant.id,
              el.pval,
              el.pvalMantissa,
              el.pvalExponent,
              el.v2dOdds,
              el.v2dBeta,
              el.bestRawGeneIds zip el.bestRawGeneScores,
              el.bestColocGeneIds zip el.bestColocGeneScores,
              el.bestL2GeneIds zip el.bestL2GeneScores,
              el.credibleSetSize,
              el.ldSetSize,
              el.totalSetSize
            )
          }.toVector
        )
      case Failure(ex) =>
        logger.error(ex.getMessage)
        ManhattanTable(studyId, associations = Vector.empty)
    }
  }

  def buildSLGTable(studyId: String,
                    variantId: String,
                    pageIndex: Option[Int],
                    pageSize: Option[Int]
  ): Future[Entities.SLGTable] = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val slgQ =
          sql"""
               |SELECT
               |    gene_id,
               |    y_proba_logi_distance,
               |    y_proba_logi_interaction,
               |    y_proba_logi_molecularQTL,
               |    y_proba_logi_pathogenicity,
               |    y_proba_full_model,
               |    has_coloc,
               |    d AS distance_to_locus
               |FROM
               |(
               |    SELECT
               |        study_id,
               |        chrom,
               |        CAST(chrom, 'Enum8(\'1\' = 1, \'2\' = 2, \'3\' = 3, \'4\' = 4, \'5\' = 5, \'6\' = 6, \'7\' = 7, \'8\' = 8, \'9\' = 9, \'10\' = 10, \'11\' = 11, \'12\'
               |= 12, \'13\' = 13, \'14\' = 14, \'15\' = 15, \'16\' = 16, \'17\' = 17, \'18\' = 18, \'19\' = 19, \'20\' = 20, \'21\' = 21, \'22\' = 22, \'X\' = 23, \'Y\' = 24,
               | \'MT\' = 25)') AS enumChrom,
               |        pos,
               |        ref,
               |        alt,
               |        gene_id,
               |        y_proba_logi_distance,
               |        y_proba_logi_interaction,
               |        y_proba_logi_molecularQTL,
               |        y_proba_logi_pathogenicity,
               |        y_proba_full_model,
               |        C.has_coloc AS has_coloc
               |    FROM ot.l2g_by_slg AS L
               |    ANY LEFT JOIN
               |    (
               |        SELECT
               |            left_study AS study_id,
               |            right_gene_id AS gene_id,
               |            left_chrom as enumChrom,
               |            left_pos as pos,
               |            left_ref as ref,
               |            left_alt as alt,
               |            count() > 0 AS has_coloc
               |        FROM ot.v2d_coloc
               |        PREWHERE coloc_h4 > 0.8
               |        GROUP BY
               |            left_study,
               |            left_chrom,
               |            left_pos,
               |            left_ref,
               |            left_alt,
               |            right_gene_id
               |    ) AS C USING (study_id, enumChrom, pos, ref, alt, gene_id)
               |    PREWHERE (study_id = $studyId) AND
               |      (chrom = ${v.chromosome}) AND
               |      (pos = ${v.position}) AND
               |      (ref = ${v.refAllele}) AND
               |      (alt = ${v.altAllele})
               |) AS LL
               |ANY LEFT JOIN
               |(
               |    SELECT
               |        chr_id AS chrom,
               |        position AS pos,
               |        ref_allele AS ref,
               |        alt_allele AS alt,
               |        gene_id,
               |        d
               |    FROM ot.v2g_scored
               |    PREWHERE (type_id = 'distance') AND
               |    (source_id = 'canonical_tss') AND
               |    (chrom = ${v.chromosome}) AND
               |      (pos = ${v.position}) AND
               |      (ref = ${v.refAllele}) AND
               |      (alt = ${v.altAllele})
               |) AS G ON (LL.enumChrom = G.chrom) AND (LL.pos = G.pos) AND (LL.ref = G.ref) AND (LL.alt = G.alt) AND (LL.gene_id = G.gene_id)
               |#$limitClause
         """.stripMargin.as[SLGRow]

        /* WHERE (study_id = $studyId) AND (chrom = '20') AND (pos = 35437976) AND (ref = 'A') AND
         * (alt = 'G') */
        db.run(slgQ.asTry).map {
          case Success(v) => SLGTable(studyId, variant.right.get.id, v)
          case Failure(ex) =>
            logger.error(ex.getMessage)
            SLGTable(studyId, variant.right.get.id, Vector.empty)
        }
      case Left(violation) =>
        Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def buildIndexVariantAssocTable(variantID: String,
                                  pageIndex: Option[Int],
                                  pageSize: Option[Int]
  ): Future[VariantToDiseaseTable] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant.fromString(variantID)

    variant match {
      case Right(v) =>
        val q = v2DsByChrPos
          .filter(r =>
            (r.tagChromosome === v.chromosome) &&
              (r.leadPosition === v.position) &&
              (r.leadRefAllele === v.refAllele) &&
              (r.leadAltAllele === v.altAllele)
          )
          .drop(limitPair._1)
          .take(limitPair._2)

        db.run(q.result.asTry).map {
          case Success(r) =>
            Entities.VariantToDiseaseTable(r)
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Entities.VariantToDiseaseTable(associations = Vector.empty)
        }

      case Left(violation) =>
        Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def buildTagVariantAssocTable(variantID: String,
                                pageIndex: Option[Int],
                                pageSize: Option[Int]
  ): Future[VariantToDiseaseTable] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant.fromString(variantID)

    variant match {
      case Right(v) =>
        val q = v2DsByChrPos
          .filter(r =>
            (r.tagChromosome === v.chromosome) &&
              (r.tagPosition === v.position) &&
              (r.tagRefAllele === v.refAllele) &&
              (r.tagAltAllele === v.altAllele)
          )
          .drop(limitPair._1)
          .take(limitPair._2)

        db.run(q.result.asTry).map {
          case Success(r) =>
            Entities.VariantToDiseaseTable(r)
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Entities.VariantToDiseaseTable(associations = Vector.empty)
        }

      case Left(violation) =>
        Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def buildRegionPlot(studyId: Option[String],
                      variantId: Option[String],
                      geneId: Option[String]
  ): Future[Option[Entities.Gecko]] =
    (studyId, variantId, geneId) match {
      case (None, None, None) =>
        Future.failed(
          InputParameterCheckError(Vector(new BaseViolation("At least one argument is needed") {}))
        )
      case (Some(_), None, _) =>
        Future.failed(
          InputParameterCheckError(
            Vector(new BaseViolation("A study ID needs a lead variant too") {})
          )
        )
      case triple =>
        val v = (r: D2V2GScored) =>
          triple._2.flatMap(o =>
            Variant
              .fromString(o)
              .toOption
              .map { iv =>
                val sv = iv.toSimpleVariant
                (r.leadChromosome === sv.chromosome
                && r.leadPosition === sv.position
                && r.leadRefAllele === sv.refAllele
                && r.leadAltAllele === sv.altAllele)
              }
          )

        val gchr = (gid: String) => genes.filter(_.id === gid).map(_.chromosome)
        val g = (r: D2V2GScored) =>
          triple._3.map(gid =>
            r.geneId === gid
              && r.leadChromosome.fToString.in(gchr(gid))
          )
        val s = (r: D2V2GScored) => triple._1.map(sid => r.studyId === sid)
        val qExpr = v :: g :: s :: Nil

        val assocsQ =
          d2v2gScored
            .filter(r =>
              qExpr
                .map(_.apply(r))
                .withFilter(_.isDefined)
                .map(_.get)
                .reduce((a, b) => a && b)
            )
            .groupBy(r => (r.studyId, r.leadVariant, r.tagVariant, r.geneId))
            .map { case (g, q) =>
              g -> (q.map(_.r2).any,
              q.map(_.log10Abf).any,
              q.map(_.posteriorProbability).any,
              q.map(_.pval).any,
              q.map(_.pvalExponent).any,
              q.map(_.pvalMantissa).any,
              q.map(_.overallScore).any,
              q.map(_.oddsCI).any,
              q.map(_.oddsCILower).any,
              q.map(_.oddsCIUpper).any,
              q.map(_.direction).any,
              q.map(_.betaCI).any,
              q.map(_.betaCILower).any,
              q.map(_.betaCIUpper).any)
            }
            .result

        db.run(assocsQ.asTry).map {
          case Success(assocs) =>
            val geckoRows = assocs.view
              .map(r =>
                GeckoRow(
                  r._1._4,
                  r._1._3,
                  r._1._2,
                  r._1._1,
                  V2DAssociation(r._2._4.get,
                                 r._2._5.get,
                                 r._2._6.get,
                                 r._2._1,
                                 r._2._2,
                                 r._2._3,
                                 None,
                                 None,
                                 None,
                                 None,
                                 None
                  ),
                  r._2._7.getOrElse(0d),
                  V2DOdds(r._2._8, r._2._9, r._2._10),
                  V2DBeta(r._2._11, r._2._12, r._2._13, r._2._14)
                )
              )
            Entities.Gecko(geckoRows, assocs.view.map(_._1._4).toSet)

          case Failure(asscsEx) =>
            logger.error(asscsEx.getMessage)
            Entities.Gecko(Seq.empty.view, Set.empty)
        }
    }

  def buildGecko(chromosome: String, posStart: Long, posEnd: Long): Future[Option[Entities.Gecko]] =
    (parseChromosome(chromosome), parseRegion(posStart, posEnd)) match {
      case (Right(chr), Right((start, end))) =>
        val inRegion = Region(chr, start, end)
        if (denseRegionChecker.matchRegion(inRegion)) {
          Future.failed(InputParameterCheckError(Vector(RegionViolation(inRegion))))
        } else {
          val geneIdsInLoci = geneIdByRegion(chr, start, end)

          val assocsQ = d2v2gScored
            .filter(r =>
              (r.leadChromosome === chr) && (
                ((r.leadPosition >= start) && (r.leadPosition <= end)) ||
                  ((r.tagPosition >= start) && (r.tagPosition <= end)) ||
                  (r.geneId in geneIdsInLoci)
              )
            )
            .groupBy(r => (r.studyId, r.leadVariant, r.tagVariant, r.geneId))
            .map { case (g, q) =>
              g -> (q.map(_.r2).any,
              q.map(_.log10Abf).any,
              q.map(_.posteriorProbability).any,
              q.map(_.pval).any,
              q.map(_.pvalExponent).any,
              q.map(_.pvalMantissa).any,
              q.map(_.overallScore).any,
              q.map(_.oddsCI).any,
              q.map(_.oddsCILower).any,
              q.map(_.oddsCIUpper).any,
              q.map(_.direction).any,
              q.map(_.betaCI).any,
              q.map(_.betaCILower).any,
              q.map(_.betaCIUpper).any)
            }

          db.run(geneIdsInLoci.result.asTry zip assocsQ.result.asTry).map {
            case (Success(geneIds), Success(assocs)) =>
              val geckoRows = assocs.view
                .map(r =>
                  GeckoRow(
                    r._1._4,
                    r._1._3,
                    r._1._2,
                    r._1._1,
                    V2DAssociation(r._2._4.get,
                                   r._2._5.get,
                                   r._2._6.get,
                                   r._2._1,
                                   r._2._2,
                                   r._2._3,
                                   None,
                                   None,
                                   None,
                                   None,
                                   None
                    ),
                    r._2._7.getOrElse(0d),
                    V2DOdds(r._2._8, r._2._9, r._2._10),
                    V2DBeta(r._2._11, r._2._12, r._2._13, r._2._14)
                  )
                )
              Entities.Gecko(geckoRows, geneIds.toSet)

            case (Success(geneIds), Failure(asscsEx)) =>
              logger.error(asscsEx.getMessage)
              Entities.Gecko(Seq().view, geneIds.toSet)

            case (_, _) =>
              logger.error(
                "Something really wrong happened while getting geneIds from gene " +
                  "dictionary and also from d2v2g table"
              )
              Entities.Gecko(Seq().view, Set.empty)
          }
        }

      case (chrEither, rangeEither) =>
        Future.failed(Backend.inputParameterCheckErrorGenerator(chrEither, rangeEither))
    }

  def buildG2VByVariant(variantId: String): Future[Seq[Entities.G2VAssociation]] = {
    val variant = DNA.Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val geneIdsInLoci = genes
          .filter(r => r.chromosome === v.chromosome)
          .map(_.id)

        val filteredV2Gs = v2gsScored.filter(r =>
          (r.chromosome === v.chromosome) &&
            (r.position === v.position) &&
            (r.refAllele === v.refAllele) &&
            (r.altAllele === v.altAllele) &&
            (r.geneId in geneIdsInLoci)
        )

        db.run(filteredV2Gs.result.asTry).map {
          case Success(r) =>
            r.view
              .map { p =>
                val v2g = p.v2g
                val score = p.pureOverallScoreRow

                ScoredG2VLine(
                  v2g.geneId,
                  v2g.variant.id,
                  score.overallScore,
                  (score.sources zip score.sourceScores).toMap,
                  v2g.typeId,
                  v2g.sourceId,
                  v2g.feature,
                  v2g.fpred.maxLabel,
                  v2g.fpred.maxScore,
                  v2g.qtl.beta,
                  v2g.qtl.se,
                  v2g.qtl.pval,
                  v2g.interval.score,
                  v2g.qtl.scoreQ,
                  v2g.interval.scoreQ,
                  v2g.distance.distance,
                  v2g.distance.score,
                  v2g.distance.scoreQ
                )
              }
              .groupBy(_.geneId)
              .mapValues(G2VAssociation(_))
              .values
              .toSeq

          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  /** query toplocus stats given study and locus information we dont need tag information so we
    * distinct it
    */
  def getStudyAndLeadVariantInfo(studyId: String, variantId: String): Future[Option[LeadRow]] = {
    val variant = DNA.Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = v2DsByStudy
          .filter { r =>
            r.studyId === studyId &&
            r.leadChromosome === v.chromosome &&
            r.leadPosition === v.position &&
            r.leadRefAllele === v.refAllele &&
            r.leadAltAllele === v.altAllele
          }
          .map(_.studyIdAndLeadVariantStats)
          .distinct

        db.run(q.result.asTry).map {
          case Success(r) => r.headOption

          case Failure(ex) =>
            logger.error(ex.getMessage)
            None
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def getStudiesAndLeadVariantsForGene(geneId: String): Future[Seq[LeadRow]] = {
    val q1 = genes
      .filter(_.id === geneId)
      .take(1)
      .result
      .headOption
      .flatMap {
        case Some(g) =>
          d2v2gScored
            .filter(l =>
              (l.leadChromosome === g.chromosome) &&
                (l.geneId === g.id)
            )
            .map(r => r.leadRow)
            .distinct
            .result

        case None =>
          DBIOAction.successful(Seq.empty)
      }

    executeQueryForSeq(q1)
  }

}

object Backend {

  /** Adds one or more violations to the InputParameterCheckError object.
    * @param violations
    *   to add to IPCE
    * @return
    *   InputParameterCheckError with violations
    */
  def inputParameterCheckErrorGenerator(
      violations: Either[Violation, Any]*
  ): InputParameterCheckError =
    InputParameterCheckError(violations.filter(_.isLeft).map(_.left.get).toVector)

}
