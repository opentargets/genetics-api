package models

import models.Violations.{ChromosomeViolation, InChromosomeRegionViolation}

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

  def toSumStatsSegment(from: Long, factor: Double = defaultSegmentDivFactor): Long =
    (from / factor).toLong

  /** Both numbers must be positive numbers and absolute chromosome coords,
    * start >= 0 and end > 0. Also, (end - start) > 0 and the diff between end and start
    * must be greater than 0 and less or equal than defaultMaxRegionSize
    *
    * @param start start position on a genome strand
    * @param end end position of the range
    * @return Some(start, end) pair or None
    */
  def parseRegion(start: Long, end: Long): Either[InChromosomeRegionViolation, (Long, Long)] = {
    if ( (start >= 0 && end > 0) &&
      ((end - start) > 0) &&
      ((end - start) <= defaultMaxRegionSize) ) {
      Right((start, end))
    } else
      Left(InChromosomeRegionViolation())
  }
  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
    * each time. The default pageSize is defaultPaginationSize
    * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
    * @param pageSize the number of elements to get per page. default number defaultPaginationSize
    * @return Clickhouse SQL dialect string to be used when you want to paginate
    */
  def parsePaginationTokens(pageIndex: Option[Int], pageSize: Option[Int] = defaultPaginationSize): String = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => s"LIMIT ${defaultPaginationSize.get}"
      case List(0, s) => s"LIMIT $s"
      case List(i, 0)  => s"LIMIT ${i*defaultPaginationSize.get}, ${defaultPaginationSize.get}"
      case List(i, s) => s"LIMIT ${i*s} , $s"
    }
  }

  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
    * each time. The default pageSize is defaultPaginationSize
    * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
    * @param pageSize the number of elements to get per page. default number defaultPaginationSize
    * @return Clickhouse SQL dialect string to be used when you want to paginate
    */
  def parsePaginationTokensForSlick(pageIndex: Option[Int], pageSize: Option[Int] = defaultPaginationSize): (Int, Int) = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => (0, 0)
      case List(0, s) => (0, s)
      case List(i, 0) => (i*defaultPaginationSize.get, defaultPaginationSize.get)
      case List(i, s) => (i*s, s)
    }
  }

  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
    * each time. The default pageSize is defaultPaginationSize
    * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
    * @param pageSize the number of elements to get per page. default number defaultPaginationSize
    * @return Clickhouse SQL dialect string to be used when you want to paginate
    */
  def parsePaginationTokensForES(pageIndex: Option[Int],
                                 pageSize: Option[Int] = defaultPaginationSizeES): (Int, Int) = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => (0, defaultPaginationSizeES.get)
      case List(0, s) => (0, s)
      case List(i, 0)  => (i*defaultPaginationSizeES.get, defaultPaginationSizeES.get)
      case List(i, s) => (i*s, s)
    }
  }

  /** parse and return the proper chromosome string or None */
  def parseChromosome(chromosome: String): Either[ChromosomeViolation,String] =
    defaultChromosomes.find(_.equalsIgnoreCase(chromosome)) match {
      case Some(chr) => Right(chr)
      case None => Left(ChromosomeViolation(chromosome))
    }
}
