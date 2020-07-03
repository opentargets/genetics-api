package controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsObject
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

class AdminControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Results {

  "The AdminController" should {
    "provide an endpoint," which {
      "returns 200 when the application is running" in {
        val request = FakeRequest("GET", "/admin/health")
        val Some(result) = route(app, request)
        status(result) mustEqual OK
      }
      "returns metadata regarding the name and version of the application" in {
        val request = FakeRequest("GET", "/admin/metadata")
        val Some(result) = route(app, request)
        val resultBody: JsObject = Helpers.contentAsJson(result).as[JsObject]
        status(result) mustEqual OK
        assert(resultBody.fields.nonEmpty)
      }
    }
  }
}
