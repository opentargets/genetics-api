package models.gql

import GQLSchema.{studiesFetcher, study}
import components.Backend
import models.entities.Entities.Study
import sangria.execution.deferred.{Fetcher, FetcherConfig, HasId}
import sangria.macros.derive.{AddFields, DocumentField, RenameField, deriveObjectType}
import sangria.schema.{Field, LongType, ObjectType, fields}

trait GQLStudy {
  implicit val studyHasId: HasId[Study, String] = HasId[Study, String](_.studyId)

  val studiesFetcher: Fetcher[Backend, Study, Study, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(100),
    fetch = (ctx: Backend, stids: Seq[String]) => {
      ctx.getStudies(stids)
    })

  val study: ObjectType[Backend, Study] = deriveObjectType[Backend, Study](
    DocumentField("traitReported", "Trait Label as reported on the publication"),
    DocumentField("source", "Database or BioBank providing the study"),
    DocumentField("traitEfos", "A list of curated efo codes"),
    DocumentField("pubId", "PubMed ID for the corresponding publication"),
    RenameField("pubId", "pmid"),
    DocumentField("pubDate", "Publication Date as YYYY-MM-DD"),
    DocumentField("pubJournal", "Publication Journal name"),
    DocumentField("hasSumstats", "Contains summary statistical information"),
    AddFields(
      Field(
        "nTotal",
        LongType,
        Some("n total cases (n initial + n replication)"),
        resolve = r => r.value.nInitial.getOrElse(0L) + r.value.nReplication.getOrElse(0L))))

  val studyForGene: ObjectType[Backend, String] = ObjectType(
    "StudyForGene",
    "",
    fields[Backend, String](
      Field(
        "study",
        study,
        Some("A study object"),
        resolve = rsl => studiesFetcher.defer(rsl.value))))
}
