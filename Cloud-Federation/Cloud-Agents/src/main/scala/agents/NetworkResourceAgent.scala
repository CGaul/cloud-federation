package agents

import java.net.InetAddress

import akka.actor._
import connectors._
import datatypes._
import messages._

import scala.util.control.Breaks._



/* Enums: */
/* ====== */

object DiscoveryState extends Enumeration {
  type DiscoveryState = Value
  val ONLINE, OFFLINE = Value
}


/* Classes: */
/* ======== */

/**
 * @author Constantin Gaul, created on 10/15/14.
 */
class NetworkResourceAgent(cloudConfig: CloudConfigurator,
													 matchMakingAgent: ActorRef)
													extends Actor with ActorLogging with Stash
{
  
/* Values: */
/* ======= */
  
		private val _ovxConn = OVXConnector(cloudConfig.cloudOvx.ovxIp, cloudConfig.cloudOvx.ovxApiPort)


/* Variables: */
/* ========== */
  
    var state = DiscoveryState.OFFLINE
  
		// Physical Topologies, received from the CCFM (hosts) and the NDA (switches)
		private val hostTopology: List[Host] = cloudConfig.cloudHosts.toList
		private var switchTopology: List[OFSwitch] = List()
	
		// Physical Mappings:
		private var hostPhysSwitchMap: Map[Host, List[OFSwitch]] = Map()
		private var tenantPhysSwitchMap: Map[Tenant, List[OFSwitch]] = Map()
		private var tenantGatewayMap: Map[Tenant, List[OFSwitch]] = Map()
		private var switchPortMap: Map[OFSwitch, List[(Short, Short, Option[NetworkComponent])]] = Map()
	
		// Virtual Mappings:
		private var tenantToOVXTenantId: Map[Tenant, Int] = Map()
		private var tenantNetMap: Map[Tenant, VirtualNetwork] = Map()
		private var tenantVirtSwitchMap: Map[Tenant, List[VirtualSwitch]] = Map()


/* Initial Startup: */
/* ================ */
	
	initActor()
	private val ndaActor: ActorRef = initChildActors()
	
	def initActor() = {
		// This NRA-Instance is inactive after boot-up:
		log.info("NetworkResourceAgent will be INACTIVE, until NDA sends a TopologyDiscovery...")
	}
	
	def initChildActors(): ActorRef = {
		// Spawn a NetworkDiscoveryAgent (NDA) for Network-Topology-Discovery:
		val ndaProps: Props = Props(classOf[NetworkDiscoveryAgent], 
																cloudConfig.cloudOvx.ovxIp, cloudConfig.cloudOvx.ovxApiPort, context.self)
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

			case ResourceFederationRequest(tenant, foreignGWSwitch, resourcesToAlloc, ovxInstance)
			=> recvResourceFederationRequest(tenant, foreignGWSwitch, resourcesToAlloc, ovxInstance)

			case ResourceFederationResult(tenant, foreignGWSwitch, resourcesAllocated, ovxInstance)
			=> recvResourceFederationResult(tenant, foreignGWSwitch, resourcesAllocated, ovxInstance)
		}

    case message: NRAFederationDest => message match {
      case FederateableResourceRequest()
      => recvFederateableResourceRequest()
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
        state = DiscoveryState.ONLINE
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
	 * 	who should then send a ResourceFederationSummary back to this NetworkResourceAgent,
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
			log.info("ResourceRequest {} could not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceToAlloc, remainResToAlloc)
			matchMakingAgent ! ResourceRequest(tenant, remainResToAlloc.get)
		}
		else {
      log.info("ResourceRequest {} was completely allocated on the local cloud!", resourceToAlloc)
      sender() ! ResourceReply(resourceToAlloc)
    }
	}
  
	/**
	 * Received from local MMA.
	 * <p>
	 * 	If a ResourceRequest could not have been processed locally,
	 * 	the NRA has asked the MMA for Federation-Resources.
	 * 	All results are included in such ResourceFederationResult,
	 * 	stating the allocated Resources per foreign Cloud
	 * 	(the ActorRef is the foreign slave MMA)
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
   *
   * @param tenant
   * @param foreignGWSwitch
   * @param federatedResources
   * @param ovxInstance
   */
	private def recvResourceFederationResult(tenant: Tenant, foreignGWSwitch: OFSwitch, 
                                           federatedResources: ResourceAlloc, ovxInstance: OvxInstance): Unit = {
    log.info("Received ResourceFederationResult at NRA from {} Preparing Federation for tenant {} on OVX-F Instance {}...", 
      sender(), tenant.id, ovxInstance)
    
    prepareFederation(tenant, foreignGWSwitch)
    uploadNetworkToFederatedOVX(tenant, ovxInstance.ovxIp, ovxInstance.ovxApiPort, ovxInstance.ovxApiPort)
	}


	//FIXME: Shortcut Implementation in 0.2 Integrated Controllers
	/**
	 * Received from local MMA.
	 * Allocate foreign resources locally that are part of a federation.
	 *
   * @param tenant
   * @param foreignGWSwitch
   * @param resourcesToAlloc
   */
	private def recvResourceFederationRequest(tenant: Tenant, foreignGWSwitch: OFSwitch, 
                                            resourcesToAlloc: ResourceAlloc, ovxInstance: OvxInstance): Unit = {
		log.info("Received ResourceFederationRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NRA.",
			resourcesToAlloc.tenantID, resourcesToAlloc.resources.size, tenant.ofcIp)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourcesToAlloc)
		
		// Prepare the locally fulfilled allocations that will be send to OVX via
		// the own OVXConnector-API:
		val hostList = allocationsPerHost.map(_._1).toList
		log.info("Mapping hosts {} to virtual tenant network.", hostList.map(_.mac))
    prepareFederation(tenant, foreignGWSwitch)
		mapAllocOnOVX(tenant, hostList)
    uploadNetworkToFederatedOVX(tenant, ovxInstance.ovxIp, ovxInstance.ovxApiPort, ovxInstance.ovxCtrlPort)

		if(remainResToAlloc.size > 0){
			// TODO: send Information about remaining Resources to Allocate back to the sender.
		}
	}

  
  private def recvFederateableResourceRequest() = {
    //TODO: send FederateableResourceReply back to sender().
  }
  
  
  /**
   * Received from NDA
   * @param switchTopology
   */
	private def recvTopologyDiscovery(switchTopology: List[OFSwitch]) = {
		log.info("Received new Switch-Topology from {}, including {} switches.", sender(), switchTopology.length)
		this.switchTopology = switchTopology
		this.hostPhysSwitchMap = hostPhysSwitchMap ++ hostTopology.map(host => host -> switchTopology.filter(_.dpid == host.endpoint.dpid))
	}


/* Private Helper Methods: */
/* ======================= */

	private def allocateLocally(resourceAlloc: ResourceAlloc): (Map[Host, ResourceAlloc], Option[ResourceAlloc]) = {
		// Will be filled with each allocation per Host that happened in this local allocation call:
		var allocationPerHost: Map[Host, ResourceAlloc] = Map()

		// Sort the potentialHosts as well as the resourceToAlloc by their resources in descending Order:
		val sortedHosts			= hostTopology.sorted(RelativeHostByResOrdering)
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
	
	private def mapAllocOnOVX(tenant: Tenant, hosts: List[Host]): Unit = {

		// If the tenant does not have an OVX tenant-network until now, create one:
		if (!tenantNetMap.keys.exists(_ == tenant)) {
			val tenantNet = _createOVXNetwork(tenant)
      if(tenantNet.isEmpty)
//        log.error("Tenant-Network could not have been created for tenant {}. Aborting allocation on OVX!", tenant)
        return
		}
		
		// The hostPhysSwitchMap defines a mapping between each known host and all physical Switches,
		// that are connected to this host and discovered via a TopologyDiscovery earlier.
		// To continue, get all physical Switches for the given host's allocation:
		// (get a flat list of each host -> List[Switch] mapping)
		val physSwitches = hosts.flatMap(hostPhysSwitchMap).distinct
		
		// Afterwards, try to solve a path from each host to all other hosts, using all
		// currently discovered physical Switches as direct gateway to them:
		_discoverHostPaths()

		// Create virtual switches for all physical Switches that are not yet part of a virtual switch in the tenant's vNet
		// and establish virtual Ports for all physical Ports that are outgoing to any other Switch:
		for (actPhysSwitch <- physSwitches 
				 if !(tenantVirtSwitchMap.keys.exists(_ == tenant) &&
						tenantVirtSwitchMap(tenant).exists(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))))
		{
			// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
			_createOVXSwitch(tenant, actPhysSwitch)

			// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
			_createAllOVXSwitchPorts(tenant, actPhysSwitch)
		}
		
		
		// Iterate over all physical Switches, get their Port-mapping from switchPortMap
		// and connect their virtual counterparts to each other on the correct virtPorts (create all topology paths):
		for (actPhysSwitch <- tenantPhysSwitchMap(tenant)) {
			for ((srcPort, srcEndpoint) <- actPhysSwitch.portMap) {
				val physSrcSwitch = actPhysSwitch
				val physDstSwitchOpt = tenantPhysSwitchMap.getOrElse(tenant, List()).find(_.dpid == srcEndpoint.dpid)
				// As the physical Destination Switch might not be in the tenant's switchMap, only continue connection if both
				// src- and dst-Switch are known:
				if(physDstSwitchOpt.isDefined) {
					val physDstSwitch = physDstSwitchOpt.get
					// Find the srcPortMapping for the actual srcPort in the switchPortMap's actPhysSwitch entry:
					val srcPortMapping = switchPortMap.getOrElse(physSrcSwitch, List()).
																find(_._1 == srcPort)
					val dstPortMapping = switchPortMap.getOrElse(physDstSwitch, List()).
																find(_._1 == srcEndpoint.port)
					val virtSrcSwitch = tenantVirtSwitchMap.getOrElse(tenant, List()).
																find(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))
					val virtDstSwitch = tenantVirtSwitchMap.getOrElse(tenant, List()).
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
			val physSwitchToConnect = switchTopology.find(_.dpid == actHost.endpoint.dpid)
			val virtSwitchToConnect = tenantVirtSwitchMap(tenant).find(_.dpids.contains(actHost.endpoint.dpid.convertToHexLong))
			if (physSwitchToConnect.isDefined && virtSwitchToConnect.isDefined) {
				val physSwitch = physSwitchToConnect.get
				val virtSwitch = virtSwitchToConnect.get
				// If no virtual Port is available for the physSwitch + physPort that the Host should connect to, createPort:
				if (! switchPortMap(physSwitch).exists(_._1 == actHost.endpoint.port)) {
					val portMapOpt = _createOVXHostPort(tenant, physSwitch, actHost)
					if(portMapOpt.isDefined) {
						_connectOVXHost(tenant, physSwitch, virtSwitch, actHost, portMapOpt.get)
					}
				}
			}
		}
		
		// Start the Tenant's OVX-Network, if not already started:
		if (tenantNetMap.keys.exists(_ == tenant) && !tenantNetMap(tenant).isBooted.getOrElse(false)) {
			_startOVXNetwork(tenant)
		}
	}

  private def prepareFederation(tenant: Tenant, foreignGWSwitch: OFSwitch) = {
    
    val localGWSwitch = cloudConfig.cloudGateway
    
    //Add localGWSwitch, if not already added:
    if(! tenantGatewayMap.getOrElse(tenant, List()).contains(localGWSwitch))
    {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _createOVXSwitch(tenant, localGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _createAllOVXSwitchPorts(tenant, localGWSwitch)
      tenantGatewayMap = tenantGatewayMap + (tenant -> (tenantGatewayMap.getOrElse(tenant, List()) :+ localGWSwitch))
    }
    
    // Add foreignGWSwitch, if not already added:
    if(! tenantGatewayMap.getOrElse(tenant, List()).contains(foreignGWSwitch))
     {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _createOVXSwitch(tenant, foreignGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _createAllOVXSwitchPorts(tenant, foreignGWSwitch)
      tenantGatewayMap = tenantGatewayMap + (tenant -> (tenantGatewayMap.getOrElse(tenant, List()) :+ foreignGWSwitch))
    }
    
    // Connect both Gateway Switches with each other, if no link is currently established:
    // Find the srcPortMapping for the actual srcPort in the switchPortMap's actPhysSwitch entry:
    val physLocalPortOpt = localGWSwitch.portMap.find(_._2.dpid == foreignGWSwitch.dpid)
    val physForeignPortOpt = foreignGWSwitch.portMap.find(_._2.dpid == localGWSwitch.dpid)
    
    val virtLocalGWOpt = tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(localGWSwitch.dpid.convertToHexLong))
    val virtForeignGWOpt = tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(foreignGWSwitch.dpid.convertToHexLong))
    
    if(physLocalPortOpt.isDefined && physForeignPortOpt.isDefined && 
      virtLocalGWOpt.isDefined && virtForeignGWOpt.isDefined) {
      
      val virtLocalGW = virtLocalGWOpt.get
      val virtForeignGW = virtForeignGWOpt.get
      
      val physicalLocalPort = physLocalPortOpt.get
      val physicalForeignPort = physForeignPortOpt.get
      val localPortMapping = switchPortMap.getOrElse(localGWSwitch, List()).find(_._1 == physicalLocalPort._1)
      val foreignPortMapping = switchPortMap.getOrElse(foreignGWSwitch, List()).find(_._1 == physicalForeignPort._1)
      
      if(localPortMapping.isDefined && foreignPortMapping.isDefined){
        val (physSrcPort, virtSrcPort, srcComponent) = localPortMapping.get
        val (physDstPort, virtDstPort, dstComponent) = foreignPortMapping.get

        // Check, if a link is already existing from dst -> src or src -> dst. Only establish a new one, if not for both:
        val alreadyConnected: Boolean = srcComponent.isDefined || dstComponent.isDefined
        if (!alreadyConnected) {

          _connectOVXSwitches(tenant, 
                              localGWSwitch, physSrcPort, virtLocalGW, virtSrcPort,
                              foreignGWSwitch, physDstPort, virtForeignGW, virtDstPort)
        }
      }
    }
  }
  
  private def uploadNetworkToFederatedOVX(tenant: Tenant, ovxIp: InetAddress, ovxApiPort: Int, ovxCtrlPort: Int) = {
    val virtNetOpt = tenantNetMap.get(tenant)
    virtNetOpt match{
        case Some(virtNet)  =>
          log.info("Removing tenant {} OFC Controller {} from network {}...",
                   tenant.id, virtNet.controllerUrls(0), virtNet.networkAddress)
          _ovxConn.removeControllers(tenant.id, List(virtNet.controllerUrls(0)))
          
//          log.info("Adding federated OVX as new Controller {} for network {} of tenant {}...",
//                   )
          //TODO: either use federated OVX Address in createNetwork from the beginning (even if federated OVX is only passive at that time), or rebuild network here.
          
        case None          => 
          log.error("No virtual tenant network registered for tenant {}!",
                    tenant.id)
          
    }
    
    
  }
	
	
  /* Helper Methods for OVXConnector: */
  /* ================================ */
  
	private def _createOVXNetwork(tenant: Tenant): Option[VirtualNetwork] = {
			val netOpt = _ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp.getHostAddress}:${tenant.ofcPort}"), tenant.subnet._1, tenant.subnet._2)
			netOpt match{
				case Some(net)  =>
					log.info(s"Created virtual Network ${tenant.subnet} for Tenant {} at OFC: {}:{}. Is Booted: {}",
						tenant.id, tenant.ofcIp, tenant.ofcPort, net.isBooted)
					tenantToOVXTenantId = tenantToOVXTenantId + (tenant -> net.tenantId.getOrElse(-1))
					tenantNetMap = tenantNetMap + (tenant -> net)
					return Some(net)
					
				case None          =>
					log.error(s"Virtual Network ${tenant.subnet} for Tenant {} at OFC: {}:{} was not started correctly!",
						tenant.id, tenant.ofcIp, tenant.ofcPort)
					return None
			}
	}

	/**
	 * Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
	 * @param tenant
	 * @param physSwitch
	 */
	private def _createOVXSwitch(tenant: Tenant, physSwitch: OFSwitch): Option[VirtualSwitch] = {
		// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
		val vSwitchOpt = _ovxConn.createSwitch(tenantToOVXTenantId(tenant), List(physSwitch.dpid.toString))
		vSwitchOpt match{
		    case Some(vSwitch) =>
					log.info(s"Created Switch (dpids: {} vdpid: {}) in Tenant-Network {}/(${tenantToOVXTenantId(tenant)})",
						physSwitch.dpid.toString, vSwitch.dpids, tenant.id)

					// After virtual Switch was successfully created, add physical and virtual Switch to respective tenantSwitchMap:
					tenantPhysSwitchMap = tenantPhysSwitchMap + (tenant -> (tenantPhysSwitchMap.getOrElse(tenant, List()) :+ physSwitch))
					tenantVirtSwitchMap = tenantVirtSwitchMap + (tenant -> (tenantVirtSwitchMap.getOrElse(tenant, List()) :+ vSwitch))
					return Some(vSwitch)
					
		    case None          =>
					log.error(s"Switch Creation (dpids: {}) in Tenant-Network {}/(${tenantToOVXTenantId(tenant)}) failed!",
						physSwitch.dpid.toString, tenant.id)
					return None
		}
	}

	/**
	 * Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch
	 * @param tenant
	 * @param physSwitch
	 */
	private def _createAllOVXSwitchPorts(tenant: Tenant, physSwitch: OFSwitch): Boolean = {
		// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
		val physSrcPorts = physSwitch.portMap.map(_._1)
		var allPortsCreated: Boolean = true
		for (actSrcPort <- physSrcPorts) {
			val portMapOpt = _createOVXSwitchPort(tenant, physSwitch, actSrcPort)
			if(portMapOpt.isEmpty)
				allPortsCreated = false
		}
		return allPortsCreated
	}
	
	//TODO: only create single Switch Port per call
	private def _createOVXSwitchPort(tenant: Tenant, physSwitch: OFSwitch, physPort: Short): Option[(Short, Short)] = {
		val portMapOpt = _ovxConn.createPort(tenantToOVXTenantId(tenant), physSwitch.dpid.toString, physPort)
		portMapOpt match{
			case Some(portMap)  =>
				val physPort = portMapOpt.get._1
				val virtPort = portMapOpt.get._2
				assert(physPort == physPort, s"Associated Physical Port $physPort after Port creation " +
					s"on Switch ${physSwitch.dpid} ist not equal to requested physical Source Port $physPort!")

				log.info(s"Created Port (phys: {} virt: {}) at Switch {} for other Switch in Tenant-Network {}/(${tenantToOVXTenantId(tenant)})",
					physPort, virtPort, physSwitch.dpid.toString, tenant.id)

				//Append a new value to switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
				switchPortMap = switchPortMap + (physSwitch -> (switchPortMap.getOrElse(physSwitch, List()) :+ (physPort, virtPort, None)))
				return Some(portMap)
				
			case None          	=>
				log.error(s"Port creation failed for Switch {} on physical Port {}!",
					physSwitch.dpid, physPort)
				return None
		}
	}
	
	private def _connectOVXSwitches(tenant: Tenant, 
													physSrcSwitch: OFSwitch, physSrcPort: Short, virtSrcSwitch: VirtualSwitch, virtSrcPort: Short,
													physDstSwitch: OFSwitch, physDstPort: Short, virtDstSwitch: VirtualSwitch, virtDstPort: Short): 
													Option[VirtualLink] = {
		
		val vLinkOpt = _ovxConn.connectLink(tenantToOVXTenantId(tenant),
																		 virtSrcSwitch.vdpid, virtSrcPort, 
																		 virtDstSwitch.vdpid, virtDstPort, "spf", 1)
		vLinkOpt match {
			case Some(vLink) =>
				log.info(s"Link connection between Switches " +
					s"(${physSrcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
					s"- ${physDstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) suceeded!")
				
				// If virtual link was established successfully, update srcPortMapping in switchPortMap with physDstSwitch:
				val newSrcPortMap = (physSrcPort, virtSrcPort, Some(physDstSwitch))
				val srcPortMapIndex = switchPortMap.getOrElse(physSrcSwitch, List()).
																indexWhere(t => t._1 == physSrcPort && t._2 == virtSrcPort)
				
				switchPortMap = switchPortMap +
					(physSrcSwitch -> switchPortMap.getOrElse(physSrcSwitch, List()).updated(srcPortMapIndex, newSrcPortMap))
				return Some(vLink)

			case None =>
				log.error(s"Link connection between Switches " +
					s"(${physSrcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
					s"- ${physDstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) failed!")
				return None
		}
	}
	
	private def _createOVXHostPort(tenant: Tenant, physSwitch: OFSwitch, host: Host): Option[(Short, Short)] = {
		val hostPortMap = _ovxConn.createPort(tenantToOVXTenantId(tenant), host.endpoint.dpid.toString, host.endpoint.port)
		hostPortMap match {
			case Some(portMap)	=>
				log.info(s"Created Port (phys: ${portMap._1} virt: ${portMap._2}) " +
					s"at Switch ${physSwitch.dpid.toString} for Host ${host.mac} " +
					s"in Tenant-Network ${tenant.id}/(${tenantToOVXTenantId(tenant)})")

				//Append a new value to switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
				val hostPortMap = (portMap._1, portMap._2, None)
				switchPortMap = switchPortMap + (physSwitch -> (switchPortMap.getOrElse(physSwitch, List()) :+ hostPortMap))
				return Some((portMap._1, portMap._2))

			case None 					=>
				log.error(s"Port creation failed for Switch {} on physical Port {}!",
					physSwitch.dpid, host.endpoint.port)
				return None
		}
	}
	
	private def _connectOVXHost(tenant: Tenant, physSwitch: OFSwitch, virtSwitch: VirtualSwitch, 
															host: Host, portMap: (Short, Short)): Option[VirtualHost] = {
		
		val vHostOpt = _ovxConn.connectHost(tenant.id, virtSwitch.vdpid, portMap._2, host.mac)
		vHostOpt match {
			case Some(vHost) =>
				log.info("Host {} connected to Switch {} at (physPort: {}, vPort {})",
					host.mac, physSwitch.dpid, portMap._1, portMap._2)
				//Update switchPortMap's last portMap entry with the just established Host:
				val newHostPortMap = (portMap._1, portMap._2, Some(host))
				val portMapIndex = switchPortMap.getOrElse(physSwitch, List()).indexWhere(t => t._1 == portMap._1 && t._2 == portMap._2)
					switchPortMap = switchPortMap +
						(physSwitch -> switchPortMap.getOrElse(physSwitch, List()).
						updated(portMapIndex, newHostPortMap))
				return Some(vHost)
				
			case None =>
				log.error("Host connection to Switch {} at (physPort: {}, vPort {}) failed!",
					host.mac, portMap._1, portMap._2)
				return None
		}
	}
	
	private def _startOVXNetwork(tenant: Tenant): Option[VirtualNetwork] = {
		val netOpt = _ovxConn.startNetwork(tenant.id)
		netOpt match{
			case Some(net)  =>
				log.info("Started Network for Tenant {} at OFC: {}:{}. Is Booted: {}",
					tenant.id, tenant.ofcIp, tenant.ofcPort, net.isBooted)
				tenantNetMap = tenantNetMap + (tenant -> net)
				return Some(net)

			case None          =>
				log.error("Network for Tenant {} at OFC: {}:{} was not started correctly!",
					tenant.id, tenant.ofcIp, tenant.ofcPort)
				return None
		}
	}
	
	private def _discoverHostPaths() = {
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
	 * @return An Akka Properties-Object
	 */
	def props(cloudConfig: CloudConfigurator, matchMakingAgent: ActorRef):
	Props = Props(new NetworkResourceAgent(cloudConfig, matchMakingAgent))
}