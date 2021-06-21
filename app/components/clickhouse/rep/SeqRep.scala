package components.clickhouse.rep

import components.clickhouse.ClickHouseProfile
import play.api.Logging

/** Clickhouse supports Array of elements from different types and this is an approximation
  * to it.
  *
  * @param from
  * @tparam T
  */
sealed abstract class SeqRep[T, C[_]](val from: String) {
  protected val minLenTokensForStr = 4
  protected val minLenTokensForNum = 2
  lazy val rep: SeqT = parse(from)
  type SeqT = C[T]
  protected def parse(from: String): SeqT
}

object SeqRep {
  def parseFastString(str: String) = str.slice(1, str.length - 1)

  sealed abstract class NumSeqRep[T](override val from: String, val f: String => T)
    extends SeqRep[T, Seq](from) with Logging {
    override protected def parse(from: String): SeqT = {
      if (from.nonEmpty) {
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

  case class StrSeqRep(override val from: String) extends SeqRep[String, Seq](from) with Logging {
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

  case class TupleSeqRep[T](override val from: String, val f: String => T)
    extends SeqRep[T, Seq](from) {
    override protected def parse(from: String): SeqT = {
      if (from.nonEmpty) {
        from.length match {
          case n if n > minLenTokensForStr =>
            val offset: Int = minLenTokensForStr / 2
            from.slice(offset, n - offset).split("\\),\\(").map(f(_))
          case _ => Seq.empty
        }
      } else
        Seq.empty
    }
  }

  object Implicits {
    import components.clickhouse.ClickHouseProfile.api._

    implicit def seqIntS(from: ISeqRep): Seq[Int] = from.rep
    implicit def seqLongS(from: LSeqRep): Seq[Long] = from.rep
    implicit def seqDoubleS(from: DSeqRep): Seq[Double] = from.rep
    implicit def seqStrS(from: StrSeqRep): Seq[String] = from.rep

    implicit val seqIntType: ClickHouseProfile.BaseColumnType[Seq[Int]] =
      MappedColumnType.base[Seq[Int], String](_.mkString("[", ",", "]"), ISeqRep(_).rep)

    implicit val seqLongType: ClickHouseProfile.BaseColumnType[Seq[Long]] =
      MappedColumnType.base[Seq[Long], String](_.mkString("[", ",", "]"), LSeqRep(_).rep)
    implicit val seqDoubleType: ClickHouseProfile.BaseColumnType[Seq[Double]] =
      MappedColumnType.base[Seq[Double], String](_.mkString("[", ",", "]"), DSeqRep(_).rep)
    implicit val seqStringType: ClickHouseProfile.BaseColumnType[Seq[String]] =
      MappedColumnType.base[Seq[String], String](_.map("'" + _ + "'")
        .mkString("[", ",", "]"), StrSeqRep(_).rep)
  }
}