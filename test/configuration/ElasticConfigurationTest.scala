package configuration

import com.sksamuel.elastic4s.ElasticProperties
import org.scalatest.{FlatSpecLike, Matchers}

class ElasticConfigurationTest extends FlatSpecLike with Matchers {
  private val validUrl = "http://elastic.co/9191"
  private val elasticProperties = ElasticProperties(validUrl)

  "Elastic config" should "not accept invalid port numbers" in {
    a[IllegalArgumentException] should be thrownBy {
      ElasticsearchConfiguration("http", "elastic.co", -1)
    }
  }

  "it" should "not accept invalid protocols" in {
    a[IllegalArgumentException] should be thrownBy {
      ElasticsearchConfiguration("smtp", "elastic.co", 9000)
    }
  }

}

