package unitspecs

import datatypes._
import org.scalatest.{GivenWhenThen, FlatSpec, ShouldMatchers}

/**
 * Created by costa on 10/28/14.
 */
class ResourceSpec extends FlatSpec with ShouldMatchers with GivenWhenThen
{
/* Definition of three HostSLAs */
/* ============================ */

behavior of "A Resource"
	Given("A small Resource res1")
	val res1 = new Resource(NodeID(1), CPUUnit.SMALL,
									ByteSize(8, ByteUnit.GiB), ByteSize(50, ByteUnit.GiB),
									ByteSize(10, ByteUnit.MB), 10, Vector())

	Given("A medium Resource res2")
	val res2= new Resource(NodeID(2), CPUUnit.MEDIUM,
									ByteSize(16, ByteUnit.GiB), ByteSize(100, ByteUnit.GiB),
									ByteSize(10, ByteUnit.MB), 10, Vector())

	Given("A large Resource res3")
	val res3 = new Resource(NodeID(3), CPUUnit.LARGE,
									ByteSize(24, ByteUnit.GiB), ByteSize(200, ByteUnit.GiB),
									ByteSize(50, ByteUnit.MB), 10, Vector())


/* Test-Specs */
/* ========== */

	it should "be equal to itself and another Resource with the same Resource footprint (even with different IDs and Neighbours)" in {
		When("res1 is directly compared to itself")
		Then("res1.equals(res1) should be true")
		assert(res1.equals(res1) === true)

		When("res1 is defined via apply()")
		val res1Applied 	= Resource(NodeID(1), CPUUnit.SMALL,
											ByteSize(8, ByteUnit.GiB), ByteSize(50, ByteUnit.GiB),
											ByteSize(10, ByteUnit.MB), 10, Vector())

		Then("res1.equals(res1Applied) should be true")
		res1.equals(res1Applied) should be (true)
		res1Applied.equals(res1) should be (true)

		val equalRes 		= Resource(NodeID(1), CPUUnit.SMALL,
											ByteSize(8, ByteUnit.GiB), ByteSize(50, ByteUnit.GiB),
											ByteSize(10, ByteUnit.MB), 10, Vector(NodeID(2), NodeID(3)))



		res1.equals(equalRes) should be (true)
		equalRes.equals(res1) should be (true)

		res1 == res1 should be (true)
		res1 == res1Applied should be (true)
		res1 == equalRes should be (true)
	}

	it should "be unequal to Resources with a different Resource footprint than the origin" in{
		res1.equals(res2) should be (false)
		res2.equals(res1) should be (false)
		res1 == res3 should be (false)
		res3 == res2 should be (false)
		res3 == res1 should be (false)
	}

	it should "be comparable with another Resource in a relative ordering" in{
		//RelativeResOrdering.compare(res1, res2)  should be()
		pending
	}
}
