package controllers

import javax.inject._

import scala.concurrent._
import models.{Backend, GQLSchema}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import sangria.ast.SchemaDefinition
import sangria.execution.deferred.DeferredResolver
import sangria.execution._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.marshalling.playJson._

import scala.util.{Failure, Success}

@Singleton
class GraphQLController @Inject()(implicit ec: ExecutionContext, dbTables: Backend, cc: ControllerComponents)
  extends AbstractController(cc) {

  def options = Action {
    NoContent
//    NoContent.withHeaders(
//      "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS",
//      "Access-Control-Allow-Origin" -> "*",
//      "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With",
//      "Access-Control-Max-Age" -> (60 * 60 * 24).toString
//    )
  }

  def gql(query: String, variables: Option[String], operation: Option[String]) = Action.async {
    executeQuery(query, variables map parseVariables, operation)
  }

  def gqlBody() = Action.async(parse.json) { request =>
    val query = (request.body \ "query").as[String]
    val operation = (request.body \ "operationName").asOpt[String]

    val variables = (request.body \ "variables").toOption.flatMap {
      case JsString(vars) => Some(parseVariables(vars))
      case obj: JsObject => Some(obj)
      case _ => None
    }

    executeQuery(query, variables, operation)
  }

  private def parseVariables(variables: String) =
    if (variables.trim == "" || variables.trim == "null") Json.obj() else Json.parse(variables).as[JsObject]

  private def executeQuery(query: String, variables: Option[JsObject], operation: Option[String]) =
    QueryParser.parse(query) match {

      // query parsed successfully, time to execute it!
      case Success(queryAst) =>
        Executor.execute(GQLSchema.schema, queryAst, dbTables,
          operationName = operation,
          variables = variables getOrElse Json.obj(),
          // deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters),
          exceptionHandler = exceptionHandler,
          queryReducers = List(
            QueryReducer.rejectMaxDepth[Backend](15),
            QueryReducer.rejectComplexQueries[Backend](4000, (_, _) => TooComplexQueryError)))
          .map(Ok(_))
          .recover {
            case error: QueryAnalysisError => BadRequest(error.resolveError)
            case error: ErrorWithResolver => InternalServerError(error.resolveError)
          }

      // can't parse GraphQL query, return error
      case Failure(error: SyntaxError) =>
        Future.successful(BadRequest(Json.obj(
          "syntaxError" -> error.getMessage,
          "locations" -> Json.arr(Json.obj(
            "line" -> error.originalError.position.line,
            "column" -> error.originalError.position.column)))))

      case Failure(error) =>
        throw error
    }

  lazy val exceptionHandler = ExceptionHandler {
    case (_, error @ TooComplexQueryError) => HandledException(error.getMessage)
    case (_, error @ MaxQueryDepthReachedError(_)) => HandledException(error.getMessage)
  }

  case object TooComplexQueryError extends Exception("Query is too expensive.")
}