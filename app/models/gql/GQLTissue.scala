package models.gql

import models.entities.Entities.{
  DistancelTissue,
  FPredTissue,
  G2VElement,
  IntervalTissue,
  QTLTissue,
  Tissue
}
import models.Backend
import sangria.schema.{
  Field,
  FloatType,
  ListType,
  LongType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

trait GQLTissue {
  this: GQLSchema.type =>

  val tissue: ObjectType[Backend, Tissue] = ObjectType(
    "Tissue",
    "",
    fields[Backend, Tissue](
      Field("id", StringType, Some(""), resolve = _.value.id),
      Field(
        "name",
        StringType,
        Some(""),
        resolve = r => r.ctx.v2gBiofeatureLabels.getOrElse(r.value.id, r.value.name))))

  val qtlTissue: ObjectType[Backend, QTLTissue] = ObjectType(
    "QTLTissue",
    "",
    fields[Backend, QTLTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field("quantile", FloatType, Some(""), resolve = _.value.quantile),
      Field("beta", OptionType(FloatType), Some(""), resolve = _.value.beta),
      Field("pval", OptionType(FloatType), Some(""), resolve = _.value.pval)))

  val intervalTissue: ObjectType[Backend, IntervalTissue] = ObjectType(
    "IntervalTissue",
    "",
    fields[Backend, IntervalTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field("quantile", FloatType, Some(""), resolve = _.value.quantile),
      Field("score", OptionType(FloatType), Some(""), resolve = _.value.score)))

  val fpredTissue: ObjectType[Backend, FPredTissue] = ObjectType(
    "FPredTissue",
    "",
    fields[Backend, FPredTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field("maxEffectLabel", OptionType(StringType), Some(""), resolve = _.value.maxEffectLabel),
      Field("maxEffectScore", OptionType(FloatType), Some(""), resolve = _.value.maxEffectScore)))

  val distanceTissue: ObjectType[Backend, DistancelTissue] = ObjectType(
    "DistanceTissue",
    "",
    fields[Backend, DistancelTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field(
        "distance",
        OptionType(LongType),
        Some("Distance to the canonical TSS"),
        resolve = _.value.distance),
      Field("score", OptionType(FloatType), Some("Score 1 / Distance"), resolve = _.value.score),
      Field(
        "quantile",
        OptionType(FloatType),
        Some("Quantile of the score"),
        resolve = _.value.quantile)))

  val qtlElement: ObjectType[Backend, G2VElement[QTLTissue]] = ObjectType(
    "QTLElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[QTLTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(qtlTissue), Some(""), resolve = _.value.tissues)))

  val intervalElement: ObjectType[Backend, G2VElement[IntervalTissue]] = ObjectType(
    "IntervalElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[IntervalTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(intervalTissue), Some(""), resolve = _.value.tissues)))

  val fPredElement: ObjectType[Backend, G2VElement[FPredTissue]] = ObjectType(
    "FunctionalPredictionElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[FPredTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(fpredTissue), Some(""), resolve = _.value.tissues)))

  val distElement: ObjectType[Backend, G2VElement[DistancelTissue]] = ObjectType(
    "DistanceElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[DistancelTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(distanceTissue), Some(""), resolve = _.value.tissues)))

}
