package controllers

import javax.inject.{Inject, Singleton}
import models.gql.GQLSchema
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import sangria.renderer.SchemaRenderer

import scala.concurrent.ExecutionContext

@Singleton
class GQLSchemaController @Inject() (implicit ec: ExecutionContext, cc: ControllerComponents)
    extends AbstractController(cc) {

  def renderSchema: Action[AnyContent] =
    Action {
      Ok(SchemaRenderer.renderSchema(GQLSchema.schema))
    }

  def renderClient: Action[AnyContent] =
    Action {
      Ok(views.html.graphiql(None))
    }
}
