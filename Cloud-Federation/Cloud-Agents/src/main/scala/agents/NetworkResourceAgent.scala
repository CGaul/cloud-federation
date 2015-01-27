package agents

import java.net._

import akka.actor._
import connectors.{OVXConnector, VirtualNetwork, VirtualSwitch}
import datatypes._
import messages._

import scala.util.control.Breaks._

/**
 * @author Constantin Gaul, created on 10/15/14.
 */
class NetworkResourceAgent(ovxIp: InetAddress, ovxApiPort: Int, val cloudHosts: List[Host],
													 matchMakingAgent: ActorRef)
													extends Actor with ActorLogging with Stash
{
	
/* Values: */
/* ======= */
	
		val _ovxConn = OVXConnector(ovxIp, ovxApiPort)
		val _ndaActor: ActorRef = initChildActors()


/* Variables: */
/* ========== */
		// Physical Topologies, received from the CCFM (hosts) and the NDA (switches)
		var _hostTopology: List[Host] = cloudHosts
		var _switchTopology: List[OFSwitch] = List()
	
		// Physical Mappings:
		var _hostPhysSwitchMap: Map[Host, List[OFSwitch]] = Map()
		var _tenantPhysSwitchMap: Map[Tenant, List[OFSwitch]] = Map()
		var _switchPortMap: Map[OFSwitch, List[(Short, Short, Option[NetworkComponent])]] = Map()
	
		// Virtual Mappings:
		var _tenantNetMap: Map[Tenant, VirtualNetwork] = Map()
		var _tenantVirtSwitchMap: Map[Tenant, List[VirtualSwitch]] = Map()


/* Initial Startup: */
/* ================ */
	
	initActor()
	
	def initActor() = {
		// This NRA-Instance is inactive after boot-up:
		context.become(inactive())
		log.info("NetworkResourceAgent will be INACTIVE, until NDA sends a TopologyDiscovery...")
	}
	
	def initChildActors(): ActorRef = {
		// Spawn a NetworkDiscoveryAgent (NDA) for Network-Topology-Discovery:
		val ndaProps: Props = Props(classOf[NetworkDiscoveryAgent], ovxIp, ovxApiPort, context.self)
		val ndaActor = context.actorOf(ndaProps, name="networkDiscoveryAgent")
		log.info("NetworkDiscoveryAgent started at path: {}", ndaActor.path)
		ndaActor ! "start"
		
		return ndaActor
	}
	
	
/* Public Methods: */
/* =============== */
	
	def active(): Receive = {
		case message: NRAResourceDest	=> message match {
			case ResourceRequest(tenant, resourcesToAlloc)
			=> recvResourceRequest(tenant, resourcesToAlloc)

			case ResourceFederationRequest(tenant, resourcesToAlloc)
			=> recvResourceFederationRequest(tenant, resourcesToAlloc)

			case ResourceFederationReply(resourcesAllocated)
			=> recvResourceFederationReply(resourcesAllocated)
		}
			
		case message: NRANetworkDest => message match{
			case TopologyDiscovery(switchList)
			=> 	recvTopologyDiscovery(switchList)
				log.info("New TopologyDiscovery received.")
		}
		case _	=> log.error("Unknown message received!")
	}
	
	def inactive(): Receive = {
		case message: NRAResourceDest	=> message match {
			case _ => stash()
		}
			
		case message: NRANetworkDest => message match{
			case TopologyDiscovery(switchList)
			=> 	recvTopologyDiscovery(switchList)
				unstashAll()
				context.become(active())
				log.info("NetworkResourceAgent is becoming ACTIVE, as TopologyDiscovery was received!")
		}
		case _	=> log.error("Unknown message received!")
	}

	def receive: Receive = inactive()

	
/* Private Receiving Methods: */
/* ========================== */

	/**
	 * Receives ResourceRequests from the CCFM.
	 * <p>
	 * 	Either local Cloud's Resources are sufficient, then the Request could be
	 * 	assigned to local resources only. If the local Resources are insufficient,
	 * 	split the Request, using all locally available resources and forward the
	 * 	federation part of the splitted Request to the MatchMakingAgent,
	 * 	who should then send a ResourceReply back to this NetworkResourceAgent,
	 * 	if all Federation attempts are finished,
	 * 	or all Resources could have been allocated.
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * Implemented in 0.2 Integrated Controllers
	 * @param tenant
	 * @param resourceToAlloc
	 */
	private def recvResourceRequest(tenant: Tenant, resourceToAlloc: ResourceAlloc): Unit = {

		log.info("Received ResourceRequest (Tenant: {}, ResCount: {}) at NetworkResourceAgent.",
			tenant, resourceToAlloc.resources.size)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourceToAlloc)

		// Prepare the locally fulfilled allocations that will be send to OVX via
		// the own OVXConnector-API:
		val hostList = allocationsPerHost.map(_._1).toList
		log.info("Mapping hosts {} to virtual tenant network.", hostList.map(_.mac))
		mapAllocOnOVX(tenant, hostList)
//		log.info("Send json-Query {} to OVX Hypervisor", jsonQuery) TODO: delete

		// If there is still a ResourceAlloc remaining, after the local cloudHosts tried to
		// allocate the whole ResourceAlloc-Request, send the remaining ResourceAlloc Split
		// to the MatchMakingAgent, in order to find a Federated Cloud that cares about the Resources:
		if(remainResToAlloc.isDefined){
			log.info("ResourceRequest {} could	 not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceToAlloc, remainResToAlloc)
			matchMakingAgent ! ResourceRequest(tenant, remainResToAlloc.get)
		}
		else log.info("ResourceRequest {} was completely allocated on the local cloud!", resourceToAlloc)
	}

	//TODO: Shortcut Implementation in 0.2 Integrated Controllers
	/**
	 * Receives a ResourceFederationReply from the MatchMakingAgent.
	 * <p>
	 * 	If a ResourceRequest could not have been processed locally,
	 * 	the NetworkFederationAgent has asked the MatchMakingAgent
	 * 	for Federation-Resources.
	 * 	All results are included in such ResourceFederationReply,
	 * 	stating the allocated Resources per foreign Cloud
	 * 	(the ActorRef is the foreign MatchMakingAgent)
	 * 	*
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * @param federationResAllocs
	 */
	private def recvResourceFederationReply(federationResAllocs: Vector[(ActorRef, ResourceAlloc)]): Unit = {
	}


	//TODO: Shortcut Implementation in 0.2 Integrated Controllers
	/**
	 * Received from local MMA.
	 *
	 * @param resourcesToAlloc
	 * @param tenant
	 */
	private def recvResourceFederationRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc): Unit = {
		log.info("Received ResourceFederationRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NetworkResourceAgent.",
			resourcesToAlloc.tenantID, resourcesToAlloc.resources.size, tenant.ofcIp)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourcesToAlloc)
		// Prepare the locally fulfilled allocations as a JSON-Message
		// that will be send to the OVX embedder:
		val hostList = allocationsPerHost.map(_._1).toList
		mapAllocOnOVX(tenant, hostList)

//		log.info("Send json-Query {} to OVX Hypervisor", jsonQuery) TODO: delete

		if(remainResToAlloc.size > 0){
			// TODO: send Information about remaing Resources to Allocate back to the sender.
		}
	}
	
	private def recvTopologyDiscovery(switchTopology: List[OFSwitch]) = {
		log.info("Received new Switch-Topology from {}, including {} switches.", sender(), switchTopology.length)
		this._switchTopology = switchTopology
		this._hostPhysSwitchMap = _hostPhysSwitchMap ++ _hostTopology.map(host => host -> switchTopology.filter(_.dpid == host.endpoint.dpid))
	}


/* Private Helper Methods: */
/* ======================= */

	private def allocateLocally(resourceAlloc: ResourceAlloc): (Map[Host, ResourceAlloc], Option[ResourceAlloc]) = {
		// Will be filled with each allocation per Host that happened in this local allocation call:
		var allocationPerHost: Map[Host, ResourceAlloc] = Map()

		// Sort the potentialHosts as well as the resourceToAlloc by their resources in descending Order:
		val sortedHosts			= _hostTopology.sorted(RelativeHostByResOrdering)
		val sortedResAlloc	= ResourceAlloc(resourceAlloc.tenantID,
			resourceAlloc.resources.sorted(RelativeResOrdering),
			resourceAlloc.requestedHostSLA)

		// Binpacking - First Fit Descending:
		// Fit each resourceToAlloc in the first potentialHost (bin)
		// that is fulfilling the resource & combined SLA requirements:
		var remainResAlloc: Option[ResourceAlloc]	= Option(sortedResAlloc)
		breakable {
			for (actHost <- sortedHosts) {
				// Try to allocate the remaining ResourceAlloc to the actual Host:
				val (allocatedSome, allocSplit, allocation) = actHost.allocate(remainResAlloc.get)

				// If an allocation took place, save this in the allocationPerHost-Map:
				if(allocatedSome && allocation.isDefined){
					allocationPerHost += (actHost -> allocation.get)
				}
				// If the actual ResourceAlloc could be allocated completely to the actHost,
				// set the remaining ResourceAlloc to None and break out of the loop.
				if (allocatedSome && allocSplit.isEmpty) {
					remainResAlloc = None
					break
				}

				// If not the whole ResourceAlloc could be allocated to the actHost,
				// the remainResAlloc for the next iteration is the allocSplit of this iteration:
				if (allocSplit.isDefined)  {
					remainResAlloc = allocSplit
				}
			}
		}
		return (allocationPerHost, remainResAlloc)
	}
	
	//TODO: Implement in 0.3 - Federated Agents
	private def mapAllocOnOVX(tenant: Tenant, hosts: List[Host]) = {

		// If the tenant does not have an OVX tenant-network until now, create one:
		if (!_tenantNetMap.keys.exists(_ == tenant)) {
			_createOVXNetwork(tenant)
		}
		
		// The _hostPhysSwitchMap defines a mapping between each known host and all physical Switches, 
		// that are connected to this host and discovered via a TopologyDiscovery earlier.
		// To continue, get all physical Switches for the given host's allocation:
		// (get a flat list of each host -> List[Switch] mapping)
		val physSwitches = hosts.flatMap(_hostPhysSwitchMap).distinct
		
		// Afterwards, try to solve a path from each host to all other hosts, using all
		// currently discovered physical Switches as direct gateway to them:
		_discoverHostPaths()

		// Create virtual switches for all physical Switches that are not yet part of a virtual switch in the tenant's vNet
		// and establish virtual Ports for all physical Ports that are outgoing to any other Switch:
		for (actPhysSwitch <- physSwitches 
				 if !(_tenantVirtSwitchMap.keys.exists(_ == tenant) &&
						_tenantVirtSwitchMap(tenant).exists(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))))
		{
			// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
			_createOVXSwitch(tenant, actPhysSwitch)

			// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
			_createOVXSwitchPorts(tenant, actPhysSwitch)
		}
		
		
		// Iterate over all physical Switches, get their Port-mapping from _switchPortMap
		// and connect their virtual counterparts to each other on the correct virtPorts (create all topology paths):
		for (actPhysSwitch <- _tenantPhysSwitchMap(tenant)) {
			for ((srcPort, srcEndpoint) <- actPhysSwitch.portMap) {
				val physSrcSwitch = actPhysSwitch
				val physDstSwitchOpt = _tenantPhysSwitchMap.getOrElse(tenant, List()).find(_.dpid == srcEndpoint.dpid)
				// As the physical Destination Switch might not be in the tenant's switchMap, only continue connection if both
				// src- and dst-Switch are known:
				if(physDstSwitchOpt.isDefined) {
					val physDstSwitch = physDstSwitchOpt.get
					// Find the srcPortMapping for the actual srcPort in the _switchPortMap's actPhysSwitch entry:
					val srcPortMapping = _switchPortMap.getOrElse(physSrcSwitch, List()).
																find(_._1 == srcPort)
					val dstPortMapping = _switchPortMap.getOrElse(physDstSwitch, List()).
																find(_._1 == srcEndpoint.port)
					val virtSrcSwitch = _tenantVirtSwitchMap.getOrElse(tenant, List()).
																find(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))
					val virtDstSwitch = _tenantVirtSwitchMap.getOrElse(tenant, List()).
																find(_.dpids.contains(srcEndpoint.dpid.convertToHexLong))

					if (srcPortMapping.isDefined && dstPortMapping.isDefined && virtSrcSwitch.isDefined && virtDstSwitch.isDefined) {
						val (physSrcPort, virtSrcPort, srcComponent) = srcPortMapping.get
						val (physDstPort, virtDstPort, dstComponent) = dstPortMapping.get
						
						// Check, if a link is already existing from dst -> src or src -> dst. Only establish a new one, if not for both:
						val alreadyConnected: Boolean = srcComponent.isDefined || dstComponent.isDefined
						if (! alreadyConnected) {
							
							_connectOVXSwitches(tenant, physSrcSwitch, physSrcPort, virtSrcSwitch.get, virtSrcPort, 
																	physDstSwitch, physDstPort, virtDstSwitch.get, virtDstPort)
						}
					}
				}
			}
		}
		

		// Create Ports at the Host's Endpoint Switch:Port connect the Host to it
		for (actHost <- hosts) {
			val physSwitchToConnect = _switchTopology.find(_.dpid == actHost.endpoint.dpid)
			val virtSwitchToConnect = _tenantVirtSwitchMap(tenant).find(_.dpids.contains(actHost.endpoint.dpid.convertToHexLong))
			if (physSwitchToConnect.isDefined && virtSwitchToConnect.isDefined) {
				val physSwitch = physSwitchToConnect.get
				val virtSwitch = virtSwitchToConnect.get
				// If no virtual Port is available for the physSwitch + physPort that the Host should connect to, createPort:
				if (! _switchPortMap(physSwitch).exists(_._1 == actHost.endpoint.port)) {
					val hostPortMap = _ovxConn.createPort(tenant.id, actHost.endpoint.dpid.toString, actHost.endpoint.port)
					hostPortMap match{
					    case Some(portMap) =>
								log.info(s"Created Port (phys: ${portMap._1} virt: ${portMap._2}) " +
									s"at Switch ${physSwitch.dpid.toString} for Host ${actHost.mac} " +
									s"in Tenant-Network ${tenant.id}")

								//Append a new value to _switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
								val hostPortMap = (portMap._1, portMap._2, None)
								_switchPortMap = _switchPortMap + (physSwitch -> (_switchPortMap.getOrElse(physSwitch, List()) :+ hostPortMap))
								
								val vHost = _ovxConn.connectHost(tenant.id, virtSwitch.vdpid, portMap._2, actHost.mac)
								vHost match{
								    case Some(result)  => 
											log.info("Host {} connected to Switch {} at (physPort: {}, vPort {})",
															 actHost.mac, physSwitch.dpid, portMap._1, portMap._2)
											//Update _switchPortMap's last portMap entry with the just established Host:
											val newHostPortMap = (hostPortMap._1, hostPortMap._2, Some(actHost))
											val portMapIndex = _switchPortMap.get(physSwitch).get.indexOf(hostPortMap)
											_switchPortMap = _switchPortMap +
												(physSwitch -> _switchPortMap.getOrElse(physSwitch, List()).updated(portMapIndex, newHostPortMap))

								    case None          => 
											log.error("Host connection to Switch {} at (physPort: {}, vPort {}) failed!",
															 actHost.mac, portMap._1, portMap._2)
								}
								
					    case None          => 
								log.error(s"Port creation failed for Switch {} on physical Port {}!",
									physSwitch.dpid, actHost.endpoint.port)
					}
				}
			}
		}
		
		// Start the Tenant's OVX-Network, if not already started:
		if (_tenantNetMap.keys.exists(_ == tenant) && !_tenantNetMap(tenant).isBooted.getOrElse(false)) {
			val netOpt = _ovxConn.startNetwork(tenant.id)
			netOpt match{
			    case Some(net)  =>
						log.info("Started Network for Tenant {} at OFC: {}:{}. Is Booted: {}",
										 tenant.id, tenant.ofcIp, tenant.ofcPort, net.isBooted)
						_tenantNetMap = _tenantNetMap + (tenant -> net)

			    case None          =>
						log.error("Network for Tenant {} at OFC: {}:{} was not started correctly!",
										  tenant.id, tenant.ofcIp, tenant.ofcPort)
			}
		}
	}
	
	
	def _createOVXNetwork(tenant: Tenant) = {
			val netOpt = _ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp.getHostAddress}:${tenant.ofcPort}"), tenant.subnet._1, tenant.subnet._2)
			netOpt match{
				case Some(net)  =>
					log.info(s"Created virtual Network ${tenant.subnet} for Tenant {} at OFC: {}:{}. Is Booted: {}",
						tenant.id, tenant.ofcIp, tenant.ofcPort, net.isBooted)
					_tenantNetMap = _tenantNetMap + (tenant -> net)

				case None          =>
					log.error(s"Virtual Network ${tenant.subnet} for Tenant {} at OFC: {}:{} was not started correctly!",
						tenant.id, tenant.ofcIp, tenant.ofcPort)
			}
	}

	/**
	 * Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
	 * @param tenant
	 * @param physSwitch
	 */
	def _createOVXSwitch(tenant: Tenant, physSwitch: OFSwitch): Boolean = {
		// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
		val vSwitchOpt = _ovxConn.createSwitch(tenant.id, List(physSwitch.dpid.toString))
		vSwitchOpt match{
		    case Some(vSwitch) =>
					log.info("Created Switch (dpids: {} vdpid: {}) in Tenant-Network {}",
						physSwitch.dpid.toString, vSwitch.dpids, tenant.id)

					// After virtual Switch was successfully created, add physical and virtual Switch to respective tenantSwitchMap:
					_tenantPhysSwitchMap = _tenantPhysSwitchMap + (tenant -> (_tenantPhysSwitchMap.getOrElse(tenant, List()) :+ physSwitch))
					_tenantVirtSwitchMap = _tenantVirtSwitchMap + (tenant -> (_tenantVirtSwitchMap.getOrElse(tenant, List()) :+ vSwitch))
					return true
					
		    case None          =>
					log.error("Switch Creation (dpids: {}) in Tenant-Network {} failed!",
						physSwitch.dpid.toString, tenant.id)
					return false
		}
	}

	/**
	 * Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch
	 * @param tenant
	 * @param physSwitch
	 */
	def _createOVXSwitchPorts(tenant: Tenant, physSwitch: OFSwitch): Boolean = {
		// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
		val physSrcPorts = physSwitch.portMap.map(_._1)
		var allPortsCreated: Boolean = true
		
		for (actSrcPort <- physSrcPorts) {
			val portMapOpt = _ovxConn.createPort(tenant.id, physSwitch.dpid.toString, actSrcPort)
			portMapOpt match{
			    case Some(portMap)  =>
						val physPort = portMapOpt.get._1
						val virtPort = portMapOpt.get._2
						assert(physPort == actSrcPort, s"Associated Physical Port $physPort after Port creation " +
							s"on Switch ${physSwitch.dpid} ist not equal to requested physical Source Port $actSrcPort!")

						log.info("Created Port (phys: {} virt: {}) at Switch {} for other Switch in Tenant-Network {}",
							physPort, virtPort, physSwitch.dpid.toString, tenant.id)

						//Append a new value to _switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
						_switchPortMap = _switchPortMap + (physSwitch -> (_switchPortMap.getOrElse(physSwitch, List()) :+ (physPort, virtPort, None)))
						
			    case None          	=>
						allPortsCreated = false
			}
		}
		return allPortsCreated
	}
	
	def _connectOVXSwitches(tenant: Tenant, 
													physSrcSwitch: OFSwitch, physSrcPort: Short, virtSrcSwitch: VirtualSwitch, virtSrcPort: Short,
													physDstSwitch: OFSwitch, physDstPort: Short, virtDstSwitch: VirtualSwitch, virtDstPort: Short): 
													Boolean = {
		
		val vLink = _ovxConn.connectLink(tenant.id, 
																		 virtSrcSwitch.vdpid, virtSrcPort, 
																		 virtDstSwitch.vdpid, virtDstPort, "spf", 1)
		vLink match {
			case Some(result) =>
				log.info(s"Link connection between Switches " +
					s"(${physSrcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
					s"- ${physDstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) suceeded!")
				
				// If virtual link was established successfully, update srcPortMapping in _switchPortMap with physDstSwitch:
				val newSrcPortMap = (physSrcPort, virtSrcPort, Some(physDstSwitch))
				val srcPortMapIndex = _switchPortMap.getOrElse(physSrcSwitch, List()).
																indexWhere(t => t._1 == physSrcPort && t._2 == virtSrcPort)
				
				_switchPortMap = _switchPortMap +
					(physSrcSwitch -> _switchPortMap.getOrElse(physSrcSwitch, List()).updated(srcPortMapIndex, newSrcPortMap))
				return true

			case None =>
				log.info(s"Link connection between Switches " +
					s"(${physSrcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
					s"- ${physDstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) failed!")
				return false
		}
		
	}
	
	def _discoverHostPaths() = {
		//TODO: implement discovery
	}
	
} // end of class NRA


/**
 * Companion Object of the NetworkResource-Agent,
 * in order to implement some default behaviours
 */
object NetworkResourceAgent
{
	/**
	 * props-method is used in the AKKA-Context, spawning a new Agent.
	 * In this case, to generate a new NetworkResource Agent, call
	 * 	val ccfmProps = Props(classOf[NetworkResourceAgent],
														ovxIP, ovxApiPort,
														mmaActorRef)
	 * 	val ccfmAgent = system.actorOf(ccfmProps, name="NetworkResourceAgent-x")
	 * @param ovxIp The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
	 * @return An Akka Properties-Object
	 */
	def props(ovxIp: InetAddress, ovxApiPort: Int, cloudHosts: List[Host], matchMakingAgent: ActorRef):
	Props = Props(new NetworkResourceAgent(ovxIp, ovxApiPort, cloudHosts, matchMakingAgent))
}