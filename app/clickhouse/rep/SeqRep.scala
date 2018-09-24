package clickhouse.rep

/** Clickhouse supports Array of elements from different types and this is an approximation
  * to it.
  * @param from
  * @tparam T
  */
sealed abstract class SeqRep[T](val from: String) {
  lazy val rep: SeqT = parse(from)
  type SeqT = Seq[T]
  protected def parse(from: String): SeqT
}

object SeqRep {
  sealed abstract class NumSeqRep[T](override val from: String, val f: String => T) extends SeqRep[T](from) {
    override protected def parse(from: String): SeqT = {
      if (from.nonEmpty) {
        from.length match {
          case n if n > 2 =>
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
          case n if n > 4 =>
            from.slice(1, n - 1).split(",").map(t => t.slice(1, t.length - 1))
          case _ => Seq.empty
        }
      } else
        Seq.empty
    }
  }

  object Implicits {
    implicit def seqInt(from: ISeqRep): Seq[Int] = from.rep
    implicit def seqLong(from: LSeqRep): Seq[Long] = from.rep
    implicit def seqDouble(from: DSeqRep): Seq[Double] = from.rep
    implicit def seqStr(from: StrSeqRep): Seq[String] = from.rep
  }
}
