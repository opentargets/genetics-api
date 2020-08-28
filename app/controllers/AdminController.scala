package controllers

import play.api.libs.json.Json.toJson
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import configuration.{Metadata, MetadataConfiguration}

import scala.concurrent.ExecutionContext

@Singleton
class AdminController @Inject()(implicit
                                ec: ExecutionContext,
                                cc: ControllerComponents,
                                configuration: Configuration,
                                environment: Environment)
  extends AbstractController(cc) {

  import MetadataConfiguration._

  def health: Action[AnyContent] =
    Action {
      Ok
    }

  def metadata: Action[AnyContent] =
    Action {
      Ok(toJson(configuration.get[Metadata]("ot.meta")))
    }

}
