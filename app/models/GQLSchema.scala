package models


import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._
import Entities._
import sangria.marshalling.ToInput

object GQLSchema {
  val studyId = Argument("studyId", StringType, description = "Study ID which links a top loci with a trait")
  val variantId = Argument("variantId", StringType, description = "Variant ID formated as CHR_POSITION_REFALLELE_ALT_ALLELE")
  val chromosome = Argument("chromosome", StringType, description = "Chromosome as String between 1..22 or X, Y, MT")
  val pageIndex = Argument("pageIndex", OptionInputType(IntType), description = "pagination index >= 0")
  val pageSize = Argument("pageSize", OptionInputType(IntType), description = "pagination size > 0")
  val dnaPosStart = Argument("start", LongType, description = "Start position in a specified chromosome")
  val dnaPosEnd = Argument("end", LongType, description = "End position in a specified chromosome")

  val gene = ObjectType("Gene",
  "This element represents a simple gene object which contains id and name",
    fields[Backend, Gene](
      Field("id", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.id),
      Field("symbol", OptionType(StringType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.symbol),
      Field("chromosome", OptionType(StringType),
        Some("Chromosome"),
        resolve = _.value.chromosome),
      Field("start", OptionType(LongType),
        Some("Start position for the gene"),
        resolve = _.value.start),
      Field("end", OptionType(LongType),
        Some("End position for the gene"),
        resolve = _.value.end),
      Field("tss", OptionType(LongType),
        Some("Transcription start site"),
        resolve = _.value.tss),
      Field("bioType", OptionType(StringType),
        Some("Bio-type of the gene"),
        resolve = _.value.bioType),
      Field("exons", ListType(LongType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.exons)
    ))

  val scoredGene = ObjectType("ScoredGene",
  "This object link a Gene with a score",
    fields[Backend, (Gene, Double)](
      Field("gene", gene,
        Some("Gene Info"),
        resolve = _.value._1),
      Field("score", FloatType,
        Some("Score a Float number between [0. .. 1.]"),
        resolve = _.value._2)
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
        resolve = _.value.locus.position),
      Field("refAllele", StringType,
        Some("Ref allele"),
        resolve = _.value.refAllele),
      Field("altAllele", StringType,
        Some("Alt allele"),
        resolve = _.value.altAllele)
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

  val indexVariantAssociation = ObjectType("IndexVariantAssociation",
    "This object represent a link between a triple (study, trait, index_variant) and a tag variant " +
      "via an expansion method (either ldExpansion or FineMapping)",
    fields[Backend, IndexVariantAssociation](
      Field("tagVariant", variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = _.value.tagVariant),
      Field("study", study,
        Some("study ID"),
        resolve = _.value.study),
      Field("pval", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.pval),
      Field("nTotal", IntType,
        Some("n total cases (n initial + n replication)"),
        resolve = _.value.nTotal),
      Field("nCases", IntType,
        Some("n cases"),
        resolve = _.value.nCases),
      Field("overallR2", OptionType(FloatType),
        Some("study ID"),
        resolve = _.value.r2),
      Field("afr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.afr1000GProp),
      Field("amr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.amr1000GProp),
      Field("eas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.eas1000GProp),
      Field("eur1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.eur1000GProp),
      Field("sas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.sas1000GProp),
      Field("log10Abf", OptionType(FloatType),
        Some(""),
        resolve = _.value.log10Abf),
      Field("posteriorProbability", OptionType(FloatType),
        Some(""),
        resolve = _.value.posteriorProbability)
    ))

  val tagVariantAssociation = ObjectType("TagVariantAssociation",
    "This object represent a link between a triple (study, trait, index_variant) and a tag variant " +
      "via an expansion method (either ldExpansion or FineMapping)",
    fields[Backend, TagVariantAssociation](
      Field("indexVariant", variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = _.value.indexVariant),
      Field("study", study,
        Some("study ID"),
        resolve = _.value.study),
      Field("pval", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.pval),
      Field("nTotal", IntType,
        Some("n total cases (n initial + n replication)"),
        resolve = _.value.nTotal),
      Field("nCases", IntType,
        Some("n cases"),
        resolve = _.value.nCases),
      Field("overallR2", OptionType(FloatType),
        Some("study ID"),
        resolve = _.value.r2),
      Field("afr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.afr1000GProp),
      Field("amr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.amr1000GProp),
      Field("eas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.eas1000GProp),
      Field("eur1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.eur1000GProp),
      Field("sas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.sas1000GProp),
      Field("log10Abf", OptionType(FloatType),
        Some(""),
        resolve = _.value.log10Abf),
      Field("posteriorProbability", OptionType(FloatType),
        Some(""),
        resolve = _.value.posteriorProbability)
    ))

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
      Field("bestGenes", ListType(scoredGene),
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

  val geneTagVariant = ObjectType("GeneTagVariant",
    "",
    fields[Backend, GeneTagVariant](
      Field("geneId", StringType,
        Some(""),
        resolve = _.value.geneId),
      Field("tagVariantId", StringType,
        Some(""),
        resolve = _.value.tagVariantId),
      Field("overallScore", OptionType(FloatType),
        Some(""),
        resolve = _.value.overallScore)
    ))

  val tagVariantIndexVariantStudy = ObjectType("TagVariantIndexVariantStudy",
    "",
    fields[Backend, TagVariantIndexVariantStudy](
      Field("tagVariantId", StringType,
        Some(""),
        resolve = _.value.tagVariantId),
      Field("indexVariantId", StringType,
        Some(""),
        resolve = _.value.indexVariantId),
      Field("studyId", StringType,
        Some(""),
        resolve = _.value.studyId),
      Field("r2", OptionType(FloatType),
        Some(""),
        resolve = _.value.r2),
      Field("posteriorProbability", OptionType(FloatType),
        Some(""),
        resolve = _.value.posteriorProb),
      Field("pval", FloatType,
        Some(""),
        resolve = _.value.pval)
    ))

  val gecko = ObjectType("Gecko",
    "",
    fields[Backend, Gecko](
      Field("genes", ListType(gene),
        Some(""),
        resolve = _.value.genes),
      Field("tagVariants", ListType(variant),
        Some(""),
        resolve = _.value.tagVariants),
      Field("indexVariants", ListType(variant),
        Some(""),
        resolve = _.value.indexVariants),
      Field("studies", ListType(study),
        Some(""),
        resolve = _.value.studies),
      Field("geneTagVariants", ListType(geneTagVariant),
        Some(""),
        resolve = _.value.geneTagVariants),
      Field("tagVariantIndexVariantStudies", ListType(tagVariantIndexVariantStudy),
        Some(""),
        resolve = _.value.tagVariantIndexVariantStudies)
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


  val tagVariantsAndStudiesForIndexVariant = ObjectType("TagVariantsAndStudiesForIndexVariant",
    "A list of rows with each link",
    fields[Backend, IndexVariantTable](
      Field("associations", ListType(indexVariantAssociation),
        Some("A list of associations connected to a Index variant and a Study through some expansion methods"),
        resolve = _.value.associations)
    ))

  val indexVariantsAndStudiesForTagVariant = ObjectType("IndexVariantsAndStudiesForTagVariant",
    "A list of rows with each link",
    fields[Backend, TagVariantTable](
      Field("associations", ListType(tagVariantAssociation),
        Some("A list of associations connected to a Index variant and a Study through some expansion methods"),
        resolve = _.value.associations)
    ))

  val tissue = ObjectType("Tissue",
    "",
    fields[Backend, Tissue](
      Field("id", StringType,
        Some(""),
        resolve = _.value.id),
      Field("name", OptionType(StringType),
        Some(""),
        resolve = _.value.name)
    ))

  val g2vSchemaElement = ObjectType("G2VSchemaElement",
    "A list of rows with each link",
    fields[Backend, G2VSchemaElement](
      Field("id", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("tissues", ListType(tissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val v2gSchema = ObjectType("G2VSchema",
    "A list of rows with each link",
    fields[Backend, G2VSchema](
      Field("qtls", ListType(g2vSchemaElement),
        Some("qtl structure definition"),
        resolve = _.value.qtls),
      Field("intervals", ListType(g2vSchemaElement),
        Some("qtl structure definition"),
        resolve = _.value.intervals),
      Field("functionalPredictions", ListType(g2vSchemaElement),
        Some("qtl structure definition"),
        resolve = _.value.functionalPredictions)
    ))

  val qtlTissue = ObjectType("QTLTissue",
    "",
    fields[Backend, QTLTissue](
      Field("tissue", tissue,
        Some(""),
        resolve = _.value.tissue),
      Field("quantile", FloatType,
        Some(""),
        resolve = _.value.quantile),
      Field("beta", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta),
      Field("pval", OptionType(FloatType),
        Some(""),
        resolve = _.value.pval)
    ))

  val intervalTissue = ObjectType("IntervalTissue",
    "",
    fields[Backend, IntervalTissue](
      Field("tissue", tissue,
        Some(""),
        resolve = _.value.tissue),
      Field("quantile", FloatType,
        Some(""),
        resolve = _.value.quantile),
      Field("score", OptionType(FloatType),
        Some(""),
        resolve = _.value.score)
    ))

  val fpredTissue = ObjectType("FPredTissue",
    "",
    fields[Backend, FPredTissue](
      Field("tissue", tissue,
        Some(""),
        resolve = _.value.tissue),
      Field("maxEffectLabel", OptionType(StringType),
        Some(""),
        resolve = _.value.maxEffectLabel),
      Field("maxEffectScore", OptionType(FloatType),
        Some(""),
        resolve = _.value.maxEffectScore)
    ))


  val qtlElement = ObjectType("QTLElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[QTLTissue]](
      Field("id", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("tissues", ListType(qtlTissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val intervalElement = ObjectType("IntervalElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[IntervalTissue]](
      Field("id", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("tissues", ListType(intervalTissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val fPredElement = ObjectType("FunctionalPredictionElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[FPredTissue]](
      Field("id", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("tissues", ListType(fpredTissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val geneForVariant = ObjectType("GeneForVariant",
    "A list of rows with each link",
    fields[Backend, G2VAssociation](
      Field("gene", gene,
        Some("Associated scored gene"),
        resolve = _.value.gene),
      Field("overallScore", FloatType,
        Some(""),
        resolve = _.value.overallScore),
      Field("qtls", ListType(qtlElement),
        Some(""),
        resolve = _.value.qtls),
      Field("intervals", ListType(intervalElement),
        Some(""),
        resolve = _.value.intervals),
      Field("functionalPredictions", ListType(fPredElement),
        Some(""),
        resolve = _.value.fpreds)
    ))

  val query = ObjectType(
    "Query", fields[Backend, Unit](
      Field("studyInfo", OptionType(study),
        arguments = studyId :: Nil,
        resolve = (ctx) => ctx.ctx.getStudyInfo(ctx.arg(studyId))),
      Field("manhattan", manhattan,
        arguments = studyId :: pageIndex :: pageSize :: Nil,
        resolve = (ctx) => ctx.ctx.buildManhattanTable(ctx.arg(studyId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("tagVariantsAndStudiesForIndexVariant", tagVariantsAndStudiesForIndexVariant,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = (ctx) =>
          ctx.ctx.buildIndexVariantAssocTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("indexVariantsAndStudiesForTagVariant", indexVariantsAndStudiesForTagVariant,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = (ctx) =>
          ctx.ctx.buildTagVariantAssocTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("pheWAS", pheWAS,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = (ctx) => ctx.ctx.buildPheWASTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("gecko", OptionType(gecko),
        arguments = chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve = (ctx) => ctx.ctx.buildGecko(ctx.arg(chromosome), ctx.arg(dnaPosStart), ctx.arg(dnaPosEnd))),
      Field("genesForVariantSchema", v2gSchema,
        arguments = Nil,
        resolve = (ctx) => ctx.ctx.getG2VSchema),
      Field("genesForVariant", ListType(geneForVariant),
        arguments = variantId :: Nil,
        resolve = (ctx) => ctx.ctx.buildG2V(ctx.arg(variantId)))
    ))

  val schema = Schema(query)
}
