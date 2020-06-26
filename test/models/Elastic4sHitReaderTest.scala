package models

import configuration.IntegrationTestTag
import org.scalatestplus.play.PlaySpec
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, Response}
import models.entities.DNA.{Gene, Variant}
import models.entities.Entities.Study

import scala.concurrent.{ExecutionContext, Future}

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

  "A query to elastic search for a gene should correctly return that gene as a case class" taggedAs IntegrationTestTag in withElasticSearch {
    client => {
      //given
      val searchId = "ENSG00000000419"
      // when
      val result: Future[Response[SearchResponse]] = client.execute {
        search("genes") query matchQuery("gene_id", searchId)
      }
      val searchResults: Seq[ElasticSearchEntity] = result.map(r => r.result.to[ElasticSearchEntity]).await

      val esrAsGene = searchResults.head.asInstanceOf[Gene]
      // then
      assert(searchResults.length.equals(1))
      assert(esrAsGene.id == searchId)
    }
  }
  "A query to elastic search for a study should correctly return that study as a case class" taggedAs IntegrationTestTag in withElasticSearch {
    client => {
      //given
      val searchId = "GCST000001"
      // when
      val result: Future[Response[SearchResponse]] = client.execute {
        search("studies") query matchQuery("study_id", searchId)
      }
      val searchResults: Seq[ElasticSearchEntity] = result.map(r => r.result.to[ElasticSearchEntity]).await

      val esrAsStudy = searchResults.head.asInstanceOf[Study]
      // then
      assert(searchResults.length.equals(1))
      assert(esrAsStudy.studyId == searchId)
    }
  }

  "A query to elastic search for a variant should correctly return that variant as a case class" taggedAs IntegrationTestTag in withElasticSearch {
    client => {
      //given
      val searchId = "X_103426259_G_A"
      // when
      val result: Future[Response[SearchResponse]] = client.execute {
        search("variant_x") query matchQuery("variant_id", searchId)
      }
      val searchResults: Seq[ElasticSearchEntity] = result.map(r => r.result.to[ElasticSearchEntity]).await

      val variant = searchResults.head.asInstanceOf[Variant]
      // then
      assert(searchResults.length.equals(1))
      assert(variant.id == searchId)
    }
  }
}
