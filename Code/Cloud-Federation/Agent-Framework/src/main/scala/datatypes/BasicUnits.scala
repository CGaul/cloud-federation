package datatypes

import datatypes.Byte_Unit.Byte_Unit
import datatypes.CPU_Unit.CPU_Unit

/**
 * A CPU_Unit gives insight about the relative strength of the CPU in a Resource-Container.
 */
object CPU_Unit extends Enumeration
{
	type CPU_Unit 	= Value
	val SMALL, MEDIUM, LARGE, XLARGE = Value
	//	val SMALL 		= Value(1)
	//	val MEDIUM 		= Value(5)
	//	val LARGE 		= Value(10)
	//	val XLARGE 		= Value(20)
}

object CPU_UnitValuator
{
	val CPU_UnitMap : Map[CPU_Unit, Int] = Map(CPU_Unit.SMALL -> 1, CPU_Unit.MEDIUM -> 5,
		CPU_Unit.LARGE -> 10, CPU_Unit.XLARGE -> 20)
	def getValue(cpuUnit : CPU_Unit): Int = {
		CPU_UnitMap(cpuUnit)
	}

	def sortUnitsAscending(): List[CPU_Unit] = {
		var cpuList = CPU_Unit.values.toList
		cpuList 		= cpuList.sortBy(x => CPU_UnitMap(x))
		return cpuList
	}

	def sortUnitsDescending(): List[CPU_Unit] = {
		var cpuList = sortUnitsAscending()
		cpuList 		= cpuList.reverse
		return cpuList
	}
}



object Byte_Unit extends Enumeration
{
	type Byte_Unit = Value
	val kB, KiB, MB, MiB, GB, GiB, TB, TiB, PB, PiB = Value
}



/**
 *
 * @param size The unitless size value, with "unit" as its metric.
 *             size * unit would be the size in bytes.
 * @param unit The Unit of the Byte size, either in Decimal Metric (kB, MB, GB) or in Binary IEC Metric (kiB, MiB, GiB)
 */
case class ByteSize(size: Double, unit: Byte_Unit) extends Comparable[ByteSize]
{
	/* Constants: */
	/* ========== */

	var sizeConversion = Map[Byte_Unit, Long]()
	sizeConversion += (Byte_Unit.kB	-> Math.round(Math.pow(1000, 1)),	Byte_Unit.KiB -> Math.round(Math.pow(1024,1)))
	sizeConversion += (Byte_Unit.MB	-> Math.round(Math.pow(1000, 2)),	Byte_Unit.MiB -> Math.round(Math.pow(1024,2)))
	sizeConversion += (Byte_Unit.GB	-> Math.round(Math.pow(1000, 3)),	Byte_Unit.GiB -> Math.round(Math.pow(1024,3)))
	sizeConversion += (Byte_Unit.TB	-> Math.round(Math.pow(1000, 4)),	Byte_Unit.TiB -> Math.round(Math.pow(1024,4)))
	sizeConversion += (Byte_Unit.PB	-> Math.round(Math.pow(1000, 5)),	Byte_Unit.PiB -> Math.round(Math.pow(1024,5)))


	/* Public Methods: */
	/* =============== */

	def convert(destUnit: Byte_Unit): ByteSize = {
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
object Img_Format extends Enumeration {
	type Img_Format = Value
	val RAW, BOCHS, CLOOP, COW, DMG, ISO, QCOW, QCOW2, QED, VMDK, VPC, IMG = Value
}



object CloudCurrency extends Enumeration {
	type CloudCurrency = Value
	//val Currency.getAvailableCurrencies
	val CLOUD_CREDIT = Value
}
