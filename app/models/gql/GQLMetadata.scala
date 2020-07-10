package models.gql

import components.Backend
import configuration.{Metadata, Version}
import sangria.macros.derive.deriveObjectType

trait GQLMetadata {
  implicit val metaDataVersion = deriveObjectType[Backend, Version]()
  implicit val metadata = deriveObjectType[Backend, Metadata]()
}
