package models


import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._

import Entities._

object GQLSchema {
  val studyID = Argument("id", StringType, description = "Study ID which links a top loci with a trait")

  val gene = ObjectType("gene",
  "This element represents a simple gene object which contains id and name",
    fields[Backend, SimpleGene](
      Field("id", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.id),
      Field("name", OptionType(StringType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.name)
    ))

  // TODO missing a lot fields but enough to test
  val manhattanAssoc = ObjectType("manhattanAssoc",
  "This element represents an association between a trait and a variant through a study",
    fields[Backend, ManhattanAssoc](
      Field("indexVariantID", StringType,
        Some("Index variant ID as ex. 1_12345_A_T"),
        resolve = _.value.indexVariantID),
      Field("indexVariantRsId", OptionType(StringType),
        Some("RSID code for the given index variant as ex. rs12345"),
        resolve = _.value.indexVariantRSID),
      Field("pval", FloatType,
        Some("Computed p-Value"),
        resolve = _.value.pval),
      Field("chromosome", StringType,
        Some("Chromosome letter from a set of (1-22, X, Y, MT)"),
        resolve = _.value.chromosome),
      Field("position", LongType,
        Some("absolute position p of the variant i in the chromosome j"),
        resolve = _.value.position),
      Field("bestGenes", ListType(gene),
        Some("A list of best genes associated"),
        resolve = _.value.bestGenes),
      Field("credibleSetSize", OptionType(IntType),
      Some("n tag variants for each index variant coming from crediblesets"),
        resolve = _.value.crediblbeSetSize),
      Field("ldSetSize", OptionType(IntType),
        Some("n tag variants for each lead variant coming from ld expansion"),
        resolve = _.value.ldSetSize)
    ))

  val query = ObjectType(
    "Query", fields[Backend, Unit](
      Field("manhattan", ListType(manhattanAssoc),
        arguments = studyID :: Nil,
        resolve = (ctx) ⇒ ctx.ctx.manhattanTable(ctx.arg(studyID),ctx.astFields))))

  val schema = Schema(query)
}
