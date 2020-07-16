package components

import configuration.IntegrationTestTag
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logging


class BackendSpec extends PlaySpec with GuiceOneAppPerSuite with Logging with ScalaFutures {

  implicit val defaultPatience =
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
    val be: Backend = app.injector.instanceOf(classOf[Backend])
    val brca1gene = "ENSG00000012048"
    // when

    val results = be.getStudiesForGene(brca1gene)
    whenReady(results) { r =>
      assert(r.nonEmpty)
    }
  }

}
