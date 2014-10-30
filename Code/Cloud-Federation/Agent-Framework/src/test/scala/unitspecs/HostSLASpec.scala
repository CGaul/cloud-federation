package unitspecs

import datatypes.CPUUnit.CPU_Unit
import datatypes.{CPUUnit, HostSLA, ImgFormat}
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
 * Created by costa on 10/28/14.
 */
class HostSLASpec extends FlatSpec with ShouldMatchers {

/* Definition of three HostSLAs */
/* ============================ */

	val hostSLA1 = new HostSLA(0.99f, Vector(ImgFormat.IMG, ImgFormat.QCOW2, ImgFormat.BOCHS),
										Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 4)))

	val hostSLA2 = new HostSLA(0.90f, Vector(ImgFormat.IMG, ImgFormat.BOCHS),
										Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 3)))

	val hostSLA3 = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.QCOW2),
										Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 2)))



/* Test-Specs */
/* ========== */

	"A HostSLA" should "be amplifiable in a combination of another HostSLA" in {


		val combinedHostSLA1_required = new HostSLA(0.99f, Vector(ImgFormat.IMG, ImgFormat.QCOW2, ImgFormat.BOCHS),
																  Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 3)))
		val combinedHostSLA1_result 	= hostSLA1.combineToAmplifiedSLA(hostSLA2)

		val combinedHostSLA2_required = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.QCOW2, ImgFormat.BOCHS),
																  Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 2)))
		val combinedHostSLA2_result 	= hostSLA2.combineToAmplifiedSLA(hostSLA3)

		combinedHostSLA1_result.equals(combinedHostSLA1_required) should be (true)
		combinedHostSLA2_result.equals(combinedHostSLA2_required) should be (true)
	}

	it should "fulfill another HostSLA's requested QoS, if every value is better in regards of QoS" in{
		hostSLA1.fulfillsQoS(hostSLA2) should be (true)
		hostSLA1.fulfillsQoS(hostSLA3) should be (true)
		hostSLA2.fulfillsQoS(hostSLA1) should be (false)
		hostSLA3.fulfillsQoS(hostSLA1) should be (false)
		hostSLA2.fulfillsQoS(hostSLA3) should be (false)
		hostSLA3.fulfillsQoS(hostSLA2) should be (false)
	}

}
