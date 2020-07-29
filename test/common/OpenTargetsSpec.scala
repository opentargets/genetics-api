package common

import org.scalatest.OptionValues
import org.scalatest.flatspec.{AnyFlatSpec, AnyFlatSpecLike}
import org.scalatest.matchers.must.Matchers
import org.scalatest.verbs.{MustVerb, ShouldVerb}
import org.scalatest.wordspec.AnyWordSpec

abstract class OpenTargetsSpec extends AnyFlatSpec with Matchers with OptionValues