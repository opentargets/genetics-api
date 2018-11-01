package models

import slick.jdbc.H2Profile.api._

object FRM {
  case class Gene(id: String, symbol: Option[String], bioType: Option[String] = None, chromosome: Option[String] = None,
                  tss: Option[Long] = None, start: Option[Long] = None, end: Option[Long] = None, fwd: Option[Boolean] = None, exons: Option[String] = None)

  class Genes(tag: Tag) extends Table[Gene](tag, "gene") {
    def id = column[String]("gene_id")
    def symbol = column[Option[String]]("gene_name")
    def bioType = column[Option[String]]("biotype")
    def chromosome = column[Option[String]]("chr")
    def tss = column[Option[Long]]("tss")
    def start = column[Option[Long]]("start")
    def end = column[Option[Long]]("end")
    def fwd = column[Option[Boolean]]("fwdstrand")
    def exons = column[Option[String]]("exons")
    def * = (id, symbol, bioType, chromosome, tss, start, end, fwd, exons) <> (Gene.tupled, Gene.unapply)
  }

  lazy val genes = TableQuery[Genes]
}
