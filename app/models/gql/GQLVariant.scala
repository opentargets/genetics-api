package models.gql

import models.Backend
import models.Functions.toSafeDouble
import GQLSchema.{studiesFetcher, study}
import models.entities.DNA.Variant
import models.entities.Entities.{GeneTagVariant, LeadRow, TagVariantIndexVariantStudy, V2DRow}
import sangria.execution.deferred.{Fetcher, FetcherConfig, HasId}
import sangria.macros.derive.{AddFields, DocumentField, ExcludeFields, deriveObjectType}
import sangria.schema.{Field, FloatType, LongType, ObjectType, OptionType, StringType, fields}

trait GQLVariant {
  self: GQLGene =>

  implicit val variantHasId: HasId[Variant, String] = HasId[Variant, String](_.id)

  val variantsFetcher: Fetcher[Backend, Variant, Variant, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(1000),
    fetch = (ctx: Backend, variantIds: Seq[String]) => {
      ctx.getVariants(variantIds)
    })

  val variant: ObjectType[Backend, Variant] = deriveObjectType[Backend, Variant](
    ExcludeFields("caddAnnotation", "gnomadAnnotation", "annotation"),
    DocumentField("rsId", "Approved symbol name of a gene"),
    DocumentField("chromosome", "Ensembl Gene ID of a gene"),
    DocumentField("position", "Approved symbol name of a gene"),
    DocumentField("chromosomeB37", "chrom ID GRCH37"),
    DocumentField("positionB37", "Approved symbol name of a gene"),
    AddFields(
      Field("id", StringType, Some("Variant ID"), resolve = _.value.id),
      Field(
        "nearestGene",
        OptionType(gene),
        Some("Nearest gene"),
        resolve = el => genesFetcher.deferOpt(el.value.annotation.nearestGeneId)),
      Field(
        "nearestGeneDistance",
        OptionType(LongType),
        Some("Distance to the nearest gene (any biotype)"),
        resolve = _.value.annotation.nearestGeneDistance),
      Field(
        "nearestCodingGene",
        OptionType(gene),
        Some("Nearest protein-coding gene"),
        resolve = el => genesFetcher.deferOpt(el.value.annotation.nearestCodingGeneId)),
      Field(
        "nearestCodingGeneDistance",
        OptionType(LongType),
        Some("Distance to the nearest gene (protein-coding biotype)"),
        resolve = _.value.annotation.nearestCodingGeneDistance),
      Field(
        "mostSevereConsequence",
        OptionType(StringType),
        Some("Most severe consequence"),
        resolve = _.value.annotation.mostSevereConsequence),
      Field(
        "caddRaw",
        OptionType(FloatType),
        Some("Combined Annotation Dependent Depletion - Raw score"),
        resolve = _.value.caddAnnotation.raw),
      Field(
        "caddPhred",
        OptionType(FloatType),
        Some("Combined Annotation Dependent Depletion - Scaled score"),
        resolve = _.value.caddAnnotation.phred),
      Field(
        "gnomadAFR",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (African/African-American population)"),
        resolve = _.value.gnomadAnnotation.afr),
      Field(
        "gnomadAMR",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Latino/Admixed American population)"),
        resolve = _.value.gnomadAnnotation.amr),
      Field(
        "gnomadASJ",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Ashkenazi Jewish population)"),
        resolve = _.value.gnomadAnnotation.asj),
      Field(
        "gnomadEAS",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (East Asian population)"),
        resolve = _.value.gnomadAnnotation.eas),
      Field(
        "gnomadFIN",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Finnish population)"),
        resolve = _.value.gnomadAnnotation.fin),
      Field(
        "gnomadNFE",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish European population)"),
        resolve = _.value.gnomadAnnotation.nfe),
      Field(
        "gnomadNFEEST",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish Eurpoean Estonian sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeEST),
      Field(
        "gnomadNFENWE",
        OptionType(FloatType),
        Some(
          "gnomAD Allele frequency (Non-Finnish Eurpoean North-Western European sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeNWE),
      Field(
        "gnomadNFESEU",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Non-Finnish Eurpoean Southern European sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeSEU),
      Field(
        "gnomadNFEONF",
        OptionType(FloatType),
        Some(
          "gnomAD Allele frequency (Non-Finnish Eurpoean Other non-Finnish European sub-population)"),
        resolve = _.value.gnomadAnnotation.nfeONF),
      Field(
        "gnomadOTH",
        OptionType(FloatType),
        Some("gnomAD Allele frequency (Other (population not assigned) population)"),
        resolve = _.value.gnomadAnnotation.oth)))

}

trait GQLIndexVariantAssociation {
  self: GQLVariant =>

  val indexVariantAssociation: ObjectType[Backend, V2DRow] = ObjectType(
    "IndexVariantAssociation",
    "This object represent a link between a triple (study, trait, index_variant) and a tag variant " +
      "via an expansion method (either ldExpansion or FineMapping)",
    fields[Backend, V2DRow](
      Field(
        "tagVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = rsl => variantsFetcher.defer(rsl.value.tag.id)),
      Field(
        "study",
        study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.study.studyId)),
      Field(
        "pval",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve =
          r => toSafeDouble(r.value.association.pvalMantissa, r.value.association.pvalExponent)
      ), // TODO TEMPORAL HACK
      Field(
        "pvalMantissa",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalMantissa),
      Field(
        "pvalExponent",
        LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalExponent),
      Field(
        "nTotal",
        LongType,
        Some("n total cases (n initial + n replication)"),
        resolve =
          r => r.value.study.nInitial.getOrElse(0L) + r.value.study.nReplication.getOrElse(0L)),
      Field("nCases", LongType, Some("n cases"), resolve = _.value.study.nCases.getOrElse(0L)),
      Field("overallR2", OptionType(FloatType), Some("study ID"), resolve = _.value.association.r2),
      Field(
        "afr1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.afr1000GProp),
      Field(
        "amr1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.amr1000GProp),
      Field(
        "eas1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eas1000GProp),
      Field(
        "eur1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eur1000GProp),
      Field(
        "sas1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.sas1000GProp),
      Field("log10Abf", OptionType(FloatType), Some(""), resolve = _.value.association.log10Abf),
      Field(
        "posteriorProbability",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.posteriorProbability),
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

}

trait GQLStudyLeadVariantAssociation {
  self: GQLVariant =>

  val studiesAndLeadVariantsForGene: ObjectType[Backend, LeadRow] = ObjectType(
    "StudiesAndLeadVariantsForGene",
    "A list of Studies and Lead Variants for a Gene",
    fields[Backend, LeadRow](
      Field(
        "indexVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = rsl => variantsFetcher.defer(rsl.value.leadVariant.id)),
      Field(
        "study",
        study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.studyId)),
      Field(
        "pval",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = r => toSafeDouble(r.value.pvalMantissa, r.value.pvalExponent)
      ), // TODO TEMPORAL HACK
      Field(
        "pvalMantissa",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.pvalMantissa),
      Field(
        "pvalExponent",
        LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.pvalExponent),
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

}

trait GQLTagVariantAssociation {
  self: GQLVariant =>

  val tagVariantAssociation: ObjectType[Backend, V2DRow] = ObjectType(
    "TagVariantAssociation",
    "This object represent a link between a triple (study, trait, index_variant) and a tag variant " +
      "via an expansion method (either ldExpansion or FineMapping)",
    fields[Backend, V2DRow](
      Field(
        "indexVariant",
        variant,
        Some("Tag variant ID as ex. 1_12345_A_T"),
        resolve = rsl => variantsFetcher.defer(rsl.value.lead.id)),
      Field(
        "study",
        study,
        Some("study ID"),
        resolve = rsl => studiesFetcher.defer(rsl.value.study.studyId)),
      Field(
        "pval",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve =
          r => toSafeDouble(r.value.association.pvalMantissa, r.value.association.pvalExponent)
      ), // TODO TEMPORAL HACK
      Field(
        "pvalMantissa",
        FloatType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalMantissa),
      Field(
        "pvalExponent",
        LongType,
        Some("p-val between a study and an the provided index variant"),
        resolve = _.value.association.pvalExponent),
      Field(
        "nTotal",
        LongType,
        Some("n total cases (n initial + n replication)"),
        resolve =
          r => r.value.study.nInitial.getOrElse(0L) + r.value.study.nReplication.getOrElse(0L)),
      Field("nCases", LongType, Some("n cases"), resolve = _.value.study.nCases.getOrElse(0L)),
      Field("overallR2", OptionType(FloatType), Some("study ID"), resolve = _.value.association.r2),
      Field(
        "afr1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.afr1000GProp),
      Field(
        "amr1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.amr1000GProp),
      Field(
        "eas1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eas1000GProp),
      Field(
        "eur1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.eur1000GProp),
      Field(
        "sas1000GProp",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.sas1000GProp),
      Field("log10Abf", OptionType(FloatType), Some(""), resolve = _.value.association.log10Abf),
      Field(
        "posteriorProbability",
        OptionType(FloatType),
        Some(""),
        resolve = _.value.association.posteriorProbability),
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

}

trait GQLTagVariantIndexVariantStudy {

  val geneTagVariant: ObjectType[Backend, GeneTagVariant] = deriveObjectType[Backend, GeneTagVariant]()

  val tagVariantIndexVariantStudy: ObjectType[Backend, TagVariantIndexVariantStudy] = ObjectType(
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

}
