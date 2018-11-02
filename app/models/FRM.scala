package models

import slick.jdbc.H2Profile.api._

object FRM {
  /**  In order to convert the db's string representation to Seq[Long] for the exons
    *  the sql approach can use `cast(exons, 'Array(UInt32)') AS exons`, which might
    *  be achievable with a SimpleExpression. Alternatively (and working), there is
    *  a MappedColumnType, which likely does the conversion in scala.
    */

//  def asIntArray = SimpleExpression.unary[String, String] { (str, qb) =>
//    qb.sqlBuilder += "CAST("
//    qb.expr(str)
//    qb.sqlBuilder += ", 'Array(UInt32)') AS "
//    qb.expr(str)
//  }

  implicit val seqLongType = MappedColumnType.base[Seq[Long], String](
    { ls => ls.mkString("[", ",", "]") },
    { s => s.filterNot("[]".toSet).split(",").map(_.toLong).toSeq }
  )
  // --------------------------------------------------------
  // GENE

  case class Gene(id: String, symbol: Option[String], bioType: Option[String] = None, chromosome: Option[String] = None,
                  tss: Option[Long] = None, start: Option[Long] = None, end: Option[Long] = None,
                  fwd: Option[Boolean] = None, exons: Seq[Long] = Seq.empty)

  class Genes(tag: Tag) extends Table[Gene](tag, "gene") {
    def id = column[String]("gene_id")
    def symbol = column[Option[String]]("gene_name")
    def bioType = column[Option[String]]("biotype")
    def chromosome = column[Option[String]]("chr")
    def tss = column[Option[Long]]("tss")
    def start = column[Option[Long]]("start")
    def end = column[Option[Long]]("end")
    def fwd = column[Option[Boolean]]("fwdstrand")
    def exons = column[Seq[Long]]("exons")
    def * = (id, symbol, bioType, chromosome, tss, start, end, fwd, exons) <> (Gene.tupled, Gene.unapply)
  }

  lazy val genes = TableQuery[Genes]

//  // --------------------------------------------------------
//  // STUDY
//
//  case class Study(studyId: String, traitCode: String, traitReported: String, traitEfos: Seq[String],
//                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
//                   pubAuthor: Option[String], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
//                   nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
//                   traitCategory: Option[String])
//
//  class Studies(tag: Tag) extends Table[Study](tag, "studies") {
//    def studyId = column[String]("study_id")
//    def traitCode = column[String]("trait_code")
//    def traitReported = column[String]("trait_reported")
//    def traitEfos = column[Seq[String]]("trait_efos")
//    def pubId = column[Option[String]]("pmid")
//    def pubDate = column[Option[String]]("pub_data")
//    def pubJournal = column[Option[String]]("pub_journal")
//    def pubTitle = column[Option[String]]("pub_title")
//    def pubAuthor = column[Option[String]]("pub_author")
//    def ancestryInitial = column[Seq[String]]("ancestry_initial")
//    def ancestryReplication = column[Seq[String]]("ancestry_replication")
//    def nInitial = column[Option[Long]]("n_initial")
//    def nCases = column[Option[Long]]("n_cases")
//    def traitCategory = column[Option[String]]("trait_category")
//  }
//
//  lazy val studies = TableQuery[Studies]
//
//  // --------------------------------------------------------
}
