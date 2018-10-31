package models

import java.nio.file.{Path, Paths}

import models.DNA.{DenseRegionChecker, Gene, Region, Variant}
import models.Violations.VariantViolation
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class DNASpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  val denseRegionsPath: Path = Paths.get("resources", "dense_regions.tsv")
  val drChecker: DenseRegionChecker = DenseRegionChecker(denseRegionsPath.toString)

  "A overlapped region in 1:150000001-152000000 must return true" in {
    val region1 = Region("1", 149000001L, 150000001L)
    val region2 = Region("1", 150000010L, 151000100L)
    val region3 = Region("1", 151900001L, 153000000L)

    drChecker.matchRegion(region1) mustBe true
    drChecker.matchRegion(region2) mustBe true
    drChecker.matchRegion(region3) mustBe true
  }

  "A NON overlapped region in 1:150000001-152000000 must return false" in {
    val region1 = Region("1", 149000001L, 150000000L)
    val region2 = Region("2", 150000010L, 151000100L)
    val region3 = Region("1", 152000001L, 153000000L)

    drChecker.matchRegion(region1) mustBe false
    drChecker.matchRegion(region2) mustBe false
    drChecker.matchRegion(region3) mustBe false
  }

  "A Variant 1_1234_C_. must equal to a Right(Variant)" in {
    val v1 = Variant("1_1234_C_.")
    val rv1 = Right(Variant(DNA.Position("1", 1234), "C", ".", None))

    v1 mustEqual rv1
  }

  "A Variant 1_1234_T must not equal to a Right(Variant)" in {
    val v1 = Variant("1_1234_.")

    v1.isLeft mustBe true
  }

  "A Gene ENSG000012.13 must equal to a Right(Gene)" in {
    val v1 = Gene("ENSG000012.13")
    val rv1 = Right(Gene("ENSG000012", None))

    v1 mustEqual rv1
  }
}
