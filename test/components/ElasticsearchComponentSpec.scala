package components

import com.sksamuel.elastic4s.requests.searches.SearchRequest
import components.elasticsearch.{ElasticsearchComponent, Pagination}
import configuration.IntegrationTestTag
import models.entities.Entities.{SearchResultSet, Study}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatest.matchers.dsl._
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logging

import scala.concurrent.Future

class ElasticsearchComponentSpec
    extends PlaySpec
    with Logging
    with ScalaFutures
    with GuiceOneAppPerSuite {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(1, Seconds))

  val esComponent: ElasticsearchComponent = app.injector.instanceOf(classOf[ElasticsearchComponent])

  "A query with multiple pages of results should return them all" taggedAs IntegrationTestTag in {
    // given
    val queryTerm = "Steroid"
    // when

    esComponent
      .search(queryTerm, Pagination.mkDefault)
      .futureValue
      .totalStudies > 40L
  }

}
