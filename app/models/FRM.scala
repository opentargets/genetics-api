package models

import models.Entities.TagVariantAssociation
import slick.jdbc.H2Profile.api._
import slick.lifted.MappedProjection

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

//  case class SimpleVariant(id: String, chromosome: String, position: Long, refAllele: String, altAllele: String, rsId: Option[String])
  case class Variant(id: String, chromosome: String, position: Long, refAllele: String, altAllele: String, rsId: Option[String],
                     nearestGeneId: Option[String] = None, nearestCodingGeneId: Option[String] = None)
//  object Variant {
////    def apply(id: String, chromosome: String, position: Long, refAllele: String, altAllele: String, rsId: Option[String],
////      nearestGeneId: Option[String] = None, nearestCodingGeneId: Option[String] = None): Variant = new Variant(id, chromosome, position, refAllele, altAllele, rsId,
////      nearestGeneId, nearestCodingGeneId)
////    def apply(id: String, chromosome: String, position: Long, refAllele: String, altAllele: String, rsId: Option[String]): Variant = new Variant(id, chromosome, position, refAllele, altAllele, rsId,)
////    def unapply(v: Variant) = (v.id, v.chromosome, v.position, v.refAllele, v.altAllele, v.rsId, v.nearestGeneId, v.nearestCodingGeneId)
////    def unapply(v: Variant) = (v.id, v.chromosome, v.position, v.refAllele, v.altAllele, v.rsId)
//  }
//  def simpleToFullVariant(v: SimpleVariant): Variant = {
//    Variant(v.id, v.chromosome, v.position, v.refAllele, v.altAllele, v.rsId, None, None)
//  }

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

  // --------------------------------------------------------
  // V2D

//  case class V2D(
//                  tagId: String, tagChromosome: String, tagPosition: Long, tagRefAllele: String, tagAltAllele: String, tagRsId: Option[String],
//                  leadId: String, leadChromosome: String, leadPosition: Long, leadRefAllele: String, leadAltAllele: String, leadRsId: Option[String],
//
//                  studyId: String, traitCode: String, traitReported: String, traitEfos: Seq[String],
//                 pubId: Option[String], pubDate: Option[String], pubJournal: Option[String], pubTitle: Option[String],
//                 pubAuthor: Option[String], ancestryInitial: Seq[String], ancestryReplication: Seq[String],
//                 nInitial: Option[Long], nReplication: Option[Long], nCases: Option[Long],
//                 traitCategory: Option[String],
//
//                   pval: Option[Double], r2: Option[Double], log10Abf: Option[Double], posteriorProbability: Option[Double],
//                  afr1000GProp: Option[Double], amr1000GProp: Option[Double], eas1000GProp: Option[Double], eur1000GProp: Option[Double], sas1000GProp: Option[Double]
//                )
  case class V2DAssociation(
                      pval: Double, r2: Option[Double], log10Abf: Option[Double], posteriorProbability: Option[Double],
                      afr1000GProp: Option[Double], amr1000GProp: Option[Double], eas1000GProp: Option[Double], eur1000GProp: Option[Double], sas1000GProp: Option[Double]
                    )
//  case class LiftedV2DAssociation(
//                             pval: Rep[Option[Double]], r2: Rep[Option[Double]], log10Abf: Rep[Option[Double]], posteriorProbability: Rep[Option[Double]],
//                             afr1000GProp: Rep[Option[Double]], amr1000GProp: Rep[Option[Double]], eas1000GProp: Rep[Option[Double]], eur1000GProp: Rep[Option[Double]], sas1000GProp: Rep[Option[Double]]
//                           )
  case class V2D(tag: Variant, lead: Variant, study: Study, association: V2DAssociation)
//  case class LiftedV2D(tag: LiftedVariant, lead: LiftedVariant, study: LiftedStudy, association: LiftedV2DAssociation)
//  implicit object V2DShape extends CaseClassShape(LiftedV2D.tupled, V2D.tupled)

//  trait V2DFields {
//    def tagId = column[String]("variant_id")
//    def tagChromosome = column[String]("chr_id")
//    def tagPosition = column[Long]("position")
//    def tagRefAllele = column[String]("ref_allele")
//    def tagAltAllele = column[String]("alt_allele")
//    def tagRsId = column[Option[String]]("rs_id")
//
//    def leadId = column[String]("index_variant_id")
//    def leadChromosome = column[String]("index_chr_id")
//    def leadPosition = column[Long]("index_position")
//    def leadRefAllele = column[String]("index_ref_allele")
//    def leadAltAllele = column[String]("index_alt_allele")
//    def leadRsId = column[Option[String]]("index_rs_id")
//
//    def studyId = column[String]("stid")
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
//
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
//    def tagProjection = (tagId, tagChromosome, tagPosition, tagRefAllele, tagAltAllele, tagRsId) <> (SimpleVariant.tupled, SimpleVariant.unapply)
//    def leadProjection = (leadId, leadChromosome, leadPosition, leadRefAllele, leadAltAllele, leadRsId) <> (SimpleVariant.tupled, SimpleVariant.unapply)
//    def studyProjection = (studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
//      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory) <> (Study.tupled, Study.unapply)
//    def associationProjection = (pval, r2, log10Abf, posteriorProbability, afr1000GProp, amr1000GProp, eas1000GProp, eur1000GProp, sas1000GProp) <> (V2DAssociation.tupled, V2DAssociation.unapply)
//
//    def * = (tagProjection, leadProjection, studyProjection, associationProjection) <> (V2D.tupled, V2D.unapply)
//  }

//  class V2DsByStudy(tag: Tag) extends Table[V2D](tag, "v2d_by_stchr") {
//    def tagId = column[String]("variant_id")
//    def tagChromosome = column[String]("chr_id")
//    def tagPosition = column[Long]("position")
//    def tagRefAllele = column[String]("ref_allele")
//    def tagAltAllele = column[String]("alt_allele")
//    def tagRsId = column[Option[String]]("rs_id")
//
//    def leadId = column[String]("index_variant_id")
//    def leadChromosome = column[String]("index_chr_id")
//    def leadPosition = column[Long]("index_position")
//    def leadRefAllele = column[String]("index_ref_allele")
//    def leadAltAllele = column[String]("index_alt_allele")
//    def leadRsId = column[Option[String]]("index_rs_id")
//
//    def studyId = column[String]("stid")
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
//
//    def pval = column[Double]("pval")
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
//    def tagProjection = (tagId, tagChromosome, tagPosition, tagRefAllele, tagAltAllele, tagRsId) <> (SimpleVariant.tupled, SimpleVariant.unapply)
//    def leadProjection = (leadId, leadChromosome, leadPosition, leadRefAllele, leadAltAllele, leadRsId) <> (SimpleVariant.tupled, SimpleVariant.unapply)
//    def studyProjection = (studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
//      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory) <> (Study.tupled, Study.unapply)
//    def associationProjection = (pval, r2, log10Abf, posteriorProbability, afr1000GProp, amr1000GProp, eas1000GProp, eur1000GProp, sas1000GProp) <> (V2DAssociation.tupled, V2DAssociation.unapply)
//    def nTotal = nInitial.getOrElse(0) + nReplication.getOrElse(0)
//
//    def * = (tagProjection, leadProjection, studyProjection, associationProjection) <> (V2D.tupled, V2D.unapply)
//  }
//  lazy val v2DsByStudy = TableQuery[V2DsByStudy]

  def tupleToVariant (t: Tuple6[String, String, Long, String, String, Option[String]]) = Variant(t._1, t._2, t._3, t._4, t._5, t._6)
  def variantToTuple (v: Variant) = Some(v.id, v.chromosome, v.position, v.refAllele, v.altAllele, v.rsId)

  class V2DsByChrPos(tag: Tag) extends Table[V2D](tag, "v2d_by_chrpos") {
    def tagId = column[String]("variant_id")
    def tagChromosome = column[String]("chr_id")
    def tagPosition = column[Long]("position")
    def tagRefAllele = column[String]("ref_allele")
    def tagAltAllele = column[String]("alt_allele")
    def tagRsId = column[Option[String]]("rs_id")

    def leadId = column[String]("index_variant_id")
    def leadChromosome = column[String]("index_chr_id")
    def leadPosition = column[Long]("index_position")
    def leadRefAllele = column[String]("index_ref_allele")
    def leadAltAllele = column[String]("index_alt_allele")
    def leadRsId = column[Option[String]]("index_rs_id")

    def studyId = column[String]("stid")
    def traitCode = column[String]("trait_code")
    def traitReported = column[String]("trait_reported")
    def traitEfos = column[Seq[String]]("trait_efos")
    def pubId = column[Option[String]]("pmid")
    def pubDate = column[Option[String]]("pub_data")
    def pubJournal = column[Option[String]]("pub_journal")
    def pubTitle = column[Option[String]]("pub_title")
    def pubAuthor = column[Option[String]]("pub_author")
    def ancestryInitial = column[Seq[String]]("ancestry_initial")
    def ancestryReplication = column[Seq[String]]("ancestry_replication")
    def nInitial = column[Option[Long]]("n_initial")
    def nReplication = column[Option[Long]]("n_replication")
    def nCases = column[Option[Long]]("n_cases")
    def traitCategory = column[Option[String]]("trait_category")

    def pval = column[Double]("pval")
    def r2 = column[Option[Double]]("r2")
    def log10Abf = column[Option[Double]]("log10_abf")
    def posteriorProbability = column[Option[Double]]("posterior_prob")

    def afr1000GProp = column[Option[Double]]("afr_1000g_prop")
    def amr1000GProp = column[Option[Double]]("amr_1000g_prop")
    def eas1000GProp = column[Option[Double]]("eas_1000g_prop")
    def eur1000GProp = column[Option[Double]]("eur_1000g_prop")
    def sas1000GProp = column[Option[Double]]("sas_1000g_prop")

//    def tagProjection = (tagId, tagChromosome, tagPosition, tagRefAllele, tagAltAllele, tagRsId) <> (t => Variant(t._1, t._2, t._3, t._4, t._5, t._6), (v: Variant) => Some(v.id, v.chromosome, v.position, v.refAllele, v.altAllele, v.rsId))
    def tagProjection = (tagId, tagChromosome, tagPosition, tagRefAllele, tagAltAllele, tagRsId) <> (tupleToVariant, variantToTuple)
//    def leadProjection = (leadId, leadChromosome, leadPosition, leadRefAllele, leadAltAllele, leadRsId) <> (SimpleVariant.tupled, SimpleVariant.unapply)
    def leadProjection = (leadId, leadChromosome, leadPosition, leadRefAllele, leadAltAllele, leadRsId) <> (tupleToVariant, variantToTuple)
    def studyProjection = (studyId, traitCode, traitReported, traitEfos, pubId, pubDate, pubJournal, pubTitle,
      pubAuthor, ancestryInitial, ancestryReplication, nInitial, nReplication, nCases, traitCategory) <> (Study.tupled, Study.unapply)
    def associationProjection = (pval, r2, log10Abf, posteriorProbability, afr1000GProp, amr1000GProp, eas1000GProp, eur1000GProp, sas1000GProp) <> (V2DAssociation.tupled, V2DAssociation.unapply)

//    def nTotal = nInitial.getOrElse(0) + nReplication.getOrElse(0)
//    def asTagVariantAssociation = TagVariantAssociation(
//      leadProjection, studyId, pval, nTotal, nCases.getOrElse(0),
//      r2, afr1000GProp, amr1000GProp, eas1000GProp,
//      eur1000GProp, sas1000GProp, log10Abf, posteriorProbability
//    )
//    def tagVariantAssociationProjection = (
//      leadProjection, studyId, pval, nInitial.getOrElse(0) + nReplication.getOrElse(0), nCases.getOrElse(0),
//      r2, afr1000GProp, amr1000GProp, eas1000GProp,
//      eur1000GProp, sas1000GProp, log10Abf, posteriorProbability
//    ) <> (TagVariantAssociation.tupled, TagVariantAssociation.unapply)

    def * = (tagProjection, leadProjection, studyProjection, associationProjection) <> (V2D.tupled, V2D.unapply)
  }
  lazy val v2DsByChrPos = TableQuery[V2DsByChrPos]

  // --------------------------------------------------------


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
