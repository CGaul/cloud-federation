package agents

import java.net.InetAddress

import akka.actor._
import connectors.{Link, OVXConnector}
import datatypes.{DPID, Endpoint, OFSwitch}
import messages.TopologyDiscovery

/**
 * @author Constantin Gaul, created on 1/20/15.
 */
class NetworkDiscoveryAgent(ovxIp: InetAddress, ovxApiPort: Int, networkResourceAgent: ActorRef) 
                           extends Actor with ActorLogging{
  
  val _workingThread = new Thread(new Runnable{
    override def run(): Unit = {
      log.info("NetworkDiscoveryAgent begins discovery-loop...")
      while(_shouldRun) {
        val topologyChanged = discoverPhysicalTopology()
        if(topologyChanged) {
          log.info("Updated Network-Topology discovered, sending TopologyDiscovery to NRA.")
          networkResourceAgent ! TopologyDiscovery(_discoveredSwitches)
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
  
  def discoverPhysicalTopology(): Boolean = {
    var topologyChanged = false
    
    val ovxConn = OVXConnector(ovxIp, ovxApiPort)
    val phTopo  = ovxConn.getPhysicalTopology
    
    // Convert String-DPIDs to regular DPID-Objects:
    val phTopoDPIDs = phTopo._1.map(DPID)
    
    // Add new Switches that are discovered in the physical Topology, but are currently not inside _discoveredSwitches:
    val newSwitches: List[OFSwitch] = for (actDPID <- phTopoDPIDs if ! _discoveredSwitches.exists(_.dpid == actDPID))
                                        yield OFSwitch(actDPID)
    
    // Find removed Switches, that were in the _discoveredSwitches List, but are not in the physical Topology anymore:
    val remSwitches: List[OFSwitch] = for (actDPID <- _discoveredSwitches.map(_.dpid) if ! phTopoDPIDs.contains(actDPID))
                                        yield OFSwitch(actDPID)
    
    if(newSwitches.length > 0) {
      this.log.info("Discovered new switches in the physical Topology: {}", newSwitches.map(_.dpid))
      topologyChanged = true
    }
    if(remSwitches.length > 0) {
      this.log.info("Discovered removal of switches in the physical Topology: {}", remSwitches.map(_.dpid))
      topologyChanged = true
    }
    
    
    // Update _discoveredSwitches by deleting remSwitches and adding newSwitches:
    _discoveredSwitches = _discoveredSwitches.filter(switch => ! remSwitches.map(_.dpid).contains(switch.dpid))
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
    
    return topologyChanged
  }

  def active(): Receive ={
    case "stop" => 
      context.become(inactive())
      log.info("NetworkDiscoveryAgent received \"stop\" command, becoming INACTIVE now!")
      _shouldRun = false
  }
  
  
  def inactive(): Receive = {
    case "start" => 
      context.become(active())
      log.info("NetworkDiscoveryAgent received \"start\" command, becoming ACTIVE now!")
      _shouldRun = true
      _workingThread.start()
  }

  override def receive: Receive = inactive()
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
   * @param ovxIp The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
   * @return An Akka Properties-Object
   */
  def props(ovxIp: InetAddress, ovxApiPort: Int, networkResourceAgent: ActorRef):
  Props = Props(new NetworkDiscoveryAgent(ovxIp, ovxApiPort, networkResourceAgent))
}
