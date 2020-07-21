package components

import configuration.IntegrationTestTag
import models.entities.Violations.InputParameterCheckError
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logging
import sangria.validation.Violation


class BackendSpec extends PlaySpec with GuiceOneAppPerSuite with Logging with ScalaFutures {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1, Seconds))
  val backend: Backend = app.injector.instanceOf(classOf[Backend])

  "getGeneChromosome returns String representation of chromosome" taggedAs IntegrationTestTag in {
    //given
    val validGene = "ENSG00000012048"
    // when
    val results = backend.getGeneChromosome(validGene)
    whenReady(results) { r =>
      assertResult("17") {
        r
      }
    }
  }

  "getStudiesForGene returns studies related to gene" taggedAs IntegrationTestTag in {
    // given
    val brca1gene = "ENSG00000012048"
    // when
    val results = backend.getStudiesForGene(brca1gene)
    whenReady(results) { r =>
      assert(r.nonEmpty)
    }
  }

  "Search expression yields results" taggedAs IntegrationTestTag in {
    // given
    val searchExpr = "BRCA1"
    // when
    val results = backend.search(searchExpr, None)
    whenReady(results) { r =>
      assert(r.totalGenes == 1)
      assertResult(searchExpr)(r.genes.head.symbol.get)
    }

  }

  "An empty search throws an exception" taggedAs IntegrationTestTag in {
    // given
    val searchExp = ""
    // then
    ScalaFutures.whenReady(backend.search(searchExp, None).failed) { r =>
      r.isInstanceOf[InputParameterCheckError]
    }
  }

}
