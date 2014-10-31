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

	override def canEqual(that: Any): Boolean = that.isInstanceOf[ResourceAlloc]
}

object CPUResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return CPUUnitOrdering.compare(x.cpu, y.cpu)
	}
}
object RAMResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round(x.ram.getBytes - y.ram.getBytes).toInt
	}
}
object StorageResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round(x.storage.getBytes - y.storage.getBytes).toInt
	}
}
object BandwidthResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round(x.ram.getBytes - y.ram.getBytes).toInt
	}
}
object LatencyResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((x.latency - y.latency) * 1000)
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
		if (cpuDiff <= 0){
			return cpuDiff
		}
		val ramDiff: Int 		= RAMResOrdering.compare(x,y)
		if (ramDiff <= 0){
			return ramDiff
		}
		val storageDiff: Int = StorageResOrdering.compare(x,y)
		if (storageDiff <= 0){
			return storageDiff
		}
		val bdwhDiff: Int 	= BandwidthResOrdering.compare(x,y)
		if (bdwhDiff <= 0){
			return bdwhDiff
		}
		val latencyDiff: Int = LatencyResOrdering.compare(x,y)
		if (latencyDiff <= 0){
			return latencyDiff
		}

		val cumSum: Int		= cpuDiff + ramDiff + storageDiff + bdwhDiff + latencyDiff
		return cumSum
	}
}



case class Host(hostDataSpec: Resource, hostSLA: HostSLA){
	override def equals(obj: scala.Any): Boolean = obj match{
		case that: Host 	=> this.hostDataSpec == that.hostDataSpec && this.hostSLA == that.hostSLA
		case _ 				=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Host]
}

object RelativeHostByResOrdering extends Ordering[Host]{
	override def compare(x: Host, y: Host): Int = {
		return RelativeResOrdering.compare(x.hostDataSpec, y.hostDataSpec)
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


//	/**
//	 * This method should be called, if and only if this.compareTo(other) was positive.
//	 * Otherwise call allocatePartially(..) for performance reasons.
//	 * <p>
//	 *    Even tough compareTo(other) was positive, it is <b><em>not guaranteed</b></em>
//	 *    that the allocation will succeed. If not, an allocationException will be thrown,
//	 *    additionally returning the bundle of allocatable resources as a return value.
//	 * </p><p>
//	 *    Both, the allocateCompletely(other) and allocatePartially(other) are trying to solve
//	 *    the binpacking problem that is part of this kind of resource allocation as optimal as possible.
//	 *    However, as this is a complex progress (combinatorial NP-hard), allocatePartially will
//	 *    only work on a subset of resources.
//	 * </p><p>
//	 *    <em> Both methods are using the <b>First Fit Decreasing</b>
//	 *    algorithm in order to solve the binpacking problem. </em>
//	 *</p>
//	 * <br />
//	 * Jira: CITMASTER-30 - Resource-Allocation Binpacking
//	 * @param other
//	 * @return
//	 */
//	def allocateCompletely(other: ResourceAlloc): ResourceAlloc = {
//		//TODO: first sort availResources as well as other in a decreasing order
//		//TODO: then allocate on a per Bundle base with First Fit
//	}
//
//
//	/**
//	 * This method should be called, if and only if this.compareTo(other) was negative or zero.
//	 * Otherwise call allocateCompletely(..) as the allocation would have good chances to be successful.
//	 * <p>
//	 *    Both, the allocateCompletely(other) and allocatePartially(other) are trying to solve
//	 *    the binpacking problem that is part of this kind of resource allocation as optimal as possible.
//	 *    However, as this is a complex progress (combinatorial NP-hard), allocatePartially will only work on a subset of resources.
//	 * </p><p>
//	 *    <em> Both methods are using the <b>First Fit Decreasing</b>
//	 *    algorithm in order to solve the binpacking problem. </em>
//	 * </p>
//	 * <br />
//	 * Jira: CITMASTER-30 - Resource-Allocation Binpacking
//	 * @param other
//	 * @return
//	 */
//	def allocatePartially(other: ResourceAlloc): ResourceAlloc = ???
//
//
//	def allocateBundle(cpu: CPUUnit, ram: ByteSize, storage: ByteSize, bandwidth: ByteSize, latency: Float) = {
//		//TODO: implement
//	}

//	def allocateByCPU(other: Resources): Resources = {
//		val cpuVectorA: Vector[CPUUnit] = this.cpu.flatMap(t => Vector(t._2))
//		val cpuVectorB: Vector[CPUUnit] = other.cpu.flatMap(t => Vector(t._2))
//
//		/* Foreach CPUUnit, filter both CPU-Vectors and compare the size.
//		 * If cpuVectorA has at least the size of cpuVectorB,
//		 * no swap needs to be taken over to the next iteration.
//		 * Otherwise, each element of cpuVectorB that could not be fulfilled
//		 * by cpuVectorA will be saved as a swap, as the next higher CPUUnit
//		 * has to be allocated to the left over elements from the current iter of cpuVectorB. */
//		for (actCPUUnit <- CPUUnitValuator.sortUnitsDescending()){
//			val filteredA = cpuVectorA.filter(cpuUnit => cpuUnit != actCPUUnit)
//			val filteredB = cpuVectorB.filter(cpuUnit => cpuUnit != actCPUUnit)
//			// all allocations for this CPUUnit size could be fulfilled:
//			if(filteredA.size >= filteredB.size){
//				//TODO: go on
//			}
//			// else, load the left overs into a temporary swap
//		}

//	}
//}