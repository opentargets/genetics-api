package controllers


import scala.util.{Failure, Success, Try}
import javax.inject._
import models.Backend
import models._
import models.Entities._
import play.api._
import play.api.mvc._
import sangria.ast._
import sangria.execution.ExceptionHandler
import sangria.renderer._

import models.Entities._

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(implicit ec: ExecutionContext, v2g: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  // example from here https://github.com/nemoo/play-slick3-example/blob/master/app/controllers/Application.scala
  def index() = Action.async { implicit request: Request[AnyContent] =>
    v2g.findAt(Position("1", 1000000), 0, 100).map {
      case Success(f) => Ok(views.html.index(f))
      case Failure(ex) => InternalServerError(ex.toString)
    }
  }

  def interval(chr: String, position: Long, from: Long, amount: Long) = {
    Action.async { implicit request: Request[AnyContent] =>
      v2g.findAt(Position(chr, position), from, amount).map {
        case Success(f) => Ok(views.html.index(f))
        case Failure(ex) => InternalServerError(ex.toString)
      }
    }
  }

//  lazy val exceptionHandler = ExceptionHandler {
//    case (_, error @ TooComplexQueryError) ⇒ HandledException(error.getMessage)
//    case (_, error @ MaxQueryDepthReachedError(_)) ⇒ HandledException(error.getMessage)
//  }

  case object TooComplexQueryError extends Exception("Query is too expensive.")

  def healthcheck() = Action { request =>
    Ok("working")
  }
}
