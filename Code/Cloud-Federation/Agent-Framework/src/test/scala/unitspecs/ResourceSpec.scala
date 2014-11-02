package unitspecs

import datatypes.CPUUnit
import datatypes.CPUUnit._
import datatypes._
import org.scalatest.{GivenWhenThen, FlatSpec, ShouldMatchers}

/**
 * @author Cosntantin Gaul, created on 10/28/14.
 */
class ResourceSpec extends FlatSpec with ShouldMatchers with GivenWhenThen
{
/* Resource-Class Unit-Spec */
/* ======================== */

	behavior of "A Resource"

	val res1 = new Resource(NodeID(1), CPUUnit.SMALL,
									ByteSize(8, ByteUnit.GiB), ByteSize(50, ByteUnit.GiB),
									ByteSize(10, ByteUnit.MB), 10, Vector())

	val res2= new Resource(NodeID(2), CPUUnit.MEDIUM,
									ByteSize(16, ByteUnit.GiB), ByteSize(100, ByteUnit.GiB),
									ByteSize(10, ByteUnit.MB), 10, Vector())

	val res3 = new Resource(NodeID(3), CPUUnit.LARGE,
									ByteSize(24, ByteUnit.GiB), ByteSize(200, ByteUnit.GiB),
									ByteSize(50, ByteUnit.MB), 10, Vector())


/* Test-Specs */
/* ========== */

	it should "be equal to itself and another Resource with the same Resource footprint (even with different IDs and Neighbours)" in {
		When("res1 is directly compared to itself")
		Then("res1.equals(res1) should be true")
		res1.equals(res1) should be(true)
		res1 == res1 should be (true)


		Given("A Resource with the res1 footprint, instantiated statically via apply()")
		val res1Applied 	= Resource(NodeID(1), CPUUnit.SMALL,
											ByteSize(8, ByteUnit.GiB), ByteSize(50, ByteUnit.GiB),
											ByteSize(10, ByteUnit.MB), 10, Vector())

		When("res1 is compared with the statically applied res1 copy")
		Then("res1.equals(res1Applied) should be true")
		res1.equals(res1Applied) should be (true)
		res1Applied.equals(res1) should be (true)
		res1 == res1Applied should be (true)
		res1Applied == res1 should be (true)

		Given("A Respource with the res1 footprint, with additional link descriptions")
		val linkRes 		= Resource(NodeID(1), CPUUnit.SMALL,
											ByteSize(8, ByteUnit.GiB), ByteSize(50, ByteUnit.GiB),
											ByteSize(10, ByteUnit.MB), 10, Vector(NodeID(2), NodeID(3)))


		When("res1 is compared with the additional link descripted res")
		Then("res1.equals(linkRes) should be true")
		res1.equals(linkRes) should be (true)
		linkRes.equals(res1) should be (true)
		res1 == linkRes should be (true)
		linkRes == res1 should be (true)
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



/* Host-Class Unit-Spec */
/* ==================== */

	behavior of "A Host"

	val hostSLA1 	= new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW, ImgFormat.CLOOP, ImgFormat.BOCHS, ImgFormat.QCOW2),
									  	  Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 10), (CPUUnit.MEDIUM, 10)))

	val hostSLA1 	= new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW, ImgFormat.CLOOP, ImgFormat.BOCHS, ImgFormat.QCOW2),
		Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 10), (CPUUnit.MEDIUM, 10)))

	val hostSLA1 	= new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW, ImgFormat.CLOOP, ImgFormat.BOCHS, ImgFormat.QCOW2),
		Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 10), (CPUUnit.MEDIUM, 10)))


	val host1 		= new Host()

/* Test-Specs */
/* ========== */

}
