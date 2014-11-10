package unitspecs

import datatypes.TupleSerializer
import org.scalatest.{Inspectors, GivenWhenThen, FlatSpec, Matchers}

/**
 * Created by costa on 11/10/14.
 */
class TupleSerializerSpec  extends FlatSpec with Matchers with GivenWhenThen with Inspectors {

  behavior of "The TupleSerializer"

  it should "be able to serialize tuples of all kinds of Tuple2" in{
    Given("A Tuple2 originalT2")
    val originalT2: (Int, Int) = (1,2)
    println("originalT2 = " + originalT2)

    When("originalT2 is serialized to XML as a serialT2")
    val serialT2 = TupleSerializer.tupleToXML(originalT2)
    println("serialT2 = " + serialT2)
    Then("The deserialization should be a Tuple2 with the correct values again")
    val deserialT2 = TupleSerializer.xmlToTuple2(serialT2)
    println("deserialT2 = " + deserialT2)
    deserialT2._1.toInt shouldEqual originalT2._1
    deserialT2._2.toInt shouldEqual originalT2._2
  }
}
