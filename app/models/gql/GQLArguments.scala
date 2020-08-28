package models.gql

import components.elasticsearch.Pagination
import sangria.marshalling.FromInput
import sangria.marshalling.playJson._
import sangria.schema.{Argument, InputObjectType, IntType, ListInputType, LongType, OptionInputType, StringType}
import sangria.util.tag.@@

trait GQLArguments {

  import sangria.macros.derive._

  val studyId: Argument[String] =
    Argument("studyId", StringType, description = "Study ID which links a top loci with a trait")

  val studyIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("studyIds", ListInputType(StringType), description = "List of study IDs")

  val geneId: Argument[String] =
    Argument("geneId", StringType, description = "Gene ID using Ensembl identifier")

  val phenotypeId: Argument[String] = Argument(
    "phenotypeId",
    StringType,
    description = "Phenotype ID using Ensembl identifier for the molecular traits")

  val variantId: Argument[String] = Argument(
    "variantId",
    StringType,
    description = "Variant ID formated as CHR_POSITION_REFALLELE_ALT_ALLELE")

  val variantIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] = Argument(
    "variantIds",
    ListInputType(StringType),
    description = "Variant ID formated as CHR_POSITION_REFALLELE_ALT_ALLELE")

  val chromosome: Argument[String] = Argument(
    "chromosome",
    StringType,
    description = "Chromosome as String between 1..22 or X, Y, MT")

  val pageIndex: Argument[Option[Int]] =
    Argument("pageIndex", OptionInputType(IntType), description = "pagination index >= 0")

  val pageSize: Argument[Option[Int]] =
    Argument("pageSize", OptionInputType(IntType), description = "pagination size > 0")

  val pagination: InputObjectType[Pagination] = deriveInputObjectType[Pagination]()

  val pageArg: Argument[Option[Pagination]] = Argument("page", OptionInputType(pagination))

  val dnaPosStart: Argument[Long] =
    Argument("start", LongType, description = "Start position in a specified chromosome")

  val dnaPosEnd: Argument[Long] =
    Argument("end", LongType, description = "End position in a specified chromosome")

  val queryString: Argument[String] =
    Argument("queryString", StringType, description = "Query text to search for")

  val bioFeature: Argument[String] = Argument(
    "bioFeature",
    StringType,
    description = "BioFeature represents either a tissue, cell type, aggregation type, ...")

}
