package models

import better.files._
import models.entities.Violations.{ChromosomeViolation, InChromosomeRegionViolation}
import play.api.libs.json._

object Functions {
  val defaultPaginationSize: Option[Int] = Some(500000)
  val defaultPaginationSizeES: Option[Int] = Some(10)
  val defaultChromosomes: Seq[String] = (1 to 22).map(_.toString) ++ Seq("X", "Y", "MT")
  val defaultMaxRegionSize: Long = 2000000L
  val defaultMaxDistantFromTSS: Long = 500000L
  val defaultQtlTypes: List[String] = List("eqtl", "pqtl")
  val defaultIntervalTypes: List[String] = List("dhscor", "fantom5", "pchic")
  val defaultFPredTypes: List[String] = List("fpred")
  val defaultDistanceTypes: List[String] = List("distance")
  val defaultSegmentDivFactor: Double = 1e6
  val defaultTopOverlapStudiesSize: Int = 10
  val defaultStudiesForGeneSize: Int = 10
  val GWASLiteral: String = "gwas"

  /** Given a `filename`, the function fully loads the content into an option and
    * maps it with `Json.parse`
    * @param filename fully filename of a resource file
    * @return A wrapped Json object from the given filename with an Option
    */
  def loadJSONFromFilename(filename: String): JsValue =
    Json.parse(filename.toFile.contentAsString)

  def loadJSONLinesIntoMap[A, B](filename: String)(f: JsValue => (A, B)): Map[A, B] = {
    val parsedLines = filename.toFile.lines.map(Json.parse)

    val pairs =
      for (l <- parsedLines)
        yield f(l)

    pairs.toMap
  }

  def toSumStatsSegment(from: Long, factor: Double = defaultSegmentDivFactor): Long =
    (from / factor).toLong

  /** Both numbers must be positive numbers and absolute chromosome coords,
   * start >= 0 and end > 0. Also, (end - start) > 0 and the diff between end and start
   * must be greater than 0 and less or equal than defaultMaxRegionSize
   *
   * @param start start position on a genome strand
   * @param end   end position of the range
   * @return Some(start, end) pair or None
   */
  def parseRegion(
                   start: Long,
                   end: Long,
                   maxDistance: Long = defaultMaxRegionSize
                 ): Either[InChromosomeRegionViolation, (Long, Long)] = {
    if (
      (start >= 0 && end > 0) &&
        ((end - start) > 0) &&
        ((end - start) <= maxDistance)
    ) {
      Right((start, end))
    } else
      Left(InChromosomeRegionViolation(maxDistance))
  }

  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
   * each time. The default pageSize is defaultPaginationSize
   *
   * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
   * @param pageSize  the number of elements to get per page. default number defaultPaginationSize
   * @return Clickhouse SQL dialect string to be used when you want to paginate
   */
  def parsePaginationTokens(
                             pageIndex: Option[Int],
                             pageSize: Option[Int] = defaultPaginationSize
                           ): String = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => s"LIMIT ${defaultPaginationSize.get}"
      case List(0, s) => s"LIMIT $s"
      case List(i, 0) => s"LIMIT ${i * defaultPaginationSize.get}, ${defaultPaginationSize.get}"
      case List(i, s) => s"LIMIT ${i * s} , $s"
    }
  }

  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
   * each time. The default pageSize is defaultPaginationSize
   *
   * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
   * @param pageSize  the number of elements to get per page. default number defaultPaginationSize
   * @return Clickhouse SQL dialect string to be used when you want to paginate
   */
  def parsePaginationTokensForSlick(
                                     pageIndex: Option[Int],
                                     pageSize: Option[Int] = defaultPaginationSize
                                   ): (Int, Int) = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => (0, defaultPaginationSize.get)
      case List(0, s) => (0, s)
      case List(i, 0) => (i * defaultPaginationSize.get, defaultPaginationSize.get)
      case List(i, s) => (i * s, s)
    }
  }

  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
   * each time. The default pageSize is defaultPaginationSize
   *
   * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
   * @param pageSize  the number of elements to get per page. default number defaultPaginationSize
   * @return Clickhouse SQL dialect string to be used when you want to paginate
   */
  def parsePaginationTokensForES(
                                  pageIndex: Option[Int],
                                  pageSize: Option[Int] = defaultPaginationSizeES
                                ): (Int, Int) = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => (0, defaultPaginationSizeES.get)
      case List(0, s) => (0, s)
      case List(i, 0) => (i * defaultPaginationSizeES.get, defaultPaginationSizeES.get)
      case List(i, s) => (i * s, s)
    }
  }

  /** parse and return the proper chromosome string or None */
  def parseChromosome(chromosome: String): Either[ChromosomeViolation, String] =
    defaultChromosomes.find(_.equalsIgnoreCase(chromosome)) match {
      case Some(chr) => Right(chr)
      case None => Left(ChromosomeViolation(chromosome))
    }

  def toSafeDouble(mantissa: Double, exponent: Double): Double = {
    val result = mantissa * Math.pow(10, exponent)
    result match {
      case Double.PositiveInfinity => Double.MaxValue
      case Double.NegativeInfinity => Double.MinValue
      case 0.0 => Double.MinPositiveValue
      case -0.0 => -Double.MinPositiveValue
      case _ => result
    }
  }
}
