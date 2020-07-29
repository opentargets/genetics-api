package components

import akka.actor.Status.Success
import configuration.IntegrationTestTag
import models.entities.Entities.SearchResultSet
import models.entities.Violations.{InputParameterCheckError, SearchStringViolation}
import org.scalatest.concurrent._
import org.scalatest.matchers.must._
import org.scalatest.matchers.should._
import org.scalatest.time._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logging

import scala.collection.immutable.Vector
import scala.util.Try

class BackendSpec extends PlaySpec with GuiceOneAppPerSuite with Logging with ScalaFutures {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(1, Seconds))

  val backend: Backend = app.injector.instanceOf(classOf[Backend])

  "getGeneChromosome returns String representation of chromosome" taggedAs IntegrationTestTag in {
    //given
    val validGene = "ENSG00000012048"
    // when
    backend.getGeneChromosome(validGene).futureValue must be("17")
  }

  "getStudiesForGene returns studies related to gene" taggedAs IntegrationTestTag in {
    // given
    val brca1gene = "ENSG00000012048"
    // when
    backend.getStudiesForGene(brca1gene).futureValue must not have size(0)
  }

  "Search expression yields results" taggedAs IntegrationTestTag in {
    // given
    val searchExpr = "BRCA1"

    whenReady(backend.search(searchExpr, None)) {
      case r => all(List(r.genes must have length(1), r.genes.head.symbol.get must be (searchExpr)))
    }
  }

  "An empty search throws an exception" taggedAs IntegrationTestTag in {
    // given
    val searchExp = ""
    // then

    assert(backend.search(searchExp, None).failed.futureValue.isInstanceOf[InputParameterCheckError])
  }
}
