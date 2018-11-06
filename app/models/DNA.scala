package models

import java.io.FileNotFoundException

import models.Violations.{GeneViolation, RegionViolation, VariantViolation}
import sangria.execution.deferred.HasId

import scala.io.Source
import slick.jdbc.GetResult
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
            * @return Some matched or not | None if denseregion map is None too
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

  case class Variant(position: Position, refAllele: String, altAllele: String, rsId: Option[String],
                     nearestGeneId: Option[String] = None, nearestCodingGeneId: Option[String] = None) {
    lazy val id: String = List(position.chrId, position.position.toString, refAllele, altAllele)
      .map(_.toUpperCase)
      .mkString("_")
  }

  object Variant {
    implicit val hasId = HasId[Variant, String](_.id)

    def apply(variantId: String): Either[VariantViolation, Variant] = Variant.apply(variantId, None)
    def apply(variantId: String, rsId: Option[String]): Either[VariantViolation, Variant] = {
      variantId.toUpperCase.split("_").toList.filter(_.nonEmpty) match {
        case List(chr: String, pos: String, ref: String, alt: String) =>
          Right(Variant(Position(chr, pos.toLong), ref, alt, rsId, None, None))
        case _ =>
          Left(VariantViolation(variantId))
      }
    }
  }

//  case class Gene(id: String, symbol: Option[String], start: Option[Long] = None, end: Option[Long] = None,
//                  chromosome: Option[String] = None, tss: Option[Long] = None,
//                  bioType: Option[String] = None, fwd: Option[Boolean] = None, exons: Seq[Long] = Seq.empty)
//
//  object Gene {
//    /** construct a gene from a gene id symbol. It only supports Ensembl ID at the moment
//      *
//      * @param geneId Ensembl Gene ID as "ENSG000000[.123]" and it will strip the version
//      * @return Either a Gene or a GeneViolation as the gene was not properly specified
//      */
//    def apply(geneId: String): Either[GeneViolation, Gene] = {
//      geneId.toUpperCase.split("\\.").toList.filter(_.nonEmpty) match {
//        case ensemblId :: _ =>
//          Right(Gene(ensemblId, None))
//        case Nil =>
//          Left(GeneViolation(geneId))
//      }
//    }
//
//    implicit val hasId = HasId[Gene, String](_.id)
//  }

  object Implicits {
    implicit def stringToVariant(variantID: String): Either[VariantViolation, Variant] =
      Variant.apply(variantID)
//
    implicit val getVariantFromDB: GetResult[Variant] =
      GetResult(r => Variant(Position(r.nextString, r.nextLong), refAllele = r.nextString,
        altAllele = r.nextString, rsId = r.nextStringOption, nearestGeneId = r.nextStringOption,
        nearestCodingGeneId = r.nextStringOption))

//    implicit val getGeneFromDB: GetResult[Gene] =
//      GetResult(r => Gene(id = r.nextString(), symbol = r.nextStringOption(), bioType = r.nextStringOption(),
//        chromosome = r.nextStringOption(), tss = r.nextLongOption(),
//        start = r.nextLongOption(), end = r.nextLongOption(), fwd = r.nextBooleanOption(),
//        exons = LSeqRep(r.nextString())))
  }
}
