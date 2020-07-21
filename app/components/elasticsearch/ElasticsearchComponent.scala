package components.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, fieldFactorScore, functionScoreQuery, matchQuery, prefixQuery, search, termQuery}
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
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GeneticsApiQueries {

  def geneQ(term: String, page: Pagination): SearchRequest =
    search("genes") query boolQuery.should(
      termQuery("gene_id.keyword", term.toUpperCase)
        .boost(100d),
      termQuery("gene_name", term.toUpperCase).boost(100d),
      matchQuery("gene_name", term)
        .fuzziness("0")
        .maxExpansions(20)
        .prefixLength(2)
        .operator("AND")
        .analyzer("autocomplete_search")) start page.offset limit page.size

  def variantQ(term: String, page: Pagination): SearchRequest = {
    val vToken: String = Variant.fromString(term) match {
      case Right(v) => v.id
      case _ => term
    }
    search("variant_*") query boolQuery.should(
      termQuery("variant_id.keyword", vToken),
      termQuery("rs_id.keyword", term.toLowerCase)) start page.offset limit page.size
  }

  def studyQ(term: String, page: Pagination): SearchRequest =
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
          .modifier(FieldValueFactorFunctionModifier.LOG2P))) start page.offset limit page.size

}

/**
 * Class to hold reference to ES Client and manage interactions with external ES.
 *
 * @param configuration to extract ES configuration
 */
@Singleton
class ElasticsearchComponent @Inject()(configuration: Configuration) extends ElasticDsl {

  private type CountAndEntities = (Long, Seq[ElasticSearchEntity])
  // Setup component
  private val log: Logger = Logger(this.getClass)

  private val elasticsearchConfiguration: ElasticsearchConfiguration =
    configuration.get[ElasticsearchConfiguration]("ot.elasticsearch")

  log.debug(
    s"Initialising ElasticsearchComponent with configuration: ${elasticsearchConfiguration.toString}")

  private val client: ElasticClient = ElasticClient(
    JavaClient(elasticsearchConfiguration.asElasticProperties))

  private def queryAndExtract[T <: ElasticSearchEntity](
                                                         searchRequest: SearchRequest): Future[CountAndEntities] = {
    client
      .execute {
        log.debug(client.show(searchRequest))
        searchRequest
      }
      .map(extractSearchResponse)
      .map(sr => {
        implicit val reader: EsHitReader.type = EsHitReader
        (sr.hits.size, sr.to[ElasticSearchEntity])
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

  def getSearchResultSet(query: String, page: Pagination): Future[SearchResultSet] = {

    query match {
      case e if e.isEmpty => Future.failed(InputParameterCheckError(Vector(SearchStringViolation())))
      case _ =>
        for {
          gene <- queryAndExtract(GeneticsApiQueries.geneQ(query, page))
          study <- queryAndExtract(GeneticsApiQueries.studyQ(query, page))
          variant <- queryAndExtract(GeneticsApiQueries.variantQ(query, page))
        } yield {
          SearchResultSet(
            gene._1,
            gene._2.asInstanceOf[Seq[Gene]],
            variant._1,
            variant._2.asInstanceOf[Seq[Variant]],
            study._1,
            study._2.asInstanceOf[Seq[Study]])
        }
    }

  }
}

