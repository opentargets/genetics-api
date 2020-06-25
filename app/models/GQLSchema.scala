package models

import models.entities.DNA.SimpleVariant
import models.entities.Entities._
import models.Functions._
import models.entities.DNA
import models.gql.{GQLArguments, GQLGene, GQLIndexVariantAssociation, GQLManhattanAssociation, GQLStudy, GQLStudyLeadVariantAssociation, GQLTagVariantAssociation, GQLVariant}
import sangria.execution.deferred._
import sangria.macros.derive._
import sangria.schema.{Field, _}

object GQLSchema
  extends GQLGene
    with GQLVariant
    with GQLStudy
    with GQLIndexVariantAssociation
    with GQLTagVariantAssociation
    with GQLManhattanAssociation
    with GQLStudyLeadVariantAssociation with GQLArguments {


  val resolvers = DeferredResolver.fetchers(studiesFetcher, genesFetcher, variantsFetcher)

  val regionalAssociation = ObjectType(
    "RegionalAssociation",
    "Variant with a p-val",
    fields[Backend, (SimpleVariant, Double)](
      Field(
        "variant",
        variant,
        Some("Summary Stats simple variant information"),
        resolve = r => DNA.Variant.fromString(r.value._1.id).right.get),
      Field("pval", FloatType, Some("p-val"), resolve = _.value._2)))

  val credSetTagElement = ObjectType(
    "CredSetTagElement",
    "Thsi element represents the tag variant with its associated statistics",
    fields[Backend, (SimpleVariant, CredSetRowStats)](
      Field(
        "tagVariant",
        variant,
        Some("Tag Variant in the credibleset table"),
        resolve = r => DNA.Variant.fromString(r.value._1.id).right.get),
      Field("pval", FloatType, Some("p-val"), resolve = _.value._2.tagPval),
      Field("se", FloatType, Some("SE"), resolve = _.value._2.tagSE),
      Field("beta", FloatType, Some("beta"), resolve = _.value._2.tagBeta),
      Field("postProb", FloatType, Some("Posterior Probability"), resolve = _.value._2.postProb),
      Field(
        "MultisignalMethod",
        StringType,
        Some("Multisignal Method"),
        resolve = _.value._2.multiSignalMethod),
      Field("logABF", FloatType, Some("Log ABF"), resolve = _.value._2.logABF),
      Field("is95", BooleanType, Some("Is over 95 percentile"), resolve = _.value._2.is95),
      Field("is99", BooleanType, Some("Is over 99 percentile"), resolve = _.value._2.is99)))

  val pheWASAssociation = ObjectType(
    "PheWASAssociation",
    "This element represents an association between a variant and a reported trait through a study",
    fields[Backend, SumStatsGWASRow](
      Field(
        "study",
        OptionType(study),
        Some("Study Object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value.studyId)),
      Field("pval", FloatType, Some("Computed p-Value"), resolve = _.value.pval),
      Field("beta", OptionType(FloatType), Some("beta"), resolve = _.value.beta),
      Field(
        "nTotal",
        OptionType(LongType),
        Some("total sample size (variant level)"),
        resolve = _.value.nTotal),
      Field("nCases", OptionType(LongType), Some("number of cases"), resolve = _.value.nCases),
      Field(
        "oddsRatio",
        OptionType(FloatType),
        Some("Odds ratio (if case control)"),
        resolve = _.value.oddsRatio),
      Field("eaf", OptionType(FloatType), Some("Effect Allele Frequency"), resolve = _.value.eaf),
      Field("se", OptionType(FloatType), Some("Standard Error"), resolve = _.value.se)))

  val geneTagVariant = ObjectType(
    "GeneTagVariant",
    "",
    fields[Backend, GeneTagVariant](
      Field("geneId", StringType, Some(""), resolve = _.value.geneId),
      Field("tagVariantId", StringType, Some(""), resolve = _.value.tagVariantId),
      Field("overallScore", OptionType(FloatType), Some(""), resolve = _.value.overallScore)))

  val tagVariantIndexVariantStudy = ObjectType(
    "TagVariantIndexVariantStudy",
    "",
    fields[Backend, TagVariantIndexVariantStudy](
      Field("tagVariantId", StringType, Some(""), resolve = _.value.tagVariantId),
      Field("indexVariantId", StringType, Some(""), resolve = _.value.indexVariantId),
      Field("studyId", StringType, Some(""), resolve = _.value.studyId),
      Field("r2", OptionType(FloatType), Some(""), resolve = _.value.v2DAssociation.r2),
      Field(
        "posteriorProbability",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.v2DAssociation.posteriorProbability),
      Field(
        "pval",
        FloatType,
        Some(""),
        resolve = r =>
          toSafeDouble(r.value.v2DAssociation.pvalMantissa, r.value.v2DAssociation.pvalExponent)
      ), // TODO TEMPORAL HACK
      Field(
        "pvalMantissa",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.v2DAssociation.pvalMantissa),
      Field(
        "pvalExponent",
        LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.v2DAssociation.pvalExponent),
      Field("oddsRatio", OptionType(FloatType), Some(""), resolve = _.value.odds.oddsCI),
      Field(
        "oddsRatioCILower",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCILower),
      Field(
        "oddsRatioCIUpper",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.odds.oddsCIUpper),
      Field("beta", OptionType(FloatType), Some(""), resolve = _.value.beta.betaCI),
      Field("betaCILower", OptionType(FloatType), Some(""), resolve = _.value.beta.betaCILower),
      Field("betaCIUpper", OptionType(FloatType), Some(""), resolve = _.value.beta.betaCIUpper),
      Field("direction", OptionType(StringType), Some(""), resolve = _.value.beta.direction)))

  val gecko = ObjectType(
    "Gecko",
    "",
    fields[Backend, Gecko](
      Field(
        "genes",
        ListType(gene),
        Some(""),
        resolve = rsl => genesFetcher.deferSeq(rsl.value.geneIds)),
      Field(
        "tagVariants",
        ListType(variant),
        Some(""),
        resolve = rsl => variantsFetcher.deferSeq(rsl.value.tagVariants)),
      Field(
        "indexVariants",
        ListType(variant),
        Some(""),
        resolve = rsl => variantsFetcher.deferSeq(rsl.value.indexVariants)),
      Field(
        "studies",
        ListType(study),
        Some(""),
        resolve = rsl => studiesFetcher.deferSeq(rsl.value.studies)),
      Field(
        "geneTagVariants",
        ListType(geneTagVariant),
        Some(""),
        resolve = _.value.geneTagVariants),
      Field(
        "tagVariantIndexVariantStudies",
        ListType(tagVariantIndexVariantStudy),
        Some(""),
        resolve = _.value.tagVariantIndexVariantStudies)))

  val overlap = ObjectType(
    "Overlap",
    "This element represent an overlap between two variants for two studies",
    fields[Backend, OverlappedVariant](
      Field("variantIdA", StringType, None, resolve = _.value.variantIdA),
      Field("variantIdB", StringType, None, resolve = _.value.variantIdB),
      Field("overlapAB", LongType, None, resolve = _.value.overlapAB),
      Field("distinctA", LongType, None, resolve = _.value.distinctA),
      Field("distinctB", LongType, None, resolve = _.value.distinctB)))

  val overlappedStudy = ObjectType(
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

  val overlappedVariantsStudies = ObjectType(
    "OverlappedVariantsStudies",
    "This element represent a overlap between two stduies",
    fields[Backend, OverlappedVariantsStudy](
      Field(
        "study",
        OptionType(study),
        Some("A study object"),
        resolve = rsl => studiesFetcher.deferOpt(rsl.value.studyId)),
      Field(
        "overlaps",
        ListType(overlap),
        Some(
          "Orig variant id which is been used for computing the " +
            "overlap with the referenced study"),
        resolve = _.value.overlaps)))

  val topOverlappedStudies = ObjectType(
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

  val studyForGene = ObjectType(
    "StudyForGene",
    "",
    fields[Backend, String](
      Field(
        "study",
        study,
        Some("A study object"),
        resolve = rsl => studiesFetcher.defer(rsl.value))))

  val overlappedInfoForStudy = ObjectType(
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

  val manhattan = ObjectType(
    "Manhattan",
    "This element represents a Manhattan like plot",
    fields[Backend, ManhattanTable](
      Field(
        "associations",
        ListType(manhattanAssociation),
        Some("A list of associations"),
        resolve = _.value.associations),
      Field(
        "topOverlappedStudies",
        OptionType(topOverlappedStudies),
        Some("A list of overlapped studies"),
        arguments = pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx
            .getTopOverlappedStudies(ctx.value.studyId, ctx.arg(pageIndex), ctx.arg(pageSize)))))

  // TODO that number should be updated with sumstats changes. not sure what to do at the moment
  val pheWAS = ObjectType(
    "PheWAS",
    "This element represents a PheWAS like plot",
    fields[Backend, Seq[SumStatsGWASRow]](
      Field(
        "totalGWASStudies",
        LongType,
        Some("A total number of unique GWAS studies in the summary stats table"),
        resolve = _ => 3618L),
      Field(
        "associations",
        ListType(pheWASAssociation),
        Some("A list of associations"),
        resolve = _.value)))

  val tagVariantsAndStudiesForIndexVariant = ObjectType(
    "TagVariantsAndStudiesForIndexVariant",
    "A list of rows with each link",
    fields[Backend, VariantToDiseaseTable](
      Field(
        "associations",
        ListType(indexVariantAssociation),
        Some(
          "A list of associations connected to a Index variant and a Study through some expansion methods"),
        resolve = _.value.associations)))

  val indexVariantsAndStudiesForTagVariant = ObjectType(
    "IndexVariantsAndStudiesForTagVariant",
    "A list of rows with each link",
    fields[Backend, VariantToDiseaseTable](
      Field(
        "associations",
        ListType(tagVariantAssociation),
        Some(
          "A list of associations connected to a Index variant and a Study through some expansion methods"),
        resolve = _.value.associations)))

  val tissue = ObjectType(
    "Tissue",
    "",
    fields[Backend, Tissue](
      Field("id", StringType, Some(""), resolve = _.value.id),
      Field(
        "name",
        StringType,
        Some(""),
        resolve = r => r.ctx.v2gBiofeatureLabels.getOrElse(r.value.id, r.value.name))))

  val g2vSchemaElement = ObjectType(
    "G2VSchemaElement",
    "A list of rows with each link",
    fields[Backend, G2VSchemaElement](
      Field("id", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("sourceLabel", OptionType(StringType), Some(""), resolve = _.value.displayLabel),
      Field(
        "sourceDescriptionOverview",
        OptionType(StringType),
        Some(""),
        resolve = _.value.overviewTooltip),
      Field(
        "sourceDescriptionBreakdown",
        OptionType(StringType),
        Some(""),
        resolve = _.value.tagSubtitle),
      Field("pmid", OptionType(StringType), Some("PubmedID"), resolve = _.value.pmid),
      Field("tissues", ListType(tissue), Some(""), resolve = _.value.tissues)))

  val v2gSchema = ObjectType(
    "G2VSchema",
    "A list of rows with each link",
    fields[Backend, G2VSchema](
      Field(
        "qtls",
        ListType(g2vSchemaElement),
        Some("qtl structure definition"),
        resolve = _.value.qtls),
      Field(
        "intervals",
        ListType(g2vSchemaElement),
        Some("qtl structure definition"),
        resolve = _.value.intervals),
      Field(
        "functionalPredictions",
        ListType(g2vSchemaElement),
        Some("qtl structure definition"),
        resolve = _.value.functionalPredictions),
      Field(
        "distances",
        ListType(g2vSchemaElement),
        Some("Distance structure definition"),
        resolve = _.value.distances)))

  val qtlTissue = ObjectType(
    "QTLTissue",
    "",
    fields[Backend, QTLTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field("quantile", FloatType, Some(""), resolve = _.value.quantile),
      Field("beta", OptionType(FloatType), Some(""), resolve = _.value.beta),
      Field("pval", OptionType(FloatType), Some(""), resolve = _.value.pval)))

  val intervalTissue = ObjectType(
    "IntervalTissue",
    "",
    fields[Backend, IntervalTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field("quantile", FloatType, Some(""), resolve = _.value.quantile),
      Field("score", OptionType(FloatType), Some(""), resolve = _.value.score)))

  val fpredTissue = ObjectType(
    "FPredTissue",
    "",
    fields[Backend, FPredTissue](
      Field("tissue", tissue, Some(""), resolve = _.value.tissue),
      Field("maxEffectLabel", OptionType(StringType), Some(""), resolve = _.value.maxEffectLabel),
      Field("maxEffectScore", OptionType(FloatType), Some(""), resolve = _.value.maxEffectScore)))

  val distanceTisse = ObjectType(
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

  val qtlElement = ObjectType(
    "QTLElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[QTLTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(qtlTissue), Some(""), resolve = _.value.tissues)))

  val intervalElement = ObjectType(
    "IntervalElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[IntervalTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(intervalTissue), Some(""), resolve = _.value.tissues)))

  val fPredElement = ObjectType(
    "FunctionalPredictionElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[FPredTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(fpredTissue), Some(""), resolve = _.value.tissues)))

  val distElement = ObjectType(
    "DistanceElement",
    "A list of rows with each link",
    fields[Backend, G2VElement[DistancelTissue]](
      Field("typeId", StringType, Some(""), resolve = _.value.id),
      Field("sourceId", StringType, Some(""), resolve = _.value.sourceId),
      Field("aggregatedScore", FloatType, Some(""), resolve = _.value.aggregatedScore),
      Field("tissues", ListType(distanceTisse), Some(""), resolve = _.value.tissues)))

  val geneForVariant = ObjectType(
    "GeneForVariant",
    "A list of rows with each link",
    fields[Backend, G2VAssociation](
      Field(
        "gene",
        gene,
        Some("Associated scored gene"),
        resolve = rsl => genesFetcher.defer(rsl.value.geneId)),
      Field("variant", StringType, Some("Associated scored variant"), resolve = _.value.variantId),
      Field("overallScore", FloatType, Some(""), resolve = _.value.overallScore),
      Field("qtls", ListType(qtlElement), Some(""), resolve = _.value.qtls),
      Field("intervals", ListType(intervalElement), Some(""), resolve = _.value.intervals),
      Field("functionalPredictions", ListType(fPredElement), Some(""), resolve = _.value.fpreds),
      Field("distances", ListType(distElement), Some(""), resolve = _.value.distances)))

  val variantSearchResult = ObjectType(
    "VariantSearchResult",
    "Variant search result object",
    fields[Backend, VariantSearchResult](
      Field("variant", variant, Some("A variant"), resolve = _.value.variant)))

  val searchResult = ObjectType(
    "SearchResult",
    "Search data by a query string",
    fields[Backend, SearchResultSet](
      Field(
        "totalGenes",
        LongType,
        Some("Total number of genes found"),
        resolve = _.value.totalGenes),
      Field(
        "totalVariants",
        LongType,
        Some("Total number of variants found"),
        resolve = _.value.totalVariants),
      Field(
        "totalStudies",
        LongType,
        Some("Total number of studies found"),
        resolve = _.value.totalStudies),
      Field("genes", ListType(gene), Some("Gene search result list"), resolve = _.value.genes),
      Field(
        "variants",
        ListType(variant),
        Some("Variant search result list"),
        resolve = _.value.variants),
      Field(
        "studies",
        ListType(study),
        Some("Study search result list"),
        resolve = _.value.studies)))

  val gwasSlimmedColocalisation = ObjectType(
    "GWASLRColocalisation",
    fields[Backend, ColocRow](
      Field(
        "leftVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = r => variantsFetcher.defer(r.value.lVariant.id)),
      Field(
        "leftStudy",
        study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.lStudy)),
      Field(
        "rightVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = r => variantsFetcher.defer(r.value.rVariant.id)),
      Field(
        "rightStudy",
        study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.rStudy)),
      Field("h3", FloatType, Some("H3"), resolve = _.value.hs.h3),
      Field("h4", FloatType, Some("H4"), resolve = _.value.hs.h4),
      Field("log2h4h3", FloatType, Some("Log2 H4/H3"), resolve = _.value.hs.log2h4h3)))

  val gwasColocalisationForQTLWithGene = ObjectType(
    "GWASColocalisationForQTLWithGene",
    fields[Backend, ColocRow](
      Field(
        "leftVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = r => variantsFetcher.defer(r.value.lVariant.id)),
      Field(
        "study",
        study,
        Some("GWAS Study"),
        resolve = rsl => studiesFetcher.defer(rsl.value.lStudy)),
      Field("qtlStudyId", StringType, Some("QTL study ID"), resolve = _.value.rStudy),
      Field("phenotypeId", StringType, Some("Phenotype ID"), resolve = _.value.rPhenotype.get),
      Field(
        "tissue",
        tissue,
        Some("QTL bio-feature"),
        resolve = r => Tissue(r.value.rBioFeature.get)),
      Field("h3", FloatType, Some("H3"), resolve = _.value.hs.h3),
      Field("h4", FloatType, Some("H4"), resolve = _.value.hs.h4),
      Field("log2h4h3", FloatType, Some("Log2 H4/H3"), resolve = _.value.hs.log2h4h3)))

  val gwasColocalisation = ObjectType(
    "GWASColocalisation",
    fields[Backend, ColocRow](
      Field(
        "indexVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = r => variantsFetcher.defer(r.value.rVariant.id)),
      Field(
        "study",
        study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.rStudy)),
      Field("beta", OptionType(FloatType), Some("Beta"), resolve = _.value.hs.lVariantRStudyBeta),
      Field("h3", FloatType, Some("H3"), resolve = _.value.hs.h3),
      Field("h4", FloatType, Some("H4"), resolve = _.value.hs.h4),
      Field("log2h4h3", FloatType, Some("Log2 H4/H3"), resolve = _.value.hs.log2h4h3)))

  val qtlColocalisation = ObjectType(
    "QTLColocalisation",
    fields[Backend, ColocRow](
      Field(
        "indexVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = r => variantsFetcher.defer(r.value.rVariant.id)),
      Field("gene", gene, Some("Gene"), resolve = rsl => genesFetcher.defer(rsl.value.rGeneId.get)),
      Field(
        "phenotypeId",
        StringType,
        Some("QTL Phenotype ID"),
        resolve = r => r.value.rPhenotype.get),
      Field(
        "tissue",
        tissue,
        Some("QTL bio-feature"),
        resolve = r => Tissue(r.value.rBioFeature.get)),
      Field("qtlStudyName", StringType, Some("QTL study ID"), resolve = r => r.value.rStudy),
      Field("beta", OptionType(FloatType), Some("Beta"), resolve = _.value.hs.lVariantRStudyBeta),
      Field("h3", FloatType, Some("H3"), resolve = _.value.hs.h3),
      Field("h4", FloatType, Some("H4"), resolve = _.value.hs.h4),
      Field("log2h4h3", FloatType, Some("Log2 H4/H3"), resolve = _.value.hs.log2h4h3)))

  implicit val slgRowImp = deriveObjectType[Backend, SLGRow](
    AddFields(
      Field(
        "gene",
        gene,
        description = Some("Gene"),
        resolve = ctx => genesFetcher.defer(ctx.value.geneId))),
    ExcludeFields("geneId"))

  implicit val slgTableImp = deriveObjectType[Backend, SLGTable](
    AddFields(
      Field(
        "study",
        OptionType(study),
        description = Some("Study"),
        resolve = ctx => studiesFetcher.deferOpt(ctx.value.studyId)),
      Field(
        "variant",
        OptionType(variant),
        description = Some("Variant"),
        resolve = ctx => variantsFetcher.deferOpt(ctx.value.variantId))),
    ExcludeFields("studyId", "variantId"))

  implicit val V2DOddsImp = deriveObjectType[Backend, V2DOdds]()
  implicit val V2DBetaImp = deriveObjectType[Backend, V2DBeta]()

  implicit val V2DL2GRowByGeneImp = deriveObjectType[Backend, V2DL2GRowByGene](
    AddFields(
      Field(
        "study",
        study,
        description = Some("Study"),
        resolve = ctx => studiesFetcher.defer(ctx.value.studyId)),
      Field(
        "variant",
        variant,
        description = Some("Variant"),
        resolve = ctx => variantsFetcher.defer(ctx.value.variantId))),
    ExcludeFields("studyId", "variantId"))

  val query = ObjectType(
    "Query",
    fields[Backend, Unit](
      Field(
        "search",
        searchResult,
        arguments = queryString :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.getSearchResultSet(ctx.arg(queryString), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field(
        "genes",
        ListType(gene),
        arguments = chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve = ctx =>
          ctx.ctx.getGenesByRegion(ctx.arg(chromosome), ctx.arg(dnaPosStart), ctx.arg(dnaPosEnd))),
      Field(
        "geneInfo",
        OptionType(gene),
        arguments = geneId :: Nil,
        resolve = ctx => genesFetcher.deferOpt(ctx.arg(geneId))),
      Field(
        "studyInfo",
        OptionType(study),
        arguments = studyId :: Nil,
        resolve = ctx => studiesFetcher.deferOpt(ctx.arg(studyId))),
      Field(
        "variantInfo",
        OptionType(variant),
        arguments = variantId :: Nil,
        resolve = ctx => variantsFetcher.deferOpt(ctx.arg(variantId))),
      Field(
        "studiesForGene",
        ListType(studyForGene),
        arguments = geneId :: Nil,
        resolve = ctx => ctx.ctx.getStudiesForGene(ctx.arg(geneId))),
      Field(
        "studyLocus2GeneTable",
        slgTableImp,
        arguments = studyId :: variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.buildSLGTable(
            ctx.arg(studyId),
            ctx.arg(variantId),
            ctx.arg(pageIndex),
            ctx.arg(pageSize))),
      Field(
        "manhattan",
        manhattan,
        arguments = studyId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.buildManhattanTable(ctx.arg(studyId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field(
        "topOverlappedStudies",
        topOverlappedStudies,
        arguments = studyId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.getTopOverlappedStudies(ctx.arg(studyId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field(
        "overlapInfoForStudy",
        overlappedInfoForStudy,
        arguments = studyId :: studyIds :: Nil,
        resolve = ctx => (ctx.arg(studyId), ctx.arg(studyIds))),
      Field(
        "tagVariantsAndStudiesForIndexVariant",
        tagVariantsAndStudiesForIndexVariant,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx
            .buildIndexVariantAssocTable(
              ctx.arg(variantId),
              ctx.arg(pageIndex),
              ctx.arg(pageSize))),
      Field(
        "indexVariantsAndStudiesForTagVariant",
        indexVariantsAndStudiesForTagVariant,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx
            .buildTagVariantAssocTable(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field(
        "pheWAS",
        pheWAS,
        arguments = variantId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.buildPhewFromSumstats(ctx.arg(variantId), ctx.arg(pageIndex), ctx.arg(pageSize))),
      Field(
        "gecko",
        OptionType(gecko),
        arguments = chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve =
          ctx => ctx.ctx.buildGecko(ctx.arg(chromosome), ctx.arg(dnaPosStart), ctx.arg(dnaPosEnd))),
      Field(
        "genesForVariantSchema",
        v2gSchema,
        arguments = Nil,
        resolve = ctx => ctx.ctx.getG2VSchema),
      Field(
        "genesForVariant",
        ListType(geneForVariant),
        arguments = variantId :: Nil,
        resolve = ctx => ctx.ctx.buildG2VByVariant(ctx.arg(variantId))),
      Field(
        "gwasRegional",
        ListType(regionalAssociation),
        arguments = studyId :: chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve = ctx =>
          ctx.ctx.gwasRegionalFromSumstats(
            ctx.arg(studyId),
            ctx.arg(chromosome),
            ctx.arg(dnaPosStart),
            ctx.arg(dnaPosEnd))),
      Field(
        "qtlRegional",
        ListType(regionalAssociation),
        arguments =
          studyId :: bioFeature :: phenotypeId :: chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve = ctx =>
          ctx.ctx.qtlRegionalFromSumstats(
            ctx.arg(studyId),
            ctx.arg(bioFeature),
            ctx.arg(phenotypeId),
            ctx.arg(chromosome),
            ctx.arg(dnaPosStart),
            ctx.arg(dnaPosEnd))),
      // getStudyAndLeadVariantInfo
      Field(
        "studyAndLeadVariantInfo",
        OptionType(studiesAndLeadVariantsForGene),
        arguments = studyId :: variantId :: Nil,
        resolve = ctx => ctx.ctx.getStudyAndLeadVariantInfo(ctx.arg(studyId), ctx.arg(variantId))),
      Field(
        "gwasCredibleSet",
        ListType(credSetTagElement),
        arguments = studyId :: variantId :: Nil,
        resolve = ctx => ctx.ctx.gwasCredibleSet(ctx.arg(studyId), ctx.arg(variantId))),
      Field(
        "qtlCredibleSet",
        ListType(credSetTagElement),
        arguments = studyId :: variantId :: phenotypeId :: bioFeature :: Nil,
        resolve = ctx =>
          ctx.ctx.qtlCredibleSet(
            ctx.arg(studyId),
            ctx.arg(variantId),
            ctx.arg(phenotypeId),
            ctx.arg(bioFeature))),
      Field(
        "colocalisationsForGene",
        ListType(gwasColocalisationForQTLWithGene),
        arguments = geneId :: Nil,
        resolve = ctx => ctx.ctx.colocalisationsForGene(ctx.arg(geneId))),
      Field(
        "gwasColocalisationForRegion",
        ListType(gwasSlimmedColocalisation),
        arguments = chromosome :: dnaPosStart :: dnaPosEnd :: Nil,
        resolve = ctx =>
          ctx.ctx.gwasColocalisationForRegion(
            ctx.arg(chromosome),
            ctx.arg(dnaPosStart),
            ctx.arg(dnaPosEnd))),
      Field(
        "gwasColocalisation",
        ListType(gwasColocalisation),
        arguments = studyId :: variantId :: Nil,
        resolve = ctx => ctx.ctx.gwasColocalisation(ctx.arg(studyId), ctx.arg(variantId))),
      Field(
        "qtlColocalisation",
        ListType(qtlColocalisation),
        arguments = studyId :: variantId :: Nil,
        resolve = ctx => ctx.ctx.qtlColocalisation(ctx.arg(studyId), ctx.arg(variantId))),
      // getStudiesAndLeadVariantsForGeneByL2G
      Field(
        "studiesAndLeadVariantsForGene",
        ListType(studiesAndLeadVariantsForGene),
        arguments = geneId :: Nil,
        resolve = ctx => ctx.ctx.getStudiesAndLeadVariantsForGene(ctx.arg(geneId))),
      Field(
        "studiesAndLeadVariantsForGeneByL2G",
        ListType(V2DL2GRowByGeneImp),
        arguments = geneId :: pageIndex :: pageSize :: Nil,
        resolve = ctx =>
          ctx.ctx.getStudiesAndLeadVariantsForGeneByL2G(
            ctx.arg(geneId),
            ctx.arg(pageIndex),
            ctx.arg(pageSize)))))

  val schema = Schema(query)
}
