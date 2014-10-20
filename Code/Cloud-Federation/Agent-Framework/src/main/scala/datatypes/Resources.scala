package datatypes

import datatypes.Byte_Unit.Byte_Unit
import datatypes.CPU_Unit.CPU_Unit

/**
 * A CPU_Unit gives insight about the relative strength of the CPU in a Resource-Container.
 */
object CPU_Unit extends Enumeration{
	type CPU_Unit 	= Value
	val SMALL, MEDIUM, LARGE, XLARGE = Value
//	val SMALL 		= Value(1)
//	val MEDIUM 		= Value(5)
//	val LARGE 		= Value(10)
//	val XLARGE 		= Value(20)
}

object CPU_UnitValuator{
	val CPU_UnitMap : Map[CPU_Unit, Int] = Map(CPU_Unit.SMALL -> 1, CPU_Unit.MEDIUM -> 5,
															 CPU_Unit.LARGE -> 10, CPU_Unit.XLARGE -> 20)
	def getValue(cpuUnit : CPU_Unit): Int = {
		CPU_UnitMap(cpuUnit)
	}
}


object Byte_Unit extends Enumeration{
	type Byte_Unit = Value
	val kB, KiB, MB, MiB, GB, GiB, TB, TiB, PB, PiB = Value
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
case class ByteSize(size: Integer, unit: Byte_Unit) extends Comparable[ByteSize] {

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

	def convert(srcUnit: Byte_Unit, destUnit: Byte_Unit): Double = {
		val srcUnitMult: Long	= sizeConversion(srcUnit)
		val destUnitDiv: Long	= sizeConversion(destUnit)

		val byteSize: Long 		= this.size * srcUnitMult
		val destSize: Double		= byteSize / destUnitDiv
		return destSize
	}


	/* Overridden Methods: */
	/* =================== */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: ByteSize 	=> (that canEqual this) && (compareTo(that) == 0)
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[ByteSize]


	override def compareTo(that: ByteSize): Int = {
		val thisUnitMult: Long	= sizeConversion(this.unit)
		val thatUnitMult: Long	= sizeConversion(that.unit)

		val thisByteSize: Long 	= this.size * thisUnitMult
		val thatByteSize: Long	= that.size * thatUnitMult
		val result: Int 			= (thisByteSize - thatByteSize).asInstanceOf[Int]
		return result
	}


	override def toString: String = size.toString +" "+ unit.toString

	override def hashCode(): Int = super.hashCode()
}


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
							latency:		Vector[(NodeID, Float)]) extends Comparable[Resources]{

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: Resources => (that canEqual this) &&
		  								(this.cpu == that.cpu) && (this.ram == that.ram) &&
									   (this.storage == that.storage) && (this.bandwidth == that.bandwidth) &&
									   (this.latency == that.latency)
		case _ 					=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Resources]



	/**
	 * <b>Approximated(!)</b> comparison of two Resource Objects with each other.
	 * This is done by a cumulative sum of all Resource-Parts (CPU, RAM, etc.), which could be calculated fast.
	 * In order to have a <em>guaranteed</em> comparison a much more complex binpacking problem
	 * has to be solved in the Resource allocation itself. <br>
	 * This compareTo(B) method has the same three result types as the Comparable Interface proposes:
	 *    <ul>
	 *       <li>If a Resource object A (this), representing available Resources, is greater than another Resource object B (that),
	 *    		 it means that a ResourceRequest(B) could <b><em>possibly</em></b> be fulfilled by A. </li>
	 *    	<li>If A is less then B, it is <b><em>guaranteed</em></b> that there are not enough Resources left,
	 *    	to fulfill a ResourceRequest(B).</li>
	 *    	<li>Equality means that the approximated, cumulative sum is exactly matching the requested sum of Resources.
	 *    		 However, as a binpacking problem is very likely not able to split the resources perfectly, an allocation
	 *    		 of a ResourceRequest(B) will <b><em>most likely fail</em></b>. As of that, equality should be treated
	 *    		 the same as A < B. </li>
	 * 	</ul>
	 * <p>
	 *    This is a combined method of compareToCPU(..), compareToRAM(..), compareToStorage(..),
	 *    compareToBandwidth(..) and compareToLatency(..) each with the same
	 *    parameter signature as compareTo(other: Resources)
	 * </p>
	 * @param other
	 * @return
	 */
	override def compareTo(other: Resources): Int = {

		val cpuDiff 			= compareToCPU(other)
		if (cpuDiff <= 0){
			return cpuDiff
		}
		val ramDiff: Int 		= compareToRAM(other)
		if (ramDiff <= 0){
			return ramDiff
		}
		val storageDiff: Int = compareToStorage(other)
		if (storageDiff <= 0){
			return storageDiff
		}
		val bdwhDiff: Int 	= compareToBandwidth(other)
		if (bdwhDiff <= 0){
			return bdwhDiff
		}
		val latencyDiff: Int = compareToLatency(other)
		if (latencyDiff <= 0){
			return latencyDiff
		}

		val cumSum: Int		= cpuDiff + ramDiff + storageDiff + bdwhDiff + latencyDiff
		return cumSum
	}

	def compareToCPU(other: Resources): Int = {
		val thisCPUSum = this.cpu.flatMap(t => Vector(t._2)).
		  								  map(cpuUnit => CPU_UnitValuator.getValue(cpuUnit)).sum
		val thatCPUSum = this.cpu.flatMap(t => Vector(t._2)).
		  								  map(cpuUnit => CPU_UnitValuator.getValue(cpuUnit)).sum

		return thisCPUSum - thatCPUSum
	}

	def compareToRAM(resources: Resources): Int = {
		val thisRAMSum = this.ram.flatMap(t => Vector(t._2)).
		  map(cpuUnit => CPU_UnitValuator.getValue(cpuUnit)).sum
		val thatRAMSum = this.ram.flatMap(t => Vector(t._2)).
		  map(cpuUnit => CPU_UnitValuator.getValue(cpuUnit)).sum

		return thisRAMSum - thatRAMSum
	}

	def compareToStorage(resources: Resources): Int = ???

	def compareToBandwidth(resources: Resources): Int = ???

	def compareToLatency(resources: Resources): Int = ???

	def allocate(other: Resources): Resources = ??? //TODO: Implement allocation of other in Resources of this, returning this.
//	def allocateByCPU(other: Resources): Resources = {
//		val cpuVectorA: Vector[CPU_Unit] = this.cpu.flatMap(t => Vector(t._2))
//		val cpuVectorB: Vector[CPU_Unit] = other.cpu.flatMap(t => Vector(t._2))
//
//		//Foreach CPU_Unit, filter both CPU-Vectors and compare the size.
//		//If cpuVectorA has at least the size of cpuVectorB,
//		// no swap needs to be taken over to the next iteration.
//		//Otherwise, each element of cpuVectorB that could not be fulfilled by cpuVectorA will be saved as a swap,
//		//as the next higher CPU_Unit has to be allocated to the left over elements from the current iter of cpuVectorB.
//		for (actCPU_Unit <- CPU_Unit.values){
//			val filteredA = cpuVectorA.filter(cpuUnit => cpuUnit != actCPU_Unit)
//			val filteredB = cpuVectorB.filter(cpuUnit => cpuUnit != actCPU_Unit)
////			if(filteredA.size >= filteredB.size) TODO: go on here.
//		}
//
//	}
}