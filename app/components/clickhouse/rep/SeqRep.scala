package components.clickhouse.rep

import components.clickhouse.ClickHouseProfile


/** Clickhouse supports Array of elements from different types and this is an approximation
 * to it.
 *
 * @param from
 * @tparam T
 */
sealed abstract class SeqRep[T](val from: String) {
  protected val minLenTokensForStr = 4
  protected val minLenTokensForNum = 2
  lazy val rep: SeqT = parse(from)
  type SeqT = Seq[T]
  protected def parse(from: String): SeqT
}

object SeqRep {

  /**
   * Slick will return items to us from the Clickhouse database as a String, and we want
   * to be abl
   *
   * @param from String returned from database
   * @param f    convert String to T
   * @tparam T
   */
  sealed abstract class NumSeqRep[T](override val from: String, val f: String => T)
    extends SeqRep[T](from) {
    override protected def parse(from: String): SeqT = {
      if (from.nonEmpty) {

        /* From is returned as "[e1, e2, ..., en]", so the empty set as a string
          looks like "[]". Because of this the below is actually fully defined. 
        * */
        from.length match {
          case n if n > minLenTokensForNum =>
            from.slice(1, n - 1).split(",").map(f(_))
          case _ => Seq.empty
        }
      } else
        Seq.empty
    }
  }

  case class DSeqRep(override val from: String) extends NumSeqRep[Double](from, _.toDouble)
  case class ISeqRep(override val from: String) extends NumSeqRep[Int](from, _.toInt)
  case class LSeqRep(override val from: String) extends NumSeqRep[Long](from, _.toLong)

  case class StrSeqRep(override val from: String) extends SeqRep[String](from) {
    override protected def parse(from: String): SeqT = {
      if (from.nonEmpty) {
        from.length match {
          case n if n > minLenTokensForStr =>
            from.slice(1, n - 1).split(",").map(t => t.slice(1, t.length - 1))
          case _ => Seq.empty
        }
      } else
        Seq.empty
    }
  }

  object Implicits {

    import components.clickhouse.ClickHouseProfile.api._

    /** ClickHouse driver allows us to get serialised Arrays of all scalar types. But
     * jdbc does not allow to map to a seq of a scalar so these columns are defined here to
     * be able to interpret them implicitly.
     */
    implicit def seqInt(from: ISeqRep): Seq[Int] = from.rep

    implicit def seqLong(from: LSeqRep): Seq[Long] = from.rep

    implicit def seqDouble(from: DSeqRep): Seq[Double] = from.rep

    implicit def seqStr(from: StrSeqRep): Seq[String] = from.rep

    implicit val seqIntType: ClickHouseProfile.BaseColumnType[Seq[Int]] =
      MappedColumnType.base[Seq[Int], String](_.mkString("[", ",", "]"), ISeqRep(_))
    implicit val seqLongType: ClickHouseProfile.BaseColumnType[Seq[Long]] =
      MappedColumnType.base[Seq[Long], String](_.mkString("[", ",", "]"), LSeqRep(_))
    implicit val seqDoubleType: ClickHouseProfile.BaseColumnType[Seq[Double]] =
      MappedColumnType.base[Seq[Double], String](_.mkString("[", ",", "]"), DSeqRep(_))
    implicit val seqStringType: ClickHouseProfile.BaseColumnType[Seq[String]] =
      MappedColumnType
        .base[Seq[String], String](_.map("'" + _ + "'").mkString("[", ",", "]"), StrSeqRep(_))
  }

}
