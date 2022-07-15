package configuration

import com.typesafe.config.Config
import play.api.ConfigLoader
import play.api.libs.json.{JsValue, Json, Writes}

/** Metadata regarding the application version. This information is made available through the
  * GraphQL API.
  */
case class Metadata(name: String, apiVersion: Version, dataVersion: Version)

case class Version(major: Int, minor: Int, patch: Int) {
  require(major >= 0)
  require(minor >= 0)
  require(patch >= 0)

  override def toString: String = s"$major.$minor.$patch"
}

case class DataVersion(major: Int, minor: Int, patch: Int)

object MetadataConfiguration {

  implicit val VersionLoader: ConfigLoader[Version] = (config: Config, path: String) => {
    val conf = config.getConfig(path)
    Version(
      conf.getInt("major"),
      conf.getInt("minor"),
      conf.getInt("patch")
    )
  }

  implicit val metadataConfigLoader: ConfigLoader[Metadata] = (config: Config, path: String) => {
    val conf = config.getConfig(path)
    Metadata(
      conf.getString("name"),
      VersionLoader.load(conf, "apiVersion"),
      VersionLoader.load(conf, "dataVersion")
    )
  }

  implicit val metadataWriter: Writes[Metadata] = (md: Metadata) =>
    Json.obj(
      "name" -> md.name,
      "api_version" -> md.apiVersion.toString,
      "data_version" -> md.dataVersion.toString
    )
}
