package unitspecs

import datatypes.TupleSerializer
import org.scalatest.{Inspectors, GivenWhenThen, FlatSpec, Matchers}

/**
 * @author Constantin Gaul, created on 11/10/14.
 */
class TupleSerializerSpec  extends FlatSpec with Matchers with GivenWhenThen with Inspectors {

  behavior of "The TupleSerializer"

  it should "be able to serialize tuples of all kinds of Tuple2" in{
    Given("An Int-Tuple2 originalT2")
    val originalT2: (Int, Int) = (1,2)
    println("originalT2 = " + originalT2)

    When("originalT2 is serialized to XML as a serialT2")
    val serialT2 = TupleSerializer.tupleToXML(originalT2)
    println("serialT2 = " + serialT2)
    Then("The deserialization should be a Tuple2 with the correct values again")
    val deserialT2 = TupleSerializer.xmlToTuple2(serialT2)
    println("deserialT2 = " + deserialT2)
    deserialT2.get._1.toInt shouldEqual originalT2._1
    deserialT2.get._2.toInt shouldEqual originalT2._2
  }

  it should "be able to serialize tuples of all kinds of Int-Tuple3" in {
    Given("An Int-Tuple3 originalT3")
    val originalT3: (Int, Int, Int) = (1, 2, 3)
    println("originalT3 = " + originalT3)

    When("originalT3 is serialized to XML as a serialT3")
    val serialT3 = TupleSerializer.tupleToXML(originalT3)
    println("serialT3 = " + serialT3)
    Then("The deserialization should be a Tuple3 with the correct values again")
    val deserialT3 = TupleSerializer.xmlToTuple3(serialT3)
    println("deserialT3 = " + deserialT3)
    deserialT3.get._1.toInt shouldEqual originalT3._1
    deserialT3.get._2.toInt shouldEqual originalT3._2
    deserialT3.get._3.toInt shouldEqual originalT3._3
  }

  it should "be abtle to serialize tuples of all kinds of String-Tuple3" in{
    Given("A String-Tuple3 originalT3")
    val originalT3: (String, String, String) = ("1.0 This is"," a test"," with three entries")
    println("originalT3 = " + originalT3)

    When("originalT3 is serialized to XML as a serialT3")
    val serialT3 = TupleSerializer.tupleToXML(originalT3)
    println("serialT3 = " + serialT3)
    Then("The deserialization should be a Tuple3 with the correct values again")
    val deserialT3 = TupleSerializer.xmlToTuple3(serialT3)
    println("deserialT3 = " + deserialT3)
    deserialT3.get._1 shouldEqual originalT3._1
    deserialT3.get._2 shouldEqual originalT3._2
    deserialT3.get._3 shouldEqual originalT3._3
  }
}
