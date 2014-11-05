package datatypes

import datatypes.CPUUnit._


/* Data Containers (case classes): */
/* =============================== */

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

	/* Basic Overrides: */
	/* ---------------- */

	override def equals(obj: scala.Any): Boolean = obj match {
		case that: Resource => (that canEqual this) &&
										(this.cpu == that.cpu) && (this.ram == that.ram) &&
										(this.storage == that.storage) && (this.bandwidth == that.bandwidth) &&
						 			 	(this.latency == that.latency)
		case _ => false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Resource]
}


//TODO: use http://docs.scala-lang.org/style/scaladoc.html to go on with ScalaDocs in code.
/**
 *  ==How to use==
 *  {{{
 *     val host = Host
 *  }}}
 * @constructor create a new Host, specifying the Host's Hardware Spec, the initial Allocation (if any)
 *             and the Host's SLA
 * @param hardwareSpec
 * @param allocatedResources
 * @param _hostSLA bla
 */
case class Host(hardwareSpec: Resource,
					 var allocatedResources: Vector[ResourceAlloc] = Vector(),
					 private var _hostSLA: HostSLA){

	/* Getter: */
	/* ======= */

	def hostSLA = _hostSLA


	/* Public Methods: */
	/* --------------- */

	/**
	 * Allocates a new Resource to this Host.
	 *
	 * Before the allocation occurs, a ''pre-allocation'' is tested.
	 * If it fails, this allocate() method will return false,
	 * otherwise the allocation will take place and true will be returned.
	 *
	 * @param resToAlloc
	 * @return A [[Tuple2]] that states, if the at least some of the resToAlloc
	 *         where allocated (result._1 == true) and returns the ResourceAlloc
	 *         Split that is left over after the allocation at this Host
	 *         (result._1 == None if resToAlloc was fully allocated, otherwise subset of resToAlloc)
	 */
	def allocate(resToAlloc: ResourceAlloc): (Boolean, Option[ResourceAlloc]) = {
		val (success, testedSLA, resSplitAmount) = testAllocation(resToAlloc)
		// If the allocation Test was successful,
		// all resToAlloc could be fulfilled by this Host.
		if(success){
			//Add the resToAlloc to Host's allocatedResources:
			allocatedResources = allocatedResources :+ resToAlloc

			//Update the Host's HostSLA with the tested pre-allocation SLA
			_hostSLA = testedSLA.get
			return (true, None)
		}
		// If the Test was not successful, only a part or no resources at all
		// could be allocated by this Host.
		else{
			// If the Option[HostSLA] is None, no allocation of the resToAlloc
			// could be handled by this host at all.
			if(testedSLA.isEmpty){
				return (false, Option(resToAlloc))
			}
			// Otherwise (if the testedSLA is not None), a ResourceAlloc split needs
			// to be defined. The one split is allocated at this Host, the other part is
			// returned as a result of the Tuple2 of this method
			else{
				val (resSplitHost, resSplitOther) = splitAllocation(testedSLA.get, resToAlloc, resSplitAmount)
				if(resSplitHost.resources.size > 0){
					allocate(resSplitHost)
					return (true, Option(resSplitOther))
				}
				else {
					return (false, Option(resSplitOther))
				}
			}
		}
	}


	/* Private Methods: */
	/* --------------- */

	/**
	 *  When [[datatypes.Host.allocate]] is called, the new allocation needs to be tested first (pre-allocation phase),
	 * before the real allocation occurs. The allocation will only take place, if this method returns `true`
	 * as the first entry in the [[Tuple3]]. Inside this method, the new ResourceAllocation's requested SLAs will be
	 * checked against the Host SLA and its currently allocated Resources.
	 *
	 * The expected input of this method is the ResourceAlloc that should be attached to this Host and was handed over to
	 * Host.allocate(resAlloc) before.
	 *
	 * The Output is quite more complex: As an all-embracing Host-to-Resource analysis is done in this method,
	 * the results (as a [[Tuple3]]) are handed over to the calling allocate(resAlloc) method, so that no
	 * calculations, already done here, have to be repeated outside of this method.
	 * @param resToAlloc The [[ResourceAlloc]] that is checkedAgainst the Host's SLA and currently allocated Resources.
	 * @return A [[Tuple3]], with(
	 * - [[Boolean]] = whether the Allocation test completed successfully or not. Only true, if ''all'' resources
	 * 								 could have been allocated by this Host. Partial allocation returns false,
	 * - Option([[HostSLA]]) = the combined, new HostSLA, or None if the combined QoS could not have been fulfilled,
	 * - [[Vector]]([[CPUUnit]], [[Int]]) = negative number of unallocateable Resources)
	 */
	private def testAllocation(resToAlloc: ResourceAlloc): (Boolean, Option[HostSLA], Vector[(CPUUnit, Int)]) = {
		val testedResAlloc: Vector[ResourceAlloc] = allocatedResources :+ resToAlloc

		// Find the hardest, combined HostSLA specification from the actual Host's SLA & the testedResAlloc:
		val combinedTestResSLA: HostSLA = combineHostResSLAs(testedResAlloc)

		// Test if the combinedTestResSLA still fulfills the Host's SLA QoS:
		val fulfillsCombinedQoS = hostSLA.fulfillsQoS(combinedTestResSLA)
		if(! fulfillsCombinedQoS){
			// If the QoS for the resToAlloc could not be fulfilled by the Host,
			// the allocation has no chance to succeed:
			return (false, None, Vector())
		}

		// If the QoS Test was successful,
		// prepare a resource Test that the combined SLA is checked against:
		var resCountByCPU: Vector[(CPUUnit, Int)] = Vector()
		for (actResAlloc <- testedResAlloc) {
			resCountByCPU = actResAlloc.countResourcesByCPU(resCountByCPU)
		}
		val fulfillsResCount: (Boolean, Vector[(CPUUnit, Int)]) = combinedTestResSLA.checkAgainstResPerCPU(resCountByCPU)
		if(! fulfillsResCount._1){
			// If the resources are not completely appliable to this Host, return false, but also append
			// the tested SLA and the resSplitAmount, as a resource-split could possibly
			// make at least some resources allocateable:
			return (false, Option(combinedTestResSLA), fulfillsResCount._2)
		}

		// If every resource could be allocated by this host, return true as the first tuple elem:
		// (the tested SLA and the resSplitAmount are no interesting return values in this case)
		return (true, Option(combinedTestResSLA), fulfillsResCount._2)
	}

	private def combineHostResSLAs(resAlloc: Vector[ResourceAlloc]): HostSLA ={
		// Extract all requested HostSLAs from the ResourceAlloc-Vector:
		val allocatedSLAs: Vector[HostSLA] 	= resAlloc.map(_.requestedHostSLA)
		// Combine all allocated-Resource's SLAs to a hardened QoS SLA:
		val combinedAllocSLA: HostSLA				= allocatedSLAs.reduce(_ combineToAmplifiedSLA _)
		// Afterwards combine this hardened SLA with the actual hostSLA and update this value:
		val combinedHostSLA									= _hostSLA combineToAmplifiedSLA combinedAllocSLA
		return combinedHostSLA
	}


	private def splitAllocation(testedSLA: HostSLA, resToAlloc: ResourceAlloc, resSplitAmount: Vector[(CPUUnit, Int)] ):
														 (ResourceAlloc, ResourceAlloc) = {

		val allocSLA = resToAlloc.requestedHostSLA
		var splitResForHost: Vector[Resource] 				= Vector()
		var splitResForOther: Vector[Resource] 				= Vector()
		//val combinedResAllocs: Vector[ResourceAlloc] 	= this.allocatedResources :+ resToAlloc

		for ((actCPU, actSplitAmount) <- resSplitAmount) {
			val allowedResByCPU: Int 		= testedSLA.maxResPerCPU.find(_._1 == actCPU).get._2

			val resToAllocByCPU: Vector[Resource] = resToAlloc.resources.filter(_.cpu == actCPU)
			val allocedResByCPU: Vector[Resource] = this.allocatedResources.flatMap(_.resources).filter(_.cpu == actCPU)

			// Check if an allocation Split is possible:
			// Another Resource for actCPU can only be allocated, if number of already
			// allocated Resources of that CPU Type is lower than allowedResByCPU
			if(allocedResByCPU.size < allowedResByCPU){
				val (resSplit1, resSplit2) 	= resToAllocByCPU.splitAt(allowedResByCPU + actSplitAmount +1) //actSplitAmount is negative
				splitResForHost = splitResForHost ++ resSplit1
				splitResForOther = splitResForOther ++ resSplit2
			}
			// Else, if allocation is not possible, just add the resToAllocByCPU to splitResForOther:
			else{
				splitResForOther = splitResForOther ++ resToAllocByCPU
			}


		}
		return (ResourceAlloc(splitResForHost, allocSLA), ResourceAlloc(splitResForOther, allocSLA))
	}


	/* Basic Overrides: */
	/* ---------------- */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: Host 	=> this.hardwareSpec == that.hardwareSpec && this.hostSLA == that.hostSLA
		case _ 				=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Host]
}




case class ResourceAlloc(resources: Vector[Resource], requestedHostSLA: HostSLA)
{
	/* Public Methods: */
	/* --------------- */

	def countResourcesByCPU(resCountByCPU: Vector[(CPUUnit, Int)] = Vector()) : Vector[(CPUUnit, Int)] = {
		// Used inner Functions:
		def func_reduceto_cpusum(t1: (CPUUnit, Int), t2: (CPUUnit, Int)): (CPUUnit, Int) = {
			//For equal CPUUnits
			val sum = t1._2 + t2._2
			return (t1._1, sum)
		}

		var dirtyResCountByCPU = resCountByCPU
		// Filter this ResourceAlloc by each CPUUnit and fill the resCount Vector with the Resource data:
		for (actCPUUnit <- CPUUnit.values) {
			val resAllocByCPU: Vector[Resource] = this.resources.filter(_.cpu == actCPUUnit)
			dirtyResCountByCPU = dirtyResCountByCPU :+(actCPUUnit, resAllocByCPU.size)
		}
		var cleanedResCountByCPU: Vector[(CPUUnit, Int)] = Vector()
		for (actCPUUnit <- CPUUnit.values) {
			// For each CPUUnit reduce the matching tuples to only one Tuple per CPUUnit, summing the Ints up:
			cleanedResCountByCPU = cleanedResCountByCPU :+ dirtyResCountByCPU.filter(_._1 == actCPUUnit).reduce(func_reduceto_cpusum)
		}
		return cleanedResCountByCPU

	}

	/* Basic Overrides: */
	/* ---------------- */

	override def equals(obj: scala.Any): Boolean = obj match {
		case that: ResourceAlloc 	=> this.resources == that.resources
		case _ 							=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[ResourceAlloc]
}




/* Ordering Objects for Resource Container: */
/* ======================================== */

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



/* Ordering Objects for Host Container: */
/* ==================================== */

object RelativeHostByResOrdering extends Ordering[Host]{
	override def compare(x: Host, y: Host): Int = {
		return RelativeResOrdering.compare(x.hardwareSpec, y.hardwareSpec)
	}
}



