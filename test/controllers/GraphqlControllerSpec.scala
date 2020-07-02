package controllers

import org.scalatest.FlatSpecLike
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

trait TestQueries {
  val simpleGeneQuery: JsValue = Json.parse(
    """
  {
   "query": "{genes(chromosome:\"5\", start: 100000, end: 200000) { id, tss }}"
  }
  """)
  val metadataQuery: JsValue = Json.parse(
    """
  {
    "query": "{ meta { name, apiVersion { major, minor, patch }, dataVersion { major, minor, patch }}}"
  }
  """)

  def generatePostRequest(query: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/graphql").withJsonBody(query)
}

class GraphqlControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Results {

  new TestQueries {
    "A post request with a valid query should return the selected items" in {
      // given
      val r = generatePostRequest(simpleGeneQuery)
      // when
      val Some(result) = route(app, r)
      val resultJson = contentAsJson(result)
      val data: JsArray = (resultJson \ "data" \ "genes").as[JsArray]
      // then
      status(result) mustEqual OK
      contentType(result) mustEqual Some("application/json")
      assert(data.value.nonEmpty)
    }

    "A post request for metadata should return all metadata information" in {
      // given
      val r = generatePostRequest(metadataQuery)
      // when
      val Some(result) = route(app, r)
      val resultJson = contentAsJson(result)
      val data: JsObject = (resultJson \ "data" \ "meta").as[JsObject]
      // then
      status(result) mustEqual OK
      assert(data.value.keySet.size > 2)
    }
  }

}
