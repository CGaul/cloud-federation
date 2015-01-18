package connectors

import java.io.IOException
import java.net.{ConnectException, InetAddress, NoRouteToHostException, URI}

import akka.event.{Logging, LoggingAdapter}
import com.fasterxml.jackson.annotation.JsonValue
import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.slf4j.{LoggerFactory, Logger}
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    if(jsonReply.isDefined){
      val switches = (jsonReply.get \ "switches").as[List[String]]
      val links = (jsonReply.get \ "links").as[List[Link]]
      return (switches, links)
    }
      return (List(), List())
  }
  
  // Method is not yet implemented in OVX:
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
//    if(jsonReply.isDefined){
//      val port: Option[Short] = (jsonReply.get \ "port").asOpt[Short]
//      return port
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
    
    if(jsonReply.isDefined){
      val hostList = jsonReply.get.as[List[PhysicalHost]]
      return hostList
    }
    else
      return List()

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
    if(jsonReply.isDefined){
      // TODO: implement
      return List()
    }
    return List()
  }
  
  /**
   * Get all flow tables of each PhysicalSwitch
   * @return
   */
  def getPhysicalFlowtables: List[String] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalFlowtable", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
      return List()
    }
    return List()
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
    if(jsonReply.isDefined){
      val subnet = jsonReply.get.toString()
      return Option(subnet)
    }
    return None
    
  }
  /**
   * List all of the virtual networks
   * @return
   */
  def listVirtualNetworks: List[Int] = {
    val jsonRequest: JsValue = this.buildJsonQuery("listVirtualNetworks", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      val tenantIds = jsonReply.get.as[List[Int]]
      return tenantIds
    }
    return List()
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
    if(jsonReply.isDefined){
      val switches = (jsonReply.get \ "switches").as[List[String]]
      val links = (jsonReply.get \ "links").as[List[Link]]
      return (switches, links)
    }
    return (List(), List())
    
  }
  /**
   * Get the PhysicalSwitch(es) mapped to an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualSwitchMapping(tenantId: Int) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the PhysicalLink(s) mapped to a virtual link
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualLinkMapping(tenantId: Int) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualLinkMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get all of the hosts on a virtual network. Hosts are described by virtual network addresses.
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualHosts(tenantId: Int) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualHosts", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the flow table of a OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @return
   */
  def getVirtualFlowtable(tenantId: Int, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualFlowtable", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the mappings between all available physical and virtual network addresses
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualAddressMapping(tenantId: Int) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualAddressMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get all of the ports on a OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualSwitchPorts(tenantId: Int, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchPorts", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "status")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }



  // Tenant API-Calls:
  // -----------------
  
  /**
   * Add a new controller to connect a virtual switch (OVXSwitch) to
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param controllerUrls String="proto:host:port" where proto is usually "tcp"
   */
  def addControllers(tenantId: Int, vpid: Int, controllerUrls: List[String]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("addControllers", 
      Map(
        "tenantId"        -> Json.toJson(tenantId),
        "vpid"            -> Json.toJson(vpid),
        "controllerUrls"  -> Json.toJson(controllerUrls)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Remove a controller from the list of controllers a OVXSwitch can connect to
   * @param tenantId The GUID of a virtual network
   * @param controllerUrls String="proto:host:port" where proto is usually "tcp"
   * @return
   */
  def removeControllers(tenantId: Int, controllerUrls: List[String]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeControllers", 
      Map(
        "tenantId"        -> Json.toJson(tenantId),
        "controllerUrls"  -> Json.toJson(controllerUrls)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Create a new virtual network (OVXNetwork)
   * @param controllerUrls String="proto:host:port" where proto is usually "tcp"
   * @param networkAddress The IP address block used in a network
   * @param mask The CIDR value (1-30) of the network mask used with NETADD
   * @return
   */
  def createNetwork(controllerUrls: List[String], networkAddress: Int, mask: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("createNetwork", 
      Map(
        "controllerUrls"  -> Json.toJson(controllerUrls),
        "networkAddress"  -> Json.toJson(networkAddress),
        "mask"            -> Json.toJson(mask)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Create a new OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param dpids One or more DPIDs of switches mapped to an OVXBigSwitch
   * @param dpid DPID of a physical network switch
   * @return
   */
  def createSwitch(tenantId: Int, dpids: List[Long], dpid: String) = {
    val jsonRequest: JsValue = this.buildJsonQuery("createSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "dpids"       -> Json.toJson(dpids),
        "dpid"        -> OVXConnector.convertString2HexDpid(dpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Add a new port to an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param dpid DPID of a physical network switch
   * @param port The port number of a switch port
   * @return
   */
  def createPort(tenantId: Int, dpid: String, port: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("createPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "dpid"        -> OVXConnector.convertString2HexDpid(dpid),
        "port"        -> Json.toJson(port)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Connect a host to an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   * @param mac Host MAC address in colon-hex: "xx:xx:xx:xx:xx:xx"
   * @return
   */
  def connectHost(tenantId: Int, vpid: Long, vport: Short, mac: String) = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectHost", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport),
        "mac"         -> Json.toJson(mac)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Add a virtual link to an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param srcDpid The colon-hex DPID of the source switch
   * @param srcPort The port on the source switch
   * @param dstDpid The DPID of the destination switch
   * @param dstPort The port on the destination switch
   * @param algorithm Method for setting routes in a OVXBigSwitch: "manual" or "spf"
   * @param backup_num Number of backup paths associated with a virtual link or route
   * @return
   */
  def connectLink(tenantId: Int, srcDpid: Long, srcPort: Short,
                  dstDpid: Long, dstPort: Short,
                  algorithm: String, backup_num: Byte) = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectLink", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "srcDpid"     -> Json.toJson(srcDpid),
        "srcPort"     -> Json.toJson(srcPort),
        "dstDpid"     -> Json.toJson(dstDpid),
        "dstPort"     -> Json.toJson(dstPort),
        "algorithm"   -> Json.toJson(algorithm),
        "backup_num"  -> Json.toJson(backup_num.toInt)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Create a new route within a OVXBigSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param srcPort The port on the source switch
   * @param dstPort The port on the destination switch
   * @param path The physical path taken by a virtual link or route
   * @return
   */
  def connectRoute(tenantId: Int, vpid: Long, srcPort: Short, dstPort: Short, path: Path) = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectRoute", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "srcPort"     -> Json.toJson(srcPort),
        "dstPort"     -> Json.toJson(dstPort),
        "path"        -> Json.toJson(path)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
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
    if(jsonReply.isDefined){
      // TODO: implement
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
   * @param vpid DPID of a virtual switch
   */
  def removeSwitch(tenantId: Int, vpid: Long): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }
  /**
   * Remove a port from an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   */
  def removePort(tenantId: Int, vpid: Long, vport: Short): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removePort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
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
   * @param vpid DPID of a virtual switch
   * @param routeId OVX-generated UUID of a route
   */
  def disconnectRoute(tenantId: Int, vpid: Long, routeId: Int): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectRoute", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "routeId"     -> Json.toJson(routeId)
      ))
    this.sendJsonQuery(jsonRequest, "tenant")
  }

  /**
   * Initialize (boot) an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def startNetwork(tenantId: Int) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Intialize (boot) an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @return
   */
  def startSwitch(tenantId: Int, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Enable a port on an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   * @return
   */
  def startPort(tenantId: Int, vpid: Long, vport: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
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
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Disable an OVXSwitch temporarily
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @return
   */
  def stopSwitch(tenantId: Int, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Disable a port on an OVXSwitch temporarily
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   * @return
   */
  def stopPort(tenantId: Int, vpid: Long, vport: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest, "tenant")
    if(jsonReply.isDefined){
      // TODO: implement
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
        return Some(responseResult)
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

  case class Endpoint(dpid: String, port: Short)
  case class Link(linkId: Int, tenantId: Option[Int], src: Endpoint, dst: Endpoint)

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
  
  implicit val physicalHostReads: Reads[PhysicalHost] =(
    (JsPath \ "hostId").read[Int] and
    (JsPath \ "dpid").read[String] and
    (JsPath \ "port").read[Short] and
    (JsPath \ "mac").read[String] and
    (JsPath \ "ipAddress").readNullable[String]
  )(PhysicalHost.apply _)

  implicit val endpointReads = Json.reads[Endpoint]
  implicit val linkReads: Reads[Link] = (
    (JsPath \ "linkId").read[Int] and 
    (JsPath \ "tenantId").readNullable[Int] and
    (JsPath \ "src").read[Endpoint] and
    (JsPath \ "dst").read[Endpoint]
    )(Link)
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
