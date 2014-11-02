package unitspecs

import datatypes.CPUUnit.CPUUnit
import datatypes.{CPUUnit, HostSLA, ImgFormat}
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
 * @author Constantin Gaul, created on 10/28/14.
 */
class HostSLASpec extends FlatSpec with ShouldMatchers
{

/* Definition of three HostSLAs */
/* ============================ */

	val hostSLA1 = new HostSLA(0.99f, Vector(ImgFormat.IMG, ImgFormat.QCOW2, ImgFormat.BOCHS),
										Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 4)))

	val hostSLA2 = new HostSLA(0.90f, Vector(ImgFormat.IMG, ImgFormat.BOCHS),
										Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 3)))

	val hostSLA3 = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.QCOW2),
										Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 2)))



/* Test-Specs */
/* ========== */

	"A HostSLA" should "be equal to itself and another HostSLA with the same Vectors (in arbitrary element order)" in{
		val hostSLA1Rebuild = new HostSLA(0.99f, Vector(ImgFormat.QCOW2, ImgFormat.IMG, ImgFormat.BOCHS),
													 Vector[(CPUUnit, Int)]((CPUUnit.MEDIUM, 4), (CPUUnit.SMALL, 2)))

		// Test equality via equals...
		hostSLA1.equals(hostSLA1) should be(true)
		hostSLA1.equals(hostSLA1Rebuild) should be(true)
		hostSLA1Rebuild.equals(hostSLA1) should be(true)

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
		val hostSLA_toTest 	= new HostSLA(0.99f, Vector(ImgFormat.QCOW2, ImgFormat.QCOW2, ImgFormat.BOCHS),
													  Vector[(CPUUnit, Int)]((CPUUnit.MEDIUM, 4), (CPUUnit.MEDIUM, 2), (CPUUnit.LARGE, 3)))
		val hostSLA_required = new HostSLA(0.99f, Vector(ImgFormat.QCOW2, ImgFormat.BOCHS),
													  Vector[(CPUUnit, Int)]((CPUUnit.LARGE, 3)))

		hostSLA_toTest.equals(hostSLA_required) should be(true)
		info("Duplicate minimization Tests passed.")
	}

	it should "be amplifiable in a combination of another HostSLA" in {
		val combinedHostSLA1_required = new HostSLA(0.99f, Vector(ImgFormat.IMG, ImgFormat.QCOW2, ImgFormat.BOCHS),
													 			  Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 3)))
		val combinedHostSLA1_result 	= hostSLA1.combineToAmplifiedSLA(hostSLA2)

		val combinedHostSLA2_required = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.QCOW2, ImgFormat.BOCHS),
													 			  Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 2)))
		val combinedHostSLA2_result 	= hostSLA2.combineToAmplifiedSLA(hostSLA3)

		combinedHostSLA1_result.equals(combinedHostSLA1_required) should be (true)
		combinedHostSLA2_result.equals(combinedHostSLA2_required) should be (true)
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

}
