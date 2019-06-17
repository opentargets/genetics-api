package models

import sangria.execution.{UserFacingError, WithViolations}
import sangria.validation.{BaseViolation, Violation}
import models.DNA.Region

object Violations {
  val searchStringErrorMsg: String =
    "I see. You didn't search for anything. I am really sorry but we are not Google with its " +
      "button 'I'm Feeling Lucky'. You may want to search for a gene or a variant or even " +
      "a trait."

  val variantErrorMsg: String =
    "Ouch! It failed to parse the variant '%s' you ask for. Maybe you didn't spell it correctly. " +
      "Please, pay attention to what you wrote before as it could be missing a letter or something else. " +
      "Let me illustrate this with an example: '1_12345_T_C'."

  val geneErrorMsg: String =
    "Ouch! It failed to parse the gene '%s' you ask for. Maybe you didn't spell it correctly. " +
      "Please, pay attention to what you wrote before as it could be missing a letter or something else. " +
      "Let me illustrate this with an example: 'ENSG00000132485'."

  val chromosomeErrorMsg: String =
    "Ouch! It failed to parse the chromosome '%s' you ask for. Maybe you didn't spell it correctly. " +
      "It is only supported chromosomes in the range [1..22] and 'X', 'Y' and 'MT'."

  val inChromosomeRegionErrorMsg: String =
    "Ouch! The chromosome region was not properly specified. 'start' argument must be a positive number " +
      "and < 'end' argument. Also, the argument 'end' must be a positive number and > 'start'. And 'end' " +
      "- 'start' <= %d."

  val denseRegionErrorMsg: String =
    "We are sorry but this region '(%s:%d-%d)' contains a lot of raw data. We are currently working on an " +
      "update that will show an aggregation of the raw data (aggregating across the tag variants). We hope " +
      "to have this ready by our next release in February 2019, so please try this region again soon."

  case class VariantViolation(msg: String) extends BaseViolation(variantErrorMsg format msg)
  case class RegionViolation(region: Region) extends BaseViolation(denseRegionErrorMsg format(region.chrId,
    region.start, region.end))
  case class GeneViolation(msg: String) extends BaseViolation(geneErrorMsg format msg)
  case class ChromosomeViolation(msg: String) extends BaseViolation(chromosomeErrorMsg format msg)
  case class InChromosomeRegionViolation(regionLength: Long) extends BaseViolation(inChromosomeRegionErrorMsg format regionLength)
  case class SearchStringViolation() extends BaseViolation(searchStringErrorMsg)

  case class InputParameterCheckError(violations: Vector[Violation])
    extends Exception(s"Error during input parameter check. " +
      s"Violations:\n\n${violations map (_.errorMessage) mkString "\n\n"}")
      with WithViolations
      with UserFacingError
}
