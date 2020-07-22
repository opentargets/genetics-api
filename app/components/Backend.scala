package components

import java.nio.file.{Path, Paths}

import components.clickhouse.ClickHouseProfile
import components.elasticsearch.{ElasticsearchComponent, Pagination}
import configuration.{Metadata, MetadataConfiguration}
import javax.inject.{Inject, Singleton}
import models.Functions._
import models.database.FRM._
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
import sangria.validation.Violation
import slick.dbio.DBIOAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

@Singleton
class Backend @Inject()(
                         @NamedDatabase("default") protected val dbConfigProvider: DatabaseConfigProvider,
                         @NamedDatabase("sumstats") protected val dbConfigProviderSumStats: DatabaseConfigProvider,
                         elasticsearch: ElasticsearchComponent,
                         config: Configuration,
                         env: Environment) {

  // Import config and settings files
  private val dbConfig = dbConfigProvider.get[ClickHouseProfile]
  private val dbConfigSumStats = dbConfigProviderSumStats.get[ClickHouseProfile]
  private val db = dbConfig.db
  private val dbSS = dbConfigSumStats.db
  private val logger: Logger = Logger(this.getClass)

  val denseRegionsPath: Path =
    Paths.get(env.rootPath.getAbsolutePath, "resources", "dense_regions.tsv")

  val denseRegionChecker: DenseRegionChecker = DenseRegionChecker(denseRegionsPath.toString)

  val v2gLabelsPath: Path =
    Paths.get(env.rootPath.getAbsolutePath, "resources", "v2g_display_labels.json")

  val v2gLabels: Map[String, JsValue] =
    loadJSONLinesIntoMap[String, JsValue](v2gLabelsPath.toString)(l =>
      (l \ "key").as[String] -> (l \ "value").get)

  val v2gBiofeatureLabelsPath: Path =
    Paths.get(env.rootPath.getAbsolutePath, "resources", "v2g_biofeature_labels.json")

  val v2gBiofeatureLabels: Map[String, String] =
    loadJSONLinesIntoMap(v2gBiofeatureLabelsPath.toString)(l =>
      (l \ "biofeature_code").as[String] -> (l \ "label").as[String])

  import dbConfig.profile.api._

  lazy val genes = TableQuery[Genes]
  lazy val variants = TableQuery[Variants]
  lazy val studies = TableQuery[Studies]
  lazy val overlaps = TableQuery[Overlaps]
  lazy val v2gStructures = TableQuery[V2GStructure]
  lazy val v2DsByChrPos = TableQuery[V2DsByChrPos]
  lazy val v2DsByStudy = TableQuery[V2DsByStudy]
  lazy val v2gs = TableQuery[V2G]
  lazy val v2gScores = TableQuery[V2GOverallScore]
  lazy val d2v2g = TableQuery[D2V2G]
  lazy val d2v2gScored = TableQuery[D2V2GScored]
  lazy val d2v2gScores = TableQuery[D2V2GOverallScore]
  lazy val sumstatsGWAS = TableQuery[SumStatsGWAS]
  lazy val sumstatsMolTraits = TableQuery[SumStatsMolTraits]
  lazy val colocs = TableQuery[Coloc]
  lazy val credsets = TableQuery[CredSet]

  def buildPhewFromSumstats(
                             variantID: String,
                             pageIndex: Option[Int],
                             pageSize: Option[Int]): Future[Seq[SumStatsGWASRow]] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant.fromString(variantID)

    variant match {
      case Right(v) =>
        val q = sumstatsGWAS
          .filter(
            r =>
              (r.chrom === v.chromosome) &&
                (r.pos === v.position) &&
                (r.ref === v.refAllele) &&
                (r.alt === v.altAllele))
          .drop(limitPair._1)
          .take(limitPair._2)

        dbSS.run(q.result.asTry).map {
          case Success(vec) => vec
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
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
            .filter(
              r =>
                (r.lChrom === g.chromosome) &&
                  (r.rGeneId === g.id) &&
                  (r.lType === GWASLiteral) &&
                  (r.rType =!= GWASLiteral))
            .result

        case None =>
          DBIOAction.successful(Seq.empty)
      }

    db.run(q1.asTry).map {
      case Success(v) => v.seq
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Seq.empty
    }
  }

  def gwasColocalisationForRegion(
                                   chromosome: String,
                                   startPos: Long,
                                   endPos: Long): Future[Seq[ColocRow]] = {
    (parseChromosome(chromosome), parseRegion(startPos, endPos, 500000L)) match {
      case (Right(chr), Right((start, end))) =>
        val q = colocs
          .filter(
            r =>
              (r.lChrom === chromosome) &&
                (r.lPos >= startPos) &&
                (r.lPos <= endPos) &&
                (r.rPos >= startPos) &&
                (r.rPos <= endPos) &&
                (r.rType === GWASLiteral))

        db.run(q.result.asTry).map {
          case Success(vec) => vec
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case (chrEither, rangeEither) =>
        Future.failed(
          InputParameterCheckError(
            Vector(chrEither, rangeEither)
              .filter(_.isLeft)
              .map(_.left.get)
              .asInstanceOf[Vector[Violation]]))
    }
  }

  def gwasColocalisation(studyId: String, variantId: String): Future[Seq[ColocRow]] = {
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = colocs
          .filter(
            r =>
              (r.lChrom === v.chromosome) &&
                (r.lPos === v.position) &&
                (r.lRef === v.refAllele) &&
                (r.lAlt === v.altAllele) &&
                (r.lStudy === studyId) &&
                (r.rType === GWASLiteral))

        db.run(q.result.asTry).map {
          case Success(vec) => vec
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def qtlColocalisation(studyId: String, variantId: String): Future[Seq[ColocRow]] = {
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = colocs
          .filter(
            r =>
              (r.lChrom === v.chromosome) &&
                (r.lPos === v.position) &&
                (r.lRef === v.refAllele) &&
                (r.lAlt === v.altAllele) &&
                (r.lStudy === studyId) &&
                (r.rType =!= GWASLiteral))

        db.run(q.result.asTry).map {
          case Success(vec) => vec
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def gwasCredibleSet(
                       studyId: String,
                       variantId: String): Future[Seq[(SimpleVariant, CredSetRowStats)]] = {
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = credsets
          .filter(
            r =>
              (r.leadChromosome === v.chromosome) &&
                (r.leadPosition === v.position) &&
                (r.leadRefAllele === v.refAllele) &&
                (r.leadAltAllele === v.altAllele) &&
                (r.studyId === studyId) &&
                (r.dataType === GWASLiteral))
          .map(_.tagVariantWithStats)

        db.run(q.result.asTry).map {
          case Success(vec) => vec
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def qtlCredibleSet(
                      studyId: String,
                      variantId: String,
                      phenotypeId: String,
                      bioFeatureId: String): Future[Seq[(SimpleVariant, CredSetRowStats)]] = {
    val variant = Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = credsets
          .filter(
            r =>
              (r.leadChromosome === v.chromosome) &&
                (r.leadPosition === v.position) &&
                (r.leadRefAllele === v.refAllele) &&
                (r.leadAltAllele === v.altAllele) &&
                (r.studyId === studyId) &&
                (r.dataType =!= GWASLiteral) &&
                (r.phenotypeId === phenotypeId) &&
                (r.bioFeature === bioFeatureId))
          .map(_.tagVariantWithStats)

        db.run(q.result.asTry).map {
          case Success(vec) => vec
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def gwasRegionalFromSumstats(
                                studyId: String,
                                chromosome: String,
                                startPos: Long,
                                endPos: Long): Future[Seq[(SimpleVariant, Double)]] = {
    (parseChromosome(chromosome), parseRegion(startPos, endPos)) match {
      case (Right(chr), Right((start, end))) =>
        val q = sumstatsGWAS
          .filter(
            r =>
              (r.chrom === chr) &&
                (r.pos >= startPos) &&
                (r.pos <= endPos) &&
                (r.studyId === studyId))
          .map(_.variantAndPVal)

        dbSS.run(q.result.asTry).map {
          case Success(xs) => xs
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case (chrEither, rangeEither) =>
        Future.failed(
          InputParameterCheckError(
            Vector(chrEither, rangeEither)
              .filter(_.isLeft)
              .map(_.left.get)
              .asInstanceOf[Vector[Violation]]))
    }
  }

  def qtlRegionalFromSumstats(
                               studyId: String,
                               bioFeature: String,
                               phenotypeId: String,
                               chromosome: String,
                               startPos: Long,
                               endPos: Long): Future[Seq[(SimpleVariant, Double)]] = {
    (parseChromosome(chromosome), parseRegion(startPos, endPos)) match {
      case (Right(chr), Right((start, end))) =>
        val q = sumstatsMolTraits
          .filter(
            r =>
              (r.chrom === chr) &&
                (r.pos >= startPos) &&
                (r.pos <= endPos) &&
                (r.bioFeature === bioFeature) &&
                (r.phenotypeId === phenotypeId) &&
                (r.studyId === studyId))
          .map(_.variantAndPVal)

        dbSS.run(q.result.asTry).map {
          case Success(xs) => xs
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }
      case (chrEither, rangeEither) =>
        Future.failed(
          InputParameterCheckError(
            Vector(chrEither, rangeEither)
              .filter(_.isLeft)
              .map(_.left.get)
              .asInstanceOf[Vector[Violation]]))
    }
  }

  def getMetadata: Future[Metadata] = {
    import MetadataConfiguration._
    Future(config.get[Metadata]("ot.meta"))
  }

  def getG2VSchema: Future[Entities.G2VSchema] = {
    def toSeqStruct(elems: Map[(String, String), Seq[String]]) = {
      (for {
        entry <- elems
      } yield Entities.G2VSchemaElement(
        entry._1._1,
        entry._1._2,
        (v2gLabels(entry._1._2) \ "display_label").asOpt[String],
        (v2gLabels(entry._1._2) \ "overview_tooltip").asOpt[String],
        (v2gLabels(entry._1._2) \ "tab_subtitle").asOpt[String],
        (v2gLabels(entry._1._2) \ "pmid").asOpt[String],
        entry._2.map(Tissue).toVector)).toSeq
    }

    db.run(v2gStructures.result.asTry).map {
      case Success(v) =>
        val mappedRows =
          v.groupBy(r => (r.typeId, r.sourceId)).mapValues(_.flatMap(_.bioFeatureSet))
        val qtlElems = toSeqStruct(mappedRows.filterKeys(p => defaultQtlTypes.contains(p._1)))
        val intervalElems =
          toSeqStruct(mappedRows.filterKeys(p => defaultIntervalTypes.contains(p._1)))
        val fpredElems = toSeqStruct(mappedRows.filterKeys(p => defaultFPredTypes.contains(p._1)))
        val distanceElems =
          toSeqStruct(mappedRows.filterKeys(p => defaultDistanceTypes.contains(p._1)))

        G2VSchema(qtlElems, intervalElems, fpredElems, distanceElems)
      case Failure(ex) =>
        logger.error(ex.getMessage)
        G2VSchema(Seq.empty, Seq.empty, Seq.empty, Seq.empty)
    }
  }

  def search(query: String, pagination: Option[Pagination]): Future[SearchResultSet] = {
    elasticsearch.search(query, pagination.getOrElse(Pagination.mkDefault))
  }

  /** get top Functions.defaultTopOverlapStudiesSize studies sorted desc by
   * the number of overlapped loci
   *
   * @param stid given a study ID
   * @return a Entities.OverlappedLociStudy which could contain empty list of ovelapped studies
   */
  def getTopOverlappedStudies(
                               stid: String,
                               pageIndex: Option[Int] = Some(0),
                               pageSize: Option[Int] = Some(defaultTopOverlapStudiesSize))
  : Future[Entities.OverlappedLociStudy] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val limitClause = parsePaginationTokens(pageIndex, pageSize)
    val tableName = "studies_overlap"

    // TODO generate a vararg select expression for uniq instead a column expression
    val plainQ =
      sql"""
           |select
           | B_study_id,
           | uniq(A_chrom, A_pos, A_ref, A_alt) as num_overlaps
           |from #$tableName
           |prewhere A_study_id = $stid
           |group by B_study_id
           |order by num_overlaps desc
           |#$limitClause
         """.stripMargin.as[(String, Int)]

//    val q = overlaps
//      .filter(_.studyIdA === stid)
//      .groupBy(_.studyIdB)
//      .map(r => (r._1, r._2.map(l => l.variantA).distinct.length))
//      .sortBy(_._2.desc)
//      .drop(limitPair._1)
//      .take(limitPair._2)
//
//    q.result.statements.foreach(println)

    db.run(plainQ.asTry).map {
      case Success(v) =>
        if (v.nonEmpty) {
          OverlappedLociStudy(stid, v.map(t => OverlapRow(t._1, t._2)))
        } else {
          OverlappedLociStudy(stid, Vector.empty)
        }
      case Failure(ex) =>
        logger.error(ex.getMessage)
        OverlappedLociStudy(stid, Vector.empty)
    }
  }

  def getOverlapVariantsIntersectionForStudies(
                                                stid: String,
                                                stids: Seq[String]): Future[Vector[String]] = {
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
  }

  def getOverlapVariantsForStudies(
                                    stid: String,
                                    stids: Seq[String]): Future[Vector[Entities.OverlappedVariantsStudy]] = {
    val q =
      overlaps
        .filter(r => (r.studyIdA === stid) && (r.studyIdB inSetBind stids))
        .groupBy(r => (r.studyIdB, r.variantA, r.variantB))
        .map {
          case (l, g) =>
            (l._1, l._2, l._3) -> (g.map(_.overlapsAB).any, g.map(_.distinctA).any, g
              .map(_.distinctB)
              .any)
        }

    db.run(q.result.asTry).map {
      case Success(v) =>
        if (v.nonEmpty) {
          v.view
            .groupBy(_._1._1)
            .map(
              pair =>
                OverlappedVariantsStudy(
                  pair._1,
                  pair._2.map(
                    t =>
                      OverlappedVariant(
                        t._1._2.id,
                        t._1._3.id,
                        t._2._1.get,
                        t._2._2.get,
                        t._2._3.get))))
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
    db.run(geneQ.result.head.asTry).map {
      case Success(s) => s
      case Failure(ex) =>
        logger.error(ex.getMessage)
        ""
    }
  }

  def getStudiesForGene(geneId: String): Future[Vector[String]] = {
    val geneChr: Future[String] = getGeneChromosome(geneId)
    for (chr <- geneChr) yield {
      val studiesQ =
        d2v2g
          .filter(r => (r.geneId === geneId) && (r.tagChromosome === chr))
          .groupBy(_.studyId)
          .map(_._1)

      db.run(studiesQ.result.asTry).map {
        case Success(v) => v.toVector
        case Failure(ex) =>
          logger.error(ex.getMessage)
          Vector.empty
      }
    }
  }.flatten

  def getStudiesAndLeadVariantsForGeneByL2G(
                                             geneId: String,
                                             pageIndex: Option[Int],
                                             pageSize: Option[Int]): Future[Vector[V2DL2GRowByGene]] = {
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

    db.run(topLociEnrich.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }

  def getGenesByRegion(chromosome: String, startPos: Long, endPos: Long): Future[Seq[Gene]] = {
    (parseChromosome(chromosome), parseRegion(startPos, endPos)) match {
      case (Right(chr), Right((start, end))) =>
        val q = genes
          .filter(
            r =>
              (r.chromosome === chr) &&
                ((r.start >= start && r.start <= end) || (r.end >= start && r.end <= end)))

        db.run(q.result.asTry).map {
          case Success(v) => v
          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case (chrEither, rangeEither) =>
        Future.failed(
          InputParameterCheckError(
            Vector(chrEither, rangeEither)
              .filter(_.isLeft)
              .map(_.left.get)
              .asInstanceOf[Vector[Violation]]))
    }
  }

  def getGenes(geneIds: Seq[String]): Future[Seq[Gene]] = {
    if (geneIds.nonEmpty) {
      val q = genes.filter(r => r.id inSetBind geneIds)

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

  /** query variants table with a list of variant ids and get all related information
    *
    * NOTE! WARNING! AT THE MOMENT, THE WAY TO DO IS USING THE VARIANT APPLY CONSTRUCTOR FROM A
    * STRING TO GET A WHITE-LABEL VARIANT WITH NO OTHER REFERENCES FROM RSID OR NEAREST GENES
    * (NONCODING AND PROTCODING)
    */
  def getVariants(variantIds: Seq[String]): Future[Seq[DNA.Variant]] = {
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
  }

  def getStudies(stids: Seq[String]): Future[Seq[Study]] = {
    if (stids.nonEmpty) {
      val q = studies.filter(_.studyId inSetBind stids)

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

  def buildManhattanTable(
                           studyId: String,
                           pageIndex: Option[Int],
                           pageSize: Option[Int]): Future[Entities.ManhattanTable] = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)

    val topLociEnrich =
      sql"""
           |SELECT *
           |FROM ot.manhattan_with_l2g
           |WHERE study = $studyId
           |#$limitClause
         """.stripMargin.as[V2DByStudy]

    // |#$limitClause
    // map to proper manhattan association with needed fields
    db.run(topLociEnrich.asTry).map {
      case Success(v) =>
        ManhattanTable(
          studyId,
          v.map(el => {
            ManhattanAssociation(
              el.variantId,
              el.pval,
              el.pval_mantissa,
              el.pval_exponent,
              el.v2dOdds,
              el.v2dBeta,
              el.topGenes,
              el.topColocGenes,
              el.topL2Genes,
              el.credibleSetSize,
              el.ldSetSize,
              el.totalSetSize)
          }))
      case Failure(ex) =>
        logger.error(ex.getMessage)
        ManhattanTable(studyId, associations = Vector.empty)
    }
  }

  def buildSLGTable(
                     studyId: String,
                     variantId: String,
                     pageIndex: Option[Int],
                     pageSize: Option[Int]): Future[Entities.SLGTable] = {
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
               |    FROM ot.v2g
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

  def buildIndexVariantAssocTable(
                                   variantID: String,
                                   pageIndex: Option[Int],
                                   pageSize: Option[Int]): Future[VariantToDiseaseTable] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant.fromString(variantID)

    variant match {
      case Right(v) =>
        val q = v2DsByChrPos
          .filter(
            r =>
              (r.tagChromosome === v.chromosome) &&
                (r.leadPosition === v.position) &&
                (r.leadRefAllele === v.refAllele) &&
                (r.leadAltAllele === v.altAllele))
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

  def buildTagVariantAssocTable(
                                 variantID: String,
                                 pageIndex: Option[Int],
                                 pageSize: Option[Int]): Future[VariantToDiseaseTable] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant.fromString(variantID)

    variant match {
      case Right(v) =>
        val q = v2DsByChrPos
          .filter(
            r =>
              (r.tagChromosome === v.chromosome) &&
                (r.tagPosition === v.position) &&
                (r.tagRefAllele === v.refAllele) &&
                (r.tagAltAllele === v.altAllele))
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

  def buildGecko(
                  chromosome: String,
                  posStart: Long,
                  posEnd: Long): Future[Option[Entities.Gecko]] = {
    (parseChromosome(chromosome), parseRegion(posStart, posEnd)) match {
      case (Right(chr), Right((start, end))) =>
        val inRegion = Region(chr, start, end)
        if (denseRegionChecker.matchRegion(inRegion)) {
          Future.failed(InputParameterCheckError(Vector(RegionViolation(inRegion))))
        } else {
          val geneIdsInLoci = genes
            .filter(
              r =>
                (r.chromosome === chr) &&
                  ((r.start >= start && r.start <= end) ||
                    (r.end >= start && r.end <= end)))
            .map(_.id)

          val assocsQ = d2v2gScored
            .filter(
              r =>
                (r.leadChromosome === chr) && (
                  ((r.leadPosition >= start) && (r.leadPosition <= end)) ||
                    ((r.tagPosition >= start) && (r.tagPosition <= end)) ||
                    (r.geneId in geneIdsInLoci)
                  ))
            .groupBy(r => (r.studyId, r.leadVariant, r.tagVariant, r.geneId))
            .map {
              case (g, q) =>
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
                .map(
                  r =>
                    GeckoRow(
                      r._1._4,
                      r._1._3,
                      r._1._2,
                      r._1._1,
                      V2DAssociation(
                        r._2._4.get,
                        r._2._5.get,
                        r._2._6.get,
                        r._2._1,
                        r._2._2,
                        r._2._3,
                        None,
                        None,
                        None,
                        None,
                        None),
                      r._2._7.getOrElse(0d),
                      V2DOdds(r._2._8, r._2._9, r._2._10),
                      V2DBeta(r._2._11, r._2._12, r._2._13, r._2._14)))
              Entities.Gecko(geckoRows, geneIds.toSet)

            case (Success(geneIds), Failure(asscsEx)) =>
              logger.error(asscsEx.getMessage)
              Entities.Gecko(Seq().view, geneIds.toSet)

            case (_, _) =>
              logger.error(
                "Something really wrong happened while getting geneIds from gene " +
                  "dictionary and also from d2v2g table")
              Entities.Gecko(Seq().view, Set.empty)
          }
        }

      case (chrEither, rangeEither) =>
        Future.failed(
          InputParameterCheckError(
            Vector(chrEither, rangeEither)
              .filter(_.isLeft)
              .map(_.left.get)
              .asInstanceOf[Vector[Violation]]))
    }
  }

  def buildG2VByVariant(variantId: String): Future[Seq[Entities.G2VAssociation]] = {
    val variant = DNA.Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val geneIdsInLoci = genes
          .filter(r => r.chromosome === v.chromosome)
          .map(_.id)

        val filteredV2Gs = v2gs.filter(
          r =>
            (r.chromosome === v.chromosome) &&
              (r.position === v.position) &&
              (r.refAllele === v.refAllele) &&
              (r.altAllele === v.altAllele) &&
              (r.geneId in geneIdsInLoci))

        val filteredV2GScores = v2gScores.filter(
          r =>
            (r.chromosome === v.chromosome) &&
              (r.position === v.position) &&
              (r.refAllele === v.refAllele) &&
              (r.altAllele === v.altAllele) &&
              (r.geneId in geneIdsInLoci))

        val q = filteredV2Gs
          .joinFull(filteredV2GScores)
          .on((l, r) => l.geneId === r.geneId)
          .map(p => (p._1, p._2))

        db.run(q.result.asTry).map {
          case Success(r) =>
            r.view
              .filter(p => p._1.isDefined && p._2.isDefined)
              .map(p => {
                val (Some(v2g), Some(score)) = p

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
                  v2g.distance.scoreQ)
              })
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

  /** query toplocus stats given study and locus information we dont need tag information
    * so we distinct it
    */
  def getStudyAndLeadVariantInfo(studyId: String, variantId: String): Future[Option[LeadRow]] = {
    val variant = DNA.Variant.fromString(variantId)

    variant match {
      case Right(v) =>
        val q = v2DsByStudy
          .filter(r => {
            r.studyId === studyId &&
              r.leadChromosome === v.chromosome &&
              r.leadPosition === v.position &&
              r.leadRefAllele === v.refAllele &&
              r.leadAltAllele === v.altAllele
          })
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
    val lociRange: Long = 5000000

    val q1 = genes
      .filter(_.id === geneId)
      .take(1)
      .result
      .headOption
      .flatMap {
        case Some(g) =>
          d2v2gScored
            .filter(
              l =>
                (l.tagChromosome === g.chromosome) &&
                  (l.tagPosition <= (g.tss.get + lociRange)) &&
                  (l.tagPosition >= (g.tss.get - lociRange)) &&
                  (l.geneId === g.id))
            .map(r => r.leadRow)
            .distinct
            .result

        case None =>
          DBIOAction.successful(Seq.empty)
      }

    db.run(q1.asTry).map {
      case Success(v) => v
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Seq.empty
    }
  }

}
