package components

import components.elasticsearch.Pagination
import org.scalatest.flatspec.AnyFlatSpecLike

class PaginationSpec extends AnyFlatSpecLike {

  behavior of "Pagination for Elasticsearch"

  it should "correctly create a default configuration" in {
    val defaultPage = Pagination.mkDefault
    assert(defaultPage.index == Pagination.indexDefault)
    assert(defaultPage.size == Pagination.sizeDefault)
  }

  it should "increment the counters on next to facilitate sequential query calls" in {
    val incrementedPage = Pagination.mkDefault.next.toES
    // offset
    assert(incrementedPage._1 == Pagination.sizeDefault)
    // size to collect
    assert(incrementedPage._2 == Pagination.sizeDefault)
  }

}
