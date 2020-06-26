package models.gql

import models.Backend
import models.entities.Entities.{OverlapRow, OverlappedLociStudy, OverlappedVariant, OverlappedVariantsStudy}
import sangria.macros.derive.{AddFields, DocumentField, ExcludeFields, ObjectTypeDescription, ObjectTypeName, RenameField, deriveObjectType}
import sangria.schema.{Field, IntType, ListType, ObjectType, OptionType, StringType, fields}

trait GQLOverlaps {
  this: GQLStudy =>

  implicit val overlappedVariant: ObjectType[Backend, OverlappedVariant] = deriveObjectType[Backend, OverlappedVariant](
    ObjectTypeName("Overlap"),
    ObjectTypeDescription("This element represents an overlap between two variants for two studies")
  )
  val overlappedStudy: ObjectType[Backend, OverlapRow] = ObjectType(
    "OverlappedStudy",
    "This element represent a overlap between two stduies",
    fields[Backend, OverlapRow](
      Field(
        "study",
        study,
        Some("A study object"),
        resolve = rsl => studiesFetcher.defer(rsl.value.stid)),
      Field(
        "numOverlapLoci",
        IntType,
        Some(
          "Orig variant id which is been used for computing the " +
            "overlap with the referenced study"),
        resolve = _.value.numOverlapLoci)))
  val overlappedVariantsStudies: ObjectType[Backend, OverlappedVariantsStudy] = deriveObjectType[Backend, OverlappedVariantsStudy](
    ObjectTypeName("OverlappedVariantsStudies"),
    ObjectTypeDescription("This element represents an overlap between two studies"),
    DocumentField("overlaps", "Orig variant id which is been used for computing the overlap with the referenced study"),
    ExcludeFields("studyId"),
    AddFields(Field(
      "study",
      OptionType(study),
      Some("A study object"),
      resolve = rsl => studiesFetcher.deferOpt(rsl.value.studyId)
    ))
  )
  val topOverlappedStudies: ObjectType[Backend, OverlappedLociStudy] = ObjectType(
    "TopOverlappedStudies",
    "This element represent a overlap between two stduies",
    fields[Backend, OverlappedLociStudy](
      Field(
        "study",
        OptionType(study),
        Some("A study object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value.studyId)),
      Field(
        "topStudiesByLociOverlap",
        ListType(overlappedStudy),
        Some("Top N studies ordered by loci overlap"),
        resolve = _.value.topOverlappedStudies)))
  val overlappedInfoForStudy: ObjectType[Backend, (String, Seq[String])] = ObjectType(
    "OverlappedInfoForStudy",
    "",
    fields[Backend, (String, Seq[String])](
      Field(
        "study",
        OptionType(study),
        Some("A study object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value._1)),
      Field(
        "overlappedVariantsForStudies",
        ListType(overlappedVariantsStudies),
        Some(""),
        resolve = rsl => rsl.ctx.getOverlapVariantsForStudies(rsl.value._1, rsl.value._2)),
      Field(
        "variantIntersectionSet",
        ListType(StringType),
        Some(""),
        resolve =
          rsl => rsl.ctx.getOverlapVariantsIntersectionForStudies(rsl.value._1, rsl.value._2))))
}
