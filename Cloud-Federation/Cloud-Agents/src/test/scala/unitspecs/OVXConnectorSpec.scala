package unitspecs

import java.net.InetAddress

import connectors.OVXConnector
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}

/**
 * @author Constantin Gaul, created on 1/15/15.
 */
class OVXConnectorSpec extends FlatSpec with Matchers with GivenWhenThen
{
  /* OVXConnector-Class Unit-Spec */
  /* ============================ */
  
  behavior of "The OVXConnector"
  
  val ovxConn: OVXConnector = OVXConnector(InetAddress.getByName("192.168.1.40"))
  val dpids     = List("00:00:00:00:00:01:11:00", "00:00:00:00:00:01:12:00", "00:00:00:00:00:01:13:00")
  val testDpid  = dpids(0)
  val vpid      = "00:a4:23:05:00:01:11:00"
  val tenantId  = 1
  val ctrlUrls  = List("tcp:192.168.1.42:10000", "tcp:192.168.1.43:10000")
  val ctrlUrl   = List("tcp:192.168.1.42:10000")
  val netAddr   = "10.0.1.1"
  val mask: Short = 16


  // Test-Specs for Monitoring API-Calls:
  // ------------------------------------
  
  it should "get the correct json values from method \"getPhysicalTopology\""
  val physicalTopology = ovxConn.getPhysicalTopology
  println("physicalTopology = " + physicalTopology)
  
//  it should "get the correct json values from method \"getPhysicalSwitchPorts\""
//  val physicalSwitchPorts = ovxConn.getPhysicalSwitchPorts(firstDpid)
    
  it should "get the correct json values from method \"getPhysicalHosts\""
  val physicalHosts = ovxConn.getPhysicalHosts
  println("physicalHosts = " + physicalHosts)
  
  it should "get the correct json values from method \"getPhysicalFlowtable\""
  val physicalFlowtable = ovxConn.getPhysicalFlowtable(testDpid)

  it should "get the correct json values from method \"getPhysicalFlowtables\""
  val physicalFlowtables = ovxConn.getPhysicalFlowtables

  it should "get the correct json values from method \"getSubnet\""
  val subnet = ovxConn.getSubnet(tenantId)
  println("subnet = " + subnet)

  it should "get the correct json values from method \"listVirtualNetworks\""
  val tenantIds = ovxConn.listVirtualNetworks
  println("tenantIds = " + tenantIds)

  it should "get the correct json values from method \"getVirtualTopology\""
  val virtualTopology = ovxConn.getVirtualTopology(tenantId)
  println("virtualTopology = " + virtualTopology)

  it should "get the correct json values from method \"getVirtualSwitchMapping\""
  val virtualSwitchMapping = ovxConn.getVirtualSwitchMapping(tenantId)
  println("virtualSwitchMapping = " + virtualSwitchMapping)
  
  it should "get the correct json values from method \"getVirtualLinkMapping\""
  val virtualLinkMapping = ovxConn.getVirtualLinkMapping(tenantId)
  println("virtualLinkMapping = " + virtualLinkMapping)

  it should "get the correct json values from method \"getVirtualHosts\""
  val virtualHosts = ovxConn.getVirtualHosts(tenantId)
  println("virtualHosts = " + virtualHosts)

  it should "get the correct json values from method \"getVirtualFlowtable\""
  val virtualFlowtable = ovxConn.getVirtualFlowtable(tenantId, vpid)
  println("virtualFlowtable = " + virtualFlowtable)

  it should "get the correct json values from method \"getVirtualFlowtables\""
  val virtualFlowtables = ovxConn.getVirtualFlowtables(tenantId)
  println("virtualFlowtables = " + virtualFlowtables)
  
  it should "get the correct json values from method \"getVirtualAddressMapping\""
  val virtualAddrMapping = ovxConn.getVirtualAddressMapping(tenantId)
  println("virtualAddrMapping = " + virtualAddrMapping)
  
  it should "get the correct json values from method \"getVirtualSwitchPorts\""
  val virtualSwitchPorts = ovxConn.getVirtualSwitchPorts(tenantId, vpid)
  println("virtualSwitchPorts = " + virtualSwitchPorts)


  // Test-Specs for Tenant API-Calls:
  // ---------------------------------

  //TODO: see, why removeControllers(..) is not undoing the addControllers command...
  //TODO: add both methods to test again, if known.
//  it should "get the correct json values from Tenant-API call: \"addControllers\""
//  val addedCtrlUrls = ovxConn.addControllers(tenantId, vpid, ctrlUrls)
//  println("addedCtrlUrls = " + addedCtrlUrls)
//  
//  it should "get the correct json values from Tenant-API call: \"removeControllers\""
//  val removedCtrlUrls = ovxConn.removeControllers(tenantId, ctrlUrls)
//  println("removedCtrlUrls = " + removedCtrlUrls)
//  
  it should "get the correct json values from Tenant-API call: \"createNetwork\""
  val createdNetwork = ovxConn.createNetwork(ctrlUrl, netAddr, mask)
  println("createdNetwork = " + createdNetwork)
  
  it should "get the correct json values from Tenant-API call: \"createSwitch\""
  val createdSwitch = ovxConn.createSwitch(tenantId, List(testDpid))
  println("createdSwitch = " + createdSwitch)
  
//  it should "get the correct json values from Tenant-API call: \"createPort\""
//  val createdPort = ovxConn.createPort()
//
//  it should "get the correct json values from Tenant-API call: \"connectHost\""
//  val connectedHost = ovxConn.connectHost()
//
//  it should "get the correct json values from Tenant-API call: \"connectLink\""
//  val connectedLink = ovxConn.connectLink()
//
//  it should "get the correct json values from Tenant-API call: \"connectRoute\""
//  val connectedRoute = ovxConn.connectRoute()
//
//  it should "get the correct json values from Tenant-API call: \"setLinkPath\""
//  val linkPath = ovxConn.setLinkPath()
//
//  it should "get the correct json values from Tenant-API call: \"removeNetwork\""
//  ovxConn.removeNetwork()
//
//  it should "get the correct json values from Tenant-API call: \"removeSwitch\""
//  ovxConn.removeSwitch()
//
//  it should "get the correct json values from Tenant-API call: \"removePort\""
//  ovxConn.removePort()
//
//  it should "get the correct json values from Tenant-API call: \"disconnectHost\""
//  ovxConn.disconnectHost()
//
//  it should "get the correct json values from Tenant-API call: \"disconnectLink\""
//  ovxConn.disconnectLink()
//
//  it should "get the correct json values from Tenant-API call: \"disconnectRoute\""
//  ovxConn.disconnectRoute()
//
//  it should "get the correct json values from Tenant-API call: \"startNetwork\""
//  val startedNetwork = ovxConn.startNetwork()
//
//  it should "get the correct json values from Tenant-API call: \"startSwitch\""
//  val startedSwitch = ovxConn.startSwitch()
//
//  it should "get the correct json values from Tenant-API call: \"startPort\""
//  val startedPort = ovxConn.startPort()
//
//  it should "get the correct json values from Tenant-API call: \"stopNetwork\""
//  val stoppedNetwork = ovxConn.stopNetwork()
//
//  it should "get the correct json values from Tenant-API call: \"stopSwitch\""
//  val stoppedSwitch = ovxConn.stopSwitch()
//
//  it should "get the correct json values from Tenant-API call: \"stopPort\""
//  val stoppedPort = ovxConn.stopPort()
  
}
