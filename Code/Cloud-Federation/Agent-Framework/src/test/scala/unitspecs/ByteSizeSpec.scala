package unitspecs

import java.io.File

import datatypes.ByteUnit._
import datatypes.ByteSize
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}

/**
 * @author Constantin Gaul, created on 10/20/14.
 */
class ByteSizeSpec extends FlatSpec with Matchers with GivenWhenThen
{

	// Create the resources-dir in Agent-Framework Module,
	// if not already existent:
	val resDir = new File("Agent-Framework/src/test/resources/")
	if(! resDir.exists()){
		resDir.mkdirs()
	}



/* ByteSize-Class Unit-Spec */
/* ======================== */

	behavior of "A ByteSize Value"

	//ByteSize 1 & 2 are equal, but with different Metrics:
	val byteSize1 = new ByteSize(1000, KiB)
	val byteSize2 = new ByteSize(1024, KB)

	val byteSize3 = new ByteSize(100, MiB)
	val byteSize4 = new ByteSize(10, GB)
	val byteSize5 = new ByteSize(100, PB)



	it should "be printed correctly" in{
		val byteSize1 = new ByteSize(5, MB)
		val byteSize2 = new ByteSize(125.123, KiB)
		byteSize1.toString should be ("5.0 MB")
		byteSize2.toString should be ("125.123 KiB")
	}
	it should "be equal and comparable to itself" in{
		byteSize1 should equal (byteSize1)
		byteSize2 should equal (byteSize2)
		byteSize3 should equal (byteSize3)
		byteSize4 should equal (byteSize4)
		byteSize5 should equal (byteSize5)

		byteSize1 should not equal byteSize3
		byteSize2 should not equal byteSize3
		byteSize3 should not equal byteSize4
		byteSize4 should not equal byteSize5

		byteSize1 compareTo byteSize1 should be (0)
		byteSize2 compareTo byteSize2 should be (0)
		byteSize3 compareTo byteSize3 should be (0)
		byteSize4 compareTo byteSize4 should be (0)
		byteSize5 compareTo byteSize5 should be (0)
	}

	it should "be equal, comparable and convertable to another Value with the same size and unit" in{
		val byteSize1New = new ByteSize(1000, KiB)
		val byteSize2New = new ByteSize(1024, KB)

	//Check for equality and comparability:
	  byteSize1 should equal (byteSize1New)
	  byteSize2 should equal (byteSize2New)
	  byteSize1.compareTo(byteSize1New) should be (0)
	  byteSize2.compareTo(byteSize2New) should be (0)

	//Convert byteSize1 first to another unit, then back and check for equality:
		val convByteSize2 = byteSize2.convert(GiB).convert(KB)
		convByteSize2 should equal (byteSize2)
	  }

	it should "be equal and comparable to another Value's size calculated in Bytes, " +
	  "using Binary IEC Metric (KiB, MiB, GiB) - used in RAM and OS internal calculation" in {
		Given("two ByteSize values, equal to bytesize3 in absolute value but with different Units")
		val byteSize2ConvToGB 	= new ByteSize(0.09765625, GiB)
		val byteSize2ConvToKiB 	= new ByteSize(102400, KiB)

	//Check for equality and comparability:
		When("bytesize3 and the two equal, but converted values are compared")
		Then("bytesize3 should bidirectionally be equal and comparable to both")

		byteSize3 should equal (byteSize2ConvToGB)
		byteSize2ConvToGB should equal (byteSize3)
		byteSize3.compareTo(byteSize2ConvToGB) should be (0)
		byteSize2ConvToGB.compareTo(byteSize3) should be (0)

		byteSize3 should equal (byteSize2ConvToKiB)
		byteSize2ConvToKiB should equal (byteSize3)
		byteSize3.compareTo(byteSize2ConvToKiB) should be (0)
		byteSize2ConvToKiB.compareTo(byteSize3) should be (0)

		byteSize2ConvToGB should equal (byteSize2ConvToKiB)
		byteSize2ConvToKiB should equal (byteSize2ConvToGB)
		byteSize2ConvToGB.compareTo(byteSize2ConvToKiB) should be (0)
		byteSize2ConvToKiB.compareTo(byteSize2ConvToGB) should be (0)

	//Convert byteSize1 first to byteSize2's unit and check for equality
		val convByteSize1 = byteSize2ConvToGB.convert(MiB)
		convByteSize1 should equal (byteSize3)
	}

	it should "be equal and comparable to another Value's size calculated in Bytes, " +
	  "using Decimal Metric (kB, MB, GB) - used in regular HDD calculation" in {
		val byteSize1 = new ByteSize(1, GB)
		val byteSize2 = new ByteSize(1000, MB)
		val byteSize3 = new ByteSize(1000000, KB)

	//Check for equality and comparability:
		byteSize1 should equal (byteSize2)
		byteSize2 should equal (byteSize1)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)

		byteSize1 should equal (byteSize3)
		byteSize3 should equal (byteSize1)
		byteSize1.compareTo(byteSize3) should be (0)
		byteSize3.compareTo(byteSize1) should be (0)

		byteSize2 should equal (byteSize3)
		byteSize3 should equal (byteSize2)
		byteSize2.compareTo(byteSize3) should be (0)
		byteSize3.compareTo(byteSize2) should be (0)

	//Convert byteSize1 first to byteSize2's unit and check for equality
		val convByteSize1 = byteSize1.convert(MB)
		convByteSize1 should equal (byteSize2)
	}

	it should "be equal and comparable between " +
	  "Decimal and Binary IEC Metric (KB vs. KiB, MB vs. MiB, GB vs. GiB)" in {
		val byteSize1 = new ByteSize(1000, KiB)
		val byteSize2 = new ByteSize(1024, KB)

		val byteSize3 = new ByteSize(100, MiB)
		val byteSize4 = new ByteSize(104.85759999999999, MB)

		val byteSize5 = new ByteSize(100, GiB)
		val byteSize6 = new ByteSize(107.37418240000001, GB)

	//Check for equality and comparability:
		byteSize1 should equal (byteSize2)
		byteSize2 should equal (byteSize1)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)

		byteSize3 should equal (byteSize4)
		byteSize4 should equal (byteSize3)
		byteSize3.compareTo(byteSize4) should be (0)
		byteSize4.compareTo(byteSize3) should be (0)

		byteSize5 should equal (byteSize6)
		byteSize6 should equal (byteSize5)
		byteSize5.compareTo(byteSize6) should be (0)
		byteSize6.compareTo(byteSize5) should be (0)

	//Convert byteSize1 first to byteSize2's unit and check for equality
		val convByteSize1 = byteSize1.convert(KB)
		convByteSize1 should equal (byteSize2)
	}

	it should "be fully serializable to and deserializable from XML" in{
		val xmlSerialBS1 = ByteSize.toXML(byteSize1)
		val xmlSerialBS2 = ByteSize.toXML(byteSize2)
		val xmlSerialBS3 = ByteSize.toXML(byteSize3)

		println("serialized ByteSize1 = " + xmlSerialBS1)
		println("serialized ByteSize2 = " + xmlSerialBS2)
		println("serialized ByteSize3 = " + xmlSerialBS3)

		val xmlDeserialBS1 = ByteSize.fromXML(xmlSerialBS1)
		val xmlDeserialBS2 = ByteSize.fromXML(xmlSerialBS2)
		val xmlDeserialBS3 = ByteSize.fromXML(xmlSerialBS3)

		byteSize1 shouldEqual xmlDeserialBS1
		byteSize2 shouldEqual xmlDeserialBS2
		byteSize3 shouldEqual xmlDeserialBS3
		byteSize3 should not equal xmlDeserialBS1
	}

	it should "be loadable from and saveable to a XML file" in{
		val xmlFile1 = new File(resDir.getAbsolutePath +"/ByteSize1.xml")
		val xmlFile2 = new File(resDir.getAbsolutePath +"/ByteSize2.xml")
		val xmlFile3 = new File(resDir.getAbsolutePath +"/ByteSize3.xml")
		ByteSize.saveToXML(xmlFile1, byteSize1)
		ByteSize.saveToXML(xmlFile2, byteSize2)
		ByteSize.saveToXML(xmlFile3, byteSize3)

		val loadedByteSize1 = ByteSize.loadFromXML(xmlFile1)
		val loadedByteSize2 = ByteSize.loadFromXML(xmlFile2)
		val loadedByteSize3 = ByteSize.loadFromXML(xmlFile3)

		println("byteSize1 = " + loadedByteSize1)
		println("byteSize2 = " + loadedByteSize2)
		println("byteSize3 = " + loadedByteSize3)

		byteSize1 shouldEqual loadedByteSize1
		byteSize2 shouldEqual loadedByteSize2
		byteSize3 shouldEqual loadedByteSize3
	}
}
