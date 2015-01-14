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
   * @return
   */
  def getPhysicalSwitchPorts(dpid: Long): Option[Short] = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalSwitchPorts", Map("DPID" -> Json.toJson(dpid)))
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
   * @return
   */
  def getPhysicalFlowtable() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getPhysicalFlowtable", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the Address/Mask of a virtual network
   * @return
   */
  def getSubnet() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getSubnet", Map())
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
   * @return
   */
  def getVirtualTopology() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualTopology", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the PhysicalSwitch(es) mapped to an OVXSwitch
   * @return
   */
  def getVirtualSwitchMapping() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchMapping", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the PhysicalLink(s) mapped to a virtual link
   * @return
   */
  def getVirtualLinkMapping() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualLinkMapping", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get all of the hosts on a virtual network. Hosts are described by virtual network addresses.
   * @return
   */
  def getVirtualHosts() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualHosts", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the flow table of a OVXSwitch
   * @return
   */
  def getVirtualFlowtable() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualFlowtable", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get the mappings between all available physical and virtual network addresses
   * @return
   */
  def getVirtualAddressMapping() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualAddressMapping", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Get all of the ports on a OVXSwitch
   * @return
   */
  def getVirtualSwitchPorts() = {
    val jsonRequest: JsValue = this.buildJsonQuery("getVirtualSwitchPorts", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }



  // Tenant API-Calls:
  // -----------------
  
  /**
   * Add a new controller to connect a virtual switch (OVXSwitch) to
   * @return
   */
  def addControllers() = {
    val jsonRequest: JsValue = this.buildJsonQuery("addControllers", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Remove a controller from the list of controllers a OVXSwitch can connect to
   * @return
   */
  def removeControllers = {
    val jsonRequest: JsValue = this.buildJsonQuery("",removeControllers Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Create a new virtual network (OVXNetwork)
   * @return
   */
  def createNetwork() = {
    val jsonRequest: JsValue = this.buildJsonQuery("createNetwork", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Create a new OVXSwitch
   * @return
   */
  def createSwitch() = {
    val jsonRequest: JsValue = this.buildJsonQuery("createSwitch", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Add a new port to an OVXSwitch
   * @return
   */
  def createPort() = {
    val jsonRequest: JsValue = this.buildJsonQuery("createPort", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Connect a host to an OVXNetwork
   * @return
   */
  def connectHost() = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectHost", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Add a virtual link to an OVXNetwork
   * @return
   */
  def connectLink() = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectLink", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Create a new route within a OVXBigSwitch
   * @return
   */
  def connectRoute() = {
    val jsonRequest: JsValue = this.buildJsonQuery("connectRoute", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Add a physical network path to a virtual link (OVXLink)
   * @return
   */
  def setLinkPath() = {
    val jsonRequest: JsValue = this.buildJsonQuery("setLinkPath", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Remove an OVXNetwork
   * @return
   */
  def removeNetwork() = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeNetwork", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Remove an OVXSwitch from an OVXNetwork
   * @return
   */
  def removeSwitch() = {
    val jsonRequest: JsValue = this.buildJsonQuery("removeSwitch", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Remove a port from an OVXSwitch
   * @return
   */
  def removePort() = {
    val jsonRequest: JsValue = this.buildJsonQuery("removePort", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Detach (remove) a host from a OVXNetwork
   * @return
   */
  def disconnectHost() = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectHost", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Detach (remove) a virtual link from an OVXNetwork
   * @return
   */
  def disconnectLink() = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectLink", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Detach (remove) a route from an OVXBigSwitch
   * @return
   */
  def disconnectRoute() = {
    val jsonRequest: JsValue = this.buildJsonQuery("disconnectRoute", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Initialize (boot) an OVXNetwork
   * @return
   */
  def startNetwork() = {
    val jsonRequest: JsValue = this.buildJsonQuery("startNetwork", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Intialize (boot) an OVXSwitch
   * @return
   */
  def startSwitch() = {
    val jsonRequest: JsValue = this.buildJsonQuery("startSwitch", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Enable a port on an OVXSwitch
   * @return
   */
  def startPort() = {
    val jsonRequest: JsValue = this.buildJsonQuery("startPort", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }

  /**
   * Disable an OVXNetwork temporarily
   * @return
   */
  def stopNetwork() = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopNetwork", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Disable an OVXSwitch temporarily
   * @return
   */
  def stopSwitch() = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopSwitch", Map())
    val jsonReply: Option[JsValue] = this.sendJsonQuery(jsonRequest)
    if(jsonReply.isDefined){
      // TODO: implement
    }
    
  }
  /**
   * Disable a port on an OVXSwitch temporarily
   * @return
   */
  def stopPort() = {
    val jsonRequest: JsValue = this.buildJsonQuery("stopPort", Map())
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
        "id" -> Json.toJson("NRA"),
        "jsonrpc" -> Json.toJson("2.0"),
        "method" -> Json.toJson(Json.toJson(method)),
        "params" -> Json.toJson(Json.toJson(params))
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
      case e: ConnectException => log.error("Connection to OVX-Embedder could not have been established at {}://{}:{}",
        ovxURI.getScheme, ovxURI.getHost, ovxURI.getPort)
        return None
      case e: NoRouteToHostException => log.error("No Route to OVX-Embedder Host at {}://{}:{}",
        ovxURI.getScheme, ovxURI.getHost, ovxURI.getPort)
        return None
      case e: Throwable => log.error("An unhandled error occurred, connecting to the OVX-Embedder Host at {}://{}:{}. Exception: {}",
        ovxURI.getScheme, ovxURI.getHost, ovxURI.getPort, e.getMessage)
        return None
    }
  }
  
}
