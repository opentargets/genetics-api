package models.gql

import models.Backend
import models.entities.DNA.Gene
import sangria.execution.deferred.{Fetcher, FetcherConfig, HasId}
import sangria.macros.derive.{RenameField, deriveObjectType}
import sangria.schema.{Field, FloatType, ObjectType, fields}

trait GQLGene {
  implicit val geneHasId: HasId[Gene, String] = HasId[Gene, String](_.id)

  val genesFetcher: Fetcher[Backend, Gene, Gene, String] = Fetcher(
    config = FetcherConfig.maxBatchSize(100),
    fetch = (ctx: Backend, geneIds: Seq[String]) => {
      ctx.getGenes(geneIds)
    })

  implicit val gene: ObjectType[Backend, Gene] = deriveObjectType[Backend, Gene](
    RenameField("fwd", "fwdStrand")
  )

  val scoredGene: ObjectType[Backend, (String, Double)] = ObjectType(
    "ScoredGene",
    "This object link a Gene with a score",
    fields[Backend, (String, Double)](
      Field("gene", gene, Some("Gene Info"), resolve = rsl => genesFetcher.defer(rsl.value._1)),
      Field(
        "score",
        FloatType,
        Some("Score a Float number between [0. .. 1.]"),
        resolve = _.value._2)))

}
