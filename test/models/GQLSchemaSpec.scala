package models

import models.gql.GQLSchema
import org.scalatest.flatspec.AnyFlatSpecLike

/**
 * The publically exposed fields for each GQL object where the object isn't publicly exposed it is
 * entirety. In that case there is no need to test as the Sangria `derivedObjectType` can be used
 * without further configuration.
 */
trait GQLFieldSpecification {
  val scoredGeneFields = List("gene", "score")
  val manhattanAssociationFields = List(
    "pvalMantissa",
    "bestGenes",
    "bestColocGenes",
    "oddsRatio",
    "direction",
    "variant",
    "bestLocus2Genes",
    "betaCILower",
    "pval",
    "totalSetSize",
    "ldSetSize",
    "oddsRatioCILower",
    "beta",
    "pvalExponent",
    "betaCIUpper",
    "credibleSetSize",
    "oddsRatioCIUpper"
  )
  val manhattanTableFields = List("topOverlappedStudies", "associations")
  val tissueFields = List("id", "name")
  val qtlTissueFields = List("tissue", "quantile", "beta", "pval")
  val intervalTissueFields = List("tissue", "quantile", "score")
  val fpredTissueFields = List("tissue", "maxEffectLabel", "maxEffectScore")
  val distanceTissueFields = List("tissue", "distance", "score", "quantile")
  val qtlElement = List("tissue", "distance", "score", "quantile")
  val intervalElement = List("tissue", "distance", "score", "quantile")
  val fpredElement = List("tissue", "distance", "score", "quantile")
  val overlappedStudyFields = List("study", "numOverlapLoci")
  val overlappedVariantsStudiesFields = List("study")
  val topOverlappedStudiesFields = List("study", "topStudiesByLociOverlap")
  val overlappedInfoForStudyFields = List("study", "overlappedVariantsForStudies", "variantIntersectionSet")
  val studyFields = List(
    "studyId",
    "traitReported",
    "traitEfos",
    "pmid",
    "pubDate",
    "pubJournal",
    "pubTitle",
    "pubAuthor",
    "hasSumstats",
    "ancestryInitial",
    "ancestryReplication",
    "nInitial",
    "nReplication",
    "nCases",
    "traitCategory",
    "numAssocLoci",
    "nTotal"
  )
}

class GqlEntities extends AnyFlatSpecLike {

  new GQLFieldSpecification {

    "The GQLGene entities" should "have the expected fields" in {
      val scoredgene: Set[String] = GQLSchema.scoredGene.fieldsByName.keySet
      assert(validate(scoredGeneFields, scoredgene), errorString("scored gene"))
    }

    "The GQLManhattan entities" should "have the expected fields" in {
      val manhattanAssociation = GQLSchema.manhattanAssociation.fieldsByName.keySet
      val manhattanTable = GQLSchema.manhattan.fieldsByName.keySet
      assert(validate(manhattanAssociationFields, manhattanAssociation), errorString("manhattan association"))
      assert(validate(manhattanTableFields, manhattanTable), errorString("manhattan table"))
    }

    "The GQLOverlaps entities" should "have the expected fields" in {
      val overlappedStudy = GQLSchema.overlappedStudy.fieldsByName.keySet
      val overlappedVariantsStudy = GQLSchema.overlappedStudy.fieldsByName.keySet
      val topOverlappedStudies = GQLSchema.topOverlappedStudies.fieldsByName.keySet
      val overlappedInfoForStudy = GQLSchema.overlappedInfoForStudy.fieldsByName.keySet
      assert(validate(overlappedStudyFields, overlappedStudy), errorString(" overlapped study"))
      assert(validate(overlappedVariantsStudiesFields, overlappedVariantsStudy), errorString(" overlapped variants study"))
      assert(validate(topOverlappedStudiesFields, topOverlappedStudies), errorString("top overlapped studies"))
      assert(validate(overlappedInfoForStudyFields, overlappedInfoForStudy), errorString(" overlapped info for study"))
    }

    "The GQLStudy entities" should "have the expected fields" in {
      val studyForGeneFields = List("study")
      val study = GQLSchema.study.fieldsByName.keySet
      val studyForGene = GQLSchema.studyForGene.fieldsByName.keySet
      assert(validate(studyFields, study), errorString("study"))
      assert(validate(studyForGeneFields, studyForGene), errorString("study for gene"))
    }

    "The GQLTissue entities" should "have the expected fields" in {
      val tissue = GQLSchema.tissue.fieldsByName.keySet
      val qtlTissue = GQLSchema.qtlTissue.fieldsByName.keySet
      val intervalTissue = GQLSchema.intervalTissue.fieldsByName.keySet
      val fpredTissue = GQLSchema.fpredTissue.fieldsByName.keySet
      val distanceTissue = GQLSchema.distanceTissue.fieldsByName.keySet
      assert(validate(tissueFields, tissue), errorString("tissue"))
      assert(validate(qtlTissueFields, qtlTissue), errorString("qtl tissue"))
      assert(validate(intervalTissueFields, intervalTissue), errorString("interval tissue"))
      assert(validate(fpredTissueFields, fpredTissue), errorString("fpred tissue"))
      assert(validate(distanceTissueFields, distanceTissue), errorString("distance tissue"))
    }

    //    "The GQLVariant entities" should "have the expected fields" in {
    //      ???
    //    }

    private def validate(expected: List[String], actual: Set[String]): Boolean =
      expected.forall(actual.contains)

    private def errorString(name: String): String = s"Not all expected $name fields present"
  }
}

class GQLSchemaSpec extends AnyFlatSpecLike {

  val elements = List(
    "CredSetTagElement",
    "DistanceElement",
    "DistanceTissue",
    "FPredTissue",
    "FunctionalPredictionElement",
    "G2VSchema",
    "G2VSchemaElement",
    "GWASColocalisation",
    "GWASColocalisationForQTLWithGene",
    "GWASLRColocalisation",
    "Gecko",
    "Gene",
    "GeneForVariant",
    "GeneTagVariant",
    "IndexVariantAssociation",
    "IndexVariantsAndStudiesForTagVariant",
    "IntervalElement",
    "IntervalTissue",
    "Manhattan",
    "ManhattanAssociation",
    "Overlap",
    "OverlappedInfoForStudy",
    "OverlappedStudy",
    "OverlappedVariantsStudies",
    "PheWAS",
    "QTLColocalisation",
    "QTLElement",
    "QTLTissue",
    "Query",
    "RegionalAssociation",
    "SLGRow",
    "SLGTable",
    "ScoredGene",
    "SearchResult",
    "StudiesAndLeadVariantsForGene",
    "Study",
    "StudyForGene",
    "PheWASAssociation",
    "TagVariantAssociation",
    "TagVariantIndexVariantStudy",
    "TagVariantsAndStudiesForIndexVariant",
    "Tissue",
    "TopOverlappedStudies",
    "V2DBeta",
    "V2DL2GRowByGene",
    "V2DOdds",
    "Variant")

  "The GraphQL schema" should "contain all of the expected fields" in {
    val schema = GQLSchema.schema
    assert(elements.forall(e => schema.availableTypeNames.contains(e)), "Not all expected types present in schema")
  }

}
