package configuration

import org.scalatest.Tag

/** Requires that:
  *   - a configured instance of Elasticsearch be reachable on localhost:9200.
  *   - a configured instance of Clickhouse be reachable on localhost:8123.
  */
object IntegrationTestTag extends Tag("configuration.IntegrationTestTag")
