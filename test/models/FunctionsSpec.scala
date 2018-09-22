package models

import models.Functions.{ISeqRep, StrSeqRep}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class FunctionsSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  // "A Stack" must {
  //    "pop values in last-in-first-out order" in {
  //      val stack = new mutable.Stack[Int]
  //      stack.push(1)
  //      stack.push(2)
  //      stack.pop() mustBe 2
  //      stack.pop() mustBe 1
  //    }
  //    "throw NoSuchElementException if an empty stack is popped" in {
  //      val emptyStack = new mutable.Stack[Int]
  //      a[NoSuchElementException] must be thrownBy {
  //        emptyStack.pop()
  //      }
  //    }
  //  }

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
