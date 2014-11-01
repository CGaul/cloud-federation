package datatypes

import datatypes.CPUUnit.CPUUnit


/**
 * Just a wrapper case class for an Integer ID.
 * Used for syntactic Sugar in the Code.
 * @param id The ID of the Node/Host
 */
case class NodeID(id: Integer)

/**
 *
 * @param nodeID The NodeID that is representing this resource. For VMs, this is the nodeID of the hypervising host
 *               (which is also represented as a Resource). May be None for Resource Requests.
 * @param cpu CPU Speed on Node [CPUUnit]
 * @param ram Amount of RAM on Node [ByteSize]
 * @param storage Amount of Storage on Node [ByteSize]
 * @param bandwidth Bandwidth, relatively monitored from GW to Node [ByteSize]
 * @param latency Latency, relatively monitored from GW to Node [ms]
 */
case class Resource(nodeID: NodeID,
						  cpu: CPUUnit,
						  ram: ByteSize,
						  storage: ByteSize,
						  bandwidth: ByteSize,
						  latency: Float,
						  links: Vector[NodeID])
{
	override def equals(obj: scala.Any): Boolean = obj match {
		case that: Resource => (that canEqual this) &&
										(this.cpu == that.cpu) && (this.ram == that.ram) &&
										(this.storage == that.storage) && (this.bandwidth == that.bandwidth) &&
						 			 	(this.latency == that.latency)
		case _ => false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Resource]
}

object ResOrderingMult{
	val CPU_MULT:		Float	= 10
	val RAM_MULT: 		Float	= 1/10
	val STORAGE_MULT: Float	= 1/10
	val BDWTH_MULT: 	Float = 1/10
	val LATENCY_MULT:	Float	= 100
}

object CPUResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round(CPUUnitOrdering.compare(x.cpu, y.cpu) * ResOrderingMult.CPU_MULT)
	}
}
object RAMResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((x.ram compareSafely y.ram) * ResOrderingMult.RAM_MULT)
	}
}
object StorageResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((x.storage compareSafely y.storage) * ResOrderingMult.STORAGE_MULT)
	}
}
object BandwidthResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((x.bandwidth compareSafely y.bandwidth) * ResOrderingMult.BDWTH_MULT)
	}
}
object LatencyResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((y.latency - x.latency) * ResOrderingMult.LATENCY_MULT)
	}
}

object RelativeResOrdering extends Ordering[Resource]{
	/**
	 * <b>Approximated(!)</b> comparison of two Resource Objects with each other.
	 * This is done by a cumulative sum of all Resource-Parts (CPU, RAM, etc.), which could be calculated fast.
	 * As a Resource contains of many different values (and units),
	 * an absolute comparison of two Resources is not meaningful.
	 * However, for sorting this compareTo should be enough, as a cumulative sum could somehow define,
	 * which resource is dominating another. <br>
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
	 */
	override def compare(x: Resource, y: Resource): Int = {
		val cpuDiff 			= CPUResOrdering.compare(x,y)
		if (cpuDiff < 0){
			return cpuDiff
		}
		val ramDiff: Int 		= RAMResOrdering.compare(x,y)
		if (ramDiff < 0){
			return ramDiff
		}
		val storageDiff: Int = StorageResOrdering.compare(x,y)
		if (storageDiff < 0){
			return storageDiff
		}
		val bdwhDiff: Int 	= BandwidthResOrdering.compare(x,y)
		if (bdwhDiff < 0){
			return bdwhDiff
		}
		val latencyDiff: Int = LatencyResOrdering.compare(x,y)
		if (latencyDiff < 0){
			return latencyDiff
		}

		val cumSum: Int		= cpuDiff + ramDiff + storageDiff + bdwhDiff + latencyDiff
		return cumSum
	}
}



case class Host(hardwareSpec: Resource, hostSLA: HostSLA){
	override def equals(obj: scala.Any): Boolean = obj match{
		case that: Host 	=> this.hardwareSpec == that.hardwareSpec && this.hostSLA == that.hostSLA
		case _ 				=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Host]
}

object RelativeHostByResOrdering extends Ordering[Host]{
	override def compare(x: Host, y: Host): Int = {
		return RelativeResOrdering.compare(x.hardwareSpec, y.hardwareSpec)
	}
}



case class ResourceAlloc(resources: Vector[Resource], requestedHostSLA: HostSLA)
{
	override def equals(obj: scala.Any): Boolean = obj match {
		case that: ResourceAlloc 	=> this.resources == that.resources
		case _ 							=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[ResourceAlloc]
}