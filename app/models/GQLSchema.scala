package models


import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema._

import scala.concurrent.Future

//  val Query = ObjectType(
//    "Query", fields[CharacterRepo, Unit](
//      Field("hero", Character,
//        arguments = EpisodeArg :: Nil,
//        deprecationReason = Some("Use `human` or `droid` fields instead"),
//        resolve = (ctx) ⇒ ctx.ctx.getHero(ctx.arg(EpisodeArg))),
//      Field("human", OptionType(Human),
//        arguments = ID :: Nil,
//        resolve = ctx ⇒ ctx.ctx.getHuman(ctx arg ID)),
//      Field("droid", Droid,
//        arguments = ID :: Nil,
//        resolve = Projector((ctx, f) ⇒ ctx.ctx.getDroid(ctx arg ID).get))
//))

object GQLSchema {
  val query = ObjectType(
    "Query", fields[Backend, Unit](
      Field("manhattan", )
    ))

  val schema = Schema(query)
}
