package unitspecs

import datatypes.{ByteSize, ByteUnit}
import org.scalatest.{Matchers, FlatSpec}

/**
 * @author Constantin Gaul, created on 10/20/14.
 */
class ByteSizeSpec extends FlatSpec with Matchers{
	"A ByteSize Value" should "be printed correctly" in{
		val byteSize1 = new ByteSize(5, ByteUnit.MB)
		val byteSize2 = new ByteSize(125.123, ByteUnit.KiB)
		byteSize1.toString should be ("5.0 MB")
		byteSize2.toString should be ("125.123 KiB")
	}
	it should "be equal and comparable to itself" in{
		val byteSize1 = new ByteSize(5, ByteUnit.MB)
		byteSize1.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize1) should be (0)
	}

	it should "be equal, comparable and convertable to another Value with the same size and unit" in{
	  val byteSize1 = new ByteSize(5, ByteUnit.MB)
	  val byteSize2 = new ByteSize(5, ByteUnit.MB)

	//Check for equality and comparability:
	  byteSize1.equals(byteSize2) should be (true)
	  byteSize2.equals(byteSize1) should be (true)
	  byteSize1.compareTo(byteSize2) should be (0)
	  byteSize2.compareTo(byteSize1) should be (0)

	//Convert byteSize1 first to another unit, then back and check for equality:
		val convByteSize1 = byteSize1.convert(ByteUnit.GiB).convert(ByteUnit.MB)
		convByteSize1.equals(byteSize2) should be (true)
	  }

	it should "be equal and comparable to another Value's size calculated in Bytes, " +
	  "using Binary IEC Metric (KiB, MiB, GiB) - used in RAM and OS internal calculation" in {
		val byteSize1 = new ByteSize(0.09765625, ByteUnit.GiB)
		val byteSize2 = new ByteSize(100, ByteUnit.MiB)
		val byteSize3 = new ByteSize(102400, ByteUnit.KiB)

	//Check for equality and comparability:
		byteSize1.equals(byteSize2) should be (true)
		byteSize2.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)

		byteSize1.equals(byteSize3) should be (true)
		byteSize3.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize3) should be (0)
		byteSize3.compareTo(byteSize1) should be (0)

		byteSize2.equals(byteSize3) should be (true)
		byteSize3.equals(byteSize2) should be (true)
		byteSize2.compareTo(byteSize3) should be (0)
		byteSize3.compareTo(byteSize2) should be (0)

	//Convert byteSize1 first to byteSize2's unit and check for equality
		val convByteSize1 = byteSize1.convert(ByteUnit.MiB)
		convByteSize1.equals(byteSize2) should be (true)
	}

	it should "be equal and comparable to another Value's size calculated in Bytes, " +
	  "using Decimal Metric (kB, MB, GB) - used in regular HDD calculation" in {
		val byteSize1 = new ByteSize(1, ByteUnit.GB)
		val byteSize2 = new ByteSize(1000, ByteUnit.MB)
		val byteSize3 = new ByteSize(1000000, ByteUnit.kB)

	//Check for equality and comparability:
		byteSize1.equals(byteSize2) should be (true)
		byteSize2.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)

		byteSize1.equals(byteSize3) should be (true)
		byteSize3.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize3) should be (0)
		byteSize3.compareTo(byteSize1) should be (0)

		byteSize2.equals(byteSize3) should be (true)
		byteSize3.equals(byteSize2) should be (true)
		byteSize2.compareTo(byteSize3) should be (0)
		byteSize3.compareTo(byteSize2) should be (0)

	//Convert byteSize1 first to byteSize2's unit and check for equality
		val convByteSize1 = byteSize1.convert(ByteUnit.MB)
		convByteSize1.equals(byteSize2) should be (true)
	}

	it should "be equal and comparable between " +
	  "Decimal and Binary IEC Metric (KB vs. KiB, MB vs. MiB, GB vs. GiB)" in {
		val byteSize1 = new ByteSize(1000, ByteUnit.KiB)
		val byteSize2 = new ByteSize(1024, ByteUnit.kB)

		val byteSize3 = new ByteSize(100, ByteUnit.MiB)
		val byteSize4 = new ByteSize(104.85759999999999, ByteUnit.MB)

		val byteSize5 = new ByteSize(100, ByteUnit.GiB)
		val byteSize6 = new ByteSize(107.37418240000001, ByteUnit.GB)

	//Check for equality and comparability:
		byteSize1.equals(byteSize2) should be (true)
		byteSize2.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)

		byteSize3.equals(byteSize4) should be (true)
		byteSize4.equals(byteSize3) should be (true)
		byteSize3.compareTo(byteSize4) should be (0)
		byteSize4.compareTo(byteSize3) should be (0)

		byteSize5.equals(byteSize6) should be (true)
		byteSize6.equals(byteSize5) should be (true)
		byteSize5.compareTo(byteSize6) should be (0)
		byteSize6.compareTo(byteSize5) should be (0)

	//Convert byteSize1 first to byteSize2's unit and check for equality
		val convByteSize1 = byteSize1.convert(ByteUnit.kB)
		convByteSize1.equals(byteSize2) should be (true)
	}
}
