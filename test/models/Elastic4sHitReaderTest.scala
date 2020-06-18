package models

import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.http.JavaClient
import org.scalatest.Tag
import org.scalatestplus.play.PlaySpec
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.FieldValueFactorFunctionModifier
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, RequestFailure, RequestSuccess, Response}
import models.DNA.Gene

import scala.concurrent.{ExecutionContext, Future}

/**
 * EsTest requires that a configured instance of Elasticsearch be reachable on localhost:9200.
 */
object EsTest extends Tag("ot-genetics-api.esTest")

trait ElasticSearchSetup {

  implicit val ec = ExecutionContext.global
  implicit val esHitReader = EsHitReader

  def withElasticSearch(testMethod: ElasticClient => Any) {
    val client = ElasticClient(JavaClient(ElasticProperties("http://localhost:9200")))
    try {
      testMethod(client)
    } finally client.close
  }
}

class Elastic4sHitReaderTest extends PlaySpec with ElasticSearchSetup {

  import com.sksamuel.elastic4s.ElasticDsl._

  "A query to elastic search for a gene should correctly return that gene as a case class" taggedAs (EsTest) in withElasticSearch {
    client => {
      //given
      val searchId = "ENSG00000000419"
      // when
      val result: Future[Response[SearchResponse]] = client.execute {
        search("genes") query matchQuery("gene_id", searchId)
      }
      val esr: Seq[ElasticSearchEntity] = result.map(r => r.result.to[ElasticSearchEntity]).await

      val esrAsGene = esr.head.asInstanceOf[Gene]
      // then
      assert(esr.length.equals(1))
      assert(esrAsGene.id == searchId)
    }
  }

}
