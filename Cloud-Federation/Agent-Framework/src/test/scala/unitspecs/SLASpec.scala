package unitspecs

import java.io.File

import datatypes.ByteUnit.GB
import datatypes.CPUUnit._
import datatypes.CloudCurrency._
import datatypes.ImgFormat._
import datatypes._
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
 * @author Constantin Gaul, created on 10/28/14.
 */
class SLASpec extends FlatSpec with ShouldMatchers
{

	// Create the resources-dir in Agent-Framework Module,
	// if not already existent:
	val resDir = new File("Agent-Framework/src/test/resources/")
	if(! resDir.exists()){
		resDir.mkdirs()
	}



/* HostSLA-Class Unit-Spec */
/* ======================= */

	behavior of "A HostSLA"


/* Definition of three HostSLAs */
/* ============================ */

	val hostSLA1 = new HostSLA(0.99f, Vector(IMG, QCOW2, BOCHS, CLOOP),
										Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 4)))

	val hostSLA2 = new HostSLA(0.90f, Vector(IMG, BOCHS, CLOOP),
										Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3), (LARGE, 1)))

	val hostSLA3 = new HostSLA(0.95f, Vector(IMG, QCOW2),
										Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 2)))



/* Test-Specs */
/* ========== */

	it should "be equal to itself and another HostSLA with the same Vectors (in arbitrary element order)" in{
		val hostSLA1Rebuild = new HostSLA(0.99f, Vector(QCOW2, IMG, BOCHS,  CLOOP),
													 Vector[(CPUUnit, Int)]((MEDIUM, 4), (SMALL, 2)))

		// Test equality via equals...
		hostSLA1 shouldEqual hostSLA1
		hostSLA1 shouldEqual hostSLA1Rebuild
		hostSLA1Rebuild shouldEqual hostSLA1

		// ... as well as via ==
		hostSLA1 == hostSLA1 should be(right = true)
		hostSLA1 == hostSLA1Rebuild should be(right = true)
		hostSLA1Rebuild == hostSLA1 should be(right = true)

		//not-equal tests:
		hostSLA1.equals(hostSLA2) should be(right = false)
		hostSLA2.equals(hostSLA1) should be(right = false)
		hostSLA2.equals(hostSLA3) should be(right = false)
		hostSLA3.equals(hostSLA2) should be(right = false)
		info("Equality Tests passed.")
	}

	it should "be able to handle duplicate input, whenever possible" in {
		val hostSLA_toTest 	= new HostSLA(0.99f, Vector(QCOW2, QCOW2, BOCHS),
													  Vector[(CPUUnit, Int)]((MEDIUM, 4), (MEDIUM, 2), (LARGE, 3)))
		val hostSLA_required = new HostSLA(0.99f, Vector(QCOW2, BOCHS),
													  Vector[(CPUUnit, Int)]((LARGE, 3)))

		hostSLA_toTest.equals(hostSLA_required) should be(right = true)
		info("Duplicate minimization Tests passed.")
	}

	it should "be amplifiable in a combination of another HostSLA" in {
		val reqHostSLA1 = new HostSLA(0.99f, Vector(IMG, QCOW2, BOCHS, CLOOP),
													 			  Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3), (LARGE, 1)))

		val reqHostSLA2 = new HostSLA(0.95f, Vector(IMG, CLOOP, BOCHS, QCOW2),
													 			  Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 2), (LARGE, 1)))


		(hostSLA1 combineToAmplifiedSLA hostSLA2).equals(reqHostSLA1) should be (right = true)
		(hostSLA2 combineToAmplifiedSLA hostSLA1).equals(reqHostSLA1) should be (right = true)

		(hostSLA2 combineToAmplifiedSLA hostSLA3).equals(reqHostSLA2) should be (right = true)
		(hostSLA3 combineToAmplifiedSLA hostSLA2).equals(reqHostSLA2) should be (right = true)
		info("HostSLA.combineToAmplifiedSLA(other) Tests passed.")
	}

	it should "fulfill another HostSLA's requested QoS, if every value is better in regards of QoS" in{
		hostSLA1.fulfillsQoS(hostSLA2) should be (right = true)
		hostSLA1.fulfillsQoS(hostSLA3) should be (right = true)
		hostSLA2.fulfillsQoS(hostSLA1) should be (right = false)
		hostSLA3.fulfillsQoS(hostSLA1) should be (right = false)
		hostSLA2.fulfillsQoS(hostSLA3) should be (right = false)
		hostSLA3.fulfillsQoS(hostSLA2) should be (right = false)
		info("HostSLA.fullfillsQoS(other) Tests passed.")
	}


	it should "be fully serializable to and deserializable from XML" in{
		val xmlSerialHostSLA = HostSLA.toXML(hostSLA1)
		println("serialized hostSLA1 = " + xmlSerialHostSLA)

		val xmlDeserialHostSLA = HostSLA.fromXML(xmlSerialHostSLA)
		println("deserialized hostSLA1 = " + xmlDeserialHostSLA)

		hostSLA1 == xmlDeserialHostSLA should be (right = true)
	}

	it should "be loadable from and saveable to a XML file" in{
		val xmlFile1 = new File(resDir.getAbsolutePath +"/HostSLA1.xml")
		val xmlFile2 = new File(resDir.getAbsolutePath +"/HostSLA2.xml")
		val xmlFile3 = new File(resDir.getAbsolutePath +"/HostSLA3.xml")
		HostSLA.saveToXML(xmlFile1, hostSLA1)
		HostSLA.saveToXML(xmlFile2, hostSLA2)
		HostSLA.saveToXML(xmlFile3, hostSLA3)

		val loadedHostSLA1 = HostSLA.loadFromXML(xmlFile1)
		val loadedHostSLA2 = HostSLA.loadFromXML(xmlFile2)
		val loadedHostSLA3 = HostSLA.loadFromXML(xmlFile3)

		println("loadedHostSLA1 = " + loadedHostSLA1)
		println("loadedHostSLA2 = " + loadedHostSLA2)
		println("loadedHostSLA3 = " + loadedHostSLA3)

		hostSLA1 shouldEqual loadedHostSLA1
		hostSLA2 shouldEqual loadedHostSLA2
		hostSLA3 shouldEqual loadedHostSLA3
	}




/* CloudSLA-Class Unit-Spec */
/* ======================== */

	behavior of "A CloudSLA"


	/* Definition of three CloudSLAs */
	/* ============================= */

	//TODO: fully define CloudSLAs:
	val cloudSLA1 = new CloudSLA(Vector((SMALL, Price(1, CLOUD_CREDIT), Price(3, CLOUD_CREDIT))),
															(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)),
															(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)))

	val cloudSLA2 = new CloudSLA(Vector((SMALL, Price(0.8f, CLOUD_CREDIT), Price(1.4f, CLOUD_CREDIT))),
															(ByteSize(1, GB), Price(0.4f, CLOUD_CREDIT), Price(1f, CLOUD_CREDIT)),
															(ByteSize(1, GB), Price(0.4f, CLOUD_CREDIT), Price(1f, CLOUD_CREDIT)))

	val cloudSLA3 = new CloudSLA(Vector((SMALL, Price(0.6f, CLOUD_CREDIT), Price(2.0f, CLOUD_CREDIT))),
															(ByteSize(1, GB), Price(0.2f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)),
															(ByteSize(1, GB), Price(0.2f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)))

	/* Test-Specs */
	/* ========== */

	it should "be equal to itself and another CloudSLA with the same Vectors (in arbitrary element order)" in {

		// Test equality via equals...
		cloudSLA1 shouldEqual cloudSLA1

		// ... as well as via ==
		cloudSLA1 == cloudSLA1 should be(right = true)

		//not-equal tests:
		cloudSLA1.equals(cloudSLA2) should be(right = false)
		cloudSLA2.equals(cloudSLA1) should be(right = false)
		cloudSLA2.equals(cloudSLA3) should be(right = false)
		cloudSLA3.equals(cloudSLA2) should be(right = false)
		info("Equality Tests passed.")
	}


	it should "be fully serializable to and deserializable from XML" in{
		val xmlSerialCloudSLA1 = CloudSLA.toXML(cloudSLA1)
		println("serialized cloudSLA1 = " + xmlSerialCloudSLA1)

		val xmlDeserialCloudSLA1 = CloudSLA.fromXML(xmlSerialCloudSLA1)
		println("deserialized cloudSLA1 = " + xmlDeserialCloudSLA1)

		cloudSLA1 shouldEqual xmlDeserialCloudSLA1
	}

	it should "be loadable from and saveable to a XML file" in{
		val xmlFile1 = new File(resDir.getAbsolutePath +"/CloudSLA1.xml")
		val xmlFile2 = new File(resDir.getAbsolutePath +"/CloudSLA2.xml")
		val xmlFile3 = new File(resDir.getAbsolutePath +"/CloudSLA3.xml")
		CloudSLA.saveToXML(xmlFile1, cloudSLA1)
		CloudSLA.saveToXML(xmlFile2, cloudSLA2)
		CloudSLA.saveToXML(xmlFile3, cloudSLA3)

		val loadedCloudSLA1 = CloudSLA.loadFromXML(xmlFile1)
		val loadedCloudSLA2 = CloudSLA.loadFromXML(xmlFile2)
		val loadedCloudSLA3 = CloudSLA.loadFromXML(xmlFile3)

		println("loadedCloudSLA1 = " + loadedCloudSLA1)
		println("loadedCloudSLA2 = " + loadedCloudSLA2)
		println("loadedCloudSLA3 = " + loadedCloudSLA3)

		cloudSLA1 shouldEqual loadedCloudSLA1
		cloudSLA2 shouldEqual loadedCloudSLA2
		cloudSLA3 shouldEqual loadedCloudSLA3
	}
}
