package models

import java.io.FileNotFoundException

import models.Violations.{GeneViolation, VariantViolation}

import scala.io.Source
import kantan.csv._
import kantan.csv.ops._
import play.api.Logger

object DNA {
  /** position in a genome comes with a chromosome ID `chrId` and a `position` */
  case class Position(chrId: String, position: Long)
  /** region in a genome is a segment on a specific chromosome `chrId` with `start` and `stop` */
  case class Region(chrId: String, start: Long, end: Long)

  abstract class DenseRegionChecker {
    val denseRegions: Map[String, List[Region]]
    def matchRegion(r: Region): Boolean
  }

  object DenseRegionChecker {
    val logger = Logger(DenseRegionChecker.getClass)

    implicit val regionDecoder: RowDecoder[Region] = RowDecoder.decoder(0, 1, 2)(Region.apply)

    private def loadDenseRegionFromTSV(filename: String): Option[String] =
      Option(Source.fromFile(filename).mkString)
    private def parseTSV2Map(regions: Option[String]): Option[Map[String, List[Region]]] =
      regions.map(_.asCsvReader[Region](rfc.withHeader.withCellSeparator('\t'))
        .filter(_.isRight).map(_.right.get).toList.groupBy(_.chrId))

    /** build a `DenseRegionChecker` implementation based o list of regions in a DNA
      *
      * @param filename name of a TSV file containing a list of regions
      * @return a `DenseRegionChecker` implementation based on the already
      *         specified list of dense regions
      */
    def apply(filename: String): DenseRegionChecker = {
      parseTSV2Map(loadDenseRegionFromTSV(filename)) match {
        case Some(regs) => new DenseRegionChecker {
          override val denseRegions: Map[String, List[Region]] =
            regs

          /** match a region (chr:start-end) in a list of highly dense regions true if overlaps false otherwise
            *
            * @param region the region to match against dense regions
            * @return matched or not
            */
          override def matchRegion(region: Region): Boolean = {
            denseRegions.get(region.chrId) match {
              case Some(r) => r.exists(p => {
                logger.debug(s"dense region found at $region")
                ((region.start >= p.start) && (region.start <= p.end)) ||
                  ((region.end >= p.start) && (region.end <= p.end))
              })
              case None => false
            }
          }
        }
        case None =>
          throw new FileNotFoundException("Failed to load dense region file")
      }
    }

  }

  case class Annotation(nearestGeneId: Option[String] = None,
                        nearestGeneDistance: Option[Long] = None,
                        nearestCodingGeneId: Option[String] = None,
                        nearestCodingGeneDistance: Option[Long] = None,
                        mostSevereConsequence: Option[String] = None)
  case class CaddAnnotation(raw: Option[Double] = None, phred: Option[Double] = None)
  case class GnomadAnnotation(afr: Option[Double] = None, seu: Option[Double] = None,
                              amr: Option[Double] = None, asj: Option[Double] = None,
                              eas: Option[Double] = None, fin: Option[Double] = None,
                              nfe: Option[Double] = None, nfeEST: Option[Double] = None,
                              nfeSEU: Option[Double] = None, nfeONF: Option[Double] = None,
                              nfeNWE: Option[Double] = None, oth: Option[Double] = None)

  case class Variant(chromosome: String, position: Long, refAllele: String, altAllele: String,
                     rsId: Option[String], annotation: Annotation,
                     caddAnnotation: CaddAnnotation,
                     gnomadAnnotation: GnomadAnnotation) {
    lazy val id: String = List(chromosome, position.toString, refAllele, altAllele)
      .map(_.toUpperCase)
      .mkString("_")
  }

  object Variant extends ((String, Long, String, String, Option[String],
    Annotation, CaddAnnotation, GnomadAnnotation) => Variant) {
    private[this] def parseVariant(variantId: String, rsId: Option[String]): Option[Variant] = {
      variantId.toUpperCase.split("_").toList.filter(_.nonEmpty) match {
        case List(chr: String, pos: String, ref: String, alt: String) =>
          Some(Variant(chr, pos.toLong, ref, alt))
        case _ => None
      }
    }

    def apply(chromosome: String, position: Long, refAllele: String, altAllele: String): Variant =
      Variant(chromosome, position, refAllele, altAllele, None, Annotation(),
        CaddAnnotation(), GnomadAnnotation())

    def apply(chromosome: String, position: Long,
              refAllele: String, altAllele: String, rsId: Option[String]): Variant =
      Variant(chromosome, position, refAllele, altAllele, rsId, Annotation(),
        CaddAnnotation(), GnomadAnnotation())

    def apply(variantId: String): Either[VariantViolation, Variant] = apply(variantId, None)

    def apply(variantId: String, rsId: Option[String]): Either[VariantViolation, Variant] = {
      val pv = parseVariant(variantId, rsId)
      Either.cond(pv.isDefined, pv.get, VariantViolation(variantId))
    }

    def unapply(v: Variant): Option[(String, Long, String, String, Option[String],
      Annotation, CaddAnnotation, GnomadAnnotation)] =
      Some(v.chromosome, v.position, v.refAllele, v.altAllele,
        v.rsId, v.annotation, v.caddAnnotation, v.gnomadAnnotation)
  }

  case class Gene(id: String, symbol: Option[String], bioType: Option[String] = None, chromosome: Option[String] = None,
                  tss: Option[Long] = None, start: Option[Long] = None, end: Option[Long] = None,
                  fwd: Option[Boolean] = None, exons: Seq[Long] = Seq.empty)

  object Gene extends ((String, Option[String], Option[String], Option[String], Option[Long],
    Option[Long], Option[Long], Option[Boolean], Seq[Long]) => Gene) {
    private[this] def parseGene(geneId: String, symbol : Option[String]): Option[Gene] = {
      geneId.toUpperCase.split("\\.").toList.filter(_.nonEmpty) match {
        case ensemblId :: _ =>
          Some(Gene(ensemblId, symbol))
        case Nil => None
      }
    }

    /** construct a gene from a gene id symbol. It only supports Ensembl ID at the moment
      *
      * @param geneId Ensembl Gene ID as "ENSG000000[.123]" and it will strip the version
      * @return Either a Gene or a GeneViolation as the gene was not properly specified
      */
    def apply(geneId: String): Either[GeneViolation, Gene] = {
      val pg = parseGene(geneId, None)
      Either.cond(pg.isDefined, pg.get, GeneViolation(geneId))
    }

    def unapply(gene: Gene): Option[(String, Option[String], Option[String], Option[String], Option[Long],
      Option[Long], Option[Long], Option[Boolean], Seq[Long])] = Some(gene.id, gene.symbol, gene.bioType,
        gene.chromosome, gene.tss, gene.start, gene.end, gene.fwd, gene.exons)
  }
}
