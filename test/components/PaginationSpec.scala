package components

import common.OpenTargetsSpec
import components.elasticsearch.Pagination

class PaginationSpec extends OpenTargetsSpec {

  it must "correctly create a default configuration" in {
    val defaultPage = Pagination.mkDefault
    all(
      List(
        defaultPage.index must be(Pagination.indexDefault),
        defaultPage.size must be(Pagination.sizeDefault)
      )
    )
  }

  it must "increment the counters on next to facilitate sequential query calls" in {
    val incrementedPage = Pagination.mkDefault.next.toES
    all(
      List(
        incrementedPage._1 must be(Pagination.sizeDefault),
        incrementedPage._2 must be(Pagination.sizeDefault)
      )
    )
  }
}
