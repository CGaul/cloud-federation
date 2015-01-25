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

		// If the tenant does not have an OVX tenant-network until now:
		if (!_tenantNetMap.keys.exists(_ == tenant)) {
			val net = _ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp.getHostAddress}:${tenant.ofcPort}"), tenant.subnet._1, tenant.subnet._2)
			if (net.isDefined) {
				log.info("Created Network for Tenant {} at OFC: {}:{}",
					tenant.id, tenant.ofcIp, tenant.ofcPort)
				_tenantNetMap = _tenantNetMap + (tenant -> net.get)
			}
		}
		// In general, this allocation includes all physical Switches, that are needed to connect all hosts with each other.
		// first only get the physical Switches, directly connected to the hosts:
		// (get a flat list of each host -> List[Switch] mapping)
		val physSwitches = hosts.flatMap(_hostPhysSwitchMap).distinct
		
		// Afterwards, try to solve a path from each host to all other hosts, using all
		// currently discovered physical Switches as direct gateway to them:
		// TODO: path discovery

		// Create virtual switches for all physical Switches that are not yet part of a virtual switch in the tenant's vNet:
		for (actPhysSwitch <- physSwitches 
				 if !(_tenantVirtSwitchMap.keys.exists(_ == tenant) &&
						_tenantVirtSwitchMap(tenant).exists(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))))
		{

			// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch,
			val vSwitch = _ovxConn.createSwitch(tenant.id, List(actPhysSwitch.dpid.toString))
			if (vSwitch.isDefined) {
				log.info("Created Switch (dpids: {} vdpid: {}) in Tenant-Network {}",
					actPhysSwitch.dpid.toString, vSwitch.get.dpids, tenant.id)
				
				// After virtual Switch was successfully created, add physical and virtual Switch to respective tenantSwitchMap:
				_tenantPhysSwitchMap = _tenantPhysSwitchMap + (tenant -> (_tenantPhysSwitchMap.getOrElse(tenant, List()) :+ actPhysSwitch))
				_tenantVirtSwitchMap = _tenantVirtSwitchMap + (tenant -> (_tenantVirtSwitchMap.getOrElse(tenant, List()) :+ vSwitch.get))
			}

			// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
			val physSrcPorts = actPhysSwitch.portMap.map(_._1)
			for (actSrcPort <- physSrcPorts) {
				val portMap = _ovxConn.createPort(tenant.id, actPhysSwitch.dpid.toString, actSrcPort)
				if (portMap.isDefined) {
					val physPort = portMap.get._1
					val virtPort = portMap.get._2
					assert(physPort == actSrcPort, s"Associated Physical Port $physPort after Port creation " +
						s"on Switch ${actPhysSwitch.dpid} ist not equal to requested physical Source Port $actSrcPort!")
					
					log.info("Created Port (phys: {} virt: {}) at Switch {} for other Switch in Tenant-Network {}",
									 physPort, virtPort, actPhysSwitch.dpid.toString, tenant.id)
					
					//Append a new value to _switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
					_switchPortMap = _switchPortMap + (actPhysSwitch -> (_switchPortMap.getOrElse(actPhysSwitch, List()) :+ (physPort, virtPort, None)))
//					_switchPortMap.get(actPhysSwitch) match {
//						case Some(list) => _switchPortMap = _switchPortMap + (actPhysSwitch -> (list :+(physPort, virtPort, None)))
//						case None => _switchPortMap = _switchPortMap + (actPhysSwitch -> List((physPort, virtPort, None)))
//					}
				}
			}
		}
		
		
		// Iterate over all physical Switches, get their Port-mapping from _switchPortMap
		// and connect their virtual counterparts to each other on the correct vPorts (create all topology paths):
		for (actPhysSwitch <- _tenantPhysSwitchMap(tenant)) {
			for ((srcPort, srcEndpoint) <- actPhysSwitch.portMap) {
				val physSrcSwitch = actPhysSwitch
				val physDstSwitch = _tenantPhysSwitchMap.getOrElse(tenant, List()).find(_.dpid == srcEndpoint.dpid)
				// As the physical Destination Switch might not be in the tenant's switchMap, only continue connection if both
				// src- and dstSwitch are known:
				if(physDstSwitch.isDefined) {
					// Find the srcPortMapping for the actual srcPort in the _switchPortMap's actPhysSwitch entry:
					val srcPortMapping = _switchPortMap.getOrElse(physSrcSwitch, List()).find(_._1 == srcPort)
					val dstPortMapping = _switchPortMap.getOrElse(physDstSwitch.get, List()).find(_._1 == srcEndpoint.port)
					val vSrcSwitch = _tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))
					val vDstSwitch = _tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(srcEndpoint.dpid.convertToHexLong))

					if (srcPortMapping.isDefined && dstPortMapping.isDefined && vSrcSwitch.isDefined && vDstSwitch.isDefined) {
						val vSrcPort = srcPortMapping.get._2
						val vDstPort = dstPortMapping.get._2

						val vLink = _ovxConn.connectLink(tenant.id, vSrcSwitch.get.vdpid, vSrcPort,
							vDstSwitch.get.vdpid, vDstPort, "spf", 1)
						vLink match {
							case Some(result) =>
								log.info("Link connection between Switches ({}:{} - {}:{}) suceeded!",
									physSrcSwitch.dpid, srcPortMapping.get._1, physDstSwitch.get.dpid, dstPortMapping.get._1)
								// If virtual link was established successfully, update srcPortMapping in _switchPortMap with physDstSwitch:
								val newSrcPortMapping = (srcPortMapping.get._1, srcPortMapping.get._2, physDstSwitch)
								val indexSrcPortMapping = _switchPortMap.get(physSrcSwitch).get.indexOf(srcPortMapping.get)
								_switchPortMap = _switchPortMap + 
									(actPhysSwitch -> _switchPortMap.getOrElse(physSrcSwitch, List()).updated(indexSrcPortMapping, newSrcPortMapping))

							case None =>
								log.error("Link connection between Switches ({}:{} - {}:{}) failed!",
									physSrcSwitch.dpid, srcPortMapping.get._1, physDstSwitch.get.dpid, dstPortMapping.get._1)
						}
					}
				}
			}
		}
		

		// Add each Host in the hosts-List to the Host's Switch-Endpoint:
		// TODO: create Ports for the Host's Endpoint and connect the Host to it
		for (actHost <- hosts) {
			val physSwitchToConnect = _switchTopology.find(_.dpid == actHost.endpoint.dpid)
			if (physSwitchToConnect.isDefined) {
				val physSwitch = physSwitchToConnect.get
				// If no virtual Port is available for the physSwitch + physPort that the Host should connect to, createPort:
				if (! _switchPortMap(physSwitch).exists(_._1 == actHost.endpoint.port)) {
					val hostPortMap = _ovxConn.createPort(tenant.id, actHost.endpoint.dpid.toString, actHost.endpoint.port)
					hostPortMap match{
					    case Some(portMap) =>
								log.info(s"Created Port (phys: ${portMap._1} virt: ${portMap._2}) " +
									s"at Switch ${physSwitch.dpid.toString} for Host ${actHost.mac} " +
									s"in Tenant-Network ${tenant.id}")

								//Append a new value to _switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
								_switchPortMap = _switchPortMap + (physSwitch -> (_switchPortMap.getOrElse(physSwitch, List()) :+(portMap._1, portMap._2, None)))
								
					    case None          => 
								log.error(s"Port creation failed for Switch {} on physical Port {}!",
									physSwitch.dpid, actHost.endpoint.port)
					}

					// TODO: Add the host to the virtual Port that existed before or was just created:
				}
			}
		}
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