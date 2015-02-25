package datatypes

import java.io.File

import datatypes.CPUUnit._

import scala.xml.Node


/* Data Containers (case classes): */
/* =============================== */

/**
 * Just a wrapper case class for an Integer ID.
 * Used for syntactic Sugar in the Code.
 * @param id The unique Component-ID of a NetworkComponent (a Host or Switch)
 */
case class ResId(id: Int){
	override def toString: String = id.toString
}
/**
 * Companion Object for CompID
 */
object ResId {

 def fromString(str: String): ResId = {
	 return ResId(str.trim.toInt)
 }
}



/**
 *
 * @param resId The Resource-Id that is representing this resource. For VMs, this is the resId of the hypervising host
 *               (which is also represented as a Resource). May be None for Resource Requests.
 * @param cpu CPU Speed on Node [CPUUnit]
 * @param ram Amount of RAM on Node [ByteSize]
 * @param storage Amount of Storage on Node [ByteSize]
 * @param bandwidth Bandwidth, relatively monitored from GW to Node [ByteSize]
 * @param latency Latency, relatively monitored from GW to Node [ms]
 */
case class Resource(resId: ResId,
						  cpu: CPUUnit,
						  ram: ByteSize,
						  storage: ByteSize,
						  bandwidth: ByteSize,
						  latency: Float,
						  links: Vector[ResId])
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
			<ID>{resource.resId.toString}</ID>
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
		var resNode = node
		// Check, whether Node is complete Resource-XML or only inner values:
		if((node \ "Resource").text != "")
			resNode = (node \ "Resource")(0)
		
		val compID: ResId 			= ResId.fromString((resNode \\ "ID").text)
		val cpu: CPUUnit 	 			= CPUUnit.fromString((resNode \\ "CPU").text)
		val ram: ByteSize	 			= ByteSize.fromString((resNode \\ "RAM").text)
		val storage: ByteSize		= ByteSize.fromString((resNode \\ "Storage").text)
		val bandwidth: ByteSize	= ByteSize.fromString((resNode \\ "Bandwidth").text)
		val latency: Float			= (resNode \\ "Latency").text.toFloat
		val links: Vector[ResId] = if((resNode \\ "Links").text.trim == "") {Vector()}
																else {(resNode \\ "Links").text.trim.split(" ").map(str => ResId.fromString(str)).toVector}

		return Resource(compID, cpu, ram, storage, bandwidth, latency, links)
	}

	def loadFromXML(file: File): Resource = {
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
		case that: ResourceAlloc 	=> (that canEqual this) &&
                                 this.tenantID == that.tenantID &&
                                 this.resources == that.resources &&
                                 this.requestedHostSLA == that.requestedHostSLA
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
		var resAllocNode = node
		// Check, whether Node is complete Resource-XML or only inner values:
		if((node \ "ResourceAlloc").text != "")
			resAllocNode = (node \ "ResourceAlloc")(0)
		
		val tenantID: Int = (resAllocNode \\ "TenantID").text.toInt
		var resources: Vector[Resource] = Vector()
		
		for (actResNode <- resAllocNode \ "Resources" \\ "Resource") {
			resources = resources :+ Resource.fromXML(actResNode)
		}
		val reqHostSLA: HostSLA = HostSLA.fromXML((resAllocNode \\ "ReqHostSLA")(0))

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
		return Math.round((x.ram compareTo y.ram) * ResOrderingMult.RAM_MULT)
	}
}
object StorageResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((x.storage compareTo y.storage) * ResOrderingMult.STORAGE_MULT)
	}
}
object BandwidthResOrdering extends Ordering[Resource]{
	override def compare(x: Resource, y: Resource): Int = {
		return Math.round((x.bandwidth compareTo y.bandwidth) * ResOrderingMult.BDWTH_MULT)
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



