package models

import components.clickhouse.rep.SeqRep._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._

/** Add your spec here. You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see
  * https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class SeqRepSpec extends PlaySpec {

  "A Clickhouse Array(Int32) must be non-empty when 1 element present" in {
    ISeqRep("[1]").rep mustBe Seq(1)
  }

  "A Clickhouse Array(String) must be Seq.empty" in {
    StrSeqRep("['']").rep mustBe Seq.empty
    StrSeqRep("[]").rep mustBe Seq.empty
  }

  "A Clickhouse Array(String) must be a proper Seq('jaime','elsa')" in {
    StrSeqRep("['jaime','elsa']").rep mustBe Seq("jaime", "elsa")
    StrSeqRep("['jaime']").rep mustBe Seq("jaime")
  }

  "A Clickhouse Array(Int32) must be Seq.empty" in {
    StrSeqRep("[]").rep mustBe Seq.empty
  }

  "A Clickhouse Array(Int32) must be a proper Seq(10,20)" in {
    ISeqRep("[10,20]").rep mustBe Seq(10, 20)
    ISeqRep("[10]").rep mustBe Seq(10)
  }

}
