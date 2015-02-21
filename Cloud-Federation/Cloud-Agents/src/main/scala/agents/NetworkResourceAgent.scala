package agents

import java.io.File
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

   /**
    * Contains all locally allocated ResourceAllocations per Tenant. 
    * Written after an allocateLocally(..) in recvResourceRequest(..).
    */
    private var _tenantResAllocs: Map[Tenant, List[ResourceAlloc]] = Map()
    /**
     * Contains all hosts that each tenant has allocated in his virtual Network via mapAllocOnOvx(..)
     */
    private var _tenantHostList: Map[Tenant, List[Host]] = Map()
  
		// Physical Topologies, received from the CCFM (hosts) and the NDA (switches)
		private val _hostTopology: List[Host] = cloudConfig.cloudHosts.toList
		private var _switchTopology: List[OFSwitch] = List()
    private var _hostPhysSwitchMap: Map[Host, List[OFSwitch]] = Map()
    private var _tenantGatewayMap: Map[Tenant, List[OFSwitch]] = Map()

    private var _ndaActor: ActorRef = null
  
    // Federated OVX-F Instance and Manager, where OVX-F is received from MMA:
    // Once NRA is ONLINE, these will be != null
    private var _ovxFedInstance: OvxInstance = null
    private var _ovxFedManager: OVXManager = null
//    private var _ndaFedActor: ActorRef = null //TODO: probably not needed. Delete, if manually adding virt OFSwitches.
  


/* Initial Startup: */
/* ================ */
	
	initActor()
	
	def initActor() = {
    log.info("Starting local NDA, as a child-Actor of {}...", context.self)
    _ndaActor = initNDA(cloudConfig.cloudOvx.ovxIp, cloudConfig.cloudOvx.ovxApiPort, "localNDA")
		// This NRA-Instance is inactive after boot-up:
		log.info("NetworkResourceAgent will be INACTIVE, until NDA sends a TopologyDiscovery...")
	}
	
	def initNDA(ovxIp: InetAddress, ovxApiPort: Short, actorName: String): ActorRef = {
		// Spawn a NetworkDiscoveryAgent (NDA) for Network-Topology-Discovery:
		val ndaProps: Props = Props(classOf[NetworkDiscoveryAgent],
                                ovxIp, ovxApiPort, context.self)
		val ndaActor = context.actorOf(ndaProps, name=actorName)
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
      case OvxInstanceReply(ovxFedInstance)
      =>  recvOvxInstanceReply(ovxFedInstance)
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
    
    // Prepare possible network OFCs, before local OVX-network creation occurs:
    val tenantOfc = (tenant.ofcIp, tenant.ofcPort)
    val federatedOVX = _ovxFedInstance.ofcEndpoint
    
    // Create a local OVX-Network that includes tenantOfc and federatedOVX as net-OFCs, if whole resAlloc was
    // completely allocated locally, and federatedOVX only, 
    // if a federation needs to take place (tenantOfc would be deleted anyway):
    val netOFCs = if(remainResToAlloc.isEmpty) {List(tenantOfc, federatedOVX)} else {List(federatedOVX)}
    val virtNet = _ovxManager.createOVXNetwork(tenant, netOFCs)
    
    // After network creation, map the allocated resAlloc to the tenant's local OVX-network:
    if(virtNet.isDefined){
      log.info("Mapping hosts {} to virtual tenant's {} network...", hostList.map(_.mac), tenant.id)
		  mapAllocOnOvx(_ovxManager, tenant, hostList)

      // Start the newly mapped tenant network on the local OVX:
      log.info("Starting virtual tenant's {} network...", tenant.id)
      _ovxManager.startOVXNetwork(tenant)
    }
    else{
      log.error("Tenant-Network could not have been created for tenant {}. Aborting allocation on OVX!", tenant.id)      
      return
    }

    // Save the local allocation and hosts mapping for the requested tenant:
    val allocationList = allocationsPerHost.map(_._2).toList
    _tenantResAllocs = _tenantResAllocs + (tenant -> (_tenantResAllocs.getOrElse(tenant, List()) ++ allocationList))
    _tenantHostList = _tenantHostList + (tenant -> (_tenantHostList.getOrElse(tenant, List()) ++ hostList))

		// If there is still a ResourceAlloc remaining, after the local cloudHosts tried to
		// allocate the whole ResourceAlloc-Request, send the remaining ResourceAlloc Split
		// to the MatchMakingAgent, in order to find a Federated Cloud that cares about the Resources:
		if(remainResToAlloc.isDefined){
			log.info("ResourceRequest {} could not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceToAlloc, remainResToAlloc)
      // before sending a ResourceReq to the MMA, 
      // start federated Network and hand tenant-OVX-mapping to federationRequest via tenant
      bootstrapFedTenantNet(tenant)

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
    
    //TODO: what else here?
    prepareFederation(tenant, foreignGWSwitch)
    
	}

  
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
		
		// Prepare the locally fulfilled allocations, managed by the local OVX and the federation-Gateways
    // for the upcoming OVX-F federation transition:
		val hostList = allocationsPerHost.map(_._1).toList
    val virtNet = _ovxManager.createOVXNetwork(tenant, List(ovxInstance.ofcEndpoint))
    if(virtNet.isDefined){
      log.info("Mapping hosts {} to virtual tenant's {} network.", hostList.map(_.mac), tenant.id)
      mapAllocOnOvx(_ovxManager, tenant, hostList)

      // Start the newly mapped tenant network on the local OVX:
      log.info("Starting virtual tenant's {} network...", tenant.id)
      _ovxManager.startOVXNetwork(tenant)
    }
    else{
      log.error("Tenant-Network could not have been created for tenant {}. Aborting allocation on OVX!", tenant.id)
      return
    }
    prepareFederation(tenant, foreignGWSwitch)
    
    // tenant carries an ovxId from the foreign master federation cloud. This is needed, so that
    // mapAllocOnOvx on the OVX-F can use this ovxId to bootstrap the local network on a federation network in OVX-F.
    _ovxFedManager.addTenantOvxId(tenant)
    
    // Virtualize DPID of the Endpoint in each Host before allocating it on OFV-F
    val virtHostList = hostList.map(Host.virtualizeOvxHost)
    
    // TODO: Map the tenant's locally virtualized OVX network on the federated OVX-F:
    // TODO: physicalSwitches are not working here, as their DPIDs have to be virtualized before..
    mapAllocOnOvx(_ovxFedManager, tenant, virtHostList)

    
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
		this._switchTopology = switchTopology
		this._hostPhysSwitchMap = _hostPhysSwitchMap ++ _hostTopology.map(host => host -> switchTopology.filter(_.dpid == host.endpoint.dpid))
    sendFederateableResourcesToMMA()
	}

  /**
   * Received from local MMA
   * Contains the own OVX-F instance, provisioned by the Federation-Broker.
   * After OVX-F was received, additional steps have to be done, that make this NRA be able to act as the
   * federation master for other slave foreign MMAs / NRAs.
   * @param ovxFedInstance The OVX-F, provisioned from the FedBroker and requested by the local MMA.
   */
  private def recvOvxInstanceReply(ovxFedInstance: OvxInstance) = {
    log.info("OvxFedInstance {} received from MMA {}", ovxFedInstance, sender())
    
    // First, establish knowledge of the ovxFedInstance and establish an ovxFedManager that will
    // be used for a tenant's virtual federated network creation, mapping and startup:
    this._ovxFedInstance = ovxFedInstance
    val fedOvxConn = OVXConnector(ovxFedInstance.ovxIp, ovxFedInstance.ovxApiPort)
    this._ovxFedManager = OVXManager(fedOvxConn)
    
    //TODO: really needed or workaround of manual virtualization better?
    // Moreover, start a NetworkDiscoveryAgent on the OVX-F, 
    // so that this NRA has knowledge about the virtualized OFC-Switches at the OVX-F's SB interface
//    log.info("Bootstrapping federated NDA on OVX-F {}...", ovxFedInstance)
//    this._ndaFedActor = initNDA(ovxFedInstance.ovxIp, ovxFedInstance.ovxApiPort, "federatedNDA")
  }
  



/* Private Helper Methods: */
/* ======================= */
  
  /**
   * Sending a FederateableResourceDiscovery to the MMA, once a new TopologyDiscovery was received.
   * FederateableResourceDiscovery contains (Host -> ResourceAlloc) tuples, where each Host is federateable.
   */
  private def sendFederateableResourcesToMMA() = {
    log.info("Sending new FederateableResources to MMA...")
    val federateableHosts = _hostTopology.filter(_.isFederateable)
    val federateableResources = federateableHosts.map(host => host -> host.allocatedResources)
    var federateableReply: Vector[(Host, ResourceAlloc)] = Vector()
    for ((actHost, actFedRes) <- federateableResources) {
      // Add each ResourceAlloc in actFedRes to the federateableReply (host, ResAlloc) vector:
      actFedRes.foreach(resAlloc => federateableReply = federateableReply :+ (actHost, resAlloc))
    }
    // Finally send a FederateableResourceDiscovery local MMA:
    matchMakingAgent ! FederateableResourceDiscovery(federateableReply)
  }
  
  private def checkOnlineStateReached() = {
    if(_switchTopology.nonEmpty && _ovxFedInstance != null) {
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

  /**
   * Maps a host-List to a previously created OVX-Network for the given tenant.
   * This method handles the whole mapping between the hosts and all relevant physical Switches in between.
   * To do that, each host's endpoint is queried, pointing to the physical Switch it is connected to.
   *
   * For each physical Switch, a virtual OVX-Switch, the virtual Switch's ports and the virtual interconnections
   * between each Switch are created here.
   * 
   * This method does NOT create or start an OVX-network! Network-creation (via ovxManager.createOVXNetwork(..)) needs
   * to be done before this method is called, starting this network (via ovxManager.startOVXNetwork(tenant)) has
   * to be done afterwards.
   *  
   * @param ovxManager The OVXManager to be used for the OVX-Connection. Either a local, or a federated OVXManager may
   *                   be used.
   * @param tenant The Tenant that owns a virtual OVX-network, previously created via ovxManager.createOVXNetwork(..)
   * @param hosts The list of hosts, that are allocated locally to the given tenant.
   */
	private def mapAllocOnOvx(ovxManager: OVXManager, 
                            tenant: Tenant, hosts: List[Host]): Unit = {

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
		for (actPhysSwitch <- physSwitches)
		{
			// Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
			ovxManager.createOVXSwitch(tenant, actPhysSwitch)

			// Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
			ovxManager.createAllOVXSwitchPorts(tenant, actPhysSwitch)
		}
		
    // After all Switches are established in the upper loop, connect them with each other:
		ovxManager.connectAllOVXSwitches(tenant)

		// Create Ports at the Host's Endpoint Switch:Port and connect the Host to it
		for (actHost <- hosts) {
      val hostSwitches = _hostPhysSwitchMap(actHost)
			val physSwitchToConnect = hostSwitches.find(_.dpid == actHost.endpoint.dpid)
      physSwitchToConnect match{
          case Some(physSwitch) =>
            log.info("Creating Port at Switch {} for Host {}...", physSwitch, actHost)
            ovxManager.createOVXHostPort(tenant, physSwitch, actHost)
            
            log.info("Connecting Host {} to Switch {}...", actHost, physSwitchToConnect)
            ovxManager.connectOVXHost(tenant, physSwitch, actHost)

          case None =>
            log.error("Switch is not existing for Host {} in tenant's {} network!", actHost, tenant.id)
      }
		}
	}

  private def prepareFederation(tenant: Tenant, foreignGWSwitch: OFSwitch) = {
    
    val localGWSwitch = cloudConfig.cloudGateway
    
    //Add localGWSwitch, if not already added:
    if(! _tenantGatewayMap.getOrElse(tenant, List()).contains(localGWSwitch))
    {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _ovxManager.createOVXSwitch(tenant, localGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _ovxManager.createAllOVXSwitchPorts(tenant, localGWSwitch)
      _tenantGatewayMap = _tenantGatewayMap + (tenant -> (_tenantGatewayMap.getOrElse(tenant, List()) :+ localGWSwitch))
    }
    
    // Add foreignGWSwitch, if not already added:
    if(! _tenantGatewayMap.getOrElse(tenant, List()).contains(foreignGWSwitch))
     {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _ovxManager.createOVXSwitch(tenant, foreignGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _ovxManager.createAllOVXSwitchPorts(tenant, foreignGWSwitch)
      _tenantGatewayMap = _tenantGatewayMap + (tenant -> (_tenantGatewayMap.getOrElse(tenant, List()) :+ foreignGWSwitch))
    }
    
    // Connect both Gateway Switches with each other, if no link is currently established:
    // Find the srcPortMapping for the actual srcPort in the switchPortMap's actPhysSwitch entry:
    _ovxManager.connectOVXSwitches(tenant, localGWSwitch, foreignGWSwitch)
  }

  /**
   * Create a tenant network on the OVX-F instance, remove the tenant OFC from the tenant's local-OVX network
   * and add all NetworkComponents to the new tenant's OVX-F network.
   * @param tenant
   */
  private def bootstrapFedTenantNet(tenant: Tenant) = {

    // Remove tenant OFC from network, as it is will be applied back again in the child NRA, responsible for the
    // upper layer virtualization (the OVX-F layer):
    _ovxManager.removeOfcFromTenantNet(tenant, tenant.ofcIp)
    
    // Start new virtual network for tenant in federated OVX-F instance and hand over ovxId of virt-Net to tenant:
    val tenantOfc = (tenant.ofcIp, tenant.ofcPort)
    val virtNet = _ovxFedManager.createOVXNetwork(tenant, List(tenantOfc))
    tenant.ovxId_(virtNet.get.tenantId.getOrElse(-1))
    
    // Virtualize DPID of the Endpoint in each Host in tenant's hostList before allocating it on OVX-F
    val virtHostList = _tenantHostList(tenant).map(Host.virtualizeOvxHost)
    
    // TODO and add virtualized OFSwitches to the _hostPhysSwitchMap for each virtualized Host:
    
    // Finally, establish the virtual Hosts and Switches mapping on the OVX-F instance:
    mapAllocOnOvx(_ovxFedManager, tenant, virtHostList)
    
    // At last, start the newly mapped tenant network on OVX-F:
    _ovxFedManager.startOVXNetwork(tenant)
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