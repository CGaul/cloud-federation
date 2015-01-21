package datatypes

import java.io.File
import java.net.{InetAddress, UnknownHostException}

import datatypes.CPUUnit._

import scala.xml.Node



case class Tenant(tenantId: Int, subnet: (String, Short), ofcIp: InetAddress, ofcPort: Short)


/**
 * Desribes a Network-Component. The base trait for Hosts and Switches
 */
sealed trait NetworkComponent

case class DPID(dpid: String) {
  // Catch an IllegalArgumentException here, if DPID is badly defined:
  require(dpid.split(":").length > 1 && dpid.split(":").forall(_.length == 2),  
    "Bad DPID in \""+dpid+"\": Need to define n Hex-Code Pairs, separated by a colon (e.g. \"xx:xx:xx:xx\")")
  
  override def toString: String = dpid
}

case class Endpoint(dpid: DPID, port: Short) {
  def this(dpid: String, port: Short) = this(DPID(dpid), port)
}

object Endpoint{
  // Additional Apply Methods:
  def apply(dpid: DPID, port: Int): Endpoint = new Endpoint(dpid, port.toShort)
  def apply(dpid: String, port: Int): Endpoint = new Endpoint(dpid, port.toShort)
  def apply(dpid: String, port: Short): Endpoint = new Endpoint(dpid, port)


  /* Serialization: */
  /* ============== */

  def toString(endpoint: Endpoint): String = {
    return s"Endpoint(${endpoint.dpid.toString},${endpoint.port.toString})"
  }

  def toXML(endpoint: Endpoint): Node =
    <Endpoint>
      <DPID>{endpoint.dpid.toString}</DPID>
      <Port>{endpoint.port.toString}</Port>
    </Endpoint>


  def saveToXML(file: File, endpoint: Endpoint) = {
    val xmlNode = toXML(endpoint)
    xml.XML.save(file.getAbsolutePath, xmlNode)
  }

  /* De-Serialization: */
  /* ================= */

  def fromString(epStr: String): Endpoint = {
    require(epStr.contains("Endpoint(") && epStr.endsWith(")"), s"Bad String format for Endpoint-String $epStr!")
    val valList: Array[String] = epStr.split("Endpoint")(1).replace("(", "").replace(")", "").split(",")
    assert(valList.length == 2, s"Parameters of Endpoint were not extracted correctly fromString $epStr")

    return Endpoint(DPID(valList(0)), valList(1).toShort)
  }

  def fromXML(node: Node): Endpoint = {
    var dpNode = node
    if((node \ "Endpoint").text != "")
      dpNode = (node \ "Endpoint")(0)
    
    val dpid: DPID = DPID((dpNode \ "DPID").text)
    val port: Short = (dpNode \ "Port").text.toShort

    return Endpoint(dpid, port)
  }

  def loadFromXML(file: File): Endpoint = {
    val xmlNode = xml.XML.loadFile(file)
    return fromXML(xmlNode)
  }
}


/**
 * The representative data class of an OpenFlow-Switch.
 * @param dpid The OpenFlow "data path id" that uniquely describes this OpenFlow-Switch
 * @param _portMap A mapping from Port-Number (as Short) to the connected Network-Component (Host or other Switch)
 */
case class OFSwitch(dpid: DPID, var _portMap: Map[Short, Endpoint])
  extends NetworkComponent{

  def this(dpid: DPID) = this(dpid, _portMap = Map())
  def this(dpid: String) = this(DPID(dpid), _portMap = Map())

  def remapPorts(portMap: Map[Short, Endpoint]): Unit ={
    this._portMap = portMap
  }

  def remapPort(port: Short, endpoint: Endpoint) = {
    this._portMap = _portMap + (port -> endpoint)
  }

  def portMap = _portMap

  /* Basic Overrides: */
  /* ---------------- */

  override def equals(obj: scala.Any): Boolean = obj match{
    case that: OFSwitch => this.dpid == that.dpid
    case _ 							=> false
  }

  override def canEqual(that: Any): Boolean = that.isInstanceOf[OFSwitch]

}

/**
 * Companion Object for Switch
 */
object OFSwitch {
// Additional Apply Methods:
  def apply(dpid: DPID): OFSwitch = new OFSwitch(dpid)
  def apply(dpid: String): OFSwitch = new OFSwitch(dpid)

  
  /* Serialization: */
  /* ============== */

  def toXML(switch: OFSwitch): Node =
    <Switch>
      <DPID>{switch.dpid.toString}</DPID>
      <Links>{switch.portMap.map(l => l._1.toString +" - "+ Endpoint.toString(l._2)).mkString(", ")}</Links>
    </Switch>


  def saveToXML(file: File, switch: OFSwitch) = {
    val xmlNode = toXML(switch)
    xml.XML.save(file.getAbsolutePath, xmlNode)
  }

  /* De-Serialization: */
  /* ================= */

  def fromXML(node: Node): OFSwitch = {
    var switchNode = node
    if((node \ "Switch").text != "")
      switchNode = (node \ "Switch")(0)
    
    val dpid: DPID = DPID((switchNode \ "DPID").text)
    val linkIter: Iterable[(Short, Endpoint)] = (switchNode \ "Links").text.trim.split(", ").map(
      l => (l.split(" - ")(0).trim.toShort, Endpoint.fromString(l.split(" - ")(1).trim)))
    var links: Map[Short, Endpoint] = Map()

    for (actLink <- linkIter) {
      links = links + (actLink._1 -> actLink._2)
    }

    return OFSwitch(dpid, links)
  }

  def loadFromXML(file: File): OFSwitch = {
    val xmlNode = xml.XML.loadFile(file)
    return fromXML(xmlNode)
  }
}



//TODO: use http://docs.scala-lang.org/style/scaladoc.html to go on with ScalaDocs in code.
/**
 *  ==How to use==
 *  {{{
 *     val host = Host
 *  }}}
 * @constructor create a new Host, specifying the Host's Hardware Spec, the initial Allocation (if any)
 *             and the Host's SLA
 */
case class Host(hardwareSpec: Resource, endpoint: Endpoint, ip: InetAddress, mac: String,
                var allocatedResources: Vector[ResourceAlloc] = Vector(),
                var sla: HostSLA)
  extends NetworkComponent{

  /* Getters: */
  /* -------- */

  def hostId = hardwareSpec.resId


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
      {Endpoint.toXML(host.endpoint)}
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
    var hostNode = node
    // Check, whether Node is complete Host-XML or only inner values:
    if((node \ "Host").text != "")
      hostNode = (node \ "Host")(0)
    
    val hardwareSpec: Resource = Resource.fromXML((hostNode \ "Hardware")(0))
    val endpoint: Endpoint = Endpoint.fromXML(hostNode)
    var hostIP: InetAddress = InetAddress.getLoopbackAddress
    try{
      hostIP = InetAddress.getByName((hostNode \\ "IP").text.trim)
    }
    catch{
      case e: UnknownHostException =>
        System.err.println(s"Address: ${(hostNode \\ "IP").text.trim} could not have been solved. Using Loopback Address")
    }
    val hostMAC: String = (hostNode \\ "MAC").text
    var allocRes: Vector[ResourceAlloc] = Vector()
    for (actResAlloc <- hostNode \\ "ResourceAllocs") {
      //Only parse, if the actual ResourceAlloc is existing.
      if(actResAlloc.text.trim != ""){
        allocRes = allocRes :+ ResourceAlloc.fromXML(actResAlloc)
      }
    }
    val hostSLA: HostSLA = HostSLA.fromXML((hostNode \ "HostSLA")(0))

    return Host(hardwareSpec, endpoint, hostIP, hostMAC, allocRes, hostSLA)
  }

  def loadFromXML(file: File): Host = {
    val xmlNode = xml.XML.loadFile(file)
    return fromXML(xmlNode)
  }
}
  
