package agents

import java.net.InetAddress

import akka.actor.Actor.Receive
import akka.actor._
import connectors.{Link, Endpoint, OVXConnector}
import datatypes.{DPID, OFSwitch, Host}

/**
 * Created by costa on 1/20/15.
 */
class NetworkDiscoveryAgent(ovxIp: InetAddress, ovxApiPort: Int, networkResourceAgent: ActorRef) 
                           extends Actor with ActorLogging{
  import context._
  become(inactive)
  
  
  /* Variables: */
  /* ========== */

  var _discoveredSwitches: List[OFSwitch] = List()
  var _discoveredHosts: List[Host] = List()
  
  
  /* Public Methods: */
  /* =============== */
  
  def discoverPhysicalTopology() = {
    val ovxConn = OVXConnector(ovxIp, ovxApiPort)
    val phTopo  = ovxConn.getPhysicalTopology

    // Add new Switches that are discovered in the physical Topology, but are currently not inside _discoveredSwitches:
    val newSwitches: List[OFSwitch] = for (actDPID <- phTopo._1 if ! _discoveredSwitches.contains(actDPID)) 
                                        yield OFSwitch(actDPID)
    
    // Find removed Switches, that were in the _discoveredSwitches List, but are not in the physical Topology anymore:
    val remSwitches: List[OFSwitch] = for (actDPID <- _discoveredSwitches.map(_.dpid.toString) if ! phTopo._1.contains(actDPID))
                                        yield OFSwitch(actDPID)
    
    if(newSwitches.length > 0)
      this.log.info("Discovered new switches in the physical Topology: {}", newSwitches.map(_.dpid))
    if(remSwitches.length > 0)
      this.log.info("Discovered removal of switches in the physical Topology: {}", remSwitches.map(_.dpid))
    
    
    // Update _discoveredSwitches by deleting remSwitches and adding newSwitches:
    _discoveredSwitches = _discoveredSwitches.filter(switch => ! remSwitches.map(_.dpid).contains(switch.dpid))
    _discoveredSwitches = _discoveredSwitches ++ newSwitches
    
    // Build up a link mapping from srcDPID -> List[LinkId]:
    var topoSrcLinkMap: Map[DPID, List[Int]] = Map()
    val topoSrcLinkIds = phTopo._2.map(link => (DPID(link.src.dpid), link.linkId))
    for (actLink <- topoSrcLinkIds) {
      val actSrcLink = topoSrcLinkMap.get(actLink._1)
      actSrcLink match{
        case Some(srcLink)  => topoSrcLinkMap = topoSrcLinkMap + (actLink._1 -> (srcLink :+ actLink._2))
        case None           => topoSrcLinkMap = topoSrcLinkMap + (actLink._1 -> List(actLink._2))
      }
    }
    
    // Update overall portMapping in all _discoveredSwitches:
    for (actSwitch <- _discoveredSwitches) {
      //Get all Links, whose src is pointing to the actSwitch:
      val actSrcLinks: List[Link] = phTopo._2.filter(link => topoSrcLinkMap(actSwitch.dpid).contains(link.linkId))
      
      // With info on all links, map srcPort -> (dstDPID, dstPort) in actSwitch:
      
    }
  }

  def active: Receive ={
    case "stop" => become(inactive)
    
  }
  
  def inactive: Receive = {
    case "start" => become(active)
    
  }

  override def receive: Receive = inactive
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
