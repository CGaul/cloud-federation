package unitspecs

import java.io.File
import java.net.InetAddress

import datatypes.ByteUnit.{GiB, MB}
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes._
import org.scalatest.{FlatSpec, GivenWhenThen, Inspectors, Matchers}

/**
 * @author Constantin Gaul, created on 10/28/14.
 */
class ResourceSpec extends FlatSpec with Matchers with GivenWhenThen with Inspectors
{

	// Create the resources-dir in Agent-Framework Module,
	// if not already existent:
	val resDir = new File("Agent-Framework/src/test/resources/")
	if(! resDir.exists()){
		resDir.mkdirs()
	}
	
	
	
/* Resource-Class Unit-Spec */
/* ======================== */

	behavior of "A Resource"

	//General Small Node
	val res1 = new Resource(ResId(1), SMALL,
									ByteSize(8, GiB), ByteSize(50, GiB),
									ByteSize(10, MB), 10, Vector())

	//General Medium Node
	val res2= new Resource(ResId(2), MEDIUM,
									ByteSize(16, GiB), ByteSize(100, GiB),
									ByteSize(10, MB), 10, Vector())

	//Equals res2, but with lower latency
	val res3 = new Resource(ResId(3), MEDIUM,
									ByteSize(16, GiB), ByteSize(100, GiB),
									ByteSize(10, MB), 5, Vector())

	//General Large Node
	val res4 = new Resource(ResId(3), LARGE,
									ByteSize(24, GiB), ByteSize(200, GiB),
									ByteSize(50, MB), 10, Vector())


/* Test-Specs */
/* ========== */

	it should "be equal to itself, independent from the object creation (applied or instantiated)" in {
		When("res1 is directly compared to itself")
		Then("res1.equals(res1) should be true")
		res1 should equal(res1)
		res1 == res1 should be (right = true)


		Given("A Resource with the res1 footprint, instantiated statically via apply()")
		val staticRes1 = Resource(ResId(1), SMALL,
			ByteSize(8, GiB), ByteSize(50, GiB),
			ByteSize(10, MB), 10, Vector())

		When("res1 is compared with the statically applied staticRes1 copy")
		Then("res1.equals(staticRes1) should be true")
		res1.equals(staticRes1) should be (right = true)
		staticRes1.equals(res1) should be (right = true)
		res1 == staticRes1 should be (right = true)
		staticRes1 == res1 should be (right = true)
	}
	
	it should  "be equal to another Resource with the same Resource footprint (even with different IDs and Neighbours)" in{

		Given("A Respource with the res1 footprint, with additional link descriptions")
		val equalRes1 		= Resource(ResId(1), SMALL,
											ByteSize(8, GiB), ByteSize(50, GiB),
											ByteSize(10, MB), 10, Vector(ResId(2), ResId(3)))


		When("res1 is compared with the link-different equalRes1")
		Then("res1.equals(equalRes1) should be true")
		res1.equals(equalRes1) should be (right = true)
		equalRes1.equals(res1) should be (right = true)
		res1 == equalRes1 should be (right = true)
		equalRes1 == res1 should be (right = true)
		info("Equality tests completed!")
	}

	it should "be unequal to Resources with a different Resource footprint than the origin" in{
		When("Two unequal resources are compared")
		Then("equals should be false")
		res1.equals(res2) should be (right = false)
		res2.equals(res1) should be (right = false)
		res1 == res3 should be (right = false)
		res3 == res2 should be (right = false)
		res3 == res1 should be (right = false)
		info("Unequality tests completed!")
	}

	it should "be comparable with another Resource in a relative ordering" in{
		When("res1 is compared to itself")
		Then("RelativeOrdering.compare(res1, res1) should be == 0")
		(RelativeResOrdering.compare(res1, res1) == 0) should be (right = true)

		When("smaller res1 is compared to medium res2")
		Then("RelativeResOrdering.compare(res1, res2) should be < 0")
		(RelativeResOrdering.compare(res1, res2) < 0) should be (right = true)

		When("medium res2 is compared to smaller res1")
		Then("RelativeResOrdering.compare(res2, res1) should be > 0")
		(RelativeResOrdering.compare(res2, res1) > 0) should be (right = true)
		info("Comparison tests completed!")
	}

	it should "be fully serializable to and deserializable from XML" in{
		val xmlSerialRes1 = Resource.toXML(res1)
		val xmlSerialRes2 = Resource.toXML(res2)
		val xmlSerialRes3 = Resource.toXML(res3)

		println("serialized Res1 = " + xmlSerialRes1)
		println("serialized Res2 = " + xmlSerialRes2)
		println("serialized Res3 = " + xmlSerialRes3)

		val xmlDeserialRes1 = Resource.fromXML(xmlSerialRes1)
		val xmlDeserialRes2 = Resource.fromXML(xmlSerialRes2)
		val xmlDeserialRes3 = Resource.fromXML(xmlSerialRes3)

		res1 shouldEqual xmlDeserialRes1
		res2 shouldEqual xmlDeserialRes2
		res3 shouldEqual xmlDeserialRes3
		res3 should not equal xmlDeserialRes1
	}

	it should "be loadable from and saveable to a XML file" in{
		val xmlFile1 = new File(resDir.getAbsolutePath +"/Resource1.xml")
		val xmlFile2 = new File(resDir.getAbsolutePath +"/Resource2.xml")
		val xmlFile3 = new File(resDir.getAbsolutePath +"/Resource3.xml")
		Resource.saveToXML(xmlFile1, res1)
		Resource.saveToXML(xmlFile2, res2)
		Resource.saveToXML(xmlFile3, res3)

		val loadedResource1 = Resource.loadFromXML(xmlFile1)
		val loadedResource2 = Resource.loadFromXML(xmlFile2)
		val loadedResource3 = Resource.loadFromXML(xmlFile3)

		println("res1 = " + loadedResource1)
		println("res2 = " + loadedResource2)
		println("res3 = " + loadedResource3)

		res1 should equal (loadedResource1)
		res2 should equal (loadedResource2)
		res3 should equal (loadedResource3)
	}
}
