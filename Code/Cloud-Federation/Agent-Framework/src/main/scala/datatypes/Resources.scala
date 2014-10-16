package datatypes

import datatypes.Byte_Unit.Byte_Unit
import datatypes.CPU_Unit.CPU_Unit

/**
 * A CPU_Unit gives insight about the relative strength of the CPU in a Resource-Container.
 */
object CPU_Unit extends Enumeration{
	type CPU_Unit = Value
	val SMALL, MEDIUM, LARGE, XLARGE = Value
}

object Byte_Unit extends Enumeration{
	type Byte_Unit = Value
	val kB, KiB, MB, MiB, GB, GiB, TB, TiB, PB, PiB = Value
}

object ByteConverter{
	var sizeConversion = Map[Byte_Unit, Long]()
	sizeConversion += (Byte_Unit.kB	-> Math.round(Math.pow(1000, 1)),	Byte_Unit.KiB -> Math.round(Math.pow(1024,1)))
	sizeConversion += (Byte_Unit.MB	-> Math.round(Math.pow(1000, 2)),	Byte_Unit.MiB -> Math.round(Math.pow(1024,2)))
	sizeConversion += (Byte_Unit.GB	-> Math.round(Math.pow(1000, 3)),	Byte_Unit.GiB -> Math.round(Math.pow(1024,3)))
	sizeConversion += (Byte_Unit.TB	-> Math.round(Math.pow(1000, 4)),	Byte_Unit.TiB -> Math.round(Math.pow(1024,4)))
	sizeConversion += (Byte_Unit.PB	-> Math.round(Math.pow(1000, 5)),	Byte_Unit.PiB -> Math.round(Math.pow(1024,5)))

	def convertBytes(size: Integer, srcUnit: Byte_Unit, destUnit: Byte_Unit): Double = {
		val srcUnitMultiplier: Long	= sizeConversion(srcUnit)
		val destUnitDivisor: Long		= sizeConversion(destUnit)

		val byteSize: Long 				= size * srcUnitMultiplier
		val destSize: Double				= byteSize / destUnitDivisor
		destSize
	}
}


/**
 * Just a wrapper case class for an Integer ID.
 * Used for syntactic Sugar in the Code.
 * @param id The ID of the Node/Host
 */
case class NodeID(id: Integer)

/**
 *
 * @param size The unitless size value, with "unit" as its metric.
 *             size * unit would be the size in bytes.
 * @param unit The Unit of the Byte size, either in Decimal Metric (kB, MB, GB) or in Binary IEC Metric (kiB, MiB, GiB)
 */
case class ByteSize(size: Integer, unit: Byte_Unit)


/**
 *
 * @param nodeIDs All NodeIDs that are used inside this Resource-Container are listed in this Vector.
 *                May be None for Resource Requests.
 * @param cpu CPU Speed per Node [CPU_Unit]
 * @param ram Amount of RAM per Node [ByteSize]
 * @param storage Amount of Storage per Node [ByteSize]
 * @param bandwidth Bandwidth, relatively monitored from GW to Node [ByteSize]
 * @param latency Latency, relatively monitored from GW to Node [ms]
 */
case class Resources(nodeIDs:		Vector[NodeID],
							cpu:			Vector[(NodeID, CPU_Unit)],
							ram: 			Vector[(NodeID, ByteSize)],
							storage:		Vector[(NodeID, ByteSize)],
							bandwidth:	Vector[(NodeID, ByteSize)],
							latency:		Vector[(NodeID, Float)])
