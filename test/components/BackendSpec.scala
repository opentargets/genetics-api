package components

import configuration.IntegrationTestTag
import models.entities.DNA.SimpleVariant
import models.entities.Entities
import models.entities.Violations.InputParameterCheckError
import org.scalatest.Assertion
import org.scalatest.concurrent._
import org.scalatest.matchers.must._
import org.scalatest.matchers.should._
import org.scalatest.time._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logging

import scala.concurrent.Future

class BackendSpec extends PlaySpec with GuiceOneAppPerSuite with Logging with ScalaFutures {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(1, Seconds))

  val backend: Backend = app.injector.instanceOf(classOf[Backend])
  import clickhouse.ClickHouseProfile.api._

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

  "topOverlappedStudies returns overlaps" taggedAs IntegrationTestTag in {
    // given
    val studyWithKnownOverlaps = "GCST004988"
    val limit = Some(2)
    //when
    val results: Entities.OverlappedLociStudy =
      backend.getTopOverlappedStudies(studyWithKnownOverlaps, Some(0), limit).futureValue
    all(
      List(
        results.topOverlappedStudies must not be empty,
        results.topOverlappedStudies.length must equal(limit.get),
        results.topOverlappedStudies.head.numOverlapLoci >= results.topOverlappedStudies
          .drop(1)
          .head
          .numOverlapLoci,
        results.studyId must equal(studyWithKnownOverlaps)))
  }

  "QtlRegionalFromSumstats" when {
    // given
    val badChromosome = "23"
    val badStartEnd = (-1, 1)
    val funUnderTest = backend.qtlRegionalFromSumstats("", "", "", _, _, _)

    // takes a future and the number of expected violations returned with exception
    def check(fViolation: Future[_], nViolations: Int): ResultOfCollectedAny[Assertion] = {
      ScalaFutures.whenReady(fViolation.failed) { e =>
        all(
          List(
            e mustBe a[InputParameterCheckError],
            e.asInstanceOf[InputParameterCheckError].violations.size mustBe (nViolations)))
      }
    }
    "given one invalid input should return 1 violation" in {

      val f1: Future[_] = funUnderTest(badChromosome, 10, 20)
      val f2: Future[_] = funUnderTest("1", badStartEnd._1, badStartEnd._2)
      check(f1,1)
      check(f2,1)
    }
    "given two invalid inputs should return 2 violations" in {
      check(funUnderTest(badChromosome, badStartEnd._1, badStartEnd._2), 2)
    }
    "given valid inputs should return ..." taggedAs IntegrationTestTag in {
      val results: Seq[(SimpleVariant, Double)] = backend.qtlRegionalFromSumstats("GEUVADIS", "LCL", "ENSG00000237491", "1", 5000, 15000).futureValue
      results must not be empty
    }
  }

  "Get G2VSchema returns schema including all elements in DB" taggedAs IntegrationTestTag in {
    // given
    val sizeOfSchema = backend.v2gStructures.length.result
    val expectedSize = backend.executeQuery(sizeOfSchema).futureValue
    // when
    val generatedSchema: Entities.G2VSchema = backend.getG2VSchema.futureValue
    val schemaElementSize = generatedSchema.distances.length +
      generatedSchema.functionalPredictions.length +
      generatedSchema.intervals.length +
      generatedSchema.qtls.length
    // then
    // there is an element for each row in DB
    schemaElementSize must equal(expectedSize)
    // each schema element has tissue entries
    assert(generatedSchema.qtls.forall(e => e.tissues.nonEmpty))
    assert(generatedSchema.intervals.forall(e => e.tissues.nonEmpty))
    assert(generatedSchema.functionalPredictions.forall(e => e.tissues.nonEmpty))
    assert(generatedSchema.distances.forall(e => e.tissues.nonEmpty))

  }

  "Search expression yields results" taggedAs IntegrationTestTag in {
    // given
    val searchExpr = "BRCA1"

    whenReady(backend.search(searchExpr, None))(r =>
      all(List(r.genes must have length (1), r.genes.head.symbol.get must be(searchExpr))))
  }

  "An empty search throws an exception" taggedAs IntegrationTestTag in {
    // given
    val searchExp = ""
    // then
    assert(
      backend.search(searchExp, None).failed.futureValue.isInstanceOf[InputParameterCheckError])
  }
}
