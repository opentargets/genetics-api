package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import play.api.Configuration
import clickhouse.ClickHouseProfile
import com.sksamuel.elastic4s.ElasticsearchClientUri
import models.Entities._
import models.Functions._
import models.DNA._
import models.Entities.DBImplicits._
import models.Entities.ESImplicits._
import models.Violations._
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
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import java.nio.file.{Path, Paths}

import models.FRM.{D2V2G, D2V2GOverallScore, D2V2GScored, Genes, Overlaps, Studies, V2DsByChrPos, V2DsByStudy, V2G, V2GOverallScore, V2GStructure, Variants}

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

  val v2gLabelsPath: Path = Paths.get(env.rootPath.getAbsolutePath, "resources", "v2g_display_labels.json")
  val v2gLabels = loadJSONFromFilename(v2gLabelsPath.toString)

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

  // you must import the DSL to use the syntax helpers
  import com.sksamuel.elastic4s.http.ElasticDsl._
  val esUri = ElasticsearchClientUri(config.get[String]("ot.elasticsearch.host"),
    config.get[Int]("ot.elasticsearch.port"))
  val esQ = HttpClient(esUri)

  def buildPheWASTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]):
  Future[Entities.PheWASTable] = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)
    val variant = Variant(variantID)

    variant match {
      case Right(v) =>
        val segment = toSumStatsSegment(v.position)
        val tableName = gwasSumStatsTName format v.chromosome
        val query =
          sql"""
               |select
               | study_id,
               | pval,
               | beta,
               | se,
               | eaf,
               | maf,
               | n_samples_variant_level,
               | n_samples_study_level,
               | n_cases_study_level,
               | n_cases_variant_level,
               | if(is_cc,exp(beta),NULL) as odds_ratio,
               | chip,
               | info
               |from #$tableName
               |prewhere chrom = ${v.chromosome} and
               |  pos_b37 = ${v.position} and
               |  segment = $segment and
               |  variant_id_b37 = ${v.id}
               |#$limitClause
         """.stripMargin.as[VariantPheWAS]

        dbSS.run(query.asTry).map {
          case Success(traitVector) => PheWASTable(traitVector)
          case Failure(ex) =>
            logger.error(ex.getMessage)
            PheWASTable(associations = Vector.empty)
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  def getG2VSchema: Future[Entities.G2VSchema] = {
    def toSeqStruct(elems: Map[(String, String), Seq[String]]) = {
      (for {
        entry <- elems
      } yield Entities.G2VSchemaElement(entry._1._1, entry._1._2,
        v2gLabels.flatMap(s => (s \ entry._1._2 \ "display_label").asOpt[String]),
        v2gLabels.flatMap(s => (s \ entry._1._2 \ "overview_tooltip").asOpt[String]),
        v2gLabels.flatMap(s => (s \ entry._1._2 \ "tab_subtitle").asOpt[String]),
        v2gLabels.flatMap(s => (s \ entry._1._2 \ "pmid").asOpt[String]),
        entry._2.map(Tissue).toVector)).toSeq
    }

    db.run(v2gStructures.result.asTry).map {
      case Success(v) =>
        val mappedRows = v.groupBy(r => (r.typeId, r.sourceId)).mapValues(_.flatMap(_.bioFeatureSet))
        val qtlElems = toSeqStruct(mappedRows.filterKeys(p => defaultQtlTypes.contains(p._1)))
        val intervalElems = toSeqStruct(mappedRows.filterKeys(p => defaultIntervalTypes.contains(p._1)))
        val fpredElems = toSeqStruct(mappedRows.filterKeys(p => defaultFPredTypes.contains(p._1)))
        val distanceElems = toSeqStruct(mappedRows.filterKeys(p => defaultDistanceTypes.contains(p._1)))

        G2VSchema(qtlElems, intervalElems, fpredElems, distanceElems)
      case Failure(ex) =>
        logger.error(ex.getMessage)
        G2VSchema(Seq.empty, Seq.empty, Seq.empty, Seq.empty)
    }
  }

  def getSearchResultSet(qString: String, pageIndex: Option[Int], pageSize: Option[Int]):
  Future[Entities.SearchResultSet] = {
    import com.sksamuel.elastic4s.playjson._
    val limitClause = parsePaginationTokensForES(pageIndex, pageSize)
    val stoken = qString.toLowerCase
    // val stoken = qString
    val cleanedTokens = stoken.replaceAll("-", " ")

    if (stoken.length > 0) {
      esQ.execute {
          search("studies") query boolQuery.should(matchQuery("study_id", stoken),
            matchQuery("pmid", stoken),
            multiMatchQuery(cleanedTokens)
              .matchType(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
              .lenient(true)
              .slop(10)
              .prefixLength(2)
              .maxExpansions(50)
              .operator("OR")
              .analyzer(WhitespaceAnalyzer)
              .fields(Map("trait_reported" -> 1.5F,
                "pub_author" -> 1.2F,
                "_all" -> 1.0F)),
            simpleStringQuery(cleanedTokens)
              .defaultOperator("AND")
            ) start limitClause._1 limit limitClause._2 sortByFieldDesc "num_assoc_loci"
      }.zip {
        esQ.execute {
          search("variant_*") query boolQuery.should(matchQuery("variant_id", stoken),
            matchQuery("rs_id", stoken)) start limitClause._1 limit limitClause._2
        }
      }.zip {
        esQ.execute {
          search("genes") query boolQuery.should(matchQuery("gene_id", stoken),
            matchQuery("gene_name", stoken),
            multiMatchQuery(cleanedTokens)
              .matchType(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
              .lenient(true)
              .slop(10)
              .prefixLength(2)
              .maxExpansions(50)
              .operator("OR")
              .analyzer(WhitespaceAnalyzer)
              .fields("gene_name")) start limitClause._1 limit limitClause._2
        }
      }.map{
        case ((studiesRS, variantsRS), genesRS) =>
          SearchResultSet(genesRS.totalHits, genesRS.to[Gene],
            variantsRS.totalHits, variantsRS.to[Variant],
            studiesRS.totalHits, studiesRS.to[Study])
      }
    } else {
      Future.failed(InputParameterCheckError(Vector(SearchStringViolation())))
    }
  }

  /** get top Functions.defaultTopOverlapStudiesSize studies sorted desc by
    * the number of overlapped loci
    *
    * @param stid given a study ID
    * @return a Entities.OverlappedLociStudy which could contain empty list of ovelapped studies
    */
  def getTopOverlappedStudies(stid: String, pageIndex: Option[Int] = Some(0), pageSize: Option[Int] = Some(defaultTopOverlapStudiesSize)):
  Future[Entities.OverlappedLociStudy] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val q = overlaps
      .filter(_.studyIdA === stid)
      .groupBy(_.studyIdB)
      .map(r => (r._1, r._2.map(r => (r.chromA, r.posA, r.refA, r.altA)).length))
      .sortBy(_._2.desc)
      .drop(limitPair._1)
      .take(limitPair._2)

    db.run(q.result.asTry).map {
      case Success(v) =>
        if (v.nonEmpty) {
          OverlappedLociStudy(stid, v.map(t => OverlapRow(t._1, t._2)).toVector)
        } else {
          OverlappedLociStudy(stid, Vector.empty)
        }
      case Failure(ex) =>
        logger.error(ex.getMessage)
        OverlappedLociStudy(stid, Vector.empty)
    }
  }

  def getOverlapVariantsIntersectionForStudies(stid: String, stids: Seq[String]): Future[Vector[String]] = {
    if (stids.nonEmpty) {
      val numStudies = stids.length.toLong
      val stidListString = stids.map("'" + _ + "'").mkString(",")

      val q = overlaps
        .filter(r => (r.studyIdA === stid) && (r.studyIdB inSetBind stids))
        .groupBy(_.variantA)
        .map { case(vA, g) => vA -> g.map(_.studyIdB).uniq }
        .filter(_._2 === numStudies)
        .map(_._1)

      // q.result.statements.foreach(println)

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

  def getOverlapVariantsForStudies(stid: String, stids: Seq[String]): Future[Vector[Entities.OverlappedVariantsStudy]] = {
    val q =
      overlaps
          .filter(r => (r.studyIdA === stid) && (r.studyIdB inSetBind stids))
          .groupBy(r => (r.studyIdB, r.variantA, r.variantB))
          .map { case (l, g) =>
            (l._1, l._2, l._3) -> (g.map(_.overlapsAB).any, g.map(_.distinctA).any, g.map(_.distinctB).any)}

//    q.result.statements.foreach(println)

    db.run(q.result.asTry).map {
      case Success(v) =>
        if (v.nonEmpty) {
          v.view.groupBy(_._1._1).map(pair =>
            OverlappedVariantsStudy(pair._1,
              pair._2.map(t => OverlappedVariant(t._1._2.id, t._1._3.id,
                t._2._1.get, t._2._2.get, t._2._3.get)))).toVector
        } else {
          Vector.empty
        }
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }

  def getStudiesForGene(geneId: String): Future[Vector[String]] = {
    val geneQ = genes.filter(_.id === geneId)

    val studiesQ =
      d2v2g.filter(r => (r.geneId in geneQ.map(_.id)) && (r.tagChromosome in geneQ.map(_.chromosome)))
        .map(_.studyId).distinct

    db.run(studiesQ.result.asTry).map {
      case Success(v) => v.toVector
      case Failure(ex) =>
        logger.error(ex.getMessage)
        Vector.empty
    }
  }

  def getGenes(geneIds: Seq[String]): Future[Seq[Gene]] = {
    if (geneIds.nonEmpty) {
      val q = genes.filter(_.id inSetBind geneIds)

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
    * NOTE! WARNING! THERE IS A DIFF AT THE MOMENT BETWEEN THE VARIANTS COMING FROM VCF FILE
    * AND THE ONES COMING FROM UKB AND WE NEED TO FILL THAT GAP WHILE THIS ISSUE IS NOT
    * ADDRESSED. AT THE MOMENT, THE WAY TO DO IS USING THE VARIANT APPLY CONSTRUCTOR FROM A
    * STRING TO GET A WHITE-LABEL VARIANT WITH ANY OTHER REFERENCES FROM RSID OR NEAREST GENES
    * (NONCODING AND PROTCODING)
    */
  def getVariants(variantIds: Seq[String]): Future[Seq[DNA.Variant]] = {
    if (variantIds.nonEmpty) {
      val q = variants.filter(_.id inSetBind variantIds)

      db.run(q.result.asTry).map {
        case Success(v) =>
          val missingVIds = variantIds diff v.map(_.id)

          v ++ missingVIds.map(DNA.Variant(_)).withFilter(_.isRight).map(_.right.get)
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

  def buildManhattanTable(studyId: String, pageIndex: Option[Int], pageSize: Option[Int]):
  Future[Entities.ManhattanTable] = {
    val limitClause = parsePaginationTokens(pageIndex, pageSize)

    val idxVariants = sql"""
      |SELECT DISTINCT
      |    lead_chrom,
      |    lead_pos,
      |    lead_ref,
      |    lead_alt,
      |    pval,
      |    pval_mantissa,
      |    pval_exponent,
      |    credibleSetSize,
      |    ldSetSize,
      |    uniq_variants,
      |    top_pairs.1 as top_genes_ids,
      |    top_pairs.2 as top_genes_scores
      |FROM
      |(
      |   SELECT
      |    lead_chrom,
      |    lead_pos,
      |    lead_ref,
      |    lead_alt,
      |    any(pval) AS pval,
      |    any(pval_mantissa) AS pval_mantissa,
      |    any(pval_exponent) AS pval_exponent,
      |    uniqIf((tag_chrom, tag_pos, tag_ref, tag_alt), posterior_prob > 0.) AS credibleSetSize,
      |    uniqIf((tag_chrom, tag_pos, tag_ref, tag_alt), overall_r2 > 0.) AS ldSetSize,
      |    uniq(tag_chrom, tag_pos, tag_ref, tag_alt) AS uniq_variants
      |FROM ot.v2d_by_stchr
      |PREWHERE study_id = $studyId
      |GROUP BY
      |    lead_chrom,
      |    lead_pos,
      |    lead_ref,
      |    lead_alt
      |)
      | ALL LEFT OUTER JOIN
      |(
      |    SELECT
      |        lead_chrom,
      |        lead_pos,
      |        lead_ref,
      |        lead_alt,
      |        arrayReverseSort(arrayReduce('groupUniqArray', groupArray((gene_id, overall_score)))) AS top_pairs
      |    FROM ot.d2v2g_scored
      |    PREWHERE study_id = $studyId
      |    GROUP BY
      |     lead_chrom,
      |     lead_pos,
      |     lead_ref,
      |     lead_alt
      |) USING (lead_chrom, lead_pos, lead_ref, lead_alt)
      |#$limitClause
      """.stripMargin.as[V2DByStudy]

    // map to proper manhattan association with needed fields
    db.run(idxVariants.asTry).map {
      case Success(v) => ManhattanTable(studyId,
        v.map(el => {
          ManhattanAssociation(SimpleVariant(el.lead_chrom, el.lead_pos, el.lead_ref, el.lead_alt).id, el.pval,
            el.pval_mantissa, el.pval_exponent,
            el.topGenes, el.credibleSetSize, el.ldSetSize, el.totalSetSize)
        })
      )
      case Failure(ex) =>
        logger.error(ex.getMessage)
        ManhattanTable(studyId, associations = Vector.empty)
    }
  }

  def buildIndexVariantAssocTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]):
  Future[VariantToDiseaseTable] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant(variantID)

    variant match {
      case Right(v) =>
        val q = v2DsByChrPos
          .filter(r => (r.tagChromosome === v.chromosome) &&
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

  def buildTagVariantAssocTable(variantID: String, pageIndex: Option[Int], pageSize: Option[Int]):
  Future[VariantToDiseaseTable] = {
    val limitPair = parsePaginationTokensForSlick(pageIndex, pageSize)
    val variant = Variant(variantID)

    variant match {
      case Right(v) =>
        val q = v2DsByChrPos
          .filter(r => (r.tagChromosome === v.chromosome) &&
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

  def buildGecko(chromosome: String, posStart: Long, posEnd: Long): Future[Option[Entities.Gecko]] = {
    (parseChromosome(chromosome), parseRegion(posStart, posEnd)) match {
      case (Right(chr), Right((start, end))) =>
        val inRegion = Region(chr, start, end)
        if (denseRegionChecker.matchRegion(inRegion)) {
          Future.failed(InputParameterCheckError(Vector(RegionViolation(inRegion))))
        } else {
          val geneIdsInLoci = genes.filter(r =>
            (r.chromosome === chr) &&
              (r.start >= start && r.start <= end) ||
              (r.end >= start && r.end <= end))
            .map(_.id)

          val assocsQ = d2v2gScored.filter(r => (r.leadChromosome === chr) && (
            ((r.leadPosition >= start) && (r.leadPosition <= end)) ||
              ((r.tagPosition >= start) && (r.tagPosition <= end)) ||
              (r.geneId in geneIdsInLoci)
            )).groupBy(r => (r.studyId, r.leadVariant, r.tagVariant, r.geneId))
            .map {case (g, q) =>
              g -> (q.map(_.r2).any,
                q.map(_.log10Abf).any,
                q.map(_.posteriorProbability).any,
                q.map(_.pval).any,
                q.map(_.pvalExponent).any,
                q.map(_.pvalMantissa).any,
                q.map(_.overallScore).any)}

          db.run(geneIdsInLoci.result.asTry zip assocsQ.result.asTry).map {
            case (Success(geneIds), Success(assocs)) =>
              val geckoRows = assocs.view
                .map(r => GeckoRow(r._1._4, r._1._3, r._1._2, r._1._1,
                  V2DAssociation(r._2._4.get, r._2._5.get, r._2._6.get, r._2._1, r._2._2, r._2._3,
                    None, None, None, None, None),
                  r._2._7.getOrElse(0D)))
              Entities.Gecko(geckoRows, geneIds.toSet)

            case (Success(geneIds), Failure(asscsEx)) =>
              logger.error(asscsEx.getMessage)
              Entities.Gecko(Seq().view, geneIds.toSet)

            case (_, _) =>
              logger.error("Something really wrong happened while getting geneIds from gene " +
                "dictionary and also from d2v2g table")
              Entities.Gecko(Seq().view, Set.empty)
          }
        }

      case (chrEither, rangeEither) =>
        Future.failed(InputParameterCheckError(
          Vector(chrEither, rangeEither).filter(_.isLeft).map(_.left.get).asInstanceOf[Vector[Violation]]))
    }
  }

  def buildG2VByVariant(variantId: String): Future[Seq[Entities.G2VAssociation]] = {
    val variant = DNA.Variant(variantId)

    variant match {
      case Right(v) =>

        val filteredV2Gs = v2gs.filter(r => (r.chromosome === v.chromosome) &&
          (r.position === v.position) &&
          (r.refAllele === v.refAllele) &&
          (r.altAllele === v.altAllele))
        val filteredV2GScores = v2gScores.filter(r => (r.chromosome === v.chromosome) &&
          (r.position === v.position) &&
          (r.refAllele === v.refAllele) &&
          (r.altAllele === v.altAllele))

        val q = filteredV2Gs
          .joinFull(filteredV2GScores)
          .on((l, r) => l.geneId === r.geneId)
            .map(p => (p._1, p._2))

        db.run(q.result.asTry).map {
          case Success(r) =>
            r.view.filter(p => p._1.isDefined && p._2.isDefined).map(p => {
              val (Some(v2g), Some(score)) = p

              ScoredG2VLine(v2g.geneId, v2g.variant.id, score.overallScore,
                (score.sources zip score.sourceScores).toMap, v2g.typeId, v2g.sourceId, v2g.feature,
                v2g.fpred.maxLabel, v2g.fpred.maxScore,
                v2g.qtl.beta, v2g.qtl.se, v2g.qtl.pval, v2g.interval.score, v2g.qtl.scoreQ,
                v2g.interval.scoreQ, v2g.distance.distance, v2g.distance.score, v2g.distance.scoreQ)
            }).groupBy(_.geneId).mapValues(G2VAssociation(_)).values.toSeq

          case Failure(ex) =>
            logger.error(ex.getMessage)
            Seq.empty
        }

      case Left(violation) => Future.failed(InputParameterCheckError(Vector(violation)))
    }
  }

  private val gwasSumStatsTName: String = "gwas_chr_%s"
}