package datatypes

import java.io.File
import java.net.{InetAddress, UnknownHostException}

import datatypes.CPUUnit._

import scala.xml.Node


/* Data Containers (case classes): */
/* =============================== */

/**
 * Just a wrapper case class for an Integer ID.
 * Used for syntactic Sugar in the Code.
 * @param id The unique Component-ID of a NetworkComponent (a Host or Switch)
 */
case class CompID(id: Int){
	override def toString: String = id.toString
}
/**
 * Companion Object for CompID
 */
object CompID {

 def fromString(str: String): CompID = {
	 return CompID(str.trim.toInt)
 }
}

/**
 * Desribes a Network-Component. The base trait for Hosts and Switches
 */
sealed trait NetworkComponent

/**
 *
 * @param compID The ComponentID that is representing this resource. For VMs, this is the compID of the hypervising host
 *               (which is also represented as a Resource). May be None for Resource Requests.
 * @param cpu CPU Speed on Node [CPUUnit]
 * @param ram Amount of RAM on Node [ByteSize]
 * @param storage Amount of Storage on Node [ByteSize]
 * @param bandwidth Bandwidth, relatively monitored from GW to Node [ByteSize]
 * @param latency Latency, relatively monitored from GW to Node [ms]
 */
case class Resource(compID: CompID,
						  cpu: CPUUnit,
						  ram: ByteSize,
						  storage: ByteSize,
						  bandwidth: ByteSize,
						  latency: Float,
						  links: Vector[CompID])
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

/**
 * Companion Object for Resource
 */
object Resource {

/* Serialization: */
/* ============== */

	def toXML(resource: Resource): Node =
		<Resource>
			<ID>{resource.compID.toString}</ID>
			<CPU>{resource.cpu.toString}</CPU>
			<RAM>{ByteSize.toXML(resource.ram)}</RAM>
			<Storage>{ByteSize.toXML(resource.storage)}</Storage>
			<Bandwidth>{ByteSize.toXML(resource.bandwidth)}</Bandwidth>
			<Latency>{resource.latency}</Latency>
			<Links>{resource.links.mkString(" ")}</Links>
		</Resource>

	def saveToXML(file: File, resource: Resource) = {
		val xmlNode = toXML(resource)
		xml.XML.save(file.getAbsolutePath, xmlNode)
	}

/* De-Serialization: */
/* ================= */

	def fromXML(node: Node): Resource = {
		val compID: CompID 			= CompID.fromString((node \\ "ID").text)
		val cpu: CPUUnit 	 			= CPUUnit.fromString((node \\ "CPU").text)
		val ram: ByteSize	 			= ByteSize.fromString((node \\ "RAM").text)
		val storage: ByteSize		= ByteSize.fromString((node \\ "Storage").text)
		val bandwidth: ByteSize	= ByteSize.fromString((node \\ "Bandwidth").text)
		val latency: Float			= (node \\ "Latency").text.toFloat
		val links: Vector[CompID] = if((node \\ "Links").text.trim == "") {Vector()}
																else {(node \\ "Links").text.trim.split(" ").map(str => CompID.fromString(str)).toVector}

		return Resource(compID, cpu, ram, storage, bandwidth, latency, links)
	}

	def loadFromXML(file: File): Resource = {
		val xmlNode = xml.XML.loadFile(file)
		return fromXML(xmlNode)
	}
}


/**
 * The representative data class of a Network-Switch.
 * @param id
 * @param dpid
 * @param links A mapping from Port-Number (as Short) to the connected Network-Component (Host or other Switch)
 */
case class Switch(id: CompID, dpid: String, links: Map[Short, CompID])//switchLinks: Vector[CompID], hostLinks: Vector[CompID])
extends NetworkComponent{

	/* Basic Overrides: */
	/* ---------------- */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: Switch => this.id == that.id && this.dpid == that.dpid
		case _ 						=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Host]

}

/**
 * Companion Object for Switch
 */
object Switch {

	/* Serialization: */
	/* ============== */

	def toXML(switch: Switch): Node =
		<Switch>
			<ID>{switch.id.toString}</ID>
			<DPID>{switch.dpid}</DPID>
			<Links>{switch.links.map(l => l._1.toString +":"+ l._2.toString).mkString(", ")}</Links>
		</Switch>


	def saveToXML(file: File, switch: Switch) = {
		val xmlNode = toXML(switch)
		xml.XML.save(file.getAbsolutePath, xmlNode)
	}

	/* De-Serialization: */
	/* ================= */

	def fromXML(node: Node): Switch = {
		val switchID: CompID = CompID.fromString((node \ "ID").text)
		val switchDPID: String = (node \ "DPID").text
		val linkIter: Iterable[(Short, CompID)] = (node \ "Links").text.trim.split(", ").map(
																								l => (l.split(":")(0).trim.toShort, CompID(l.split(":")(1).trim.toInt)))
		var links: Map[Short, CompID] = Map()

		for (actLink <- linkIter) {
			links = links + (actLink._1 -> actLink._2)
		}
//		val switchLinks: Vector[CompID] = (node \ "Links").text.trim.split(" ").map(CompID.fromString).toVector
//		val hostLinks: Vector[CompID] = (node \ "HostLinks").text.trim.split(" ").map(CompID.fromString).toVector


		return Switch(switchID, switchDPID, links)
	}

	def loadFromXML(file: File): Switch = {
		val xmlNode = xml.XML.loadFile(file)
		return fromXML(xmlNode)
	}
}


case class Tenant(tenantId: Int, subnet: (String, Short), ofcIp: InetAddress, ofcPort: Short)


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
 * @param sla bla
 */
case class Host(hardwareSpec: Resource, ip: InetAddress, mac: String,
					 var allocatedResources: Vector[ResourceAlloc] = Vector(),
					 var sla: HostSLA)
extends NetworkComponent{

	/* Getters: */
	/* -------- */

	def compID = hardwareSpec.compID


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
	 * @return A [[Tuple3]] that states, if the at least some of the resToAlloc
	 *         where allocated (result._1 == true), returns the ResourceAlloc
	 *         Split that is left over after the allocation at this Host
	 *         (result._1 == None if resToAlloc was fully allocated, otherwise subset of resToAlloc)
	 *         and the allocated resources at the third position of the Tuple (None if nothing was allocated).
	 */
	def allocate(resToAlloc: ResourceAlloc): (Boolean, Option[ResourceAlloc], Option[ResourceAlloc]) = {
		val (success, testedSLA, resSplitAmount) = testAllocation(resToAlloc)
		// If the allocation Test was successful,
		// all resToAlloc could be fulfilled by this Host.
		if(success){
			//Add the resToAlloc to Host's allocatedResources:
			allocatedResources = allocatedResources :+ resToAlloc

			//Update the Host's HostSLA with the tested pre-allocation SLA
			sla = testedSLA.get
			// Complete Allocation (allocatedSome = true, non-allocated Split = None, allocation = resToAlloc):
			return (true, None, Option(resToAlloc))
		}
		// If the Test was not successful, only a part or no resources at all
		// could be allocated by this Host.
		else{
			// If the Option[HostSLA] is None, no allocation of the resToAlloc
			// could be handled by this host at all.
			if(testedSLA.isEmpty){
				// No Allocation (allocatedSome = false, non-allocated Split = resToAlloc, allocation = None):
				return (false, Option(resToAlloc), None)
			}
			// Otherwise (if the testedSLA is not None), a ResourceAlloc split needs
			// to be defined. The one split is allocated at this Host, the other part is
			// returned as a result of the Tuple2 of this method
			else{
				val (resSplitHost, resSplitOther) = splitAllocation(testedSLA.get, resToAlloc, resSplitAmount)
				if(resSplitHost.resources.size > 0){
					allocate(resSplitHost)
					// Partial Allocation (allocatedSome = true, non-allocated Split = resSplitOther, allocation = resSplitHost):
					return (true, Option(resSplitOther), Option(resSplitHost))
				}
				else {
					// No Allocation (allocatedSome = false, non-allocated Split = resSplitOther (=resToAlloc), allocation = None):
					return (false, Option(resSplitOther), None)
				}
			}
		}
	}


	/* Private Methods: */
	/* ---------------- */

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
		val fulfillsCombinedQoS = sla.fulfillsQoS(combinedTestResSLA)
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
		val combinedHostSLA									= sla combineToAmplifiedSLA combinedAllocSLA
		return combinedHostSLA
	}


	private def splitAllocation(testedSLA: HostSLA, resToAlloc: ResourceAlloc, resSplitAmount: Vector[(CPUUnit, Int)] ):
														 (ResourceAlloc, ResourceAlloc) = {

		val allocTenant = resToAlloc.tenantID
		val allocSLA 		= resToAlloc.requestedHostSLA
		var splitResForHost: Vector[Resource] 				= Vector()
		var splitResForOther: Vector[Resource] 				= Vector()

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
		return (ResourceAlloc(allocTenant, splitResForHost, allocSLA),
						ResourceAlloc(allocTenant, splitResForOther, allocSLA))
	}


	/* Basic Overrides: */
	/* ---------------- */

	override def equals(obj: scala.Any): Boolean = obj match{
		case that: Host 	=> this.hardwareSpec == that.hardwareSpec && this.sla == that.sla &&
												 this.ip == that.ip && this.mac == that.mac
		case _ 						=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[Host]

}

/**
 * Companion Object for Host
 */
object Host {

/* Serialization: */
/* ============== */

	def toXML(host: Host): Node =
		<Host>
			<Hardware>{Resource.toXML(host.hardwareSpec)}</Hardware>
			<IP>{host.ip.getHostAddress}</IP>
			<MAC>{host.mac}</MAC>
			<ResourceAllocs>{host.allocatedResources.map(resAlloc => ResourceAlloc.toXML(resAlloc))}</ResourceAllocs>
			<HostSLA>{HostSLA.toXML(host.sla)}</HostSLA>
		</Host>

	def saveToXML(file: File, host: Host) = {
		val xmlNode = toXML(host)
		xml.XML.save(file.getAbsolutePath, xmlNode)
	}

//	def toJson(host: Host): JsValue = {
//		val jsonHost = Json.toJson(Map("Host" -> Seq(CompID.toJson(host.compID), host.ip, host.mac, host.allocatedResources
//	}

/* De-Serialization: */
/* ================= */

	def fromXML(node: Node): Host = {
		val hardwareSpec: Resource = Resource.fromXML((node \ "Hardware")(0))
		var hostIP: InetAddress = InetAddress.getLoopbackAddress
		try{
			hostIP = InetAddress.getByName((node \\ "IP").text.trim)
		}
		catch{
			case e: UnknownHostException =>
				System.err.println(s"Address: ${(node \\ "IP").text.trim} could not have been solved. Using Loopback Address")
		}
		val hostMAC: String = (node \\ "MAC").text
		var allocRes: Vector[ResourceAlloc] = Vector()
		for (actResAlloc <- node \\ "ResourceAllocs") {
			//Only parse, if the actual ResourceAlloc is existing.
			if(actResAlloc.text.trim != ""){
				allocRes = allocRes :+ ResourceAlloc.fromXML(actResAlloc)
			}
		}
		val hostSLA: HostSLA = HostSLA.fromXML((node \ "HostSLA")(0))

		return Host(hardwareSpec, hostIP, hostMAC, allocRes, hostSLA)
	}

	def loadFromXML(file: File): Host = {
		val xmlNode = xml.XML.loadFile(file)
		return fromXML(xmlNode)
	}
}




case class ResourceAlloc(tenantID: Int, resources: Vector[Resource], requestedHostSLA: HostSLA)
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
		case that: ResourceAlloc 	=> this.tenantID == that.tenantID && this.resources == that.resources
		case _ 							=> false
	}

	override def canEqual(that: Any): Boolean = that.isInstanceOf[ResourceAlloc]
}

/** 
 * Companion Object for ResourceAlloc
 */
object ResourceAlloc {


//	def apply(tenantID: Int, resources: Vector[Resource], requestedHostSLA: HostSLA): ResourceAlloc ={
//		return new ResourceAlloc(tenantID, resources, requestedHostSLA)
//	}
	
/* Serialization: */
/* ============== */

	def toXML(resourceAlloc: ResourceAlloc): Node =
		<ResourceAlloc>
			<TenantID>{resourceAlloc.tenantID}</TenantID>
			<Resources>{resourceAlloc.resources.map(res => Resource.toXML(res))}</Resources>
			<ReqHostSLA>{HostSLA.toXML(resourceAlloc.requestedHostSLA)}</ReqHostSLA>
		</ResourceAlloc>

	def saveToXML(file: File, resource_alloc: ResourceAlloc) = {
		val xmlNode = toXML(resource_alloc)
		xml.XML.save(file.getAbsolutePath, xmlNode)
	}

/* De-Serialization: */
/* ================= */

	def fromXML(node: Node): ResourceAlloc = {
		val tenantID: Int 							= (node \\ "TenantID").text.toInt
		var resources: Vector[Resource] = Vector()
		for (actResNode <- node \\ "Resources") {
			resources = resources :+ Resource.fromXML(actResNode)
		}
		val reqHostSLA: HostSLA 				= HostSLA.fromXML((node \\ "ReqHostSLA")(0))

		return ResourceAlloc(tenantID, resources, reqHostSLA)
	}

	def loadFromXML(file: File): ResourceAlloc = {
		val xmlNode = xml.XML.loadFile(file)
		return fromXML(xmlNode)
	}	
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



