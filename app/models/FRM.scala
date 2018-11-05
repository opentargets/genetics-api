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
  implicit val seqStringType = MappedColumnType.base[Seq[String], String](
    { ss => ss.map("'" + _ + "'").mkString("[", ",", "]") },
    { s => s.filterNot("[]".toSet).split(",").toSeq }
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

  // --------------------------------------------------------
  // VARIANT

  case class Variant(id: String, chromosome: String, position: Long, refAllele: String, altAllele: String, rsId: Option[String],
                     nearestGeneId: Option[String] = None, nearestCodingGeneId: Option[String] = None)

  class Variants(tag: Tag) extends Table[Variant](tag, "variants") {
    def id = column[String]("variant_id")
    def chromosome = column[String]("chr_id")
    def position = column[Long]("position")
    def refAllele = column[String]("ref_allele")
    def altAllele = column[String]("alt_allele")
    def rsId = column[Option[String]]("rs_id")
    def nearestGeneId = column[Option[String]]("gene_id")
    def nearestCodingGeneId = column[Option[String]]("gene_id_prot_coding")
    def * = (id, chromosome, position, refAllele, altAllele, rsId, nearestGeneId, nearestCodingGeneId) <> (Variant.tupled, Variant.unapply)
  }

  lazy val variants = TableQuery[Variants]

  // --------------------------------------------------------
  // STUDY

  case class Study(studyId: String, traitCode: String, traitReported: String, traitEfos: Seq[String],
                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
                   pubAuthor: Option[String], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
                   nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
                   traitCategory: Option[String])

  class Studies(tag: Tag) extends Table[Study](tag, "studies") {
    def studyId = column[String]("study_id")
    def traitCode = column[String]("trait_code")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_date")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")
    def * = (
      studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory
    ) <> (Study.tupled, Study.unapply)
  }

  lazy val studies = TableQuery[Studies]

//  // --------------------------------------------------------
//  // V2D_BY_CHRPOS
//
//  case class V2D(studyId: String, traitCode: String, traitReported: String, traitEfos: Seq[String],
//                   pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
//                   pubAuthor: Option[String], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
//                   nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
//                   traitCategory: Option[String])
//
//  class V2DsByChrPos(tag: Tag) extends Table[V2D](tag, "v2d_by_chrpos") {
//    def tagId = column[String]("variant_id")
//    def tagChromosome = column[String]("chr_id")
//    def tagPosition = column[Long]("position")
//    def tagRefAllele = column[String]("ref_allele")
//    def tagAltAllele = column[String]("alt_allele")
//    def tagRsId = column[Option[String]]("rs_id")
//    def leadId = column[String]("index_variant_id")
//    def leadChromosome = column[String]("index_chr_id")
//    def leadPosition = column[Long]("index_position")
//    def leadRefAllele = column[String]("index_ref_allele")
//    def leadAltAllele = column[String]("index_alt_allele")
//    def leadRsId = column[Option[String]]("index_rs_id")
//
//    def studyId = column[String]("stid")
//    def pval = column[Option[Double]]("pval")
//    def r2 = column[Option[Double]]("r2")
//    def log10Abf = column[Option[Double]]("log10_abf")
//    def posteriorProbability = column[Option[Double]]("posterior_prob")
//
//    def afr1000GProp = column[Option[Double]]("afr_1000g_prop")
//    def amr1000GProp = column[Option[Double]]("amr_1000g_prop")
//    def eas1000GProp = column[Option[Double]]("eas_1000g_prop")
//    def eur1000GProp = column[Option[Double]]("eur_1000g_prop")
//    def sas1000GProp = column[Option[Double]]("sas_1000g_prop")
//
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
//    def nReplication = column[Option[Long]]("n_replication")
//    def nCases = column[Option[Long]]("n_cases")
//    def traitCategory = column[Option[String]]("trait_category")
//    def * = (
//      studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
//      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory
//    ) <> (Study.tupled, Study.unapply)
//  }
//
//  lazy val v2DsByChrPos = TableQuery[V2DsByChrPos]
//
//  // --------------------------------------------------------
}
