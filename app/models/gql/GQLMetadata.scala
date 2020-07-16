package models.gql

import components.Backend
import configuration.{Metadata, Version}
import sangria.macros.derive.deriveObjectType
import sangria.schema.ObjectType

trait GQLMetadata {
  implicit val metaDataVersion: ObjectType[Backend, Version] = deriveObjectType[Backend, Version]()
  implicit val metadata: ObjectType[Backend, Metadata] = deriveObjectType[Backend, Metadata]()
}
