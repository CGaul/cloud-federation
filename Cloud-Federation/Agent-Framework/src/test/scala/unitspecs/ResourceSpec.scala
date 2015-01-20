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

/* Switch-Class Unit-Spec */
/* ====================== */

	behavior of "A Switch"

	// All Hosts are starting with SLAs that allow 10 SMALL, 5 MEDIUM and 2 LARGE VMs at the beginning:

	val switchDPID1 = DPID("00:00:00:00:00:00:01:00")
	val switchDPID2 = DPID("00:00:00:00:00:00:02:00")
	val switchDPID3 = DPID("00:00:00:00:00:00:03:00")
	
	val switch1 	= new OFSwitch(switchDPID1, Map(1.toShort->switchDPID2, 2.toShort->switchDPID3))
	val switch2 	= new OFSwitch(switchDPID2, Map(1.toShort->switchDPID1, 2.toShort->switchDPID3))
	val switch3 	= new OFSwitch(switchDPID3, Map(1.toShort->switchDPID1, 2.toShort->switchDPID2))

	it should "be equal equal to itself" in{
		switch1 should equal(switch1)
	}
	
	it should "be equal to another Switch with the same values" in{
		val switch1Comp = new OFSwitch(switchDPID1, Map(1.toShort->switchDPID1, 2.toShort->switchDPID2))
		switch1 should equal(switch1Comp)
	}
	
	it should "be fully serializable to and deserializable from XML" in{
		val xmlSerialSwitch1 = OFSwitch.toXML(switch1)
		val xmlSerialSwitch2 = OFSwitch.toXML(switch2)
		val xmlSerialSwitch3 = OFSwitch.toXML(switch3)

		println("serialized Switch1 = " + xmlSerialSwitch1)
		println("serialized Switch2 = " + xmlSerialSwitch2)
		println("serialized Switch3 = " + xmlSerialSwitch3)

		val xmlDeserialRes1 = OFSwitch.fromXML(xmlSerialSwitch1)
		val xmlDeserialRes2 = OFSwitch.fromXML(xmlSerialSwitch2)
		val xmlDeserialRes3 = OFSwitch.fromXML(xmlSerialSwitch3)

		switch1 shouldEqual xmlDeserialRes1
		switch2 shouldEqual xmlDeserialRes2
		switch3 shouldEqual xmlDeserialRes3
		switch3 should not equal xmlDeserialRes1
	}

	it should "be loadable from and saveable to a XML file" in{
		val xmlFile1 = new File(resDir.getAbsolutePath +"/Switch1.xml")
		val xmlFile2 = new File(resDir.getAbsolutePath +"/Switch2.xml")
		val xmlFile3 = new File(resDir.getAbsolutePath +"/Switch3.xml")
		OFSwitch.saveToXML(xmlFile1, switch1)
		OFSwitch.saveToXML(xmlFile2, switch2)
		OFSwitch.saveToXML(xmlFile3, switch3)

		val loadedSwitch1 = OFSwitch.loadFromXML(xmlFile1)
		val loadedSwitch2 = OFSwitch.loadFromXML(xmlFile2)
		val loadedSwitch3 = OFSwitch.loadFromXML(xmlFile3)

		println("switch1 = " + loadedSwitch1)
		println("switch2 = " + loadedSwitch2)
		println("switch3 = " + loadedSwitch3)

		switch1 should equal (loadedSwitch1)
		switch2 should equal (loadedSwitch2)
		switch3 should equal (loadedSwitch3)
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
	// host3 isres2

	val reqSLA1		= new HostSLA(0.90f, Vector(IMG, DMG),
															Vector[(CPUUnit, Int)]((SMALL, 3), (MEDIUM, 2)))
	val reqSLA2		= new HostSLA(0.91f, Vector(IMG, CLOOP, BOCHS),
															Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 1)))
	val reqSLA3		= new HostSLA(0.99f, Vector(IMG, CLOOP, QCOW2),
															Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 1)))

	val resAlloc1 = new ResourceAlloc(1, Vector(res1), reqSLA1)
	val resAlloc2 = new ResourceAlloc(1, Vector(res2), reqSLA2)
	val resAlloc3 = new ResourceAlloc(1, Vector(res3), reqSLA3)


	val host1 		= new Host(res2, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA1)
	val host2 		= new Host(res3, InetAddress.getByName("192.168.1.2"), "00:00:00:02", Vector(), hostSLA2)
	val host3 		= new Host(res4, InetAddress.getByName("192.168.1.3"), "00:00:00:03", Vector(), hostSLA3)



/* Test-Specs */
/* ========== */

	it should "be equal to itself, independent from the object creation (applied or instantiated)" in{
		When("host1 is directly compared to itself")
		Then("host1.equals(host1) should be true")
		host1.equals(host1) should be (right = true)
		host1 == host1 should be (right = true)

		Given("A Host with the host1 footprint, instantiated statically via apply()")
		val staticHost1 = Host(res2, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA1)

		When("host1 is compared with the statically applied staticHost1 copy")
		Then("host1.equals(staticHost1) should be true")
		host1.equals(staticHost1) should be (right = true)
		staticHost1.equals(host1) should be (right = true)
		host1 == staticHost1 should be (right = true)
		staticHost1 == host1 should be (right = true)
	}

	it should "be equal to another Host with the same Host footprint (even with different Resource Allocations)" in{

		Given("A Respource with the host1 footprint, with additional link descriptions")
		val equalHost1 = Host(res2, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(resAlloc1), hostSLA1)


		When("host1 is compared with the resourceAlloc-different equalHost1")
		Then("host1.equals(equalHost1) should be true")
		host1.equals(equalHost1) should be (right = true)
		equalHost1.equals(host1) should be (right = true)
		host1 == equalHost1 should be (right = true)
		equalHost1 == host1 should be (right = true)
		info("Equality tests completed!")
	}

	it should "be unequal to Hosts with a different Host footprint than the origin" in{
		When("Two unequal Hosts are compared")
		Then("equals should be false")
		host1.equals(host2) should be (right = false)
		host2.equals(host1) should be (right = false)
		host2.equals(host3) should be (right = false)
		host1 == host3 should be (right = false)
		host3 == host2 should be (right = false)
		host3 == host1 should be (right = false)
		info("Unequality tests completed!")
	}

	it should "be comparable with another Host in a relative ordering" in{
		When("host1 is compared to itself")
		Then("RelativeHostByResOrdering.compare(host1, host1) should be == 0")
		(RelativeHostByResOrdering.compare(host1, host1) == 0) should be (right = true)

		When("smaller host1 is compared to medium host2")
		Then("RelativeHostByResOrdering.compare(host1, host2) should be < 0")
		(RelativeHostByResOrdering.compare(host1, host2) < 0) should be (right = true)

		When("medium host2 is compared to smaller host1")
		Then("RelativeHostByResOrdering.compare(host2, host1) should be > 0")
		(RelativeHostByResOrdering.compare(host2, host1) > 0) should be (right = true)
		info("Comparison tests completed!")
	}

	it should "allocate new ResourceAllocations, when allocation is acceptable" in{
		// For Host1:
		When("resAlloc1 is allocated to host1")
		Then("host1.allocate(resAlloc1) should be true")
		And ("resAlloc1 should be in host1's allocatedResources")
		host1.allocate(resAlloc1)._1 should be (right = true)
		host1.allocatedResources.size should be (1)
		host1.allocatedResources.contains(resAlloc1) should be (right = true)
		host2.allocatedResources.contains(resAlloc2) should be (right = false)
		host2.allocatedResources.contains(resAlloc3) should be (right = false)

		When("resAlloc2 is allocated to host1")
		Then("host1.allocate(resAlloc2) should be false, as the requested resAlloc2's image-formats are not supported")
		And("resAlloc1 should still be in host1's allocatedResources")
		host1.allocate(resAlloc2)._1 should be (right = false)
		host1.allocatedResources.size should be (1)
		host1.allocatedResources.contains(resAlloc1) should be (right = true)
		host1.allocatedResources.contains(resAlloc2) should be (right = false)
		host1.allocatedResources.contains(resAlloc3) should be (right = false)

		When("resAlloc3 is allocated to host1")
		Then("host1.allocate(resAlloc3) should be false, as the resAlloc3 SLA uptime limit is above Host1's uptime possibility")
		And("resAlloc1 should still be in host1's allocatedResources")
		host1.allocate(resAlloc3)._1 should be (right = false)
		host1.allocatedResources.size should be (1)
		host1.allocatedResources.contains(resAlloc1) should be (right = true)
		host1.allocatedResources.contains(resAlloc2) should be (right = false)
		host1.allocatedResources.contains(resAlloc3) should be (right = false)


		// For Host2:
		When("resAlloc1 is allocated to host2")
		Then("host2.allocate(resAlloc1) should be false, as the requested resAlloc1's image-formats are not supported")
		And ("resAlloc1 should not be in host2's allocatedResources")
		host2.allocate(resAlloc1)._1 should be (right = false)
		host2.allocatedResources.size should be (0)
		host2.allocatedResources.contains(resAlloc1) should be (right = false)
		host2.allocatedResources.contains(resAlloc2) should be (right = false)
		host2.allocatedResources.contains(resAlloc3) should be (right = false)

		When("resAlloc2 is allocated to host2")
		Then("host2.allocate(resAlloc2) should be true")
		And("resAlloc2 should be in host2's allocatedResources")
		host2.allocate(resAlloc2)._1 should be (right = true)
		host2.allocatedResources.size should be (1)
		host2.allocatedResources.contains(resAlloc1) should be (right = false)
		host2.allocatedResources.contains(resAlloc2) should be (right = true)
		host2.allocatedResources.contains(resAlloc3) should be (right = false)

		When("resAlloc3 is allocated to host2")
		Then("host2.allocate(resAlloc3) should be false, as the resAlloc3 SLA uptime limit is above host2's uptime possibility")
		And("resAlloc1 should still be in host2's allocatedResources")
		host2.allocate(resAlloc3)._1 should be (right = false)
		host2.allocatedResources.size should be (1)
		host2.allocatedResources.contains(resAlloc1) should be (right = false)
		host2.allocatedResources.contains(resAlloc2) should be (right = true)
		host2.allocatedResources.contains(resAlloc3) should be (right = false)


		// For Host3:
		When("resAlloc1 is allocated to host3")
		Then("host3.allocate(resAlloc1) should be true")
		And ("resAlloc1 should be in host3's allocatedResources")
		host3.allocate(resAlloc1)._1 should be (right = true)
		host3.allocatedResources.size should be (1)
		host3.allocatedResources.contains(resAlloc1) should be (right = true)
		host3.allocatedResources.contains(resAlloc2) should be (right = false)
		host3.allocatedResources.contains(resAlloc3) should be (right = false)

		When("resAlloc2 is allocated to host3")
		Then("host3.allocate(resAlloc2) should be true, as there is only a SMALL Node in it")
		And("resAlloc2 should be in host3's allocatedResources")
		host3.allocate(resAlloc2)._1 should be (right = true)
		host3.allocatedResources.size should be (2)
		host3.allocatedResources.contains(resAlloc1) should be (right = true)
		host3.allocatedResources.contains(resAlloc2) should be (right = true)
		host3.allocatedResources.contains(resAlloc3) should be (right = false)

		When("resAlloc3 is allocated to host3")
		Then("host3.allocate(resAlloc3) should be false, as the current SLA prohibits to have more than one MEDIUM Node on this host")
		And("resAlloc3 should not be in host3's allocatedResources, but the other two should be")
		host3.allocate(resAlloc3)._1 should be (right = false)
		host3.allocatedResources.size should be (2)
		host3.allocatedResources.contains(resAlloc1) should be (right = true)
		host3.allocatedResources.contains(resAlloc2) should be (right = true)
		host3.allocatedResources.contains(resAlloc3) should be (right = false)
	}

	it should "be able to split ResourceAllocations, if the allocation would violate the" +
						"QoS, defined by the combined SLA" in {
		pending
		//TODO: check allocation split via host.allocate()
	}

	it should "be fully serializable to and deserializable from XML" in{

		val host1 		= new Host(res2, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA1)
		val host2 		= new Host(res3, InetAddress.getByName("192.168.1.2"), "00:00:00:02", Vector(resAlloc1), hostSLA2)
		val host3 		= new Host(res4, InetAddress.getByName("192.168.1.3"), "00:00:00:03", Vector(resAlloc2, resAlloc3), hostSLA3)

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
		val xmlFile1 = new File(resDir.getAbsolutePath +"/Host1.xml")
		val xmlFile2 = new File(resDir.getAbsolutePath +"/Host2.xml")
		val xmlFile3 = new File(resDir.getAbsolutePath +"/Host3.xml")
		Host.saveToXML(xmlFile1, host1)
		Host.saveToXML(xmlFile2, host2)
		Host.saveToXML(xmlFile3, host3)

		val loadedHost1 = Host.loadFromXML(xmlFile1)
		val loadedHost2 = Host.loadFromXML(xmlFile2)
		val loadedHost3 = Host.loadFromXML(xmlFile3)

		println("host1 = " + loadedHost1)
		println("host2 = " + loadedHost2)
		println("host3 = " + loadedHost3)

		host1 should equal (loadedHost1)
		host2 should equal (loadedHost2)
		host3 should equal (loadedHost3)
	}

}
