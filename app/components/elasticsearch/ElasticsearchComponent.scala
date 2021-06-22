package components.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl.{
  boolQuery,
  fieldFactorScore,
  functionScoreQuery,
  matchQuery,
  prefixQuery,
  search,
  termQuery
}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.FieldValueFactorFunctionModifier
import configuration.ElasticsearchConfiguration
import javax.inject.{Inject, Singleton}
import models.entities.DNA.{Gene, Variant}
import models.entities.Entities.{SearchResultSet, Study}
import models.entities.Violations.{InputParameterCheckError, SearchStringViolation}
import models.implicits.{ElasticSearchEntity, EsHitReader}
import play.api.{Configuration, Logger, Logging}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object GeneticsApiQueries {

  def geneQ(term: String, page: Pagination): SearchRequest = {
    val p = page.toES
    search("genes") query boolQuery.should(
      termQuery("gene_id.keyword", term.toUpperCase)
        .boost(100d),
      termQuery("gene_name", term.toUpperCase).boost(100d),
      matchQuery("gene_name", term)
        .fuzziness("0")
        .maxExpansions(20)
        .prefixLength(2)
        .operator("AND")
        .analyzer("autocomplete_search")) start p._1 limit p._2 trackTotalHits true
  }

  def variantQ(term: String, page: Pagination): SearchRequest = {
    val p = page.toES
    val vToken: String = Variant.fromString(term) match {
      case Right(v) => v.id
      case _        => term
    }
    search("variant_*") query boolQuery.should(
      termQuery("variant_id.keyword", vToken),
      termQuery("rs_id.keyword", term.toLowerCase)) start p._1 limit p._2 trackTotalHits true
  }

  def studyQ(term: String, page: Pagination): SearchRequest = {
    val p = page.toES
    search("studies") query boolQuery.should(
      prefixQuery("study_id.keyword", term.toUpperCase),
      termQuery("pmid", term),
      functionScoreQuery(
        matchQuery("mixed_field", term)
          .fuzziness("AUTO")
          .maxExpansions(20)
          .prefixLength(2)
          .operator("AND")
          .analyzer("autocomplete_search")).functions(
        fieldFactorScore("num_assoc_loci")
          .factor(1.1)
          .missing(1d)
          .modifier(
            FieldValueFactorFunctionModifier.LOG2P))) start p._1 limit p._2 trackTotalHits true
  }

}

case class ElasticSearchSummaryResult(totalHits: Long, hits: Seq[ElasticSearchEntity]) {
  def allResultsReturned: Boolean = hits.length == totalHits
}

/**
  * Class to hold reference to ES Client and manage interactions with external ES.
  *
  * @param configuration to extract ES configuration
  */
@Singleton
class ElasticsearchComponent @Inject() (configuration: Configuration) extends ElasticDsl {

  // Setup component
  // declare logger here rather than with trait because of conflict with ElasticDsl import.
  private val log: Logger = Logger(this.getClass)

  private val elasticsearchConfiguration: ElasticsearchConfiguration =
    configuration.get[ElasticsearchConfiguration]("ot.elasticsearch")

  log.info(
    s"Initialising ElasticsearchComponent with configuration: ${elasticsearchConfiguration.toString}")

  private val client: ElasticClient = ElasticClient(
    JavaClient(elasticsearchConfiguration.asElasticProperties))

  /**
    * Single method to interact with Elasticsearch, since we're only using it for searching and
    * everything else should run through the Clickhouse database.
    * @param query to search for
    * @param page of results
    * @return all results matching the query.
    */
  def search(query: String, page: Pagination = Pagination.mkDefault): Future[SearchResultSet] = {

    val results = query match {
      // basic validation
      case e if e.isEmpty =>
        Future.failed(InputParameterCheckError(Vector(SearchStringViolation())))
      // query all indexes
      case _ =>
        for {
          gene <- Future {
            getSearchResultSet(GeneticsApiQueries.geneQ(query, _), page)
          }
          study <- Future {
            getSearchResultSet(GeneticsApiQueries.studyQ(query, _), page)
          }
          variant <- Future {
            getSearchResultSet(GeneticsApiQueries.variantQ(query, _), page)
          }
        } yield {
          // log results
          log.debug(s"Gene -- total hits: ${gene.totalHits} -- hits collected: ${gene.hits.size}")
          log.debug(
            s"Variant -- total hits: ${variant.totalHits} -- hits collected: ${variant.hits.size}")
          log.debug(
            s"Study -- total hits: ${study.totalHits} -- hits collected: ${study.hits.size}")
          // build output
          SearchResultSet(
            gene.totalHits,
            gene.hits.asInstanceOf[Seq[Gene]],
            variant.totalHits,
            variant.hits.asInstanceOf[Seq[Variant]],
            study.totalHits,
            study.hits.asInstanceOf[Seq[Study]])
        }
    }
    results
  }

  private def getSearchResultSet(
    query: Pagination => SearchRequest,
    page: Pagination,
    acc: Seq[ElasticSearchEntity] = Seq()): ElasticSearchSummaryResult = {

    val result = queryAndExtract(query(page)).await
    val summaryResult = result.copy(hits = result.hits ++ acc)
    summaryResult
  }

  private def queryAndExtract[T <: ElasticSearchEntity](
    searchRequest: SearchRequest): Future[ElasticSearchSummaryResult] = {

    client
      .execute {
        log.debug(client.show(searchRequest))
        searchRequest
      }
      .map(extractSearchResponse)
      .map(sr => {
        implicit val reader: EsHitReader.type = EsHitReader
        ElasticSearchSummaryResult(sr.totalHits, sr.to[ElasticSearchEntity])
      })
  }

  private def extractSearchResponse(resp: Response[SearchResponse]): SearchResponse = {
    resp match {
      case failure: RequestFailure =>
        logger.error(s"Error querying elasticsearch: ${failure.error}")
        failure.result
      case success: RequestSuccess[SearchResponse] => success.result
    }
  }

}
