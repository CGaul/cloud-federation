package agents

import java.io.File
import java.net.InetAddress

import agents.ActorMode.ActorMode
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import connectors._
import datatypes._
import messages._

import scala.util.control.Breaks._
import scala.concurrent.duration._


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
  
    private val _ovxLocalInstance = cloudConfig.cloudOvx
		private val _ovxLocalConn = OVXConnector(cloudConfig.cloudOvx.ovxIp, cloudConfig.cloudOvx.ovxApiPort)
    private val _ovxLocalManager = OVXManager(_ovxLocalConn)


/* Variables: */
/* ========== */
  
    var state = DiscoveryState.OFFLINE
  
// Topology related Variables:
// ---------------------------
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
		private val _hostTopology: Map[OVXManager, List[Host]] = Map(_ovxLocalManager -> cloudConfig.cloudHosts.toList)
		private var _switchTopology: Map[OVXManager, List[OFSwitch]] = Map()
    private var _hostPhysSwitchMap: Map[(OVXManager, Host), List[OFSwitch]] = Map()
    private var _tenantGatewayMap: Map[Tenant, List[OFSwitch]] = Map()

  
// OVX related Variables:
// ----------------------
    /**
     * The local NetworkDiscoveryAgent, directly connected to the _ovxLocalInstance: 
     */
    private var _localNDA: ActorRef = null

    /**
     * Ovx-Instance to OVXManager mapping, in order to use the physical-Topology maps above, keyed by OvxInstance instead of OVXManager
     */
    private var _ovxInstMngrMap: Map[OvxInstance, OVXManager] = Map(_ovxLocalInstance -> _ovxLocalManager)
      
  
    // Federated Master OVX-F Instance and Manager, where OVX-F is received from MMA via OvxInstanceReply:
    // Once NRA is ONLINE, these will be != null
    private var _ovxFedMasterInstance: OvxInstance = null
    private var _ovxFedMasterManager: OVXManager = null
    private var _fedMasterNDA: ActorRef = null
  
  //TODO: use:
    // Federated Slave OVX-F Instances and Managers, where OVX-F is received from MMA via ResourceFederationRequest:
    /**
     * Contains all slave federation NDAs.
     * For each incoming ResourceFederationRequest, set up a new NDAFedActor, that discovers the topology
     */
    private var _fedSlaveNDAs: List[ActorRef] = List()


/* Initial Startup: */
/* ================ */
	
	initActor()
	
	def initActor() = {
    log.info("Starting local NDA, as a child-Actor of {}...", context.self)
    _localNDA = initNDA(cloudConfig.cloudOvx, "localNDA", ActorMode.AUTO)
		// This NRA-Instance is inactive after boot-up:
		log.info("NetworkResourceAgent will be INACTIVE, until NDA sends a TopologyDiscovery...")
	}
	
	def initNDA(ovxInstance: OvxInstance, actorName: String, mode: ActorMode): ActorRef = {
		// Spawn a NetworkDiscoveryAgent (NDA) for Network-Topology-Discovery:
		val ndaProps: Props = Props(classOf[NetworkDiscoveryAgent],
                                ovxInstance, context.self, mode)
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
			case TopologyDiscovery(ovxInstance, switchList)
			=> 	recvTopologyDiscovery(ovxInstance, switchList)
				log.info("New TopologyDiscovery received.")
		}
		case _	=> log.error("Unknown message received!")
	}
	
	def inactive(): Receive = {
		case message: NRAResourceDest	=> message match {
			case _ => stash()
		}
			
		case message: NRANetworkDest => message match{
			case TopologyDiscovery(ovxInstance, switchList)
			=> 	recvTopologyDiscovery(ovxInstance, switchList)
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
    val federatedOVX = _ovxFedMasterInstance.ofcEndpoint
    
    // Create a local OVX-Network that includes tenantOfc and federatedOVX as net-OFCs, if whole resAlloc was
    // completely allocated locally, and federatedOVX only, 
    // if a federation needs to take place (tenantOfc would be deleted anyway):
    val netOFCs = if(remainResToAlloc.isEmpty) {List(tenantOfc, federatedOVX)} else {List(federatedOVX)}
    val virtNet = _ovxLocalManager.createOVXNetwork(tenant, netOFCs)
    
    // After network creation, map the allocated resAlloc to the tenant's local OVX-network:
    if(virtNet.isDefined){
      log.info("Mapping hosts {} to virtual tenant's {} network...", hostList.map(_.mac), tenant.id)
		  val (_, hostConnPortMap) = mapAllocOnOvx(_ovxLocalManager, tenant, hostList)

      // Start the newly mapped tenant network on the local OVX:
      log.info("Starting virtual tenant's {} network...", tenant.id)
      _ovxLocalManager.startOVXNetwork(tenant)

      // Save the local allocation and hosts mapping for the requested tenant:
      val allocationList = allocationsPerHost.map(_._2).toList
      _tenantResAllocs = _tenantResAllocs + (tenant -> (_tenantResAllocs.getOrElse(tenant, List()) ++ allocationList))
      _tenantHostList = _tenantHostList + (tenant -> (_tenantHostList.getOrElse(tenant, List()) ++ hostList))
      
      
      //TODO: after local mapping, wait for the federated NDA to send a TopologyDiscovery...
      val futureDiscoveryReply = _fedMasterNDA.ask(DiscoveryRequest()) (Timeout(10 seconds).duration)
      futureDiscoveryReply onSuccess  {
        case topoDiscovery: TopologyDiscovery =>
          log.info("Received asked TopologyDiscovery {} from OVX-F NDA!", topoDiscovery.switches)
          //TODO: remap virtual hosts before recvTopologyDiscovery-call:

          // insert the discovered, virtualized Topology from OVX-F the topology mappings,
          // using the pre-mapped virtualized hosts:
          recvTopologyDiscovery(topoDiscovery.ovxInstance, topoDiscovery.switches)
          
          // ResAlloc Federation:
          // If remaining resAllocs are left over from allocateLocally(..), 
          // bootstrap a virtual tenant federation-net on OVX-F and send a ResourceRequest to the MMA.  
          if(remainResToAlloc.isDefined){
            log.info("ResourceRequest {} could not have been allocated completely on the local cloud Remaining ResAlloc: {}. " +
              "Initializing Federation...", resourceToAlloc, remainResToAlloc)
            // before sending a ResourceReq to the MMA, 
            // start federated Network and hand tenant-OVX-mapping to federationRequest via tenant
            log.info("Bootstrapping a virtual federated tenant-net on OVX-F {} for tenant {}",
              _ovxFedMasterInstance, tenant.id)
            bootstrapFedTenantNet(tenant, hostConnPortMap)

            log.info("Forwarding remainingResAlloc {} as a ResourceRequest to MMA, " +
              "in order to get federated resources from foreign clouds!", remainResToAlloc)
            matchMakingAgent ! ResourceRequest(tenant, remainResToAlloc.get)
          }
          else {
            log.info("ResourceRequest {} was completely allocated on the local cloud!", resourceToAlloc)
            sender() ! ResourceReply(resourceToAlloc)
          }
      }
      futureDiscoveryReply onFailure {
        case _ => log.error("No asked OvxInstanceReply could be received from the federator in an async future!")
      }
      
    }
    else{
      log.error("Tenant-Network could not have been created for tenant {}. Aborting allocation on OVX!", tenant.id)      
      return
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
    val virtNet = _ovxLocalManager.createOVXNetwork(tenant, List(ovxInstance.ofcEndpoint))
    if(virtNet.isDefined){
      log.info("Mapping hosts {} to virtual tenant's {} network.", hostList.map(_.mac), tenant.id)
      mapAllocOnOvx(_ovxLocalManager, tenant, hostList)

      // Start the newly mapped tenant network on the local OVX:
      log.info("Starting virtual tenant's {} network...", tenant.id)
      _ovxLocalManager.startOVXNetwork(tenant)
    }
    else{
      log.error("Tenant-Network could not have been created for tenant {}. Aborting allocation on OVX!", tenant.id)
      return
    }
    prepareFederation(tenant, foreignGWSwitch)
    
    // tenant carries an ovxId from the foreign master federation cloud. This is needed, so that
    // mapAllocOnOvx on the OVX-F can use this ovxId to bootstrap the local network on a federation network in OVX-F.
    _ovxFedMasterManager.addTenantOvxId(tenant)
    
    // Virtualize DPID of the Endpoint in each Host before allocating it on OFV-F
    val virtHostList = hostList.map(Host.virtualizedByOvx)
    
    // TODO: Map the tenant's locally virtualized OVX network on the federated OVX-F:
    // TODO: physicalSwitches are not working here, as their DPIDs have to be virtualized before..
    mapAllocOnOvx(_ovxFedMasterManager, tenant, virtHostList)

    
		if(remainResToAlloc.size > 0){
			// TODO: send Information about remaining Resources to Allocate back to the sender.
		}
	}
    
  
  /**
   * Received from local or one of the federation NDAs.
   * Maps the discovered Topology to the corresponding ovxManager in the switchTopology and hostPhysSwitchMap
   * and sends a FederateableResourceDiscovery to the MMA, if the incoming ovxInstance was the local one.
   * 
   * IMPLICIT: As the hostTopology is not updated within this method (OVX simply lacks the API return values to do so)
   *           the _hostTopology needs to be set before this method is called.
   * @param switchTopology
   */
	private def recvTopologyDiscovery(ovxInstance: OvxInstance, switchTopology: List[OFSwitch]) = {
		log.info("Received new Switch-Topology from {}, including {} switches.", sender(), switchTopology.length)
    // Check, whether the incoming ovxInstance is already registered (if so, an ovxMngr-mapping is available)
    // and add the discovered switchTopology to the ovxManager's topology mapping accordingly:
    val ovxMngrOpt = _ovxInstMngrMap.get(ovxInstance)
    ovxMngrOpt match{
        case Some(ovxMngr) =>
          log.info("Adding elements to switchTopology and hostPhysSwitchMap for ovxManager {}...", ovxMngr)
          //Add all discovered Switches to the global switchTopology, grouped by ovxManager:
          this._switchTopology = _switchTopology + (ovxMngr -> switchTopology)
          
          // IMPLICIT: virtual host remapping was added to _hostTopology(ovxMngr), before this method was called!
          // get all OFSwitches that are connected to each host and save it as a host -> List[OFSwitch] mapping per Host:
          val hostsSwitchesMapping = _hostTopology(ovxMngr)
                                        .map(host => host -> switchTopology
                                        .filter(_.dpid == host.endpoint.dpid))
          
          // for each host, get the host -> List[OFSwitch] mapping and add it to the _hostPhysSwitchMap, enriching
          // it with the ovxManager-Key:
          for (hostSwitchMap <- hostsSwitchesMapping) {
            this._hostPhysSwitchMap = _hostPhysSwitchMap + ((ovxMngr, hostSwitchMap._1) -> hostSwitchMap._2)
          }
          
        case None          => 
          log.error("No ovxInstance {} registered for TopologyDiscovery {}!", ovxInstance, switchTopology)
    }
    
    //For the local ovxInstance, send a FederateableResourceDiscovery at the end:
    if(ovxInstance == _ovxLocalInstance) {
      log.info("Received Topology's origin was the local OVX instance. Sending federateable Resource to MMA now...")
      sendFederateableResourcesToMMA()
    }
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
    this._ovxFedMasterInstance = ovxFedInstance
    val fedOvxConn = OVXConnector(ovxFedInstance.ovxIp, ovxFedInstance.ovxApiPort)
    this._ovxFedMasterManager = OVXManager(fedOvxConn)
    
    this._ovxInstMngrMap = _ovxInstMngrMap + (_ovxFedMasterInstance -> _ovxFedMasterManager)
    
    // Moreover, start a NetworkDiscoveryAgent on the OVX-F, 
    // so that this NRA has knowledge about the virtualized OFC-Switches at the OVX-F's SB interface
    log.info("Bootstrapping federated NDA on OVX-F {}...", ovxFedInstance)
    this._fedMasterNDA = initNDA(ovxFedInstance, "federatedNDA", ActorMode.MANUAL)
  }
  



/* Private Helper Methods: */
/* ======================= */
  
  /**
   * Sending a FederateableResourceDiscovery to the MMA, once a new TopologyDiscovery was received.
   * FederateableResourceDiscovery contains (Host -> ResourceAlloc) tuples, where each Host is federateable.
   */
  private def sendFederateableResourcesToMMA() = {
    log.info("Sending new FederateableResources to MMA...")
    val federateableHosts = _hostTopology(_ovxLocalManager).filter(_.isFederateable)
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
    if(_switchTopology.nonEmpty && _ovxFedMasterInstance != null) {
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
		val sortedHosts			= _hostTopology(_ovxLocalManager).sorted(RelativeHostByResOrdering)
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
   * @return A (phys -> virt) port map for each srcSwitch, pointing to the dstSwitch. 
   *         Returned by OVXManager.connectAllOvxSwitches(tenant)
   */
	private def mapAllocOnOvx(ovxManager: OVXManager, 
                            tenant: Tenant, hosts: List[Host]): 
                  (Map[OFSwitch, List[(Short, Short, OFSwitch)]], Map[Host, (Short, Short, OFSwitch)]) = {

		// The _hostPhysSwitchMap defines a mapping between each known host and all physical Switches,
		// that are connected to this host and discovered via a TopologyDiscovery earlier.
		// To continue, get all physical Switches for the given host's allocation:
		// (get a flat list of each host -> List[Switch] mapping)
		val physSwitches = hosts.flatMap(_hostPhysSwitchMap(ovxManager, _)).distinct
		
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
		val switchConnPortMap = ovxManager.connectAllOVXSwitches(tenant)

    var hostConnPortMap: Map[Host, (Short, Short, OFSwitch)] = Map()
		// Create Ports at the Host's Endpoint Switch:Port and connect the Host to it
		for (actHost <- hosts) {
      val hostSwitches = _hostPhysSwitchMap(ovxManager, actHost)
			val physSwitchToConnect = hostSwitches.find(_.dpid == actHost.endpoint.dpid)
      physSwitchToConnect match{
          case Some(physSwitch) =>
            log.info("Creating Port at Switch {} for Host {}...", physSwitch, actHost)
            val portMapOpt = ovxManager.createOVXHostPort(tenant, physSwitch, actHost)
            
            log.info("Connecting Host {} to Switch {}...", actHost, physSwitchToConnect)
            val virtHostOpt = ovxManager.connectOVXHost(tenant, physSwitch, actHost)

            // If host port creation and connection were successful, add the mapping to the hostConnPortMap:
            if(portMapOpt.isDefined && virtHostOpt.isDefined){
              val portSwitchTuple = (portMapOpt.get._1, portMapOpt.get._2, physSwitch)
              hostConnPortMap = hostConnPortMap + (actHost -> portSwitchTuple)
            }

          case None =>
            log.error("Switch is not existing for Host {} in tenant's {} network!", actHost, tenant.id)
      }
		}
    
    return (switchConnPortMap, hostConnPortMap)
	}

  private def prepareFederation(tenant: Tenant, foreignGWSwitch: OFSwitch) = {
    
    val localGWSwitch = cloudConfig.cloudGateway
    
    //Add localGWSwitch, if not already added:
    if(! _tenantGatewayMap.getOrElse(tenant, List()).contains(localGWSwitch))
    {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _ovxLocalManager.createOVXSwitch(tenant, localGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _ovxLocalManager.createAllOVXSwitchPorts(tenant, localGWSwitch)
      _tenantGatewayMap = _tenantGatewayMap + (tenant -> (_tenantGatewayMap.getOrElse(tenant, List()) :+ localGWSwitch))
    }
    
    // Add foreignGWSwitch, if not already added:
    if(! _tenantGatewayMap.getOrElse(tenant, List()).contains(foreignGWSwitch))
     {
      // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch:
      _ovxLocalManager.createOVXSwitch(tenant, foreignGWSwitch)

      // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
      _ovxLocalManager.createAllOVXSwitchPorts(tenant, foreignGWSwitch)
      _tenantGatewayMap = _tenantGatewayMap + (tenant -> (_tenantGatewayMap.getOrElse(tenant, List()) :+ foreignGWSwitch))
    }
    
    // Connect both Gateway Switches with each other, if no link is currently established:
    // Find the srcPortMapping for the actual srcPort in the switchPortMap's actPhysSwitch entry:
    _ovxLocalManager.connectOVXSwitches(tenant, localGWSwitch, foreignGWSwitch)
  }

  /**
   * Create a tenant network on the OVX-F instance, remove the tenant OFC from the tenant's local-OVX network
   * and add all NetworkComponents to the new tenant's OVX-F network.
   * @param tenant
   */
  private def bootstrapFedTenantNet(tenant: Tenant) = {

    // Remove tenant OFC from network, as it is will be applied back again in the child NRA, responsible for the
    // upper layer virtualization (the OVX-F layer):
    _ovxLocalManager.removeOfcFromTenantNet(tenant, tenant.ofcIp)
    
    // Start new virtual network for tenant in federated OVX-F instance and hand over ovxId of virt-Net to tenant:
    val tenantOfc = (tenant.ofcIp, tenant.ofcPort)
    val virtNet = _ovxFedMasterManager.createOVXNetwork(tenant, List(tenantOfc))
    tenant.ovxId_(virtNet.get.tenantId.getOrElse(-1))
    
    // Virtualize DPID of the Endpoint in each Host in tenant's hostList before allocating it on OVX-F
    val virtHostList = _tenantHostList(tenant).map(Host.virtualizedByOvx)
    
    // TODO and add virtualized OFSwitches to the _hostPhysSwitchMap for each virtualized Host:
    
    // Finally, establish the virtual Hosts and Switches mapping on the OVX-F instance:
    mapAllocOnOvx(_ovxFedMasterManager, tenant, virtHostList)
    
    // At last, start the newly mapped tenant network on OVX-F:
    _ovxFedMasterManager.startOVXNetwork(tenant)
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