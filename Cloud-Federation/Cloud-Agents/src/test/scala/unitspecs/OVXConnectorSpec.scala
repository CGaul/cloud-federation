package unitspecs

import java.net.InetAddress

import connectors.{VirtualSwitch, OVXConnector}
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
  
  it should "get the correct json values from method \"getPhysicalTopology\"" in {
    val physicalTopology = ovxConn.getPhysicalTopology
    println("physicalTopology = " + physicalTopology)
  }
  
//  it should "get the correct json values from method \"getPhysicalSwitchPorts\"" in {
//  val physicalSwitchPorts = ovxConn.getPhysicalSwitchPorts(firstDpid)
    
  it should "get the correct json values from method \"getPhysicalHosts\"" in {
    val physicalHosts = ovxConn.getPhysicalHosts
    println("physicalHosts = " + physicalHosts)
  }
  
  it should "get the correct json values from method \"getPhysicalFlowtable\"" in {
    val physicalFlowtable = ovxConn.getPhysicalFlowtable(dpids(0))
    println("physicalFlowtable = " + physicalFlowtable)
  }
    
  it should "get the correct json values from method \"getPhysicalFlowtables\"" in {
    val physicalFlowtables = ovxConn.getPhysicalFlowtables
    println("physicalFlowtables = " + physicalFlowtables)
  }

  it should "get the correct json values from method \"getSubnet\"" in {
    val subnet = ovxConn.getSubnet(tenantId)
    println("subnet = " + subnet)
  }

  it should "get the correct json values from method \"listVirtualNetworks\"" in {
    val tenantIds = ovxConn.listVirtualNetworks
    println("tenantIds = " + tenantIds)
  }

  it should "get the correct json values from method \"getVirtualTopology\"" in {
    val virtualTopology = ovxConn.getVirtualTopology(tenantId)
    println("virtualTopology = " + virtualTopology)
  }

  it should "get the correct json values from method \"getVirtualSwitchMapping\"" in {
    val virtualSwitchMapping = ovxConn.getVirtualSwitchMapping(tenantId)
    println("virtualSwitchMapping = " + virtualSwitchMapping)
  }
  
  it should "get the correct json values from method \"getVirtualLinkMapping\"" in {
    val virtualLinkMapping = ovxConn.getVirtualLinkMapping(tenantId)
    println("virtualLinkMapping = " + virtualLinkMapping)
  }

  it should "get the correct json values from method \"getVirtualHosts\"" in {
    val virtualHosts = ovxConn.getVirtualHosts(tenantId)
    println("virtualHosts = " + virtualHosts)
  }

  it should "get the correct json values from method \"getVirtualFlowtable\"" in {
    val virtualFlowtable = ovxConn.getVirtualFlowtable(tenantId, vdpids(0))
    println("virtualFlowtable = " + virtualFlowtable)
  }

  it should "get the correct json values from method \"getVirtualFlowtables\"" in {
    val virtualFlowtables = ovxConn.getVirtualFlowtables(tenantId)
    println("virtualFlowtables = " + virtualFlowtables)
  }
  
  it should "get the correct json values from method \"getVirtualAddressMapping\"" in {
    val virtualAddrMapping = ovxConn.getVirtualAddressMapping(tenantId)
    println("virtualAddrMapping = " + virtualAddrMapping)
  }
  
//  it should "get the correct json values from method \"getVirtualSwitchPorts\"" in {
//  val virtualSwitchPorts = ovxConn.getVirtualSwitchPorts(tenantId, vdpids(0))
//  println("virtualSwitchPorts = " + virtualSwitchPorts)


  // Test-Specs for Tenant API-Calls:
  // ---------------------------------

  //TODO: see, why removeControllers(..) is not undoing the addControllers command...
  //TODO: add both methods to test again, if known.
//  it should "get the correct json values from Tenant-API call: \"addControllers\"" in {
//  val addedCtrlUrls = ovxConn.addControllers(tenantId, vdpids(0), ctrlUrls)
//  println("addedCtrlUrls = " + addedCtrlUrls)
//  
//  it should "get the correct json values from Tenant-API call: \"removeControllers\"" in {
//  val removedCtrlUrls = ovxConn.removeControllers(tenantId, ctrlUrls)
//  println("removedCtrlUrls = " + removedCtrlUrls)
//  
  it should "get the correct json values from Tenant-API call: \"createNetwork\"" in {
    val createdNetwork = ovxConn.createNetwork(ctrlUrl, netAddr, mask)
    println("createdNetwork = " + createdNetwork)
  }
  
  var createdSwitch1, createdSwitch2: Option[VirtualSwitch] = None
  it should "get the correct json values from Tenant-API call: \"createSwitch\"" in {
    val createdSwitch1 = ovxConn.createSwitch(tenantId, List(dpids(0)))
    println("createdSwitch1 = " + createdSwitch1)
    val createdSwitch2 = ovxConn.createSwitch(tenantId, List(dpids(1)))
    println("createdSwitch2 = " + createdSwitch2)
  }

  var switch1Port1, switch1Port2, switch1Port3, switch1Port4: Option[(Short, Short)] = None
  var switch2Port1, switch2Port2, switch2Port3, switch2Port4: Option[(Short, Short)] = None
  it should "get the correct json values from Tenant-API call: \"createPort\"" in {
    //Switch-1 Ports:
    switch1Port1 = ovxConn.createPort(tenantId, dpids(0), 1)
    println("switch1Port1 = " + switch1Port1)
    switch1Port2 = ovxConn.createPort(tenantId, dpids(0), 2)
    println("switch1Port2 = " + switch1Port2)
    switch1Port3 = ovxConn.createPort(tenantId, dpids(0), 3)
    println("switch1Port3 = " + switch1Port3)
    switch1Port4 = ovxConn.createPort(tenantId, dpids(0), 4)
    println("switch1Port4 = " + switch1Port4)


    //Switch-2 Ports:
    switch2Port1 = ovxConn.createPort(tenantId, dpids(1), 1)
    println("switch2Port1 = " + switch2Port1)
    switch2Port2 = ovxConn.createPort(tenantId, dpids(1), 2)
    println("switch2Port2 = " + switch2Port2)
    switch2Port3 = ovxConn.createPort(tenantId, dpids(1), 3)
    println("switch2Port3 = " + switch2Port3)
  }
  
  it should "get the correct json values from Tenant-API call: \"connectLink\"" in {
    // Connect SWITCH-1 with SWITCH-2
    if (createdSwitch1.isDefined && createdSwitch2.isDefined &&
      switch1Port4.isDefined && switch2Port2.isDefined) {
      val connectedLink = ovxConn.connectLink(tenantId,
        createdSwitch1.get.vdpid, switch1Port4.get._2,
        createdSwitch2.get.vdpid, switch2Port2.get._2, "spf", 1)
      println("connectedLink = " + connectedLink)
    }
  }

  it should "get the correct json values from Tenant-API call: \"connectHost\"" in {
    // Host 00:00:00:00:01:11 on SWITCH-1:
    if (switch1Port1.isDefined && switch1Port2.isDefined && switch2Port1.isDefined) {
      val connectedHost1 = ovxConn.connectHost(tenantId, vdpids(0), switch1Port1.get._2, hostMACs(0))
      println("connectedHost1 = " + connectedHost1)
      // Host 00:00:00:00:01:12 on SWITCH-1:
      val connectedHost2 = ovxConn.connectHost(tenantId, vdpids(0), switch1Port2.get._2, hostMACs(1))
      println("connectedHost2 = " + connectedHost2)

      // Host 00:00:00:00:01:13 on SWITCH-2:
      val connectedHost3 = ovxConn.connectHost(tenantId, vdpids(1), switch2Port1.get._2, hostMACs(2))
      println("connectedHost3 = " + connectedHost3)
    }
  }
//  it should "get the correct json values from Tenant-API call: \"connectRoute\"" in {
//  val connectedRoute = ovxConn.connectRoute()
//
//  it should "get the correct json values from Tenant-API call: \"setLinkPath\"" in {
//  val linkPath = ovxConn.setLinkPath()
//
  it should "get the correct json values from Tenant-API call: \"removeNetwork\"" in {
    ovxConn.removeNetwork(tenantId)
  }
//
//  it should "get the correct json values from Tenant-API call: \"removeSwitch\"" in {
//  ovxConn.removeSwitch()
//
//  it should "get the correct json values from Tenant-API call: \"removePort\"" in {
//  ovxConn.removePort()
//
//  it should "get the correct json values from Tenant-API call: \"disconnectHost\"" in {
//  ovxConn.disconnectHost()
//
//  it should "get the correct json values from Tenant-API call: \"disconnectLink\"" in {
//  ovxConn.disconnectLink()
//
//  it should "get the correct json values from Tenant-API call: \"disconnectRoute\"" in {
//  ovxConn.disconnectRoute()
//
  it should "get the correct json values from Tenant-API call: \"startNetwork\"" in {
    val startedNetwork = ovxConn.startNetwork(tenantId)
  }
  
//  it should "get the correct json values from Tenant-API call: \"startSwitch\"" in {
//  val startedSwitch = ovxConn.startSwitch(tenantId, vdpids(0))
  
//
//  it should "get the correct json values from Tenant-API call: \"startPort\"" in {
//  val startedPort = ovxConn.startPort()
//
//  it should "get the correct json values from Tenant-API call: \"stopNetwork\"" in {
//  val stoppedNetwork = ovxConn.stopNetwork()
//
//  it should "get the correct json values from Tenant-API call: \"stopSwitch\"" in {
//  val stoppedSwitch = ovxConn.stopSwitch()
//
//  it should "get the correct json values from Tenant-API call: \"stopPort\"" in {
//  val stoppedPort = ovxConn.stopPort()
  
}
