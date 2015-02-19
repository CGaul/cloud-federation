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
    private val _ovxManager = OVXManager(_ovxConn)


/* Variables: */
/* ========== */
  
    var state = DiscoveryState.OFFLINE
  
		// Physical Topologies, received from the CCFM (hosts) and the NDA (switches)
		private val hostTopology: List[Host] = cloudConfig.cloudHosts.toList
		private var switchTopology: List[OFSwitch] = List()
    private var hostPhysSwitchMap: Map[Host, List[OFSwitch]] = Map()
    private var tenantGatewayMap: Map[Tenant, List[OFSwitch]] = Map()
	  
    // Physical OvxInstance, received from MMA:
    private var ovxInstance: OvxInstance = null
  


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
				  checkOnlineStateReached()
		}

    case message: NRAFederationDest => message match{
      case OvxInstanceReply(ovxInstance)
      =>  recvOvxInstanceReply(ovxInstance)
          checkOnlineStateReached()

    }
		case _	=> log.error("Unknown message received!")
	}

	def receive: Receive = inactive()

	
/* Private Receiving Methods: */
/* ========================== */

	/**
	 * Reiceved from local CCFM.
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
    
  
  /**
   * Received from local NDA
   * @param switchTopology
   */
	private def recvTopologyDiscovery(switchTopology: List[OFSwitch]) = {
		log.info("Received new Switch-Topology from {}, including {} switches.", sender(), switchTopology.length)
		this.switchTopology = switchTopology
		this.hostPhysSwitchMap = hostPhysSwitchMap ++ hostTopology.map(host => host -> switchTopology.filter(_.dpid == host.endpoint.dpid))
    sendFederateableResourcesToMMA()
	}

  /**
   * Received from local MMA
   * @param ovxInstance
   */
  private def recvOvxInstanceReply(ovxInstance: OvxInstance) = {
    log.info("OvxInstance {} received from MMA {}", ovxInstance, sender())
    this.ovxInstance = ovxInstance
  }
  



/* Private Helper Methods: */
/* ======================= */
  
  /**
   * Sending a FederateableResourceDiscovery to the MMA, once a new TopologyDiscovery was received.
   * FederateableResourceDiscovery contains (Host -> ResourceAlloc) tuples, where each Host is federateable.
   */
  private def sendFederateableResourcesToMMA() = {
    log.info("Sending new FederateableResources to MMA...")
    val federateableHosts = hostTopology.filter(_.isFederateable)
    val federateableResources = federateableHosts.map(host => (host -> host.allocatedResources))
    var federateableReply: Vector[(Host, ResourceAlloc)] = Vector()
    for ((actHost, actFedRes) <- federateableResources) {
      // Add each ResourceAlloc in actFedRes to the federateableReply (host, ResAlloc) vector:
      actFedRes.foreach(resAlloc => federateableReply = federateableReply :+ (actHost, resAlloc))
    }
    // Finally send a FederateableResourceDiscovery local MMA:
    matchMakingAgent ! FederateableResourceDiscovery(federateableReply)
  }
  
  private def checkOnlineStateReached() = {
    if(switchTopology.nonEmpty && ovxInstance != null) {
      unstashAll()
      context.become(active())
      state = DiscoveryState.ONLINE
      log.info("NetworkResourceAgent is becoming ACTIVE, as TopologyDiscovery was received!")
    }
  }

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
    val tenantNet = _ovxManager.createOVXNetwork(tenant, Some(ovxInstance))
    if(tenantNet.isEmpty){
      log.error("Tenant-Network could not have been created for tenant {}. Aborting allocation on OVX!", tenant)
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
		for (actPhysSwitch <- physSwitches)
		{
			// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
			_ovxManager.createOVXSwitch(tenant, actPhysSwitch)

			// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
			_ovxManager.createAllOVXSwitchPorts(tenant, actPhysSwitch)
		}
		
    // After all Switches are established in the upper loop, connect them with each other:
		_ovxManager.connectAllOVXSwitches(tenant)

		// Create Ports at the Host's Endpoint Switch:Port connect the Host to it
		for (actHost <- hosts) {
			val physSwitchToConnect = switchTopology.find(_.dpid == actHost.endpoint.dpid)
			_ovxManager.connectOVXHost(tenant, physSwitchToConnect, actHost)
		}
		
		// Start the Tenant's OVX-Network, if not already started:
			_ovxManager.startOVXNetwork(tenant)
	}

  private def prepareFederation(tenant: Tenant, foreignGWSwitch: OFSwitch) = {
    
    val localGWSwitch = cloudConfig.cloudGateway
    
    //Add localGWSwitch, if not already added:
    if(! tenantGatewayMap.getOrElse(tenant, List()).contains(localGWSwitch))
    {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _ovxManager.createOVXSwitch(tenant, localGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _ovxManager.createAllOVXSwitchPorts(tenant, localGWSwitch)
      tenantGatewayMap = tenantGatewayMap + (tenant -> (tenantGatewayMap.getOrElse(tenant, List()) :+ localGWSwitch))
    }
    
    // Add foreignGWSwitch, if not already added:
    if(! tenantGatewayMap.getOrElse(tenant, List()).contains(foreignGWSwitch))
     {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _ovxManager.createOVXSwitch(tenant, foreignGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _ovxManager.createAllOVXSwitchPorts(tenant, foreignGWSwitch)
      tenantGatewayMap = tenantGatewayMap + (tenant -> (tenantGatewayMap.getOrElse(tenant, List()) :+ foreignGWSwitch))
    }
    
    // Connect both Gateway Switches with each other, if no link is currently established:
    // Find the srcPortMapping for the actual srcPort in the switchPortMap's actPhysSwitch entry:
//    val physLocalPortOpt = localGWSwitch.portMap.find(_._2.dpid == foreignGWSwitch.dpid)
//    val physForeignPortOpt = foreignGWSwitch.portMap.find(_._2.dpid == localGWSwitch.dpid)
//
//    val virtLocalGWOpt = tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(localGWSwitch.dpid.convertToHexLong))
//    val virtForeignGWOpt = tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(foreignGWSwitch.dpid.convertToHexLong))
//
//    if(physLocalPortOpt.isDefined && physForeignPortOpt.isDefined &&
//      virtLocalGWOpt.isDefined && virtForeignGWOpt.isDefined) {
//
//      val virtLocalGW = virtLocalGWOpt.get
//      val virtForeignGW = virtForeignGWOpt.get
//
//      val physicalLocalPort = physLocalPortOpt.get
//      val physicalForeignPort = physForeignPortOpt.get
//      val localPortMapping = switchPortMap.getOrElse(localGWSwitch, List()).find(_._1 == physicalLocalPort._1)
//      val foreignPortMapping = switchPortMap.getOrElse(foreignGWSwitch, List()).find(_._1 == physicalForeignPort._1)
//
//      if(localPortMapping.isDefined && foreignPortMapping.isDefined){
//        val (physSrcPort, virtSrcPort, srcComponent) = localPortMapping.get
//        val (physDstPort, virtDstPort, dstComponent) = foreignPortMapping.get
//
//        // Check, if a link is already existing from dst -> src or src -> dst. Only establish a new one, if not for both:
//        val alreadyConnected: Boolean = srcComponent.isDefined || dstComponent.isDefined
//        if (!alreadyConnected) {

          _ovxManager.connectOVXSwitches(tenant, localGWSwitch, foreignGWSwitch)
//                              localGWSwitch, physSrcPort, virtLocalGW, virtSrcPort,
//                              foreignGWSwitch, physDstPort, virtForeignGW, virtDstPort)
//        }
//      }
//    }
  }
  
  //TODO: move to OVXMananger or refactor
//  private def uploadNetworkToFederatedOVX(tenant: Tenant, ovxIp: InetAddress, ovxApiPort: Int, ovxCtrlPort: Int) = {
//    val virtNetOpt = tenantNetMap.get(tenant)
//    virtNetOpt match{
//        case Some(virtNet)  =>
//          log.info("Removing tenant {} OFC Controller {} from network {}...",
//                   tenant.id, virtNet.controllerUrls(0), virtNet.networkAddress)
//          _ovxConn.removeControllers(tenant.id, List(virtNet.controllerUrls(0)))
//          
//          // OVX-F has already complete knowledge over this network, as it is the secondary controller of it since network-creation.
//          // TODO: forward this physical network as a 1to1-mapping to the tenant-OFC from the OVX-F instead of the local OVX
//          // TODO (1): Bootstrap new NRA and NDA here for the OVX-F and send a ResourceRequest to it, including the tenant's whole Network
//          // TODO (2): This NRA acts as the Master NRA of the OVX-F NRA and has to route each message to and from the MMA correctly.
//          
//          
//        case None          => 
//          log.error("No virtual tenant network registered for tenant {}!",
//                    tenant.id)
//          
//    }
//  }
	
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