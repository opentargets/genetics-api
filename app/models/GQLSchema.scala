package models

import sangria.execution.deferred._
import sangria.schema._
import Entities._
import models.DNA.{Gene, Variant}

object GQLSchema {
  val studyId = Argument("studyId", StringType, description = "Study ID which links a top loci with a trait")
  val geneId = Argument("geneId", StringType, description = "Gene ID using Ensembl identifier")
  val studyIds = Argument("studyIds", ListInputType(StringType), description = "List of study IDs")
  val variantId = Argument("variantId", StringType, description = "Variant ID formated as CHR_POSITION_REFALLELE_ALT_ALLELE")
  val variantIds = Argument("variantIds", ListInputType(StringType), description = "Variant ID formated as CHR_POSITION_REFALLELE_ALT_ALLELE")
  val chromosome = Argument("chromosome", StringType, description = "Chromosome as String between 1..22 or X, Y, MT")
  val pageIndex = Argument("pageIndex", OptionInputType(IntType), description = "pagination index >= 0")
  val pageSize = Argument("pageSize", OptionInputType(IntType), description = "pagination size > 0")
  val dnaPosStart = Argument("start", LongType, description = "Start position in a specified chromosome")
  val dnaPosEnd = Argument("end", LongType, description = "End position in a specified chromosome")
  val queryString = Argument("queryString", StringType, description = "Query text to search for")

  implicit val geneHasId = HasId[Gene, String](_.id)
  implicit val variantHasId = HasId[Variant, String](_.id)
  implicit val studyHasId = HasId[Study, String](_.studyId)

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
      Field("fwdStrand", OptionType(BooleanType),
        Some("Forward strand true or false"),
        resolve = _.value.fwd),
      Field("exons", ListType(LongType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.exons)
    ))

  val scoredGene = ObjectType("ScoredGene",
  "This object link a Gene with a score",
    fields[Backend, (String, Double)](
      Field("gene", gene,
        Some("Gene Info"),
        resolve = rsl => genesFetcher.defer(rsl.value._1)),
      Field("score", FloatType,
        Some("Score a Float number between [0. .. 1.]"),
        resolve = _.value._2)
    ))

  val variant = ObjectType("Variant",
    "This element represents a variant object",
    fields[Backend, DNA.Variant](
      Field("id", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.id),
      Field("rsId", OptionType(StringType),
        Some("Approved symbol name of a gene"),
        resolve = _.value.rsId),
      Field("chromosome", StringType,
        Some("Ensembl Gene ID of a gene"),
        resolve = _.value.chromosome),
      Field("position", LongType,
        Some("Approved symbol name of a gene"),
        resolve = _.value.position),
      Field("refAllele", StringType,
        Some("Ref allele"),
        resolve = _.value.refAllele),
      Field("altAllele", StringType,
        Some("Alt allele"),
        resolve = _.value.altAllele),
      Field("nearestGene", OptionType(gene),
        Some("Nearest gene"),
        resolve = _.value.annotation.nearestGeneId match {
          case Some(ng) => genesFetcher.deferOpt(ng)
          case _ => None
        }),
      Field("nearestGeneDistance", OptionType(LongType),
        Some("Distance to the nearest gene (any biotype)"),
        resolve = _.value.annotation.nearestGeneDistance),
      Field("nearestCodingGene", OptionType(gene),
        Some("Nearest protein-coding gene"),
        resolve = _.value.annotation.nearestCodingGeneId match {
          case Some(ng) => genesFetcher.deferOpt(ng)
          case _ => None
        }),
      Field("nearestCodingGeneDistance", OptionType(LongType),
        Some("Distance to the nearest gene (protein-coding biotype)"),
        resolve = _.value.annotation.nearestCodingGeneDistance),
      Field("mostSevereConsequence", OptionType(StringType),
        Some("Most severe consequence"),
        resolve = _.value.annotation.mostSevereConsequence),
      Field("caddRaw", OptionType(FloatType),
        Some("Combined Annotation Dependent Depletion - Raw score"),
        resolve = _.value.caddAnnotation.raw),
      Field("caddPhred", OptionType(FloatType),
        Some("Combined Annotation Dependent Depletion - Scaled score"),
        resolve = _.value.caddAnnotation.phred),
      Field("gnomadAFR", OptionType(FloatType),
        Some("gnomAD Allele frequency (African/African-American population)"),
        resolve = _.value.gnomadAnnotation.afr),
      Field("gnomadAMR", OptionType(FloatType),
        Some("gnomAD Allele frequency (Latino/Admixed American population)"),
        resolve = _.value.gnomadAnnotation.amr),
      Field("gnomadASJ", OptionType(FloatType),
        Some("gnomAD Allele frequency (Ashkenazi Jewish population)"),
        resolve = _.value.gnomadAnnotation.asj),
      Field("gnomadEAS", OptionType(FloatType),
        Some("gnomAD Allele frequency (East Asian population)"),
        resolve = _.value.gnomadAnnotation.eas),
      Field("gnomadFIN", OptionType(FloatType),
        Some("gnomAD Allele frequency (Finnish population)"),
        resolve = _.value.gnomadAnnotation.fin),
      Field("gnomadNFE", OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish European population)"),
        resolve = _.value.gnomadAnnotation.nfe),
      Field("gnomadNFEEST", OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish Eurpoean Estonian sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeEST),
      Field("gnomadNFENWE", OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish Eurpoean North-Western European sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeNWE),
      Field("gnomadNFESEU", OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish Eurpoean Southern European sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeSEU),
      Field("gnomadNFEONF", OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish Eurpoean Other non-Finnish European sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeONF),
      Field("gnomadOTH", OptionType(FloatType),
        Some("gnomAD Allele frequency (Other (population not assigned) population)"),
        resolve = _.value.gnomadAnnotation.oth)
    ))

  val studiesFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100),
    fetch = (ctx: Backend, stids: Seq[String]) => {ctx.getStudies(stids)})

  val genesFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(100),
    fetch = (ctx: Backend, geneIds: Seq[String]) => {ctx.getGenes(geneIds)})

  val variantsFetcher = Fetcher(
    config = FetcherConfig.maxBatchSize(1000),
    fetch = (ctx: Backend, variantIds: Seq[String]) => {ctx.getVariants(variantIds)})

  val resolvers = DeferredResolver.fetchers(studiesFetcher, genesFetcher, variantsFetcher)

  val study = ObjectType("Study",
  "This element contains all study fields",
    fields[Backend, Study](
      Field("studyId", StringType,
        Some("Study Identifier"),
        resolve = _.value.studyId),
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
        resolve = _.value.pubAuthor),
      Field("ancestryInitial", ListType(StringType),
        Some("Ancestry initial"),
        resolve = _.value.ancestryInitial),
      Field("ancestryReplication", ListType(StringType),
        Some("Ancestry replication"),
        resolve = _.value.ancestryReplication),
      Field("nInitial", OptionType(LongType),
        Some("N initial"),
        resolve = _.value.nInitial),
      Field("nReplication", OptionType(LongType),
        Some("N replication"),
        resolve = _.value.nReplication),
      Field("nCases", OptionType(LongType),
        Some("N cases"),
        resolve = _.value.nCases),
      Field("traitCategory", OptionType(StringType),
        Some("Trait category"),
        resolve = _.value.traitCategory),
      Field("numAssocLoci", OptionType(LongType),
        Some("Number of associated loci"),
        resolve = _.value.numAssocLoci)
    ))

  val indexVariantAssociation = ObjectType("IndexVariantAssociation",
    "This object represent a link between a triple (study, trait, index_variant) and a tag variant " +
      "via an expansion method (either ldExpansion or FineMapping)",
    fields[Backend, V2DRow](
      Field("tagVariant", variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = rsl => variantsFetcher.defer(rsl.value.tag.id)),
      Field("study", study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.study.studyId)),
      Field("pval", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pval),
      Field("pvalMantissa", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalMantissa),
      Field("pvalExponent", LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalExponent),
      Field("nTotal", LongType,
        Some("n total cases (n initial + n replication)"),
        resolve = r => r.value.study.nInitial.getOrElse(0L) + r.value.study.nReplication.getOrElse(0L)),
      Field("nCases", LongType,
        Some("n cases"),
        resolve = _.value.study.nCases.getOrElse(0L)),
      Field("overallR2", OptionType(FloatType),
        Some("study ID"),
        resolve = _.value.association.r2),
      Field("afr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.afr1000GProp),
      Field("amr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.amr1000GProp),
      Field("eas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eas1000GProp),
      Field("eur1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eur1000GProp),
      Field("sas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.sas1000GProp),
      Field("log10Abf", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.log10Abf),
      Field("posteriorProbability", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.posteriorProbability),
      Field("oddsRatio", OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCI),
      Field("oddsRatioCILower", OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCILower),
      Field("oddsRatioCIUpper", OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCIUpper),
      Field("beta", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta.betaCI),
      Field("betaCILower", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta.betaCILower),
      Field("betaCIUpper", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta.betaCIUpper),
      Field("direction", OptionType(StringType),
        Some(""),
        resolve = _.value.beta.direction)
    ))


  val tagVariantAssociation = ObjectType("TagVariantAssociation",
    "This object represent a link between a triple (study, trait, index_variant) and a tag variant " +
      "via an expansion method (either ldExpansion or FineMapping)",
    fields[Backend, V2DRow](
      Field("indexVariant", variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = rsl => variantsFetcher.defer(rsl.value.lead.id)),
      Field("study", study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.study.studyId)),
      Field("pval", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pval),
      Field("pvalMantissa", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalMantissa),
      Field("pvalExponent", LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalExponent),
      Field("nTotal", LongType,
        Some("n total cases (n initial + n replication)"),
        resolve = r => r.value.study.nInitial.getOrElse(0L) + r.value.study.nReplication.getOrElse(0L)),
      Field("nCases", LongType,
        Some("n cases"),
        resolve = _.value.study.nCases.getOrElse(0L)),
      Field("overallR2", OptionType(FloatType),
        Some("study ID"),
        resolve = _.value.association.r2),
      Field("afr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.afr1000GProp),
      Field("amr1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.amr1000GProp),
      Field("eas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eas1000GProp),
      Field("eur1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eur1000GProp),
      Field("sas1000GProp", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.sas1000GProp),
      Field("log10Abf", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.log10Abf),
      Field("posteriorProbability", OptionType(FloatType),
        Some(""),
        resolve = _.value.association.posteriorProbability),
      Field("oddsRatio", OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCI),
      Field("oddsRatioCILower", OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCILower),
      Field("oddsRatioCIUpper", OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCIUpper),
      Field("beta", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta.betaCI),
      Field("betaCILower", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta.betaCILower),
      Field("betaCIUpper", OptionType(FloatType),
        Some(""),
        resolve = _.value.beta.betaCIUpper),
      Field("direction", OptionType(StringType),
        Some(""),
        resolve = _.value.beta.direction)
    ))

  val manhattanAssociation = ObjectType("ManhattanAssociation",
  "This element represents an association between a trait and a variant through a study",
    fields[Backend, ManhattanAssociation](
      Field("variant", variant,
        Some("Index variant"),
        resolve = r => variantsFetcher.defer(r.value.variantId)),
      Field("pval", FloatType,
        Some("Computed p-Value"),
        resolve = _.value.pval),
      Field("bestGenes", ListType(scoredGene),
        Some("A list of best genes associated"),
        resolve = _.value.bestGenes),
      Field("credibleSetSize", OptionType(LongType),
      Some("The cardinal of the set defined as tag variants for an index variant coming from crediblesets"),
        resolve = _.value.crediblbeSetSize),
      Field("ldSetSize", OptionType(LongType),
        Some("The cardinal of the set defined as tag variants for an index variant coming from ld expansion"),
        resolve = _.value.ldSetSize),
      Field("totalSetSize", LongType,
        Some("The cardinal of the set defined as tag variants for an index variant coming from any expansion"),
        resolve = _.value.totalSetSize)
    ))


  val pheWASAssociation = ObjectType("PheWASAssociation",
    "This element represents an association between a variant and a reported trait through a study",
    fields[Backend, VariantPheWAS](
      Field("study", OptionType(study),
        Some("Study Object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value.stid)),
      Field("pval", FloatType,
        Some("Computed p-Value"),
        resolve = _.value.pval),
      Field("beta", FloatType,
        Some("beta"),
        resolve = _.value.beta),
      Field("nTotal", OptionType(LongType),
        Some("total sample size (variant level)"),
        resolve = _.value.nSamplesVariant),
      Field("nCases", OptionType(LongType),
        Some("number of cases (variant level)"),
        resolve = _.value.nCasesVariant),
      Field("nTotalStudy", OptionType(LongType),
        Some("total sample size (study level, available when variant level is not provided)"),
        resolve = _.value.nSamplesStudy),
      Field("nCasesStudy", OptionType(LongType),
        Some("number of cases (study level, available when variant level is not provided)"),
        resolve = _.value.nCasesStudy),
      Field("oddsRatio", OptionType(FloatType),
        Some("Odds ratio (if case control)"),
        resolve = _.value.oddRatio),
      Field("chip", StringType,
        Some("Chip type: 'metabochip' 'inmunochip', 'genome wide'"),
        resolve = _.value.chip),
      Field("info", OptionType(FloatType),
        Some("Info"),
        resolve = _.value.info)
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
        resolve = _.value.v2DAssociation.r2),
      Field("posteriorProbability", OptionType(FloatType),
        Some(""),
        resolve = _.value.v2DAssociation.posteriorProbability),
      Field("pval", FloatType,
        Some(""),
        resolve = _.value.v2DAssociation.pval),
      Field("pvalMantissa", FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.v2DAssociation.pvalMantissa),
      Field("pvalExponent", LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.v2DAssociation.pvalExponent)
    ))

  val gecko = ObjectType("Gecko",
    "",
    fields[Backend, Gecko](
      Field("genes", ListType(gene),
        Some(""),
        resolve = rsl => genesFetcher.deferSeq(rsl.value.geneIds)),
      Field("tagVariants", ListType(variant),
        Some(""),
        resolve = rsl => variantsFetcher.deferSeq(rsl.value.tagVariants)),
      Field("indexVariants", ListType(variant),
        Some(""),
        resolve = rsl => variantsFetcher.deferSeq(rsl.value.indexVariants)),
      Field("studies", ListType(study),
        Some(""),
        resolve = rsl => studiesFetcher.deferSeq(rsl.value.studies)),
      Field("geneTagVariants", ListType(geneTagVariant),
        Some(""),
        resolve = _.value.geneTagVariants),
      Field("tagVariantIndexVariantStudies", ListType(tagVariantIndexVariantStudy),
        Some(""),
        resolve = _.value.tagVariantIndexVariantStudies)
    ))

  val overlap = ObjectType("Overlap",
  "This element represent an overlap between two variants for two studies",
    fields[Backend, OverlappedVariant](
      Field("variantIdA", StringType, None, resolve = _.value.variantIdA),
      Field("variantIdB", StringType, None, resolve = _.value.variantIdB),
      Field("overlapAB", LongType, None, resolve = _.value.overlapAB),
      Field("distinctA", LongType, None, resolve = _.value.distinctA),
      Field("distinctB", LongType, None, resolve = _.value.distinctB),
    ))

  val overlappedStudy = ObjectType("OverlappedStudy",
    "This element represent a overlap between two stduies",
    fields[Backend, OverlapRow](
      Field("study", study,
        Some("A study object"),
        resolve = rsl => studiesFetcher.defer(rsl.value.stid)),
      Field("numOverlapLoci", IntType,
        Some("Orig variant id which is been used for computing the " +
          "overlap with the referenced study"),
        resolve = _.value.numOverlapLoci)
    ))

  val overlappedVariantsStudies = ObjectType("OverlappedVariantsStudies",
    "This element represent a overlap between two stduies",
    fields[Backend, OverlappedVariantsStudy](
      Field("study", OptionType(study),
        Some("A study object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value.studyId)),
      Field("overlaps", ListType(overlap),
        Some("Orig variant id which is been used for computing the " +
          "overlap with the referenced study"),
        resolve = _.value.overlaps)
    ))

  val topOverlappedStudies = ObjectType("TopOverlappedStudies",
    "This element represent a overlap between two stduies",
    fields[Backend, OverlappedLociStudy](
      Field("study", OptionType(study),
        Some("A study object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value.studyId)),
      Field("topStudiesByLociOverlap", ListType(overlappedStudy),
        Some("Top N studies ordered by loci overlap"),
        resolve = _.value.topOverlappedStudies)
    ))

  val studyForGene = ObjectType("StudyForGene", "",
    fields[Backend, String](
      Field("study", study,
        Some("A study object"),
        resolve = rsl => studiesFetcher.defer(rsl.value))
    ))

  val overlappedInfoForStudy = ObjectType("OverlappedInfoForStudy", "",
    fields[Backend, (String, Seq[String])](
      Field("study", OptionType(study),
        Some("A study object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value._1)),
      Field("overlappedVariantsForStudies", ListType(overlappedVariantsStudies),
        Some(""),
        resolve = rsl => rsl.ctx.getOverlapVariantsForStudies(rsl.value._1, rsl.value._2)),
      Field("variantIntersectionSet", ListType(StringType),
        Some(""),
        resolve = rsl => rsl.ctx.getOverlapVariantsIntersectionForStudies(rsl.value._1, rsl.value._2))
    ))

  val manhattan = ObjectType("Manhattan",
    "This element represents a Manhattan like plot",
    fields[Backend, ManhattanTable](
      Field("associations", ListType(manhattanAssociation),
        Some("A list of associations"),
        resolve = _.value.associations),
      Field("topOverlappedStudies", OptionType(topOverlappedStudies),
        Some("A list of overlapped studies"),
        arguments = pageIndex :: pageSize :: Nil,
        resolve = ctx => ctx.ctx.getTopOverlappedStudies(ctx.value.studyId, ctx.arg(pageIndex), ctx.arg(pageSize)))
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
    fields[Backend, VariantToDiseaseTable](
      Field("associations", ListType(indexVariantAssociation),
        Some("A list of associations connected to a Index variant and a Study through some expansion methods"),
        resolve = _.value.associations)
    ))

  val indexVariantsAndStudiesForTagVariant = ObjectType("IndexVariantsAndStudiesForTagVariant",
    "A list of rows with each link",
    fields[Backend, VariantToDiseaseTable](
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
        resolve = _.value.functionalPredictions),
      Field("distances", ListType(g2vSchemaElement),
        Some("Distance structure definition"),
        resolve = _.value.distances)
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

  val distanceTisse = ObjectType("DistanceTissue",
    "",
    fields[Backend, DistancelTissue](
      Field("tissue", tissue,
        Some(""),
        resolve = _.value.tissue),
      Field("distance", OptionType(LongType),
        Some("Distance to the canonical TSS"),
        resolve = _.value.distance),
      Field("score", OptionType(FloatType),
        Some("Score 1 / Distance"),
        resolve = _.value.score),
      Field("quantile", OptionType(FloatType),
        Some("Quantile of the score"),
        resolve = _.value.quantile)
    ))

  val qtlElement = ObjectType("QTLElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[QTLTissue]](
      Field("typeId", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType,
        Some(""),
        resolve = _.value.aggregatedScore),
      Field("tissues", ListType(qtlTissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val intervalElement = ObjectType("IntervalElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[IntervalTissue]](
      Field("typeId", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType,
        Some(""),
        resolve = _.value.aggregatedScore),
      Field("tissues", ListType(intervalTissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val fPredElement = ObjectType("FunctionalPredictionElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[FPredTissue]](
      Field("typeId", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType,
        Some(""),
        resolve = _.value.aggregatedScore),
      Field("tissues", ListType(fpredTissue),
        Some(""),
        resolve = _.value.tissues)
    ))

  val distElement = ObjectType("DistanceElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[DistancelTissue]](
      Field("typeId", StringType,
        Some(""),
        resolve = _.value.id),
      Field("sourceId", StringType,
        Some(""),
        resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType,
        Some(""),
        resolve = _.value.aggregatedScore),
      Field("tissues", ListType(distanceTisse),
        Some(""),
        resolve = _.value.tissues)
    ))

  val geneForVariant = ObjectType("GeneForVariant",
    "A list of rows with each link",
    fields[Backend, G2VAssociation](
      Field("gene", gene,
        Some("Associated scored gene"),
        resolve = rsl => genesFetcher.defer(rsl.value.geneId)),
      Field("variant", StringType,
        Some("Associated scored variant"),
        resolve = _.value.variantId),
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
        resolve = _.value.fpreds),
      Field("distances", ListType(distElement),
        Some(""),
        resolve = _.value.distances)
    ))

  val variantSearchResult = ObjectType("VariantSearchResult",
    "Variant search result object",
    fields[Backend, VariantSearchResult](
      Field("variant", variant,
        Some("A variant"),
        resolve = _.value.variant)
    ))

  val searchResult = ObjectType("SearchResult",
    "Search data by a query string",
    fields[Backend, SearchResultSet](
      Field("totalGenes", LongType,
        Some("Total number of genes found"),
        resolve = _.value.totalGenes),
      Field("totalVariants", LongType,
        Some("Total number of variants found"),
        resolve = _.value.totalVariants),
      Field("totalStudies", LongType,
        Some("Total number of studies found"),
        resolve = _.value.totalStudies),
      Field("genes", ListType(gene),
        Some("Gene search result list"),
        resolve = _.value.genes),
      Field("variants", ListType(variantSearchResult),
        Some("Variant search result list"),
        resolve = _.value.variants),
      Field("studies", ListType(study),
        Some("Study search result list"),
        resolve = _.value.studies)
    ))

  val query = ObjectType(
    "Query", fields[Backend, Unit](
      Field("search", searchResult,
        arguments = queryString :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.getSearchResultSet(ctx.arg(queryString), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("studyInfo", OptionType(study),
        arguments = studyId :: Nil,
        resolve = ctx => studiesFetcher.deferOpt(ctx.arg(studyId))),
      Field("variantInfo", OptionType(variant),
        arguments = variantId :: Nil,
        resolve = ctx => variantsFetcher.deferOpt(ctx.arg(variantId))),
      Field("studiesForGene", ListType(studyForGene),
        arguments = geneId :: Nil,
        resolve = ctx => ctx.ctx.getStudiesForGene(ctx.arg(geneId))),
      Field("manhattan", manhattan,
        arguments = studyId :: pageIndex :: pageSize :: Nil,
        resolve = ctx => ctx.ctx.buildManhattanTable(ctx.arg(studyId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("topOverlappedStudies", topOverlappedStudies,
        arguments = studyId :: pageIndex :: pageSize :: Nil,
        resolve = ctx => ctx.ctx.getTopOverlappedStudies(ctx.arg(studyId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("overlapInfoForStudy", overlappedInfoForStudy,
        arguments = studyId :: studyIds :: Nil,
        resolve = ctx => (ctx.arg(studyId),  ctx.arg(studyIds))),
      Field("tagVariantsAndStudiesForIndexVariant", tagVariantsAndStudiesForIndexVariant,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.buildIndexVariantAssocTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("indexVariantsAndStudiesForTagVariant", indexVariantsAndStudiesForTagVariant,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.buildTagVariantAssocTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("pheWAS", pheWAS,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx => ctx.ctx.buildPheWASTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field("gecko", OptionType(gecko),
        arguments = chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve = ctx => ctx.ctx.buildGecko(ctx.arg(chromosome), ctx.arg(dnaPosStart), ctx.arg(dnaPosEnd))),
      Field("genesForVariantSchema", v2gSchema,
        arguments = Nil,
        resolve = ctx => ctx.ctx.getG2VSchema),
      Field("genesForVariant", ListType(geneForVariant),
        arguments = variantId :: Nil,
        resolve = ctx => ctx.ctx.buildG2VByVariant(ctx.arg(variantId))),
//      Field("variantsForGene", ListType(geneForVariant),
//        arguments = geneId :: Nil,
//        resolve = ctx => ctx.ctx.buildG2VByGene(ctx.arg(geneId)))
    ))

  val schema = Schema(query)
}
