package components

import com.sksamuel.elastic4s.requests.searches.SearchRequest
import components.elasticsearch.ElasticsearchComponent
import configuration.IntegrationTestTag
import models.entities.Entities.SearchResultSet
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logging

import scala.concurrent.Future

class ElasticsearchComponentSpec
  extends PlaySpec
    with Logging
    with ScalaFutures
    with GuiceOneAppPerSuite {

  val esComponent: ElasticsearchComponent = app.injector.instanceOf(classOf[ElasticsearchComponent])

  "A query with multiple pages of results should return them all" taggedAs IntegrationTestTag in {
    //given
    val queryTerm = "Steroid"
    // when
    val results: Future[SearchResultSet] = esComponent.search(queryTerm)
    // then
    whenReady(results) { r =>
      assert(r.totalStudies > 40)
    }
  }

}
