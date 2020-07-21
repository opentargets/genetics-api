package configuration

import com.sksamuel.elastic4s.ElasticProperties
import com.typesafe.config.Config
import play.api.ConfigLoader

case class ElasticsearchConfiguration(
                                       protocol: String,
                                       host: String,
                                       port: Int) {
  require(List("http", "https").contains(protocol), "Protocol must be either http or https")
  require(port > 0, "Port number must not be negative")

  def asElasticProperties: ElasticProperties = ElasticProperties(s"$protocol://$host:$port")

}

object ElasticsearchConfiguration {
  implicit val elasticConfigLoader: ConfigLoader[ElasticsearchConfiguration] =
    new ConfigLoader[ElasticsearchConfiguration] {

      def load(config: Config, path: String): ElasticsearchConfiguration = {
        val conf = config.getConfig(path)
        ElasticsearchConfiguration(
          conf.getString("protocol"),
          conf.getString("host"),
          conf.getInt("port")
        )
      }

    }

}
