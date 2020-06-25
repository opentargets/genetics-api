package models.gql

import configuration.{Metadata, Version}
import models.Backend
import sangria.macros.derive.deriveObjectType

trait GQLMetadata {
  implicit val metaDataVersion = deriveObjectType[Backend, Version]()
  implicit val metadata = deriveObjectType[Backend, Metadata]()
}
