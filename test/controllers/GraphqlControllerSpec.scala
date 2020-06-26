package controllers

import org.scalatest.FlatSpecLike
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsArray, JsValue, Json}
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
}

class GraphqlControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Results {


  new TestQueries {
    "A post request with a valid query should return the selected items" in {
      // given
      val r = FakeRequest("POST", "/graphql").withJsonBody(simpleGeneQuery)
      // when
      val Some(result) = route(app, r)
      val resultJson = contentAsJson(result)
      val data: JsArray = (resultJson \ "data" \ "genes").as[JsArray]
      // then
      status(result) mustEqual OK
      contentType(result) mustEqual Some("application/json")
      assert(data.value.nonEmpty)
    }
  }

}
