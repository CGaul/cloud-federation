package connectors

import java.net.InetAddress

import datatypes._
import org.slf4j.LoggerFactory

/**
 * @author Constantin Gaul, created on 2/15/15.
 */
class OVXManager(ovxConn: OVXConnector) 
{

/* Values: */
/* ======= */

  val log = LoggerFactory.getLogger(classOf[OVXConnector])

/* Variables: */
/* ========== */
  
  // Physical Mappings:
  // ------------------
  /**
   * Contains a List of all physical Switches in a tenant's virtual network
   */
  private var tenantPhysSwitchMap: Map[Tenant, List[OFSwitch]] = Map()
  /**
   * Contains a List of physPort -> virtPort mappings + an optional NetworkComponent, that is attached to the virt Port
   * for each physical OFSwitch in a tenant's virtual network 
   */
  private var tenantSwitchPortMap: Map[(Tenant, OFSwitch), List[(Short, Short, Option[NetworkComponent])]] = Map()

  
  // Virtual Mappings:
  // -----------------
  /**
   * Maps a Cloud Tenant to the internally handled OVX Tenant-ID, which has to be a consecutive number, starting by 1.
   * The OVXTenantId is receided from createOVXNetwork(..), so the mapping is written there. 
   * If the OVXTenantId is -1, the OVX network creation failed.
   */
  private var tenantToOVXTenantId: Map[Tenant, Int] = Map()
  /**
   * Maps the tenant's virtual Network to the Tenant itself. The tenantNetMap is written inside createOVXNetwork(..)
   */
  private var tenantNetMap: Map[Tenant, VirtualNetwork] = Map()
  /**
   *  Contains the List of all virtual OVX Switches inside the virtual network of a given Tenant.
   *  This mapping is written inside createOVXSwitch(..)
   */
  private var tenantVirtSwitchMap: Map[Tenant, List[VirtualSwitch]] = Map()



/* Public Methods: */
/* =============== */

  /**
   * Creates an OVXNetwork with the given Tenant-OFC and the predefined OVX-F instance as the Network-Controllers
   * @param tenant
   * @return
   */
  def createOVXNetwork(tenant: Tenant, networkOFCs: List[(InetAddress, Short)]): Option[VirtualNetwork] = {
    if(tenantNetMap.keys.exists(_ == tenant)){
      log.info(s"Virtual Network for tenant $tenant already exists. Returning existing OVX-Network: ${tenantNetMap(tenant)}")
      return tenantNetMap.get(tenant)
    }
    
    val netOfcList: List[String] = networkOFCs.map(ofc => s"tcp:${ofc._1}:${ofc._2}")
    log.info(s"Creating Network for tenant $tenant with OFCs $netOfcList...")
    val netOpt: Option[VirtualNetwork] = ovxConn.createNetwork(netOfcList, tenant.subnet._1, tenant.subnet._2)

    netOpt match{
      case Some(net)  =>
        log.info(s"Created virtual Network ${tenant.subnet} for Tenant ${tenant.id} " +
                 s"at OFC: ${tenant.ofcIp}:${tenant.ofcPort}. Is Booted: ${net.isBooted}")
        val ovxTenantId = net.tenantId.getOrElse(-1)
        tenantToOVXTenantId = tenantToOVXTenantId + (tenant -> ovxTenantId)
        tenant.ovxId_(ovxTenantId)
        tenantNetMap = tenantNetMap + (tenant -> net)
        return Some(net)

      case None          =>
        log.error(s"Virtual Network ${tenant.subnet} for Tenant ${tenant.id} " +
                  s"at OFC: ${tenant.ofcIp}:${tenant.ofcPort} was not started correctly!")
        return None
    }
  }

  /**
   * Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
   * @param tenant
   * @param physSwitch
   */
  def createOVXSwitch(tenant: Tenant, physSwitch: OFSwitch): Option[VirtualSwitch] = {
    val physSwitchDpid: Long = physSwitch.dpid.convertToHexLong
    if(tenantVirtSwitchMap.getOrElse(tenant, List()).exists(_.dpids.contains(physSwitchDpid)) &&
       tenantPhysSwitchMap.getOrElse(tenant, List()).exists(_.dpid == physSwitch.dpid)){
      log.info(s"Virtual Switch for physSwitch $physSwitch and tenant $tenant already exists. " +
               s"Returning existing OVX-Switch: ${tenantVirtSwitchMap(tenant)}")
    
      return tenantVirtSwitchMap.getOrElse(tenant, List())
                                .find(vSwitch => vSwitch.dpids.contains(physSwitchDpid))
    }
    
    // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
    val vSwitchOpt = ovxConn.createSwitch(tenantToOVXTenantId(tenant), List(physSwitch.dpid.toString))
    vSwitchOpt match{
      case Some(vSwitch) =>
        log.info(s"Created Switch (dpids: ${physSwitch.dpid} vdpid: ${vSwitch.dpids}) " +
                 s"in Tenant-Network ${tenant.id}/(${tenantToOVXTenantId(tenant)})")

        // After virtual Switch was successfully created, add physical and virtual Switch to respective tenantSwitchMap:
        tenantPhysSwitchMap = tenantPhysSwitchMap + (tenant -> (tenantPhysSwitchMap.getOrElse(tenant, List()) :+ physSwitch))
        tenantVirtSwitchMap = tenantVirtSwitchMap + (tenant -> (tenantVirtSwitchMap.getOrElse(tenant, List()) :+ vSwitch))
        return Some(vSwitch)

      case None          =>
        log.error(s"Switch Creation (dpids: ${physSwitch.dpid}) in " +
                  s"Tenant-Network ${tenant.id}/(${tenantToOVXTenantId(tenant)}) failed!")
        return None
    }
  }

  /**
   * Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch
   * @param tenant
   * @param physSwitch
   */
  def createAllOVXSwitchPorts(tenant: Tenant, physSwitch: OFSwitch): Boolean = {
    // Add all known physical Ports to the new virtual Switch, that are outgoing to any other switch:
    val physSrcPorts = physSwitch.portMap.map(_._1)
    var allPortsCreated: Boolean = true
    for (actSrcPort <- physSrcPorts) {
      val portMapOpt = createOVXSwitchPort(tenant, physSwitch, actSrcPort)
      if(portMapOpt.isEmpty)
        allPortsCreated = false
    }
    return allPortsCreated
  }

  def createOVXSwitchPort(tenant: Tenant, physSwitch: OFSwitch, physPort: Short): Option[(Short, Short)] = {
    //TODO: check if virtual device already existent for tenant
    
    val portMapOpt = ovxConn.createPort(tenantToOVXTenantId(tenant), physSwitch.dpid.toString, physPort)
    portMapOpt match{
      case Some(portMap)  =>
        val physPort = portMapOpt.get._1
        val virtPort = portMapOpt.get._2
        assert(physPort == physPort, s"Associated Physical Port $physPort after Port creation " +
          s"on Switch ${physSwitch.dpid} ist not equal to requested physical Source Port $physPort!")

        log.info(s"Created Port (phys: $physPort virt: $virtPort) " +
                 s"at Switch ${physSwitch.dpid} for other Switch " +
                 s"in Tenant-Network ${tenant.id}/(${tenantToOVXTenantId(tenant)})")

        //Append a new value to tenantSwitchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
        val portMapList = tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()) :+ (physPort, virtPort, None)
        tenantSwitchPortMap = tenantSwitchPortMap + ((tenant, physSwitch) -> portMapList)
        return Some(portMap)

      case None          	=>
        log.error(s"Port creation failed for Switch ${physSwitch.dpid} on physical Port $physPort!")
        return None
    }
  }

  /**
   * Connect all OVX-Switches with each other, that are included in the tenant's virtual network.
   * In order to put an OVX-Switch in this physicalSwitch-Map, call "createOVXSwitch(tenant, physSwitch)"
   * and "createAllOVXSwitchPorts(tenant, physSwitch)" for each Switch that should be in the tenant's virtual network.
   * @param tenant
   */
  def connectAllOVXSwitches(tenant: Tenant) = {
    // Iterate over all physical Switches, get their Port-mapping from tenantSwitchPortMap
    // and connect their virtual counterparts to each other on the correct virtPorts (create all topology paths):
    for (actPhysSwitch <- tenantPhysSwitchMap(tenant)) {
      for ((srcPort, srcEndpoint) <- actPhysSwitch.portMap) {
        val physSrcSwitch = actPhysSwitch
        val physDstSwitchOpt = tenantPhysSwitchMap.getOrElse(tenant, List()).find(_.dpid == srcEndpoint.dpid)
        // As the physical Destination Switch might not be in the tenant's switchMap, only continue connection if both
        // src- and dst-Switch are known:
        if (physDstSwitchOpt.isDefined) {
          val physDstSwitch = physDstSwitchOpt.get
          this.connectOVXSwitches(tenant, physSrcSwitch, physDstSwitch)
        }
      }
    }
  }

  /**
   * Connects srcSwitch with dstSwitch, if they have a bidirectionally existing port map and already established
   * virtual OVX-Switch counterparts in the virtual tenant network 
   * @param tenant
   * @param srcSwitch
   * @param dstSwitch
   * @return
   */
  def connectOVXSwitches(tenant: Tenant, srcSwitch: OFSwitch, dstSwitch: OFSwitch):
  Option[VirtualLink] = {

    val physSrcPortMapOpt = srcSwitch.portMap.find(_._2.dpid == dstSwitch.dpid)
    val physDstPortMapOpt = dstSwitch.portMap.find(_._2.dpid == srcSwitch.dpid)
    
    val virtSrcSwitchOpt = tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(srcSwitch.dpid.convertToHexLong))
    val virtDstSwitchOpt = tenantVirtSwitchMap.getOrElse(tenant, List()).find(_.dpids.contains(dstSwitch.dpid.convertToHexLong))

    if(!(physSrcPortMapOpt.isDefined && physDstPortMapOpt.isDefined &&
      virtSrcSwitchOpt.isDefined && virtDstSwitchOpt.isDefined)) {
      
      log.warn(s"Connection between srcSwitch $srcSwitch and dstSwitch $dstSwitch in tenant $tenant's network can't be established, " +
        "as no virtual counterparts are available or bidirectional port mapping is not existent!")
      return None
    }

    val virtSrcSwitch = virtSrcSwitchOpt.get
    val virtDstSwitch = virtDstSwitchOpt.get
    val physSrcPortMap = physSrcPortMapOpt.get
    val physDstPortMap = physDstPortMapOpt.get
    
    val physVirtSrcPortMap = tenantSwitchPortMap.getOrElse((tenant, srcSwitch), List()).find(_._1 == physSrcPortMap._1)
    val physVirtDstPortMap = tenantSwitchPortMap.getOrElse((tenant, dstSwitch), List()).find(_._1 == physDstPortMap._1)

    if(!(physVirtSrcPortMap.isDefined && physVirtDstPortMap.isDefined)){
      return None
    }
    
    val (physSrcPort, virtSrcPort, srcComponent) = physVirtSrcPortMap.get
    val (physDstPort, virtDstPort, dstComponent) = physVirtDstPortMap.get

    // Check, if a link is already existing from dst -> src or src -> dst. Only establish a new one, if not for both:
    val alreadyConnected: Boolean = srcComponent.isDefined || dstComponent.isDefined
    if (alreadyConnected) {
     log.info(s"Connection between srcSwitch $srcSwitch and dstSwitch $dstSwitch in tenant $tenant's network is already established." +
       "Returning existing virtual link")
      
      //TODO: return existing vLink
      return None
    }
    
    // If every mapping could be resolved and the OVX-Connection for both Switches is not yet established,
    // establish it now:
    val vLinkOpt = ovxConn.connectLink(tenantToOVXTenantId(tenant),
                                        virtSrcSwitch.vdpid, virtSrcPort,
                                        virtDstSwitch.vdpid, virtDstPort, "spf", 1)
    vLinkOpt match {
      case Some(vLink) =>
        log.info(s"Link connection between Switches " +
          s"(${srcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
          s"- ${dstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) suceeded!")

        // If virtual link was established successfully, update srcPortMapping in tenantSwitchPortMap with dstSwitch:
        val newSrcPortMap = (physSrcPort, virtSrcPort, Some(dstSwitch))
        val srcPortMapIndex = tenantSwitchPortMap.getOrElse((tenant, srcSwitch), List()).
          indexWhere(t => t._1 == physSrcPort && t._2 == virtSrcPort)

        // Update the tenantSwitchPortMap with the added NetworkComponent (the dstSwitch):
        tenantSwitchPortMap = tenantSwitchPortMap +
          ((tenant, srcSwitch) -> tenantSwitchPortMap.getOrElse((tenant, srcSwitch), List()).updated(srcPortMapIndex, newSrcPortMap))
        return Some(vLink)

      case None =>
        log.error(s"Link connection between Switches " +
          s"(${srcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
          s"- ${dstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) failed!")
        return None
    }
  }

  def createOVXHostPort(tenant: Tenant, physSwitch: OFSwitch, host: Host): Option[(Short, Short)] = {
    val hostPortMap = ovxConn.createPort(tenantToOVXTenantId(tenant), host.endpoint.dpid.toString, host.endpoint.port)
    hostPortMap match {
      case Some(portMap)	=>
        log.info(s"Created Port (phys: ${portMap._1} virt: ${portMap._2}) " +
          s"at Switch ${physSwitch.dpid.toString} for Host ${host.mac} " +
          s"in Tenant-Network ${tenant.id}/(${tenantToOVXTenantId(tenant)})")

        //Append a new value to tenantSwitchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
        val hostPortMap = (portMap._1, portMap._2, None)
        tenantSwitchPortMap = tenantSwitchPortMap + ((tenant, physSwitch) -> (tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()) :+ hostPortMap))
        return Some((portMap._1, portMap._2))

      case None 					=>
        log.error(s"Port creation failed for Switch ${physSwitch.dpid} on physical Port ${host.endpoint.port}!")
        return None
    }
  }
  
  def connectOVXHost(tenant: Tenant, physSwitchOpt: Option[OFSwitch], host: Host): Option[VirtualHost] = {
    val virtSwitchOpt = tenantVirtSwitchMap(tenant).find(_.dpids.contains(host.endpoint.dpid.convertToHexLong))
    if (!(physSwitchOpt.isDefined && virtSwitchOpt.isDefined)) {
      return None
    }
    val physSwitch = physSwitchOpt.get
    val virtSwitch = virtSwitchOpt.get
    var portMapOpt: Option[(Short, Short)] = None
    // If a virtual Port is available for the physSwitch + physPort
    if (tenantSwitchPortMap((tenant, physSwitch)).exists(_._1 == host.endpoint.port)) {
      portMapOpt = tenantSwitchPortMap((tenant, physSwitch)).find(_._1 == host.endpoint.port).map(t3 => (t3._1, t3._2))
    }
    // If no virtual Port is available for the physSwitch + physPort that the Host should connect to, createPort:
    else {
      portMapOpt = this.createOVXHostPort(tenant, physSwitch, host)
    }
    
    // Finally add the OVX-Host to the correct portMap:
    if (portMapOpt.isDefined) {
      return this.connectOVXHost(tenant, physSwitch, virtSwitch, host, portMapOpt.get)
    }
    else{
      log.warn(s"PortMap is undefined! Host $host can't be connected to Switch $physSwitch in tenant network $tenant")
      return None
    }
  }

  def connectOVXHost(tenant: Tenant, physSwitch: OFSwitch, virtSwitch: VirtualSwitch,
                              host: Host, portMap: (Short, Short)): Option[VirtualHost] = {
    //TODO: check if virtual device already existent for tenant
    

    val vHostOpt = ovxConn.connectHost(tenant.id, virtSwitch.vdpid, portMap._2, host.mac)
    vHostOpt match {
      case Some(vHost) =>
        log.info(s"Host ${host.mac} connected to Switch ${physSwitch.dpid} at (physPort: ${portMap._1}, vPort ${portMap._2})")
        //Update tenantSwitchPortMap's last portMap entry with the just established Host:
        val newHostPortMap = (portMap._1, portMap._2, Some(host))
        val portMapIndex = tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()).indexWhere(t => t._1 == portMap._1 && t._2 == portMap._2)
        tenantSwitchPortMap = tenantSwitchPortMap +
          ((tenant, physSwitch) -> tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()).
            updated(portMapIndex, newHostPortMap))
        return Some(vHost)

      case None =>
        log.error("Host connection to Switch ${host.mac} at (physPort: ${portMap._1}, vPort ${portMap._2}) failed!")
        return None
    }
  }

  def startOVXNetwork(tenant: Tenant): Option[VirtualNetwork] = {
    if (tenantNetMap.keys.exists(_ == tenant) && tenantNetMap(tenant).isBooted.getOrElse(false)) {
      log.info(s"Virtual Network ${tenantNetMap(tenant)} for tenant $tenant is already booted up.")
      return None
    }
    val netOpt = ovxConn.startNetwork(tenant.id)
    netOpt match{
      case Some(net)  =>
        log.info(s"Started Network for Tenant ${tenant.id} at OFC: ${tenant.ofcIp}:${tenant.ofcPort}. " +
                 s"Is Booted: ${net.isBooted}")
        tenantNetMap = tenantNetMap + (tenant -> net)
        return Some(net)

      case None          =>
        log.error(s"Network for Tenant ${tenant.id} at OFC: ${tenant.ofcIp}:${tenant.ofcPort} was not started correctly!")
        return None
    }
  }
  
  
  def removeOfcFromTenantNet(tenant: Tenant, ofcIp: InetAddress) = {
    val virtNetOpt = tenantNetMap.get(tenant)
    virtNetOpt match {
      case Some(virtNet) =>
        log.info(s"Removing tenant ${tenant.id} OFC Controller ${virtNet.controllerUrls(0)} " +
                 s"from network ${virtNet.networkAddress}...")
        ovxConn.removeControllers(tenant.id, List(virtNet.controllerUrls(0)))

      case None =>
        log.warn(s"Tenant $tenant has no virtual net to remove the OFC-IP $ofcIp from!")

    }
  }
}

/**
 * Companion Object for OVXManager
 */
object OVXManager {
  def apply(ovxConn: OVXConnector) = new OVXManager(ovxConn)
}