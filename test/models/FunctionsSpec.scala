package models

import org.scalacheck.{Gen, Prop, Properties}
import org.scalacheck.Prop.{forAll, propBoolean}

object FunctionsSpec extends Properties("Functions") {

  val posLongTup = for {
    start <- Gen.choose(0, Functions.defaultMaxRegionSize)
    end <- Gen.choose(0, Functions.defaultMaxRegionSize)
  } yield (start, end)

  // parse region
  property("Parse region: is limited to valid inputs") = Prop.forAll(posLongTup) {
    (in: (Long, Long)) =>
      {
        val (start, end) = in
        (
          start >= 0 &&
          end > 0 &&
          (end - start) > 0 &&
          (end - start) <= Functions.defaultMaxRegionSize
        ) ==>
          (Functions.parseRegion(start, end).isRight)
      }
  }

}
