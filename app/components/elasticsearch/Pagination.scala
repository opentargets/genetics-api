package components.elasticsearch

import play.api.libs.json.{Json, OFormat}

/** Pagination case class takes an index from 0..page-1 and size indicate
 * the batch of each page.
 * */
case class Pagination(index: Int, size: Int) {
  lazy val offset: Int = toES._1

  def hasValidRange(maxSize: Int = Pagination.sizeMax): Boolean = size <= maxSize

  val toSQL: String = (index, size) match {
    case (0, 0) => s"LIMIT ${Pagination.sizeDefault}"
    case (0, s) => s"LIMIT $s"
    case (i, 0) => s"LIMIT ${i * Pagination.sizeDefault}, ${Pagination.sizeDefault}"
    case (i, s) => s"LIMIT ${i * s} , $s"
    case _ => s"LIMIT ${Pagination.indexDefault}, ${Pagination.sizeDefault}"
  }
  val toES: (Int, Int) = (index, size) match {
    case (0, 0) => (0, Pagination.sizeDefault)
    case (0, s) => (0, s)
    case (i, 0) => (i * Pagination.sizeDefault, Pagination.sizeDefault)
    case (i, s) => (i * s, s)
    case _ => (0, Pagination.sizeDefault)
  }
}

object Pagination {
  val sizeMax: Int = 5000
  val sizeDefault: Int = 25
  val indexDefault: Int = 0

  def mkDefault: Pagination = Pagination(indexDefault, sizeDefault)

  implicit val paginationFormatImp: OFormat[Pagination] = Json.format[Pagination]
}

