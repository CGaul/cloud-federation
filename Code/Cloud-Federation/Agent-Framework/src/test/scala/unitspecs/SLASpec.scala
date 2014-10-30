package unitspecs

import datatypes.CPU_Unit.CPU_Unit
import datatypes.{CPU_Unit, HostSLA, Img_Format}
import org.scalatest.{FlatSpec, ShouldMatchers}

/**
 * Created by costa on 10/28/14.
 */
class SLASpec extends FlatSpec with ShouldMatchers {
	"Two Hard SLAs" should "be amplifiable in a combination of them" in {
		val hardSLA1 = new HostSLA(0.9f, Vector(Img_Format.IMG, Img_Format.CLOOP),
											Vector[(CPU_Unit, Integer)]((CPU_Unit.SMALL, 2), (CPU_Unit.MEDIUM, 4)))
		val hardSLA2 = new HostSLA(0.99f, Vector(Img_Format.IMG, Img_Format.BOCHS),
			Vector[(CPU_Unit, Integer)]((CPU_Unit.SMALL, 1), (CPU_Unit.MEDIUM, 2)))

		val amplHardSLARequired = new HostSLA(0.99f, Vector(Img_Format.IMG, Img_Format.CLOOP, Img_Format.BOCHS),
			Vector[(CPU_Unit, Integer)]((CPU_Unit.SMALL, 1), (CPU_Unit.MEDIUM, 2)))
		val amplHardSLAResult 	= hardSLA1.combineToAmplifiedSLA(hardSLA2)

		amplHardSLAResult.equals(amplHardSLARequired) should be (true)
	}

}
