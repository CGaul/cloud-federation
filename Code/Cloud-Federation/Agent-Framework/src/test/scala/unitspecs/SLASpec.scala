package unitspecs

import datatypes.ByteUnit.GB
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes.CloudCurrency._
import datatypes._
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
 * @author Constantin Gaul, created on 10/28/14.
 */
class SLASpec extends FlatSpec with ShouldMatchers
{
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

	"A HostSLA" should "be equal to itself and another HostSLA with the same Vectors (in arbitrary element order)" in{
		val hostSLA1Rebuild = new HostSLA(0.99f, Vector(QCOW2, IMG, BOCHS,  CLOOP),
													 Vector[(CPUUnit, Int)]((MEDIUM, 4), (SMALL, 2)))

		// Test equality via equals...
		hostSLA1 shouldEqual hostSLA1
		hostSLA1 shouldEqual hostSLA1Rebuild
		hostSLA1Rebuild shouldEqual hostSLA1

		// ... as well as via ==
		hostSLA1 == hostSLA1 should be(true)
		hostSLA1 == hostSLA1Rebuild should be(true)
		hostSLA1Rebuild == hostSLA1 should be(true)

		//not-equal tests:
		hostSLA1.equals(hostSLA2) should be(false)
		hostSLA2.equals(hostSLA1) should be(false)
		hostSLA2.equals(hostSLA3) should be(false)
		hostSLA3.equals(hostSLA2) should be(false)
		info("Equality Tests passed.")
	}

	it should "be able to handle duplicate input, whenever possible" in {
		val hostSLA_toTest 	= new HostSLA(0.99f, Vector(QCOW2, QCOW2, BOCHS),
													  Vector[(CPUUnit, Int)]((MEDIUM, 4), (MEDIUM, 2), (LARGE, 3)))
		val hostSLA_required = new HostSLA(0.99f, Vector(QCOW2, BOCHS),
													  Vector[(CPUUnit, Int)]((LARGE, 3)))

		hostSLA_toTest.equals(hostSLA_required) should be(true)
		info("Duplicate minimization Tests passed.")
	}

	it should "be amplifiable in a combination of another HostSLA" in {
		val reqHostSLA1 = new HostSLA(0.99f, Vector(IMG, QCOW2, BOCHS, CLOOP),
													 			  Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3), (LARGE, 1)))

		val reqHostSLA2 = new HostSLA(0.95f, Vector(IMG, CLOOP, BOCHS, QCOW2),
													 			  Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 2), (LARGE, 1)))


		(hostSLA1 combineToAmplifiedSLA hostSLA2).equals(reqHostSLA1) should be (true)
		(hostSLA2 combineToAmplifiedSLA hostSLA1).equals(reqHostSLA1) should be (true)

		(hostSLA2 combineToAmplifiedSLA hostSLA3).equals(reqHostSLA2) should be (true)
		(hostSLA3 combineToAmplifiedSLA hostSLA2).equals(reqHostSLA2) should be (true)
		info("HostSLA.combineToAmplifiedSLA(other) Tests passed.")
	}

	it should "fulfill another HostSLA's requested QoS, if every value is better in regards of QoS" in{
		hostSLA1.fulfillsQoS(hostSLA2) should be (true)
		hostSLA1.fulfillsQoS(hostSLA3) should be (true)
		hostSLA2.fulfillsQoS(hostSLA1) should be (false)
		hostSLA3.fulfillsQoS(hostSLA1) should be (false)
		hostSLA2.fulfillsQoS(hostSLA3) should be (false)
		hostSLA3.fulfillsQoS(hostSLA2) should be (false)
		info("HostSLA.fullfillsQoS(other) Tests passed.")
	}


	it should "be fully serializable to XML" in{
		val xmlSerialHostSLA = HostSLA.toXML(hostSLA1)
		println("serialized hostSLA1 = " + xmlSerialHostSLA)

		val xmlDeserialHostSLA = HostSLA.fromXML(xmlSerialHostSLA)
		println("deserialized hostSLA1 = " + xmlDeserialHostSLA)

		hostSLA1 == xmlDeserialHostSLA should be (true)
	}



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

	"A CloudSLA" should "be equal to itself and another CloudSLA with the same Vectors (in arbitrary element order)" in {

		// Test equality via equals...
		cloudSLA1 shouldEqual cloudSLA1

		// ... as well as via ==
		cloudSLA1 == cloudSLA1 should be(true)

		//not-equal tests:
		cloudSLA1.equals(cloudSLA2) should be(false)
		cloudSLA2.equals(cloudSLA1) should be(false)
		cloudSLA2.equals(cloudSLA3) should be(false)
		cloudSLA3.equals(cloudSLA2) should be(false)
		info("Equality Tests passed.")
	}


	it should "be fully serializable to XML" in{
		val xmlSerialCloudSLA1 = CloudSLA.toXML(cloudSLA1)
		println("serialized cloudSLA1 = " + xmlSerialCloudSLA1)

		val xmlDeserialCloudSLA1 = CloudSLA.fromXML(xmlSerialCloudSLA1)
		println("deserialized cloudSLA1 = " + xmlDeserialCloudSLA1)

		cloudSLA1 == xmlDeserialCloudSLA1 should be (true)
	}
}
