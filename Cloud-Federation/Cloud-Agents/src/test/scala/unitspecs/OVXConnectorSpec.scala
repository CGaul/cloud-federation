package unitspecs

import java.net.InetAddress

import connectors.OVXConnector
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

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
  val vdpids    = List("00:a4:23:05:00:01:11:00", "00:a4:23:05:00:01:12:00", "00:a4:23:05:00:01:13:00")
  val hostMACs  = List("00:00:00:00:01:11", "00:00:00:00:01:12", "00:00:00:00:01:13", "00:00:00:00:01:14")

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
  val physicalFlowtable = ovxConn.getPhysicalFlowtable(dpids(0))

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
  val virtualFlowtable = ovxConn.getVirtualFlowtable(tenantId, vdpids(0))
  println("virtualFlowtable = " + virtualFlowtable)

  it should "get the correct json values from method \"getVirtualFlowtables\""
  val virtualFlowtables = ovxConn.getVirtualFlowtables(tenantId)
  println("virtualFlowtables = " + virtualFlowtables)
  
  it should "get the correct json values from method \"getVirtualAddressMapping\""
  val virtualAddrMapping = ovxConn.getVirtualAddressMapping(tenantId)
  println("virtualAddrMapping = " + virtualAddrMapping)
  
//  it should "get the correct json values from method \"getVirtualSwitchPorts\""
//  val virtualSwitchPorts = ovxConn.getVirtualSwitchPorts(tenantId, vdpids(0))
//  println("virtualSwitchPorts = " + virtualSwitchPorts)


  // Test-Specs for Tenant API-Calls:
  // ---------------------------------

  //TODO: see, why removeControllers(..) is not undoing the addControllers command...
  //TODO: add both methods to test again, if known.
//  it should "get the correct json values from Tenant-API call: \"addControllers\""
//  val addedCtrlUrls = ovxConn.addControllers(tenantId, vdpids(0), ctrlUrls)
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
  val createdSwitch1 = ovxConn.createSwitch(tenantId, List(dpids(0)))
  println("createdSwitch1 = " + createdSwitch1)
  val createdSwitch2 = ovxConn.createSwitch(tenantId, List(dpids(1)))
  println("createdSwitch2 = " + createdSwitch2)
  
  it should "get the correct json values from Tenant-API call: \"createPort\""
  //Switch-1 Ports:
  val switch1Port1 = ovxConn.createPort(tenantId, dpids(0), 1)
  println("switch1Port1 = " + switch1Port1)
  val switch1Port2 = ovxConn.createPort(tenantId, dpids(0), 2)
  println("switch1Port2 = " + switch1Port2)
  val switch1Port3 = ovxConn.createPort(tenantId, dpids(0), 3)
  println("switch1Port3 = " + switch1Port2)
  val switch1Port4 = ovxConn.createPort(tenantId, dpids(0), 4)
  println("switch1Port4 = " + switch1Port2)
  
  //Switch-2 Ports:
  val switch2Port1 = ovxConn.createPort(tenantId, dpids(1), 1)
  println("switch2Port1 = " + switch2Port1)
  val switch2Port2 = ovxConn.createPort(tenantId, dpids(1), 2)
  println("switch2Port2 = " + switch2Port2)
  val switch2Port3 = ovxConn.createPort(tenantId, dpids(1), 3)
  println("switch2Port3 = " + switch2Port2)
  
  it should "get the correct json values from Tenant-API call: \"connectLink\""
  // Connect SWITCH-1 with SWITCH-2
  if(createdSwitch1.isDefined && createdSwitch2.isDefined && 
    switch1Port4.isDefined && switch2Port2.isDefined) {
    val connectedLink = ovxConn.connectLink(tenantId,
      createdSwitch1.get.vdpid, switch1Port4.get._2,
      createdSwitch2.get.vdpid, switch2Port2.get._2, "spf", 1)
    println("connectedLink = " + connectedLink)
  }

  it should "get the correct json values from Tenant-API call: \"connectHost\""
  // Host 00:00:00:00:01:11 on SWITCH-1:
  val connectedHost1 = ovxConn.connectHost(tenantId, vdpids(0), switch1Port1.get._2, hostMACs(0))
  println("connectedHost1 = " + connectedHost1)
  // Host 00:00:00:00:01:12 on SWITCH-1:
  val connectedHost2 = ovxConn.connectHost(tenantId, vdpids(0), switch1Port2.get._2, hostMACs(1))
  println("connectedHost2 = " + connectedHost2)
  
  // Host 00:00:00:00:01:13 on SWITCH-2:
  val connectedHost3 = ovxConn.connectHost(tenantId, vdpids(1), switch2Port1.get._2, hostMACs(2))
  println("connectedHost3 = " + connectedHost3)
  
//  it should "get the correct json values from Tenant-API call: \"connectRoute\""
//  val connectedRoute = ovxConn.connectRoute()
//
//  it should "get the correct json values from Tenant-API call: \"setLinkPath\""
//  val linkPath = ovxConn.setLinkPath()
//
  it should "get the correct json values from Tenant-API call: \"removeNetwork\""
  ovxConn.removeNetwork(tenantId)
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
  it should "get the correct json values from Tenant-API call: \"startNetwork\""
  val startedNetwork = ovxConn.startNetwork(tenantId)
  
//  it should "get the correct json values from Tenant-API call: \"startSwitch\""
//  val startedSwitch = ovxConn.startSwitch(tenantId, vdpids(0))
  
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
