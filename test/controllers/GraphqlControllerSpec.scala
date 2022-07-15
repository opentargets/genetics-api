package controllers

import configuration.IntegrationTestTag
import org.scalatest.verbs.ShouldVerb
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

trait TestQueries {
  val simpleGeneQuery: JsValue = Json.parse("""
  {
   "query": "{genes(chromosome:\"5\", start: 100000, end: 200000) { id, tss }}"
  }
  """)
  val metadataQuery: JsValue = Json.parse("""
  {
    "query": "{ meta { name, apiVersion { major, minor, patch }, dataVersion { major, minor, patch }}}"
  }
  """)

  def generatePostRequest(query: JsValue): FakeRequest[AnyContentAsJson] =
    FakeRequest("POST", "/graphql").withJsonBody(query)
}

class GraphqlControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Results with ShouldVerb {

  new TestQueries {
    "A post request with a valid query should return the selected items" taggedAs IntegrationTestTag in {
      // given
      val r = generatePostRequest(simpleGeneQuery)
      // when
      val Some(result) = route(app, r)
      val resultJson = contentAsJson(result)
      val data: JsArray = (resultJson \ "data" \ "genes").as[JsArray]
      // then
      all(
        List(status(result) mustEqual OK,
             contentType(result).value must be("application/json"),
             data.value must not have length(0)
        )
      )
    }

    "A post request for metadata should return all metadata information" taggedAs IntegrationTestTag in {
      // given
      val r = generatePostRequest(metadataQuery)
      // when
      val Some(result) = route(app, r)
      val resultJson = contentAsJson(result)
      val data: JsObject = (resultJson \ "data" \ "meta").as[JsObject]
      // then
      all(
        List(
          status(result) must be(OK),
          data.value.keySet.size > 2
        )
      )
    }
  }

}
