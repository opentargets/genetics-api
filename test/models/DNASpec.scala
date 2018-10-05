package models

import models.DNA.Region
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class DNASpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  "A overlapped region in 1:150000001-152000000 must return true" in {
    val region1 = Region("1", 149000001L, 150000001L)
    val region2 = Region("1", 150000010L, 151000100L)
    val region3 = Region("1", 151900001L, 153000000L)

    DNA.matchDenseRegion(region1) mustBe true
    DNA.matchDenseRegion(region2) mustBe true
    DNA.matchDenseRegion(region3) mustBe true
  }

  "A NON overlapped region in 1:150000001-152000000 must return false" in {
    val region1 = Region("1", 149000001L, 150000000L)
    val region2 = Region("2", 150000010L, 151000100L)
    val region3 = Region("1", 152000001L, 153000000L)

    DNA.matchDenseRegion(region1) mustBe false
    DNA.matchDenseRegion(region2) mustBe false
    DNA.matchDenseRegion(region3) mustBe false
  }
}
