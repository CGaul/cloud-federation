package agents

import java.net.InetAddress

import agents.ActorMode.ActorMode
import akka.actor._
import connectors.{Link, OVXConnector}
import datatypes.{OvxInstance, DPID, Endpoint, OFSwitch}
import messages.{DiscoveryRequest, TopologyDiscovery}


/* Classes: */
/* ======== */

/**
 * @author Constantin Gaul, created on 1/20/15.
 */
class NetworkDiscoveryAgent(ovxInstance: OvxInstance,
                            networkResourceAgent: ActorRef, mode: ActorMode)
                           extends Actor with ActorLogging{
  
  val _workingThread = new Thread(new Runnable{
    override def run(): Unit = {
      log.info("NetworkDiscoveryAgent begins discovery-loop...")
      while(_shouldRun) {
        val (topologyChanged, newSwitches, removedSwitches) = discoverPhysicalTopology()
        if(topologyChanged) {
          log.info("Updated Network-Topology discovered, sending TopologyDiscovery to NRA.")
          networkResourceAgent ! TopologyDiscovery(ovxInstance, newSwitches, removedSwitches)
        }
        Thread.sleep(10 * 1000) //sleep 10 seconds between each discovery
      }
      log.info("NetworkDiscoveryAgent stopped discovery-loop.")
    }
  })


  /* Variables: */
  /* ========== */

  var _shouldRun = false
  var _discoveredSwitches: List[OFSwitch] = List()


/* Initial Startup: */
/* ================ */
  
  initActor()

  def initActor() = {
    // This NRA-Instance is inactive after boot-up:
    log.info("NetworkDiscoveryAgent will be INACTIVE, until NRA sends the \"start\" command...")
  }



  /* Public Methods: */
  /* =============== */
  
  def discoverPhysicalTopology(): (Boolean, List[OFSwitch], List[OFSwitch]) = {
    var topologyChanged = false
    
    val ovxConn = OVXConnector(ovxInstance.ovxIp, ovxInstance.ovxApiPort)
    val phTopo  = ovxConn.getPhysicalTopology
    
    // Convert String-DPIDs to regular DPID-Objects:
    val phTopoDPIDs = phTopo._1.map(new DPID(_))
    
    // Add new Switches that are discovered in the physical Topology, but are currently not inside _discoveredSwitches:
    val newSwitches: List[OFSwitch] = for (actDPID <- phTopoDPIDs if ! _discoveredSwitches.exists(_.dpid == actDPID))
                                        yield new OFSwitch(actDPID)
    
    // Find removed Switches, that were in the _discoveredSwitches List, but are not in the physical Topology anymore:
    val removedSwitches: List[OFSwitch] = for (actDPID <- _discoveredSwitches.map(_.dpid) if ! phTopoDPIDs.contains(actDPID))
                                        yield new OFSwitch(actDPID)
    
    if(newSwitches.length > 0) {
      this.log.info("Discovered new switches in the physical Topology: {}", newSwitches.map(_.dpid))
      topologyChanged = true
    }
    if(removedSwitches.length > 0) {
      this.log.info("Discovered removal of switches in the physical Topology: {}", removedSwitches.map(_.dpid))
      topologyChanged = true
    }
    
    
    // Update _discoveredSwitches by deleting removedSwitches and adding newSwitches:
    _discoveredSwitches = _discoveredSwitches.filter(switch => ! removedSwitches.map(_.dpid).contains(switch.dpid))
    _discoveredSwitches = _discoveredSwitches ++ newSwitches
    
    // Build up a link mapping from srcDPID -> List[LinkId]:
    var topoSrcLinkMap: Map[DPID, List[Int]] = Map()
    val topoSrcLinkIds = phTopo._2.map(link => (DPID(link.src.dpid), link.linkId))
    for (actLink <- topoSrcLinkIds) {
      val actSrcLink = topoSrcLinkMap.get(actLink._1)
      actSrcLink match{
        case Some(linkList) => topoSrcLinkMap = topoSrcLinkMap + (actLink._1 -> (linkList :+ actLink._2))
        case None           => topoSrcLinkMap = topoSrcLinkMap + (actLink._1 -> List(actLink._2))
      }
    }
    
    // Update overall portMapping in all _discoveredSwitches:
    for (actSwitch <- _discoveredSwitches) {
      //Get all Links, whose src is pointing to the actSwitch:
      val actSrcLinks: List[Link] = phTopo._2.filter(link => topoSrcLinkMap(actSwitch.dpid).contains(link.linkId))

      // With info on all links, map srcPort -> (dstDPID, dstPort) in actSwitch:
      // TODO: fix really ugly connectors.Endpoint -> datatypes.Endpoint transformation:
      val srcPortRemap = actSrcLinks.map(link => link.src.port -> Endpoint(DPID(link.dst.dpid), link.dst.port)).toMap
      actSwitch.remapPorts(srcPortRemap)
    }
    
    val newLinkedSwitches = _discoveredSwitches.filter(newSwitches.contains)
    return (topologyChanged,  newLinkedSwitches, removedSwitches)
  }

  def active(): Receive ={
    case "stop" => 
      context.become(inactive())
      log.info("NetworkDiscoveryAgent received \"stop\" command, becoming INACTIVE now!")
      _shouldRun = false

    case DiscoveryRequest => 
      log.info("Received DiscoveryRequest. Checking for new discoveries...")
      replyOnTopologyChange(6, networkResourceAgent)
      
  }
  
  
  def inactive(): Receive = {
    case "start" => 
      context.become(active())
      log.info("NetworkDiscoveryAgent received \"start\" command, becoming ACTIVE now!")
      if(mode == ActorMode.AUTO) {
        log.info("Started NDA in Mode AUTO. " +
          "Starting workingThread and sending periodically updated TopologyDiscoveries now...")
        _shouldRun = true
        _workingThread.start()
      }
  }

  override def receive: Receive = inactive()

  /**
   * Runs a TopologyDiscovery on the ovxInstance and replies to the caller, if a topology-change was detected.
   * If no topology-change was detected, retry the discovery for number of given iterations, 
   * after waiting for 1 second beforehand.
   * @param iters The number of iterations that will be retried, if no Topology was discovered in the last round.
   * @param caller The requester that has sent a DiscoveryRequest before. A TopologyDiscovery reply will be send,
   *               If some changes were discovered within the number of iterations.
   */
  def replyOnTopologyChange(iters: Int, caller: ActorRef): Unit = {
    if(iters == 0) {
      log.warning("No Topology discovery was found at all!")
      return
    }
    
    val (topologyChanged, newSwitches, removedSwitches) = discoverPhysicalTopology()
    if(topologyChanged) {
      log.info("Discovered Topology change in {} round. new Switches: {}, removed Switches: {}",
               iters, newSwitches, removedSwitches)
      caller ! TopologyDiscovery(ovxInstance, newSwitches, removedSwitches)
      return
    }
    else{
      log.info("No Topology change was discovered in round {}. Waiting and restarting discovery...", iters)
      Thread.sleep(1000)
      replyOnTopologyChange(iters - 1, caller)
    }
  }
}


/**
 * Companion Object of the NetworkDiscoveryAgent,
 * in order to implement some default behaviours
 */
object NetworkDiscoveryAgent
{
  /**
   * props-method is used in the AKKA-Context, spawning a new Agent.
   * In this case, to generate a new NetworkDiscoveryAgent, call
   * 	val ccfmProps = Props(classOf[NetworkDiscoveryAgent],
   * 	                      ovxIp, ovxApiPort, nraRef)
   * 	val ccfmAgent = system.actorOf(ccfmProps, name="NetworkDiscoveryAgent-x")
   * @param ovxInstance The ovxInstance that this NetworkDiscoveryAgent discovers physicalTopologies on.
   * @param networkResourceAgent The NRA that this NDA is bound to as a child actor
   * @param mode Either MANUAL, where this NDA listens on DiscoveryRequests only or AUTOMATIC, where
   *      the NDA automatically sends updated TopologyDiscoveries to the NRA 
   *      *      on top of the unconditional DiscoveryRequest reply.
   *      IN AUTOMATIC mode, a discoveryThread is kept up and running.
   *
   * @return An Akka Properties-Object
   */
  def props(ovxInstance: OvxInstance, networkResourceAgent: ActorRef, mode: ActorMode):
  Props = Props(new NetworkDiscoveryAgent(ovxInstance, networkResourceAgent, mode))
}
