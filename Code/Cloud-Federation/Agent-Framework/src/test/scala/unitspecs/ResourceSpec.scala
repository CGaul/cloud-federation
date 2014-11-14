package unitspecs

import java.io.File

import datatypes.ByteUnit.{MB, GiB}
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes._
import org.scalatest.{Inspectors, Matchers, GivenWhenThen, FlatSpec}

/**
 * @author Constantin Gaul, created on 10/28/14.
 */
class ResourceSpec extends FlatSpec with Matchers with GivenWhenThen with Inspectors
{
/* Resource-Class Unit-Spec */
/* ======================== */

	behavior of "A Resource"

	//General Small Node
	val res1 = new Resource(NodeID(1), SMALL,
									ByteSize(8, GiB), ByteSize(50, GiB),
									ByteSize(10, MB), 10, Vector())

	//General Medium Node
	val res2= new Resource(NodeID(2), MEDIUM,
									ByteSize(16, GiB), ByteSize(100, GiB),
									ByteSize(10, MB), 10, Vector())

	//Equals res2, but with lower latency
	val res3 = new Resource(NodeID(3), MEDIUM,
									ByteSize(16, GiB), ByteSize(100, GiB),
									ByteSize(10, MB), 5, Vector())

	//General Large Node
	val res4 = new Resource(NodeID(3), LARGE,
									ByteSize(24, GiB), ByteSize(200, GiB),
									ByteSize(50, MB), 10, Vector())


/* Test-Specs */
/* ========== */

	it should "be equal to itself, independent from the object creation (applied or instantiated)" in {
		When("res1 is directly compared to itself")
		Then("res1.equals(res1) should be true")
		res1.equals(res1) should be(true)
		res1 == res1 should be(true)


		Given("A Resource with the res1 footprint, instantiated statically via apply()")
		val staticRes1 = Resource(NodeID(1), SMALL,
			ByteSize(8, GiB), ByteSize(50, GiB),
			ByteSize(10, MB), 10, Vector())

		When("res1 is compared with the statically applied staticRes1 copy")
		Then("res1.equals(staticRes1) should be true")
		res1.equals(staticRes1) should be(true)
		staticRes1.equals(res1) should be(true)
		res1 == staticRes1 should be(true)
		staticRes1 == res1 should be(true)
	}
	
	it should  "be equal to another Resource with the same Resource footprint (even with different IDs and Neighbours)" in{

		Given("A Respource with the res1 footprint, with additional link descriptions")
		val equalRes1 		= Resource(NodeID(1), SMALL,
											ByteSize(8, GiB), ByteSize(50, GiB),
											ByteSize(10, MB), 10, Vector(NodeID(2), NodeID(3)))


		When("res1 is compared with the link-different equalRes1")
		Then("res1.equals(equalRes1) should be true")
		res1.equals(equalRes1) should be (true)
		equalRes1.equals(res1) should be (true)
		res1 == equalRes1 should be (true)
		equalRes1 == res1 should be (true)
		info("Equality tests completed!")
	}

	it should "be unequal to Resources with a different Resource footprint than the origin" in{
		When("Two unequal resources are compared")
		Then("equals should be false")
		res1.equals(res2) should be (false)
		res2.equals(res1) should be (false)
		res1 == res3 should be (false)
		res3 == res2 should be (false)
		res3 == res1 should be (false)
		info("Unequality tests completed!")
	}

	it should "be comparable with another Resource in a relative ordering" in{
		When("res1 is compared to itself")
		Then("RelativeOrdering.compare(res1, res1) should be == 0")
		(RelativeResOrdering.compare(res1, res1) == 0) should be (true)

		When("smaller res1 is compared to medium res2")
		Then("RelativeResOrdering.compare(res1, res2) should be < 0")
		(RelativeResOrdering.compare(res1, res2) < 0) should be (true)

		When("medium res2 is compared to smaller res1")
		Then("RelativeResOrdering.compare(res2, res1) should be > 0")
		(RelativeResOrdering.compare(res2, res1) > 0) should be (true)
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




/* Host-Class Unit-Spec */
/* ==================== */

	behavior of "A Host"

	// All Hosts are starting with SLAs that allow 10 SMALL, 5 MEDIUM and 2 LARGE VMs at the beginning:

	val hostSLA1 	= new HostSLA(0.91f, Vector(IMG, DMG, CLOOP, QCOW2),
									  	  			Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 5), (LARGE, 2)))

	val hostSLA2 	= new HostSLA(0.91f, Vector(IMG, CLOOP, BOCHS),
															Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 5), (LARGE, 2)))

	val hostSLA3 	= new HostSLA(0.99f, Vector(IMG, DMG, CLOOP, BOCHS, QCOW2),
															Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 5), (LARGE, 2)))

	// The required SLAs for the resource-allocation are mapped as follows:
	// host1 can only allocate by the resourceAlloc with reqSLA1
	// host2 can only allocate by the resourceAlloc with reqSLA2
	// host3 is able to allocate all three resourceAllocs, however should fail due to VMs per CPU limitations
	val reqSLA1		= new HostSLA(0.90f, Vector(IMG, DMG),
															Vector[(CPUUnit, Int)]((SMALL, 3), (MEDIUM, 2)))
	val reqSLA2		= new HostSLA(0.91f, Vector(IMG, CLOOP, BOCHS),
															Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 1)))
	val reqSLA3		= new HostSLA(0.99f, Vector(IMG, CLOOP, QCOW2),
															Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 1)))

	val resAlloc1 = new ResourceAlloc(1, Vector(res1), reqSLA1)
	val resAlloc2 = new ResourceAlloc(1, Vector(res2), reqSLA2)
	val resAlloc3 = new ResourceAlloc(1, Vector(res3), reqSLA3)


	val host1 		= new Host(res2, Vector(), hostSLA1)
	val host2 		= new Host(res3, Vector(), hostSLA2)
	val host3 		= new Host(res4, Vector(), hostSLA3)



/* Test-Specs */
/* ========== */

	it should "be equal to itself, independent from the object creation (applied or instantiated)" in{
		When("host1 is directly compared to itself")
		Then("host1.equals(host1) should be true")
		host1.equals(host1) should be(true)
		host1 == host1 should be(true)

		Given("A Host with the host1 footprint, instantiated statically via apply()")
		val staticHost1 = Host(res2, Vector(), hostSLA1)

		When("host1 is compared with the statically applied staticHost1 copy")
		Then("host1.equals(staticHost1) should be true")
		host1.equals(staticHost1) should be(true)
		staticHost1.equals(host1) should be(true)
		host1 == staticHost1 should be(true)
		staticHost1 == host1 should be(true)
	}

	it should "be equal to another Host with the same Host footprint (even with different Resource Allocations)" in{

		Given("A Respource with the host1 footprint, with additional link descriptions")
		val equalHost1 = Host(res2, Vector(resAlloc1), hostSLA1)


		When("host1 is compared with the resourceAlloc-different equalHost1")
		Then("host1.equals(equalHost1) should be true")
		host1.equals(equalHost1) should be (true)
		equalHost1.equals(host1) should be (true)
		host1 == equalHost1 should be (true)
		equalHost1 == host1 should be (true)
		info("Equality tests completed!")
	}

	it should "be unequal to Hosts with a different Host footprint than the origin" in{
		When("Two unequal Hosts are compared")
		Then("equals should be false")
		host1.equals(host2) should be (false)
		host2.equals(host1) should be (false)
		host2.equals(host3) should be (false)
		host1 == host3 should be (false)
		host3 == host2 should be (false)
		host3 == host1 should be (false)
		info("Unequality tests completed!")
	}

	it should "be comparable with another Host in a relative ordering" in{
		When("host1 is compared to itself")
		Then("RelativeHostByResOrdering.compare(host1, host1) should be == 0")
		(RelativeHostByResOrdering.compare(host1, host1) == 0) should be (true)

		When("smaller host1 is compared to medium host2")
		Then("RelativeHostByResOrdering.compare(host1, host2) should be < 0")
		(RelativeHostByResOrdering.compare(host1, host2) < 0) should be (true)

		When("medium host2 is compared to smaller host1")
		Then("RelativeHostByResOrdering.compare(host2, host1) should be > 0")
		(RelativeHostByResOrdering.compare(host2, host1) > 0) should be (true)
		info("Comparison tests completed!")
	}

	it should "allocate new ResourceAllocations, when allocation is acceptable" in{
		// For Host1:
		When("resAlloc1 is allocated to host1")
		Then("host1.allocate(resAlloc1) should be true")
		And ("resAlloc1 should be in host1's allocatedResources")
		host1.allocate(resAlloc1)._1 should be (true)
		host1.allocatedResources.size should be (1)
		host1.allocatedResources.contains(resAlloc1) should be (true)
		host2.allocatedResources.contains(resAlloc2) should be (false)
		host2.allocatedResources.contains(resAlloc3) should be (false)

		When("resAlloc2 is allocated to host1")
		Then("host1.allocate(resAlloc2) should be false, as the requested resAlloc2's image-formats are not supported")
		And("resAlloc1 should still be in host1's allocatedResources")
		host1.allocate(resAlloc2)._1 should be (false)
		host1.allocatedResources.size should be (1)
		host1.allocatedResources.contains(resAlloc1) should be (true)
		host1.allocatedResources.contains(resAlloc2) should be (false)
		host1.allocatedResources.contains(resAlloc3) should be (false)

		When("resAlloc3 is allocated to host1")
		Then("host1.allocate(resAlloc3) should be false, as the resAlloc3 SLA uptime limit is above Host1's uptime possibility")
		And("resAlloc1 should still be in host1's allocatedResources")
		host1.allocate(resAlloc3)._1 should be (false)
		host1.allocatedResources.size should be (1)
		host1.allocatedResources.contains(resAlloc1) should be (true)
		host1.allocatedResources.contains(resAlloc2) should be (false)
		host1.allocatedResources.contains(resAlloc3) should be (false)


		// For Host2:
		When("resAlloc1 is allocated to host2")
		Then("host2.allocate(resAlloc1) should be false, as the requested resAlloc1's image-formats are not supported")
		And ("resAlloc1 should not be in host2's allocatedResources")
		host2.allocate(resAlloc1)._1 should be (false)
		host2.allocatedResources.size should be (0)
		host2.allocatedResources.contains(resAlloc1) should be (false)
		host2.allocatedResources.contains(resAlloc2) should be (false)
		host2.allocatedResources.contains(resAlloc3) should be (false)

		When("resAlloc2 is allocated to host2")
		Then("host2.allocate(resAlloc2) should be true")
		And("resAlloc2 should be in host2's allocatedResources")
		host2.allocate(resAlloc2)._1 should be (true)
		host2.allocatedResources.size should be (1)
		host2.allocatedResources.contains(resAlloc1) should be (false)
		host2.allocatedResources.contains(resAlloc2) should be (true)
		host2.allocatedResources.contains(resAlloc3) should be (false)

		When("resAlloc3 is allocated to host2")
		Then("host2.allocate(resAlloc3) should be false, as the resAlloc3 SLA uptime limit is above host2's uptime possibility")
		And("resAlloc1 should still be in host2's allocatedResources")
		host2.allocate(resAlloc3)._1 should be (false)
		host2.allocatedResources.size should be (1)
		host2.allocatedResources.contains(resAlloc1) should be (false)
		host2.allocatedResources.contains(resAlloc2) should be (true)
		host2.allocatedResources.contains(resAlloc3) should be (false)


		// For Host3:
		When("resAlloc1 is allocated to host3")
		Then("host3.allocate(resAlloc1) should be true")
		And ("resAlloc1 should be in host3's allocatedResources")
		host3.allocate(resAlloc1)._1 should be (true)
		host3.allocatedResources.size should be (1)
		host3.allocatedResources.contains(resAlloc1) should be (true)
		host3.allocatedResources.contains(resAlloc2) should be (false)
		host3.allocatedResources.contains(resAlloc3) should be (false)

		When("resAlloc2 is allocated to host3")
		Then("host3.allocate(resAlloc2) should be true, as there is only a SMALL Node in it")
		And("resAlloc2 should be in host3's allocatedResources")
		host3.allocate(resAlloc2)._1 should be (true)
		host3.allocatedResources.size should be (2)
		host3.allocatedResources.contains(resAlloc1) should be (true)
		host3.allocatedResources.contains(resAlloc2) should be (true)
		host3.allocatedResources.contains(resAlloc3) should be (false)

		When("resAlloc3 is allocated to host3")
		Then("host3.allocate(resAlloc3) should be false, as the current SLA prohibits to have more than one MEDIUM Node on this host")
		And("resAlloc3 should not be in host3's allocatedResources, but the other two should be")
		host3.allocate(resAlloc3)._1 should be (false)
		host3.allocatedResources.size should be (2)
		host3.allocatedResources.contains(resAlloc1) should be (true)
		host3.allocatedResources.contains(resAlloc2) should be (true)
		host3.allocatedResources.contains(resAlloc3) should be (false)
	}

	it should "be able to split ResourceAllocations, if the allocation would violate the" +
						"QoS, defined by the combined SLA" in {
		pending
		//TODO: check allocation split via host.allocate()
	}

	it should "be fully serializable to and deserializable from XML" in{

		val host1 		= new Host(res2, Vector(), hostSLA1)
		val host2 		= new Host(res3, Vector(resAlloc1), hostSLA2)
		val host3 		= new Host(res4, Vector(resAlloc2, resAlloc3), hostSLA3)

		val xmlSerialHost1 = Host.toXML(host1)
		val xmlSerialHost2 = Host.toXML(host2)
		val xmlSerialHost3 = Host.toXML(host3)

		val xmlDeserialHost1 = Host.fromXML(xmlSerialHost1)
		val xmlDeserialHost2 = Host.fromXML(xmlSerialHost2)
		val xmlDeserialHost3 = Host.fromXML(xmlSerialHost3)

		host1 shouldEqual xmlDeserialHost1
		host2 shouldEqual xmlDeserialHost2
		host3 shouldEqual xmlDeserialHost3
		host3 should not equal xmlDeserialHost1
	}

	it should "be loadable from and saveable to a XML file" in{
		val xmlFile1 = new File("Agent-Framework/src/test/resources/Host1.xml")
		val xmlFile2 = new File("Agent-Framework/src/test/resources/Host2.xml")
		val xmlFile3 = new File("Agent-Framework/src/test/resources/Host3.xml")
		Host.saveToXML(xmlFile1, host1)
		Host.saveToXML(xmlFile2, host2)
		Host.saveToXML(xmlFile3, host3)

		val loadedHost1 = Host.loadFromXML(xmlFile1)
		val loadedHost2 = Host.loadFromXML(xmlFile2)
		val loadedHost3 = Host.loadFromXML(xmlFile3)
		val loadedHost1Rewritten = Host.loadFromXML(new File("Agent-Framework/src/test/resources/resconfig/Host1.xml"))

		println("host1 = " + loadedHost1)
		println("host1 = " + loadedHost1Rewritten)
		println("host2 = " + loadedHost2)
		println("host3 = " + loadedHost3)

		host1 should equal (loadedHost1)
		host1 should equal (loadedHost1Rewritten)
		host2 should equal (loadedHost2)
		host3 should equal (loadedHost3)
	}

}
