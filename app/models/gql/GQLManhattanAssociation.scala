package models.gql

import components.Backend
import models.Functions.toSafeDouble
import models.entities.Entities.ManhattanAssociation
import sangria.macros.derive.{AddFields, DocumentField, ExcludeFields, deriveObjectType}
import sangria.schema.{Field, FloatType, ListType, ObjectType, OptionType, StringType}

trait GQLManhattanAssociation {
  self: GQLGene with GQLVariant =>

  val manhattanAssociation: ObjectType[Backend, ManhattanAssociation] = deriveObjectType[Backend, ManhattanAssociation](
    ExcludeFields("v2dOdds", "v2dBeta", "variantId", "pval", "bestGenes", "bestColocGenes", "bestL2Genes"),
    DocumentField("credibleSetSize", "The cardinal of the set defined as tag variants for an index variant coming from crediblesets"),
    DocumentField("ldSetSize", "The cardinal of the set defined as tag variants for an index variant coming from ld expansion"),
    DocumentField("totalSetSize", "The cardinal of the set defined as tag variants for an index variant coming from any expansion"),
    AddFields(
      Field(
        "variant",
        variant,
        Some("Index variant"),
        resolve = r => variantsFetcher.defer(r.value.variantId)),
      Field(
        "pval",
        FloatType,
        Some("Computed p-Value"),
        resolve = r => toSafeDouble(r.value.pvalMantissa, r.value.pvalExponent)),
      Field("oddsRatio", OptionType(FloatType), Some(""), resolve = _.value.v2dOdds.oddsCI),
      Field(
        "oddsRatioCILower",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.v2dOdds.oddsCILower),
      Field(
        "oddsRatioCIUpper",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.v2dOdds.oddsCIUpper),
      Field("beta", OptionType(FloatType), Some(""), resolve = _.value.v2dBeta.betaCI),
      Field("betaCILower", OptionType(FloatType), Some(""), resolve = _.value.v2dBeta.betaCILower),
      Field("betaCIUpper", OptionType(FloatType), Some(""), resolve = _.value.v2dBeta.betaCIUpper),
      Field("direction", OptionType(StringType), Some(""), resolve = _.value.v2dBeta.direction),
      Field(
        "bestGenes",
        ListType(scoredGene),
        Some("A list of best genes associated"),
        resolve = _.value.bestGenes),
      Field(
        "bestColocGenes",
        ListType(scoredGene),
        Some("A list of best genes associated"),
        resolve = _.value.bestColocGenes),
      Field(
        "bestLocus2Genes",
        ListType(scoredGene),
        Some("A list of best L2G scored genes associated"),
        resolve = _.value.bestL2Genes))
  )

}
