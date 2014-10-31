package datatypes

import datatypes.ByteUnit.ByteUnit
import datatypes.CPUUnit.CPUUnit

/**
 * A CPUUnit gives insight about the relative strength of the CPU in a Resource-Container.
 */
object CPUUnit extends Enumeration
{
	type CPUUnit 	= Value
	val SMALL, MEDIUM, LARGE, XLARGE = Value
}

object CPUUnitOrdering extends Ordering[CPUUnit]
{
	val CPUUnitMap : Map[CPUUnit, Int] = Map(CPUUnit.SMALL -> 1, CPUUnit.MEDIUM -> 5,
		CPUUnit.LARGE -> 10, CPUUnit.XLARGE -> 20)

	def getValue(cpuUnit : CPUUnit): Int = {
		CPUUnitMap(cpuUnit)
	}

	def sortUnitsAscending(): List[CPUUnit] = {
		var cpuList = CPUUnit.values.toList
		cpuList 		= cpuList.sortBy(x => CPUUnitMap(x))
		return cpuList
	}

	def sortUnitsDescending(): List[CPUUnit] = {
		var cpuList = sortUnitsAscending()
		cpuList 		= cpuList.reverse
		return cpuList
	}

	override def compare(x: CPUUnit, y: CPUUnit): Int = {
		return getValue(x) - getValue(y)
	}
}



object ByteUnit extends Enumeration
{
	type ByteUnit = Value
	val kB, KiB, MB, MiB, GB, GiB, TB, TiB, PB, PiB = Value
}



/**
 *
 * @param size The unitless size value, with "unit" as its metric.
 *             size * unit would be the size in bytes.
 * @param unit The Unit of the Byte size, either in Decimal Metric (kB, MB, GB) or in Binary IEC Metric (kiB, MiB, GiB)
 */
case class ByteSize(size: Double, unit: ByteUnit) extends Comparable[ByteSize]
{
	/* Constants: */
	/* ========== */

	var sizeConversion = Map[ByteUnit, Long]()
	sizeConversion += (ByteUnit.kB	-> Math.round(Math.pow(1000, 1)),	ByteUnit.KiB -> Math.round(Math.pow(1024,1)))
	sizeConversion += (ByteUnit.MB	-> Math.round(Math.pow(1000, 2)),	ByteUnit.MiB -> Math.round(Math.pow(1024,2)))
	sizeConversion += (ByteUnit.GB	-> Math.round(Math.pow(1000, 3)),	ByteUnit.GiB -> Math.round(Math.pow(1024,3)))
	sizeConversion += (ByteUnit.TB	-> Math.round(Math.pow(1000, 4)),	ByteUnit.TiB -> Math.round(Math.pow(1024,4)))
	sizeConversion += (ByteUnit.PB	-> Math.round(Math.pow(1000, 5)),	ByteUnit.PiB -> Math.round(Math.pow(1024,5)))


	/* Public Methods: */
	/* =============== */

	def convert(destUnit: ByteUnit): ByteSize = {
		val srcUnitMult: Long	= sizeConversion(this.unit)
		val destUnitDiv: Long	= sizeConversion(destUnit)

		val byteSize: Double 	= this.size * srcUnitMult
		val destSize: Double		= byteSize / destUnitDiv
		return new ByteSize(destSize, destUnit)
	}

	def getBytes: Double = {
		val srcUnitMult: Long	= sizeConversion(this.unit)
		val byteSize: Double 	= this.size * srcUnitMult
		return byteSize
	}


	/* Overridden Methods: */
	/* =================== */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: ByteSize 	=> (that canEqual this) && (compareTo(that) == 0)
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[ByteSize]


	override def compareTo(that: ByteSize): Int = {
		val thisUnitMult: Long		= sizeConversion(this.unit)
		val thatUnitMult: Long		= sizeConversion(that.unit)

		val thisByteSize: Double	= this.size * thisUnitMult
		val thatByteSize: Double	= that.size * thatUnitMult
		val result: Int 				= Math.round(thisByteSize - thatByteSize).asInstanceOf[Int]
		return result
	}


	override def toString: String = size.toString +" "+ unit.toString

	override def hashCode(): Int = super.hashCode()
}



/**
 * Lists all possible virtualization image formats
 * that are accessible via libvirt.
 * from: http://libvirt.org/storage.html
 */
object ImgFormat extends Enumeration {
	type ImgFormat = Value
	val RAW, BOCHS, CLOOP, COW, DMG, ISO, QCOW, QCOW2, QED, VMDK, VPC, IMG = Value
}



object CloudCurrency extends Enumeration {
	type CloudCurrency = Value
	//val Currency.getAvailableCurrencies
	val CLOUD_CREDIT = Value
}
