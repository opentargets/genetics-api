package configuration

import com.typesafe.config.ConfigFactory
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}
import org.scalatest.{FlatSpecLike, Matchers}
import play.api.Configuration

class MetadataConfigurationTest extends FlatSpecLike with Matchers with TableDrivenPropertyChecks {

  import MetadataConfiguration._

  "Configuration" should "successfully load" in {
    /* the config needs to be loaded this way due to a bug with Scalatest and
     * how configuration is loaded.
     */
    val conf = Configuration(ConfigFactory.load(this.getClass.getClassLoader))
    val metadata = conf.get[Metadata]("ot.meta")
    assert(conf != null)
    assert(metadata.apiVersion != null)
    assert(metadata.dataVersion != null)
  }

  private val bad_versions =
    Table(("major", "minor", "patch"), (-1, 0, 0), (1, -3, 0), (-3, -1, 0))

  "Versioning configuration" should "not accept invalid (negative) version numbers" in {
    forAll(bad_versions) { (m, mi, p) => {
      a[IllegalArgumentException] should be thrownBy {
        Version(m, mi, p)
      }
    }
    }
  }

  "Version" should "print nicely" in {
    val v = Version(0, 0, 1)
    v.toString.equals("0.0.1")
  }

}
