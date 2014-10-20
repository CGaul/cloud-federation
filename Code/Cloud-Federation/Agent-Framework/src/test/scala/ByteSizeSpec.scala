import datatypes.{Byte_Unit, ByteSize}
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by costa on 10/20/14.
 */
class ByteSizeSpec extends FlatSpec with Matchers{
	"A ByteSize Value" should "be equal and comparable to another Value with the same size and unit" in{
		val byteSize1 = new ByteSize(5, Byte_Unit.MB)
		val byteSize2 = new ByteSize(5, Byte_Unit.MB)

		byteSize1.equals(byteSize2) should be (true)
		byteSize2.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)
	}

	it should "be equal to another Value's size calculated in Bytes, using Binary IEC Metric (KiB, MiB, GiB)" in {
		val byteSize1 = new ByteSize(0.09765625, Byte_Unit.GiB)
		val byteSize2 = new ByteSize(100, Byte_Unit.MiB)
		val byteSize3 = new ByteSize(102400, Byte_Unit.KiB)

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
	}

	it should "be equal to another Value's size calculated in Bytes, using Decimal Metric (kB, MB, GB)" in {
		val byteSize1 = new ByteSize(1, Byte_Unit.GB)
		val byteSize2 = new ByteSize(1000, Byte_Unit.MB)
		val byteSize3 = new ByteSize(1000000, Byte_Unit.kB)

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
	}

	it should "be comparable between Decimal and Binary IEC Metric (KB vs. KiB, MB vs. MiB, GB vs. GiB)" in {
		val byteSize1 = new ByteSize(100, Byte_Unit.MiB)
		val byteSize2 = new ByteSize(104.85759999999999, Byte_Unit.MB)

		val byteSize3 = new ByteSize(100, Byte_Unit.GiB)
		val byteSize4 = new ByteSize(107.37418240000001, Byte_Unit.GB)

		byteSize1.equals(byteSize2) should be (true)
		byteSize2.equals(byteSize1) should be (true)
		byteSize1.compareTo(byteSize2) should be (0)
		byteSize2.compareTo(byteSize1) should be (0)

		byteSize3.equals(byteSize4) should be (true)
		byteSize4.equals(byteSize3) should be (true)
		byteSize3.compareTo(byteSize4) should be (0)
		byteSize4.compareTo(byteSize3) should be (0)
	}
}
