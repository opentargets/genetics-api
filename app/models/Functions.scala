package models

object Functions {
  def parseOffsetLimit(pageIndex: Option[Int], pageSize: Option[Int]): String = {
    val defSize: Int = 1000000
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => s"LIMIT $defSize"
      case List(0, s) => s"LIMIT $s"
      case List(i, 0)  => s"LIMIT ${i*defSize}, $defSize"
      case List(i, s) => s"LIMIT ${i*s} , $s"
    }
  }
}
