package datatypes

import java.io.File

import datatypes.ByteUnit.ByteUnit
import datatypes.CPUUnit.CPUUnit

/**
 * A CPUUnit gives insight about the relative strength of the CPU in a Resource-Container.
 */
object CPUUnit extends Enumeration
{
	type CPUUnit 	= Value
	val SMALL, MEDIUM, LARGE, XLARGE = Value

	def fromString(str: String): CPUUnit = str match{
		case "SMALL" 	=> SMALL
		case "MEDIUM" => MEDIUM
		case "LARGE"	=> LARGE
		case "XLARGE"	=> XLARGE
	}
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
	val KB, KiB, MB, MiB, GB, GiB, TB, TiB, PB, PiB = Value

	def fromString(str: String): ByteUnit = str match{
		case "KB" 	=> KB
		case "KiB" 	=> KiB
		case "MB" 	=> MB
		case "MiB" 	=> MiB
		case "GB" 	=> GB
		case "GiB" 	=> GiB
		case "TB" 	=> TB
		case "TiB" 	=> TiB
		case "PB" 	=> PB
		case "PiB" 	=> PiB
	}
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
	sizeConversion += (ByteUnit.KB	-> Math.round(Math.pow(1000, 1)),	ByteUnit.KiB -> Math.round(Math.pow(1024,1)))
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

	def compareSafely(other: ByteSize): Int = {
		//Normalize each ByteUnit to MB first, as it is nearly the Median of the sizeConversion table:
		val mbDiff: Double  = this.convert(ByteUnit.MB).size - other.convert(ByteUnit.MB).size
		try{
			val diffRnd: Int = Math.toIntExact(Math.round(mbDiff))
			return diffRnd
		}
		catch {
			case e: ArithmeticException =>
				//If mbDiff is not fitting in Integer cast, reduce it to gbDiff, then try again:
				val gbDiff			 = mbDiff / 1000;
				val reducedDiffRnd = Math.toIntExact(Math.round(gbDiff))
				return reducedDiffRnd
		}
	}

	def normalizeToMB: Double = {
		val normalizedBytes : ByteSize = convert(ByteUnit.MB)
		return normalizedBytes.size
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
 * Companion Object for ByteSize case-class
 */
object ByteSize {

	/* Serialization: */
	/* ============== */

	def toXML(byteSize: ByteSize): xml.Node =
		<ByteSize>
			{byteSize.size} {byteSize.unit}
		</ByteSize>

	def saveToXML(file: File, byteSize: ByteSize) = {
		val xmlNode = toXML(byteSize)
		xml.XML.save(file.getAbsolutePath, xmlNode)
	}

	/* De-Serialization: */
	/* ================= */

	def fromString(str: String): ByteSize = {
		val strSplit = str.split(" ")
		val (size, unit): (Double, ByteUnit) = (strSplit(0).toDouble, ByteUnit.fromString(strSplit(1)))
		return ByteSize(size, unit)
	}
	def fromXML(node: xml.Node): ByteSize = {
		val xmlString = (node \ "ByteSize").text
		return fromString(xmlString)
	}

	def loadFromXML(file: File): ByteSize = {
		val xmlNode = xml.XML.loadFile(file)
		return fromXML(xmlNode)
	}
}



/**
 * Lists all possible virtualization image formats
 * that are accessible via libvirt.
 * from: http://libvirt.org/storage.html
 */
object ImgFormat extends Enumeration {
	type ImgFormat = Value
	val BOCHS, CLOOP, COW, DMG, IMG, ISO, QCOW, QCOW2, QED, RAW, VMDK, VPC = Value

	def fromString(str: String): ImgFormat = str match{
			case "BOCHS" => BOCHS
			case "CLOOP" => CLOOP
			case "COW"	 => COW
			case "DMG"	 => DMG
			case "IMG"	 => IMG
			case "ISO"	 => ISO
			case "QCOW"	 => QCOW
			case "QCOW2" => QCOW2
			case "QED"	 => QED
			case "RAW"	 => RAW
			case "VMDK"	 => VMDK
			case "VPC"	 => VPC
	}
}



object CloudCurrency extends Enumeration {
	type CloudCurrency = Value
	//val Currency.getAvailableCurrencies
	val CLOUD_CREDIT = Value

	def fromString(str: String) = str match {
		case "CLOUD_CREDIT" => CLOUD_CREDIT
	}
}
