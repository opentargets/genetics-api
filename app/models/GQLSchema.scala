package models


import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._

import Entities._

import scala.concurrent.Future

// val Droid = ObjectType(
//    "Droid",
//    "A mechanical creature in the Star Wars universe.",
//    interfaces[CharacterRepo, Droid](Character),
//    fields[CharacterRepo, Droid](
//      Field("id", StringType,
//        Some("The id of the droid."),
//        tags = ProjectionName("_id") :: Nil,
//        resolve = _.value.id),
//      Field("name", OptionType(StringType),
//        Some("The name of the droid."),
//        resolve = ctx ⇒ Future.successful(ctx.value.name)),
//      Field("friends", ListType(Character),
//        Some("The friends of the droid, or an empty list if they have none."),
//        complexity = Some((_, _, children) ⇒ 100 + 1.5 * children),
//        resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
//      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
//        Some("Which movies they appear in."),
//        resolve = _.value.appearsIn map (e ⇒ Some(e))),
//      Field("primaryFunction", OptionType(StringType),
//        Some("The primary function of the droid."),
//        resolve = _.value.primaryFunction)
//))

//  case class SimpleGene(id: String, name: Option[String])
//  case class ManhattanAssoc(indexVariantID: String, indexVariantRSID: Option[String], pval: Double,
//                                    chromosome: String, position: Long, bestGenes: List[SimpleGene],
//                                    crediblbeSetSize: Option[Int], ldSetSize: Option[Int])

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
