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


  // Test-Specs for Monitoring API-Calls:
  // ------------------------------------
  
  it should "get the correct json values from method \"getPhysicalTopology\""
  val physicalTopo = ovxConn.getPhysicalTopology
  System.out.println(physicalTopo)
  
  it should "get the correct json values from method \"getPhysicalSwitchPorts\""
  val physicalSwitchPorts = ovxConn.getPhysicalSwitchPorts()
    
  it should "get the correct json values from method \"getPhysicalHosts\""
  val physicalHosts = ovxConn.getPhysicalHosts
  
  it should "get the correct json values from method \"getPhysicalFlowtable\""
  val physicalFlowtable = ovxConn.getPhysicalFlowtable()
  
  it should "get the correct json values from method \"getSubnet\""
  val subnet = ovxConn.getSubnet()
    
  it should "get the correct json values from method \"listVirtualNetworks\""
  val tenantIds = ovxConn.listVirtualNetworks
  System.out.println(tenantIds.mkString(", "))
  
  it should "get the correct json values from method \"getVirtualTopology\""
  val virtualTopology = ovxConn.getVirtualTopology()
  
  it should "get the correct json values from method \"getVirtualSwitchMapping\""
  val virtualSwitchMapping = ovxConn.getVirtualSwitchMapping()
  
  it should "get the correct json values from method \"getVirtualLinkMapping\""
  val virtualLinkMapping = ovxConn.getVirtualLinkMapping()
  
  it should "get the correct json values from method \"getVirtualHosts\""
  val virtualHosts = ovxConn.getVirtualHosts()
    
  it should "get the correct json values from method \"getVirtualFlowtable\""
  val virtualFlowtable = ovxConn.getVirtualFlowtable()
    
  it should "get the correct json values from method \"getVirtualAddressMapping\""
  val virtualAddrMapping = ovxConn.getVirtualAddressMapping()
    
  it should "get the correct json values from method \"getVirtualSwitchPorts\""
  val virtualSwitchPorts = ovxConn.getVirtualSwitchPorts()


  // Test-Specs for Tenant API-Calls:
  // ---------------------------------
  
  it should "get the correct json values from Tenant-API call: \"addControllers\""
  val addedCtrlUrls = ovxConn.addControllers()
  
  it should "get the correct json values from Tenant-API call: \"removeControllers\""
  val removedCtrlUrls = ovxConn.removeControllers()
  
  it should "get the correct json values from Tenant-API call: \"createNetwork\""
  val createdNetwork = ovxConn.createNetwork()
  
  it should "get the correct json values from Tenant-API call: \"createSwitch\""
  val createdSwitch = ovxConn.createSwitch()
  
  it should "get the correct json values from Tenant-API call: \"createPort\""
  val createdPort = ovxConn.createPort()
  
  it should "get the correct json values from Tenant-API call: \"connectHost\""
  val connectedHost = ovxConn.connectHost()
  
  it should "get the correct json values from Tenant-API call: \"connectLink\""
  val connectedLink = ovxConn.connectLink()
  
  it should "get the correct json values from Tenant-API call: \"connectRoute\""
  val connectedRoute = ovxConn.connectRoute()
  
  it should "get the correct json values from Tenant-API call: \"setLinkPath\""
  val linkPath = ovxConn.setLinkPath()
  
  it should "get the correct json values from Tenant-API call: \"removeNetwork\""
  ovxConn.removeNetwork()
  
  it should "get the correct json values from Tenant-API call: \"removeSwitch\""
  ovxConn.removeSwitch()
  
  it should "get the correct json values from Tenant-API call: \"removePort\""
  ovxConn.removePort()
  
  it should "get the correct json values from Tenant-API call: \"disconnectHost\""
  ovxConn.disconnectHost()
  
  it should "get the correct json values from Tenant-API call: \"disconnectLink\""
  ovxConn.disconnectLink()
  
  it should "get the correct json values from Tenant-API call: \"disconnectRoute\""
  ovxConn.disconnectRoute()
  
  it should "get the correct json values from Tenant-API call: \"startNetwork\""
  val startedNetwork = ovxConn.startNetwork()
  
  it should "get the correct json values from Tenant-API call: \"startSwitch\""
  val startedSwitch = ovxConn.startSwitch()
  
  it should "get the correct json values from Tenant-API call: \"startPort\""
  val startedPort = ovxConn.startPort()
  
  it should "get the correct json values from Tenant-API call: \"stopNetwork\""
  val stoppedNetwork = ovxConn.stopNetwork()
  
  it should "get the correct json values from Tenant-API call: \"stopSwitch\""
  val stoppedSwitch = ovxConn.stopSwitch()
  
  it should "get the correct json values from Tenant-API call: \"stopPort\""
  val stoppedPort = ovxConn.stopPort()
  
}
