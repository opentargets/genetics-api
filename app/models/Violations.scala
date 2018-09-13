package models

import sangria.execution.{UserFacingError, WithViolations}
import sangria.validation.{BaseViolation, Violation}

object Violations {
  val searchStringErrorMsg: String =
    "I see. You didn't search for anything. I am really sorry but we are not Google with its " +
      "button 'I'm Feeling Lucky'. You may want to search for a gene or a variant or even " +
      "a trait."
  val variantErrorMsg: String =
    "Ouch! It failed to parse the variant '%s' you ask for. Maybe you didn't spell it correctly. " +
      "Please, pay attention to what you wrote before as it could be missing a letter or something else. " +
      "Let me illustrate this with an example: '1_12345_T_C'."

  val chromosomeErrorMsg: String =
    "Ouch! It failed to parse the chromosome '%s' you ask for. Maybe you didn't spell it correctly. " +
      "It is only supported chromosomes in the range [1..22] and 'X', 'Y' and 'MT'."

  val inChromosomeRegionErrorMsg: String =
    "Ouch! The chromosome region was not properly specified. 'start' argument must be a positive number " +
      "and < 'end' argument. Also, the argument 'end' must be a positive number and > 'start'."

  case class VariantViolation(msg: String) extends BaseViolation(variantErrorMsg format(msg))
  case class ChromosomeViolation(msg: String) extends BaseViolation(chromosomeErrorMsg format(msg))
  case class InChromosomeRegionViolation() extends BaseViolation(inChromosomeRegionErrorMsg)
  case class SearchStringViolation() extends BaseViolation(searchStringErrorMsg)

  case class InputParameterCheckError(violations: Vector[Violation])
    extends Exception(s"Error during input parameter check. " +
      s"Violations:\n\n${violations map (_.errorMessage) mkString "\n\n"}")
      with WithViolations
      with UserFacingError
}