package models

import models.entities.Entities.SearchResultSet
import org.scalatest.FlatSpecLike
import org.scalatestplus.play.PlaySpec

class EntitiesSpec extends FlatSpecLike {

  "Two SearchResultSets" should "combine" in {
    // given
    val srs1 = SearchResultSet(1, Seq(), 1, Seq(), 1, Seq())
    val srs2 = SearchResultSet(1, Seq(), 1, Seq(), 1, Seq())
    // when
    val res = srs1.combine(srs2)
    // then
    assertResult(2)(res.totalStudies)
    assertResult(2)(res.totalGenes)
    assertResult(2)(res.totalVariants)
    assert(res.genes.isEmpty)
    assert(res.studies.isEmpty)
    assert(res.variants.isEmpty)

  }

}
