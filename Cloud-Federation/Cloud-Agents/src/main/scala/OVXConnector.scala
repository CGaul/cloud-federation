import java.net.{NoRouteToHostException, ConnectException, URI, InetAddress}

import akka.event.LoggingAdapter
import org.apache.http.HttpEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{HttpClients, CloseableHttpClient}
import org.apache.http.util.EntityUtils
import play.api.libs.json.{Json, JsValue}

/**
 * Created by costa on 1/14/15.
 */
class OVXConnector(ovxApiAddr: InetAddress, ovxApiPort: Int, log: LoggingAdapter)
{

/* Public Methods: */
/* =============== */
  
  // Monitoring API-Calls:
  // ---------------------

  /**
   * Get the current PhysicalNetwork topology
   * @return
   */
  def getPhysicalTopology(): (Option[List[Long]], Option[List[Map[Integer, Map[(Long, Short), (Long, Short)]]]]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalTopology", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      val switches: List[Long] = (jsonReply.get \ "switches").as
      // TODO: make better:
      val links: List[Map[Integer, Map[(Long, Short), (Long, Short)]]] = (jsonReply.get \ "links").as
      
      return (Option(switches), Option(links))
    }
    else 
      return (None, None)
  }
  /**
   * Get all of the ports on a PhysicalSwitch
   * @param dpid DPID of a physical network switch
   * @return
   */
  def getPhysicalSwitchPorts(dpid: Long): Option[Short] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalSwitchPorts", 
      Map(
        "dpid"      -> Json.toJson(dpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      val port: Option[Short] = (jsonReply.get \ "PORT").asOpt[Short]
      return port
    }
    else
      return None
  }
  /**
   * Get all hosts from all virtual networks. Hosts are described by physical network addresses.
   * @return
   */
  def getPhysicalHosts() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalHosts", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the flow table of a PhysicalSwitch
   * @param dpid DPID of a physical network switch
   * @return  
   */
  def getPhysicalFlowtable(dpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalFlowtable", 
      Map(
        "dpid"      -> Json.toJson(dpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the Address/Mask of a virtual network
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getSubnet(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getSubnet", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * List all of the virtual networks
   * @return
   */
  def listVirtualNetworks() = {
    val jsonRequest: JsValue = this.buildJsonQuery("listVirtualNetworks", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the topology of a virtual network
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualTopology(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualTopology", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the PhysicalSwitch(es) mapped to an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualSwitchMapping(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the PhysicalLink(s) mapped to a virtual link
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualLinkMapping(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualLinkMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get all of the hosts on a virtual network. Hosts are described by virtual network addresses.
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualHosts(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualHosts", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def getVirtualFlowtable(tenantId: Integer, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualFlowtable", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the mappings between all available physical and virtual network addresses
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualAddressMapping(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualAddressMapping", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get all of the ports on a OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def getVirtualSwitchPorts(tenantId: Integer, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchPorts", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def addControllers(tenantId: Integer, vpid: Integer, controllerUrls: List[String]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("addControllers", 
      Map(
        "tenantId"        -> Json.toJson(tenantId),
        "vpid"            -> Json.toJson(vpid),
        "controllerUrls"  -> Json.toJson(controllerUrls)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def removeControllers(tenantId: Integer, controllerUrls: List[String]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeControllers", 
      Map(
        "tenantId"        -> Json.toJson(tenantId),
        "controllerUrls"  -> Json.toJson(controllerUrls)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def createNetwork(controllerUrls: List[String], networkAddress: Integer, mask: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("createNetwork", 
      Map(
        "controllerUrls"  -> Json.toJson(controllerUrls),
        "networkAddress"  -> Json.toJson(networkAddress),
        "mask"            -> Json.toJson(mask)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def createSwitch(tenantId: Integer, dpids: List[Long], dpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("createSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "dpids"       -> Json.toJson(dpids),
        "dpid"        -> Json.toJson(dpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def createPort(tenantId: Integer, dpid: Long, port: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("createPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "dpid"        -> Json.toJson(dpid),
        "port"        -> Json.toJson(port)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def connectHost(tenantId: Integer, vpid: Long, vport: Short, mac: String) = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectHost", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport),
        "mac"         -> Json.toJson(mac)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def connectLink(tenantId: Integer, srcDpid: Long, srcPort: Short, 
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
        "backup_num"  -> Json.toJson(backup_num)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def connectRoute(tenantId: Integer, vpid: Long, srcPort: Short, dstPort: Short, path: List[(Long, Short, Long, Short)]) = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectRoute", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "srcPort"     -> Json.toJson(srcPort),
        "dstPort"     -> Json.toJson(dstPort),
        "path"        -> Json.toJson(path)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def setLinkPath(tenantId: Integer, linkId: Integer, path: List[(Long, Short, Long, Short)], priority: Byte) = {
    val jsonRequest: JsValue = this.buildJsonQuery("setLinkPath", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "linkId"      -> Json.toJson(linkId),
        "path"        -> Json.toJson(path),
        "priority"    -> Json.toJson(priority)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Remove an OVXNetwork
   * @param tenantId The GUID of a virtual network
   */
  def removeNetwork(tenantId: Integer): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    this.sendJsonQuery(jsonRequest)
  }
  /**
   * Remove an OVXSwitch from an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   */
  def removeSwitch(tenantId: Integer, vpid: Long): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    this.sendJsonQuery(jsonRequest)
  }
  /**
   * Remove a port from an OVXSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param vport The port number of a virtual switch port
   */
  def removePort(tenantId: Integer, vpid: Long, vport: Short): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("removePort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport)
      ))
    this.sendJsonQuery(jsonRequest)
  }

  /**
   * Detach (remove) a host from a OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param hostId UUID of a host
   */
  def disconnectHost(tenantId: Integer, hostId: Integer): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectHost", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "hostId"      -> Json.toJson(hostId)
      ))
    this.sendJsonQuery(jsonRequest)
  }
  /**
   * Detach (remove) a virtual link from an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @param linkId OVX-generated UUID of a virtual link
   */
  def disconnectLink(tenantId: Integer, linkId: Integer): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectLink", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "linkId"      -> Json.toJson(linkId)
      ))
    this.sendJsonQuery(jsonRequest)
  }
  /**
   * Detach (remove) a route from an OVXBigSwitch
   * @param tenantId The GUID of a virtual network
   * @param vpid DPID of a virtual switch
   * @param routeId OVX-generated UUID of a route
   */
  def disconnectRoute(tenantId: Integer, vpid: Long, routeId: Integer): Unit = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectRoute", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "routeId"     -> Json.toJson(routeId)
      ))
    this.sendJsonQuery(jsonRequest)
  }

  /**
   * Initialize (boot) an OVXNetwork
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def startNetwork(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def startSwitch(tenantId: Integer, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def startPort(tenantId: Integer, vpid: Long, vport: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("startPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Disable an OVXNetwork temporarily
   * @param tenantId The GUID of a virtual network
   * @return
   */
  def stopNetwork(tenantId: Integer) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopNetwork", 
      Map(
        "tenantId"    -> Json.toJson(tenantId)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def stopSwitch(tenantId: Integer, vpid: Long) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopSwitch", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
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
  def stopPort(tenantId: Integer, vpid: Long, vport: Short) = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopPort", 
      Map(
        "tenantId"    -> Json.toJson(tenantId),
        "vpid"        -> Json.toJson(vpid),
        "vport"       -> Json.toJson(vport)
      ))
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }


/* Private Methods: */
/* ================ */
  
  private def buildJsonQuery(method: String, params: Map[String, Object]): JsValue = {
    val jsonQuery: JsValue = Json.toJson(
      Map(
        "id"          -> Json.toJson("NRA"),
        "jsonrpc"     -> Json.toJson("2.0"),
        "method"      -> Json.toJson(Json.toJson(method)),
        "params"      -> Json.toJson(Json.toJson(params))
      )
    )
    return jsonQuery
  }

  private def sendJsonQuery(jsonQuery: JsValue): Option[JsValue] = {
    //Use the Apache HTTP-Client to send a JSON message via HTTP Post to the OVX-Jetty API Server:
    val httpclient: CloseableHttpClient = HttpClients.createDefault()

    val ovxURI: URI = new URIBuilder()
      .setHost(ovxApiAddr.getHostAddress)
      .setPort(ovxApiPort)
      .setScheme("http")
      .build()
    val httpPost: HttpPost = new HttpPost(ovxURI)
    httpPost.setEntity(new StringEntity(Json.stringify(jsonQuery), "UTF-8"))
    httpPost.addHeader("content-type", "application/json")

    try {
      val response: CloseableHttpResponse = httpclient.execute(httpPost)
      System.out.println(response.getStatusLine)
      val entity: HttpEntity = response.getEntity
      val jsonResponse: JsValue = Json.parse(EntityUtils.toString(response.getEntity))
      
      EntityUtils.consume(entity)
      response.close()
      
      return Some(jsonResponse)
    }
    catch{
      case e: ConnectException => log.error("Connection to OVX-API could not have been established at {}://{}:{}",
        ovxURI.getScheme, ovxURI.getHost, ovxURI.getPort)
        return None
      case e: NoRouteToHostException => log.error("No Route to OVX-API Host at {}://{}:{}",
        ovxURI.getScheme, ovxURI.getHost, ovxURI.getPort)
        return None
      case e: Throwable => log.error("An unhandled error occurred, connecting to the OVX-API Host at {}://{}:{}. Exception: {}",
        ovxURI.getScheme, ovxURI.getHost, ovxURI.getPort, e.getMessage)
        return None
    }
  }
  
}
