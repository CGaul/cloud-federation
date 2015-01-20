package connectors

import java.io.IOException
import java.net.{InetAddress, URI}

import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.collection.Set

/**
 * @author Constantin Gaul, created on 1/14/15.
 */
class OVXConnector(ovxApiAddr: InetAddress, ovxApiPort: Int, 
                   userName: String, userPW: String)
{

/* Values: */
/* ======= */
  
  val log = LoggerFactory.getLogger(classOf[OVXConnector])

  
/* Public Methods: */
/* =============== */
  
  // Monitoring API-Calls:
  // ---------------------

  /**
   * Get the current PhysicalNetwork topology
   * @return
   */
  def getPhysicalTopology: (List[String], List[Link]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalTopology", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        val switches = (result \ "switches").as[List[String]]
        val links = (result \ "links").as[List[Link]]
        return (switches, links)
        
      case None =>
        return (List(), List())
    }
  }
  
  // TODO: Add or delete. Method is not yet implemented in OVX:
//  /**
//   * Get all of the ports on a PhysicalSwitch
//   * @param dpid DPID of a physical network switch
//   * @return
//   */
//  def getPhysicalSwitchPorts(dpid: String): Option[Short] = {
//    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalSwitchPorts", 
//      Map(
//        "dpid"      -> OVXConnector.convertStringDpid(dpid)
//      ))
//    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
//    jsonReply match{
//      case Some(result) =>
//        val port: Option[Short] = (result \ "port").asOpt[Short]
  
//      case None =>
//        return port
//    }
//    else
//      return None
//  }
  /**
   * Get all hosts from all virtual networks. Hosts are described by physical network addresses.
   * @return A list of Tuple4, where each entry in the list is a host's virtual representation
   */
  def getPhysicalHosts: List[PhysicalHost] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalHosts", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    
    jsonReply match{
      case Some(result) =>
        val hostList = result.as[List[PhysicalHost]]
        return hostList
        
      case None =>
        return List()
    }
  }
  /**
   * Get the flow table of a PhysicalSwitch
   * @param dpid DPID of a physical network switch
   * @return  
   */
  def getPhysicalFlowtable(dpid: String): List[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalFlowtable",
      Map(
        "dpid"        -> OVXConnector.convertString2HexDpid(dpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        // TODO: implement
        return List()
        
      case None =>
        return List()
    }
  }
  /**
   * Get all flow tables of each PhysicalSwitch
   * @return
   */
  def getPhysicalFlowtables: List[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalFlowtable", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        // TODO: implement
        return List()
        
      case None =>
        return List()
    }
  }
  /**
   * Get the Address/Mask of a virtual network
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getSubnet(tenantId: Int): Option[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getSubnet", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        val subnet = result.toString()
        return Option(subnet)
        
      case None =>
        return None
    }
  }
  /**
   * List all of the virtual networks
   * @return
   */
  def listVirtualNetworks: List[Int] = {
    val jsonRequest: JsValue = this.buildJsonQuery("listVirtualNetworks", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        val tenantIds = result.as[List[Int]]
        return tenantIds
        
      case None =>
        return List()
    }
  }
  /**
   * Get the topology of a virtual network
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualTopology(tenantId: Int): (List[String], List[Link]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualTopology", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        val switches = (result \ "switches").as[List[String]]
        val links = (result \ "links").as[List[Link]]
        return (switches, links)
        
      case None =>
        return (List(), List())
    }
  }
  /**
   * Get the PhysicalSwitch(es) mapped to an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualSwitchMapping(tenantId: Int): Map[String, (List[Link], List[String])] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        var vSwitchMapping: Map[String, (List[Link], List[String])] = Map()

        val vSwitchDpids: Set[String] = result.asInstanceOf[JsObject].keys
        for (actSwitchDpid <- vSwitchDpids) {
          // TODO: test if links are correctly returned (when are links not empty in OVX here?)
          val links = (result \ actSwitchDpid \ "links").as[List[Link]]
          val pSwitches = (result \ actSwitchDpid \ "switches").as[List[String]]
          vSwitchMapping = vSwitchMapping + (actSwitchDpid -> (links,  pSwitches))
        }
        return vSwitchMapping
        
      case None =>
        return Map()
    }
  }
  /**
   * Get the PhysicalLink(s) mapped to a virtual link
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualLinkMapping(tenantId: Int): Map[Int, List[Int]] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualLinkMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        val vLinkIds: Set[String] = result.asInstanceOf[JsObject].keys
        var vLinkMapping: Map[Int, List[Int]] = Map()

        for (actLinkId <- vLinkIds) {
          val pLinks = (result \ actLinkId).asInstanceOf[JsArray].value.flatMap(_.as[List[Int]]).toList
          vLinkMapping = vLinkMapping + (actLinkId.toInt -> pLinks)
        }
        return vLinkMapping
        
      case None =>
        return Map()

    }
  }
  /**
   * Get all physical hosts on a virtual network. Hosts are described by virtual network addresses.
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualHosts(tenantId: Int): List[PhysicalHost] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualHosts", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        return result.as[List[PhysicalHost]]
        
      case None =>
        return List()
    }
  }
  /**
   * Get the flow table of a OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of the virtual switch
   * @return
   */
  def getVirtualFlowtable(tenantId: Int, vdpid: String): List[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualFlowtable",
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        return result.as[List[String]]
        
      case None =>
        return List()
    }
  }
  /**
   * Get all flow tables of each OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualFlowtables(tenantId: Int): Map[String, List[String]] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualFlowtable",
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        val vSwitchIds: Set[String] = result.asInstanceOf[JsObject].keys
        var flowtableMap: Map[String, List[String]] = Map() //TODO: check if FlowMod is a String repr
        for (actSwitchId <- vSwitchIds) {
          val flowmod =  (result \ actSwitchId).as[List[String]]
          flowtableMap = flowtableMap + (actSwitchId -> flowmod)
        }

        return flowtableMap

      case None =>
        return Map()
    }
  }
  /**
   * Get the mappings between all available physical and virtual network addresses
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualAddressMapping(tenantId: Int): Map[String, String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualAddressMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    jsonReply match{
      case Some(result) =>
        return result.as[Map[String, String]]
        
      case None => 
        return Map()
    }
  }

  // TODO: Add or delete. Method is not yet implemented in OVX:
//  /**
//   * Get all of the ports on an OVXSwitch
//   * @param tenantId The GUID of a virtual network
//   * @return
//   */
//  def getVirtualSwitchPorts(tenantId: Int, vdpid: String): List[Short] = {
//    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchPorts",
//      Map(
//        "tenantId"    -> Json.toJson(tenantId),
//        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid)
//      ))
//    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
//    jsonReply match{
//      case Some(result) =>
//        return result.as[List[Short]] //TODO: check if portlist is a List[Short] repr
//
//      case None =>
//        return List()
//    }
//  }



  // Tenant API-Calls:
  // -----------------
  
  /**
   * Add a new controller to connect a virtual switch (OVXSwitch) to
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param controllerUrls List of String="proto:host:port" where proto is usually "tcp"
   */
  def addControllers(tenantId: Int, vdpid: String, controllerUrls: List[String]): List[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("addControllers", 
      Map(
        "tenantId"        -> Json.toJson(tenantId),
        "vdpid"           -> OVXConnector.convertString2HexDpid(vdpid),
        "controllerUrls"  -> Json.toJson(controllerUrls)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match{
      case Some(result) =>
        return result.as[List[String]]
        
      case None =>
        return List()
    }
  }
  /**
   * Remove a controller from the list of controllers a OVXSwitch can connect to
   * @param tenantId The GUID of a virtual network
   * @param controllerUrls List of String="proto:host:port" where proto is usually "tcp"
   * @return
   */
  def removeControllers(tenantId: Int, controllerUrls: List[String]): List[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeControllers", 
      Map(
        "tenantId"        -> Json.toJson(tenantId),
        "controllerUrls"  -> Json.toJson(controllerUrls)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
        //TODO: test if works.
      case Some(result) =>
        return result.as[List[String]]
        
      case None =>
        return List()
    }
  }

  /**
   * Create a new virtual network (OVXNetwork)
   * @param controllerUrls List of String="proto:host:port" where proto is usually "tcp"
   * @param networkAddress The IP address block used in a network
   * @param mask The CIDR value (1-30) of the network mask used with NETADD
   * @return
   */
  def createNetwork(controllerUrls: List[String], networkAddress: String, mask: Short): Option[Network] = {
    val jsonRequest: JsValue = this.buildJsonQuery("createNetwork", 
      Map(
        "controllerUrls"  -> Json.toJson(controllerUrls),
        "networkAddress"  -> Json.toJson(networkAddress),
        "mask"            -> Json.toJson(mask)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        return result.asOpt[Network]

      case None =>
        return None
    }
  }
  /**
   * Create a new OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param dpids One or more DPIDs of switches mapped to an OVXBigSwitch
   * @param dpid DPID of a physical network switch
   * @return
   */
  def createSwitch(tenantId: Int, dpids: List[String], dpid: String = ""): Option[VirtualSwitch] = {
    val optDpid: Map[String, JsNumber] = if(dpid == "") Map() else 
      Map(
        "dpid"        -> OVXConnector.convertString2HexDpid(dpid)
      )
    val jsonRequest: JsValue = this.buildJsonQuery("createSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "dpids"       -> JsArray(dpids.map(OVXConnector.convertString2HexDpid))
      ) ++ optDpid)
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        return result.asOpt[VirtualSwitch]
        
      case None =>
        return None
    }
  }
  /**
   * Add a new port to an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param dpid DPID of a physical network switch
   * @param port The port number of a switch port
   * @return A Tuple of Short, representing the physical Port -> virtual Port mapping.
   */
  def createPort(tenantId: Int, dpid: String, port: Short): Option[(Short, Short)] = {
    val jsonRequest: JsValue = this.buildJsonQuery("createPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "dpid"        -> OVXConnector.convertString2HexDpid(dpid),
        "port"        -> Json.toJson(port)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        val pPort = (result \ "port").as[Short]
        val vPort = (result \ "vport").as[Short]
        return Option(pPort, vPort)
        
      case None =>
        return None
    }
  }

  /**
   * Connect a host to an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   * @param mac Host MAC address in colon-hex: "xx:xx:xx:xx:xx:xx"
   * @return
   */
  def connectHost(tenantId: Int, vdpid: String, vport: Short, mac: String): Option[VirtualHost] = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectHost", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid),
        "vport"       -> Json.toJson(vport),
        "mac"         -> Json.toJson(mac)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        return result.asOpt[VirtualHost]
        
      case None =>
        return None
    }
  }
  /**
   * Add a virtual link to an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param srcVDpid The virtual colon-hex DPID of the source switch
   * @param srcVPort The virtual port on the source switch
   * @param dstVDpid The virtual  DPID of the destination switch
   * @param dstVPort The virtual  port on the destination switch
   * @param algorithm Method for setting routes in a OVXBigSwitch: "manual" or "spf"
   * @param backup_num Number of backup paths associated with a virtual link or route
   * @return
   */
  def connectLink(tenantId: Int, srcVDpid: Long, srcVPort: Short,
                  dstVDpid: Long, dstVPort: Short,
                  algorithm: String, backup_num: Byte): Option[VirtualLink] = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectLink", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "srcDpid"     -> Json.toJson(srcVDpid),
        "srcPort"     -> Json.toJson(srcVPort),
        "dstDpid"     -> Json.toJson(dstVDpid),
        "dstPort"     -> Json.toJson(dstVPort),
        "algorithm"   -> Json.toJson(algorithm),
        "backup_num"  -> Json.toJson(backup_num.toInt)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        return result.asOpt[VirtualLink]
        
      case None =>
        return None
    }
  }
  /**
   * Create a new route within a OVXBigSwitch
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param srcPort The port on the source switch
   * @param dstPort The port on the destination switch
   * @param path The physical path taken by a virtual link or route
   * @return
   */
  def connectRoute(tenantId: Int, vdpid: String, srcPort: Short, dstPort: Short, path: Path) = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectRoute",
      Map(
        "tenantId" -> Json.toJson(tenantId),
        "vdpid" -> OVXConnector.convertString2HexDpid(vdpid),
        "srcPort" -> Json.toJson(srcPort),
        "dstPort" -> Json.toJson(dstPort),
        "path" -> Json.toJson(path)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>

      case None =>
    }
  }

  /**
   * Add a physical network path to a virtual link (OVXLink)
   * @param tenantId The GUID of a virtual network
   * @param linkId OVX-generated UUID of a virtual link
   * @param path The physical path taken by a virtual link or route
   * @param priority
   * @return
   */
  def setLinkPath(tenantId: Int, linkId: Int, path: Path, priority: Byte) = {
    val jsonRequest: JsValue = this.buildJsonQuery("setLinkPath", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "linkId"      -> Json.toJson(linkId),
        "path"        -> Json.toJson(path),
        "priority"    -> Json.toJson(priority.toInt)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>

      case None =>
    }
  }

  /**
   * Remove an OVXNetwork
   * @param tenantId The GUID of a virtual network
   */
  def removeNetwork(tenantId: Int): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }
  /**
   * Remove an OVXSwitch from an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   */
  def removeSwitch(tenantId: Int, vdpid: String): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }
  /**
   * Remove a port from an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   */
  def removePort(tenantId: Int, vdpid: String, vport: Short): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removePort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid),
        "vport"       -> Json.toJson(vport)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }

  /**
   * Detach (remove) a host from a OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param hostId UUID of a host
   */
  def disconnectHost(tenantId: Int, hostId: Int): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectHost", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "hostId"      -> Json.toJson(hostId)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }
  /**
   * Detach (remove) a virtual link from an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param linkId OVX-generated UUID of a virtual link
   */
  def disconnectLink(tenantId: Int, linkId: Int): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectLink", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "linkId"      -> Json.toJson(linkId)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }
  /**
   * Detach (remove) a route from an OVXBigSwitch
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param routeId OVX-generated UUID of a route
   */
  def disconnectRoute(tenantId: Int, vdpid: String, routeId: Int): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectRoute", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid),
        "routeId"     -> Json.toJson(routeId)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }

  /**
   * Initialize (boot) an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def startNetwork(tenantId: Int): Option[Network] = {
    val jsonRequest: JsValue = this.buildJsonQuery("startNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        return result.asOpt[Network]
      case None =>
        return None
    }
  }
  /**
   * Intialize (boot) an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @return
   */
  def startSwitch(tenantId: Int, vdpid: String): Option[VirtualSwitch] = {
    val jsonRequest: JsValue = this.buildJsonQuery("startSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>
        return result.asOpt[VirtualSwitch]
      case None =>
        return None
    }
  }
  /**
   * Enable a port on an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   * @return
   */
  def startPort(tenantId: Int, vdpid: String, vport: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid),
        "vport"       -> Json.toJson(vport)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>

      case None =>
    }
  }

  /**
   * Disable an OVXNetwork temporarily
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def stopNetwork(tenantId: Int) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>

      case None =>
    }
  }
  /**
   * Disable an OVXSwitch temporarily
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @return
   */
  def stopSwitch(tenantId: Int, vdpid: String) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>

      case None =>
    }
  }
  /**
   * Disable a port on an OVXSwitch temporarily
   * @param tenantId The GUID of a virtual network
   * @param vdpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   * @return
   */
  def stopPort(tenantId: Int, vdpid: String, vport: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vdpid"       -> OVXConnector.convertString2HexDpid(vdpid),
        "vport"       -> Json.toJson(vport)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    jsonReply match {
      case Some(result) =>

      case None =>
    }
  }


/* Private Methods: */
/* ================ */
  
  private def buildJsonQuery(method: String, params: Map[String, JsValue]): JsValue = {
    val jsonQuery: JsValue = Json.toJson(
      Map(
        "id"          -> Json.toJson("NRA"),
        "jsonrpc"     -> Json.toJson("2.0"),
        "method"      -> Json.toJson(Json.toJson(method)),
        "params"      -> Json.toJson(params)
      )
    )
    return jsonQuery
  }

  private def sendJsonQuery(jsonQuery: JsValue, apiServiceName: String): Option[JsValue] = {
    //Use the Apache HTTP-Client to send a JSON message via HTTP Post to the OVX-Jetty API Server:
    val httpclient: CloseableHttpClient = HttpClients.createDefault()

    val ovxURI: URI = new URIBuilder()
      .setHost(ovxApiAddr.getHostAddress)
      .setPort(ovxApiPort)
      .setScheme("http")
      .setUserInfo(userName, userPW)
      .setPath(s"/$apiServiceName")
      .build()
    val httpPost: HttpPost = new HttpPost(ovxURI)
    httpPost.setEntity(new StringEntity(Json.stringify(jsonQuery), "UTF-8"))
    httpPost.addHeader("content-type", "application/json")

    try {
      val response: CloseableHttpResponse = httpclient.execute(httpPost)
      val entity: HttpEntity = response.getEntity
      val jsonResponse: JsValue = Json.parse(EntityUtils.toString(response.getEntity))
      EntityUtils.consume(entity)
      response.close()
      
      // Check, whether the response belongs to the request (via id):
      val requestId = (jsonQuery \ "id").as[String]
      val responseId = (jsonResponse \ "id").as[String]
      if(requestId == responseId){
        val responseResult = jsonResponse \ "result"
        responseResult match {
          case result: JsUndefined =>
            log.warn("OVX-API returned error: {}", jsonResponse)
            return None //TODO: throw something here.
          case _ =>
            return Some(responseResult)
        }
      }
      return None
    }
    catch{
      case e: IOException => 
        log.error("IOException occured, during connection to OVX-API Server. \nError-Message: {}",
                  e.getMessage)
        return None
    }
  }


  
  /* Implicit Conversions for Containers: */
  /* ==================================== */
  
  implicit val pathWrites = new Writes[Path] {
    override def writes(path: Path): JsValue = {
      Json.parse(s"[${path.route.map(link => link.toString).mkString(",")}}]")
    }
  }

  implicit val connectionWrites = new Writes[Connection] {
    override def writes(link: Connection): JsValue = {
      Json.parse(link.toString)
    }
  }

  implicit val pHostReads: Reads[PhysicalHost] = Json.reads[PhysicalHost]
  implicit val vHostReads: Reads[VirtualHost] = Json.reads[VirtualHost]

  implicit val endpointReads = Json.reads[Endpoint]
  implicit val linkReads: Reads[Link] = Json.reads[Link]
  implicit val vLinkReads: Reads[VirtualLink] = Json.reads[VirtualLink]

  implicit val networkReads: Reads[Network] = Json.reads[Network]
  implicit val vSwitchReads: Reads[VirtualSwitch] = Json.reads[VirtualSwitch]
}

/**
 * Companion Object of the connectors.OVXConnector,
 * in order to implement some static behaviours and the apply method.
 */
object OVXConnector
{
  //Constructors, with default OVX - Username/PW:
  //---------------------------------------------

  def apply(ovxApiAddr: InetAddress = InetAddress.getLocalHost, ovxApiPort: Int = 8080) =
    new OVXConnector(ovxApiAddr, ovxApiPort, "admin", "")


  //Constructors, with specific OVX - Username/PW:
  //----------------------------------------------

  def apply(ovxApiAddr: InetAddress, ovxApiPort: Int,
            userName: String, userPW: String) =
    new OVXConnector(ovxApiAddr, ovxApiPort, userName, userPW)



  // Static Methods:
  //----------------

  def convertString2HexDpid(string: String): JsNumber = JsNumber(java.lang.Long.parseLong(string.replace(":",""), 16))

}



/* Container Classes for OVXConnector: */
/* =================================== */

case class Path(route: List[Connection])

case class Connection(srcDpid: Long, srcPort: Short,
                      dstDpid: Long, dstPort: Short){

  override def toString = {
    s"$srcDpid/$srcPort-$dstDpid/$dstPort"
  }

}

case class PhysicalHost(hostId: Int, dpid: String, port: Short, mac: String, ipAddress: Option[String])
//TODO: mac address should be a String by definition in http://ovx.onlab.us/documentation/api/
case class VirtualHost(tenantId: Int, hostId: Int, vdpid: Long, vport: Short, mac: Int, ipAddress: Option[String])

case class Endpoint(dpid: String, port: Short)
case class Link(linkId: Int, tenantId: Option[Int], src: Endpoint, dst: Endpoint)
case class VirtualLink(tenantId: Int, linkId: Int, backup_num: Int, priority: Int, path: Option[List[String]], algorithm: String,
                       srcDpid: Long, srcPort: Int, dstDpid: Long, dstPort: Int)

case class Network(tenantId: Option[Int], isBooted: Option[Boolean],
                   networkAddress: Int, mask: Short, controllerUrls: List[String])
case class VirtualSwitch(tenantId: Int, dpids: List[Long], vdpid: Long)

