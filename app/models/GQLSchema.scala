package models


import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._
import Entities._
import sangria.marshalling.ToInput

object GQLSchema {
  val studyId = Argument("studyId", StringType, description = "Study ID which links a top loci with a trait")
  val variantId = Argument("variantId", StringType, description = "Variant ID formated as CHR_POSITION_REFALLELE_ALT_ALLELE")
  val pageIndex = Argument("pageIndex", OptionInputType(IntType), description = "pagination index >= 0")
  val pageSize = Argument("pageSize", OptionInputType(IntType), description = "pagination size > 0")

  val gene = ObjectType("Gene",
  "This element represents a simple gene object which contains id and name",
    fields[Backend, Gene](
      Field("id", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.id),
      Field("symbol", OptionType(StringType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.symbol)
    ))

  val variant = ObjectType("Variant",
    "This element represents a variant object",
    fields[Backend, Variant](
      Field("id", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.id),
      Field("rsId", OptionType(StringType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.rsId),
      Field("chromosome", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.locus.chrId),
      Field("position", LongType,
        Some("Approved symbol name of a gene"),
        resolve = _.value.locus.position)
    ))

  val study = ObjectType("Study",
  "This element contains all study fields",
    fields[Backend, Study](
      Field("studyId", StringType,
        Some("Study Identifier"),
        resolve = _.value.studyId),
      Field("traitCode", StringType,
        Some("Trait Identifier"),
        resolve = _.value.traitCode),
      Field("traitReported", StringType,
        Some("Trait Label as reported on the publication"),
        resolve = _.value.traitReported),
      Field("traitEfos", ListType(StringType),
        Some("A list of curated efo codes"),
        resolve = _.value.traitEfos),
      Field("pmid", OptionType(StringType),
        Some("PubMed ID for the corresponding publication"),
        resolve = _.value.pubId),
      Field("pubDate", OptionType(StringType),
        Some("Publication Date as YYYY-MM-DD"),
        resolve = _.value.pubDate),
      Field("pubJournal", OptionType(StringType),
        Some("Publication Journal name"),
        resolve = _.value.pubJournal),
      Field("pubTitle", OptionType(StringType),
        Some("Publication Title"),
        resolve = _.value.pubTitle),
      Field("pubAuthor", OptionType(StringType),
        Some("Publication author"),
        resolve = _.value.pubAuthor)
    ))

  // TODO missing a lot fields but enough to test
  val manhattanAssociation = ObjectType("ManhattanAssociation",
  "This element represents an association between a trait and a variant through a study",
    fields[Backend, ManhattanAssociation](
      Field("variantId", StringType,
        Some("Index variant ID as ex. 1_12345_A_T"),
        resolve = _.value.variant.id),
      Field("variantRsId", OptionType(StringType),
        Some("RSID code for the given index variant as ex. rs12345"),
        resolve = _.value.variant.rsId),
      Field("pval", FloatType,
        Some("Computed p-Value"),
        resolve = _.value.pval),
      Field("chromosome", StringType,
        Some("Chromosome letter from a set of (1-22, X, Y, MT)"),
        resolve = _.value.variant.locus.chrId),
      Field("position", LongType,
        Some("absolute position p of the variant i in the chromosome j"),
        resolve = _.value.variant.locus.position),
      Field("bestGenes", ListType(gene),
        Some("A list of best genes associated"),
        resolve = _.value.bestGenes),
      Field("credibleSetSize", LongType,
      Some("The cardinal of the set defined as tag variants for an index variant coming from crediblesets"),
        resolve = _.value.crediblbeSetSize),
      Field("ldSetSize", LongType,
        Some("The cardinal of the set defined as tag variants for an index variant coming from ld expansion"),
        resolve = _.value.ldSetSize),
      Field("totalSetSize", LongType,
        Some("The cardinal of the set defined as tag variants for an index variant coming from any expansion"),
        resolve = _.value.totalSetSize)
    ))

  val pheWASAssociation = ObjectType("PheWASAssociation",
    "This element represents an association between a variant and a reported trait through a study",
    fields[Backend, PheWASAssociation](
      Field("studyId", StringType,
        Some("Study ID"),
        resolve = _.value.studyId),
      Field("traitReported", StringType,
        Some("Trait reported"),
        resolve = _.value.traitReported),
      Field("traitId", OptionType(StringType),
        Some("Trait ID reported"),
        resolve = _.value.traitId),
      Field("pval", FloatType,
        Some("Computed p-Value"),
        resolve = _.value.pval),
      Field("beta", FloatType,
        Some("beta"),
        resolve = _.value.beta),
      Field("nTotal", LongType,
        Some("total sample size (variant level)"),
        resolve = _.value.nTotal),
      Field("nCases", LongType,
        Some("number of cases (variant level)"),
        resolve = _.value.nCases)
    ))


  val manhattan = ObjectType("Manhattan",
    "This element represents a Manhattan like plot",
    fields[Backend, ManhattanTable](
      Field("associations", ListType(manhattanAssociation),
        Some("A list of associations"),
        resolve = _.value.associations)
    ))

  val studyInfo = ObjectType("StudyInfo",
  "This element represents a Study with a reported trait",
    fields[Backend, StudyInfo](
      Field("study", OptionType(study),
        Some("A Study object"),
        resolve = _.value.study)
    ))

  val pheWAS = ObjectType("PheWAS",
    "This element represents a PheWAS like plot",
    fields[Backend, PheWASTable](
      Field("associations", ListType(pheWASAssociation),
        Some("A list of associations"),
        resolve = _.value.associations)
    ))

  val query = ObjectType(
    "Query", fields[Backend, Unit](
      Field("studyInfo", OptionType(study),
        arguments = studyId :: Nil,
        resolve = (ctx) => ctx.ctx.getStudyInfo(ctx.arg(studyId))),
      Field("manhattan", manhattan,
        arguments = studyId :: pageIndex :: pageSize :: Nil,
        resolve = (ctx) => ctx.ctx.buildManhattanTable(ctx.arg(studyId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("pheWAS", pheWAS,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = (ctx) => ctx.ctx.buildPheWASTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize)))
    ))

  val schema = Schema(query)
}
