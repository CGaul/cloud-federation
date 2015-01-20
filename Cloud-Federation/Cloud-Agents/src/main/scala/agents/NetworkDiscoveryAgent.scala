package agents

import java.net.InetAddress

import akka.actor.Actor.Receive
import akka.actor.ActorRef
import connectors.{OVXConnector, Network}
import datatypes.{Host, OFSwitch$, Tenant}
import messages.{ResourceFederationReply, ResourceFederationRequest, ResourceRequest, NRAResourceDest}

/**
 * Created by costa on 1/20/15.
 */
class NetworkDiscoveryAgent(ovxIp: InetAddress, ovxApiPort: Int, networkResourceAgent: ActorRef) {

  /* Variables: */
  /* ========== */

  var _discoveredSwitches: List[OFSwitch] = List()
  var _discoveredHosts: List[Host] = List()
  
  
  /* Public Methods: */
  /* =============== */
  
  def discoverPhysicalTopology() = {
    val ovxConn = OVXConnector(ovxIp, ovxApiPort)
    val phTopo  = ovxConn.getPhysicalTopology
  }

}
