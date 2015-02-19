package connectors

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
  def createOVXNetwork(tenant: Tenant, foreignOvxInstance: Option[OvxInstance] = None): Option[VirtualNetwork] = {
    if(tenantNetMap.keys.exists(_ == tenant)){
      log.info("Virtual Network for tenant {} already exists. Returning existing OVX-Network: {}",
               tenant, tenantNetMap(tenant))
      
      return tenantNetMap.get(tenant)
    }
    
    var netOpt: Option[VirtualNetwork] = None
    foreignOvxInstance match{
    // If a foreign OVX-Instance is given, create a Network with an additional OFC entry in the Controller list:
      case Some(ovxInst) =>
        log.info("Creating Network with additional OFC connection to foreign OVX-Instance {}...", ovxInst)
        netOpt = ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp.getHostAddress}:${tenant.ofcPort}," +
          s"tcp:${ovxInst.ovxIp.getHostAddress}:${ovxInst.ovxCtrlPort}"),
          tenant.subnet._1, tenant.subnet._2)
        
      // Otherwise just create the Network with one Controller entry (the tenant's OFC):
      case None =>
        log.info("Creation Network with single, tenant based OFC connection to tenant OFC ({}:{})...", tenant.ofcIp, tenant.ofcPort)
        netOpt = ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp.getHostAddress}:${tenant.ofcPort}"),
          tenant.subnet._1, tenant.subnet._2)
    }

    netOpt match{
      case Some(net)  =>
        log.info(s"Created virtual Network ${tenant.subnet} for Tenant {} at OFC: {}:{}. Is Booted: {}",
          tenant.id, tenant.ofcIp, tenant.ofcPort, net.isBooted)
        tenantToOVXTenantId = tenantToOVXTenantId + (tenant -> net.tenantId.getOrElse(-1))
        tenantNetMap = tenantNetMap + (tenant -> net)
        return Some(net)

      case None          =>
        log.error(s"Virtual Network ${tenant.subnet} for Tenant {} at OFC: {}:{} was not started correctly!",
          tenant.id, tenant.ofcIp, tenant.ofcPort)
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
      log.info("Virtual Switch for physSwitch {} and tenant {} already exists. Returning existing OVX-Switch: {}",
               physSwitch, tenant, tenantVirtSwitchMap(tenant))
    
      return tenantVirtSwitchMap.getOrElse(tenant, List())
                                .find(vSwitch => vSwitch.dpids.contains(physSwitchDpid))
    }
    
    // Create the virtual Switch as a direct one-to-one mapping from OFSwitch -> virtSwitch
    val vSwitchOpt = ovxConn.createSwitch(tenantToOVXTenantId(tenant), List(physSwitch.dpid.toString))
    vSwitchOpt match{
      case Some(vSwitch) =>
        log.info(s"Created Switch (dpids: {} vdpid: {}) in Tenant-Network {}/(${tenantToOVXTenantId(tenant)})",
          physSwitch.dpid.toString, vSwitch.dpids, tenant.id)

        // After virtual Switch was successfully created, add physical and virtual Switch to respective tenantSwitchMap:
        tenantPhysSwitchMap = tenantPhysSwitchMap + (tenant -> (tenantPhysSwitchMap.getOrElse(tenant, List()) :+ physSwitch))
        tenantVirtSwitchMap = tenantVirtSwitchMap + (tenant -> (tenantVirtSwitchMap.getOrElse(tenant, List()) :+ vSwitch))
        return Some(vSwitch)

      case None          =>
        log.error(s"Switch Creation (dpids: {}) in Tenant-Network {}/(${tenantToOVXTenantId(tenant)}) failed!",
          physSwitch.dpid.toString, tenant.id)
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

        log.info(s"Created Port (phys: {} virt: {}) at Switch {} for other Switch in Tenant-Network {}/(${tenantToOVXTenantId(tenant)})",
          physPort, virtPort, physSwitch.dpid.toString, tenant.id)

        //Append a new value to tenantSwitchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
        val portMapList = tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()) :+ (physPort, virtPort, None)
        tenantSwitchPortMap = tenantSwitchPortMap + ((tenant, physSwitch) -> portMapList)
        return Some(portMap)

      case None          	=>
        log.error(s"Port creation failed for Switch {} on physical Port {}!",
          physSwitch.dpid, physPort)
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
//          // Find the srcPortMapping for the actual srcPort in the tenantSwitchPortMap's actPhysSwitch entry:
//          val srcPortMapping = tenantSwitchPortMap.getOrElse((tenant, physSrcSwitch), List()).
//            find(_._1 == srcPort)
//          val dstPortMapping = tenantSwitchPortMap.getOrElse((tenant, physDstSwitch), List()).
//            find(_._1 == srcEndpoint.port)
//          val virtSrcSwitch = tenantVirtSwitchMap.getOrElse(tenant, List()).
//            find(_.dpids.contains(actPhysSwitch.dpid.convertToHexLong))
//          val virtDstSwitch = tenantVirtSwitchMap.getOrElse(tenant, List()).
//            find(_.dpids.contains(srcEndpoint.dpid.convertToHexLong))
//
//          if (srcPortMapping.isDefined && dstPortMapping.isDefined && virtSrcSwitch.isDefined && virtDstSwitch.isDefined) {
//            val (physSrcPort, virtSrcPort, srcComponent) = srcPortMapping.get
//            val (physDstPort, virtDstPort, dstComponent) = dstPortMapping.get
//
//            // Check, if a link is already existing from dst -> src or src -> dst. Only establish a new one, if not for both:
//            val alreadyConnected: Boolean = srcComponent.isDefined || dstComponent.isDefined
//            if (!alreadyConnected) {

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
      
      log.warn("Connection between srcSwitch {} and dstSwitch {} in tenant {}'s network can't be established, " +
        "as no virtual counterparts are available or bidirectional port mapping is not existent!", 
        srcSwitch, dstSwitch, tenant)
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
     log.info("Connection between srcSwitch {} and dstSwitch {} in tenant {}'s network is already established." +
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
        log.error(s"Port creation failed for Switch {} on physical Port {}!",
          physSwitch.dpid, host.endpoint.port)
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
      log.warn("PortMap is undefined! Host {} can't be connected to Switch {} in tenant network {}",
      host, physSwitch, tenant)
      return None
    }
  }

  def connectOVXHost(tenant: Tenant, physSwitch: OFSwitch, virtSwitch: VirtualSwitch,
                              host: Host, portMap: (Short, Short)): Option[VirtualHost] = {
    //TODO: check if virtual device already existent for tenant
    

    val vHostOpt = ovxConn.connectHost(tenant.id, virtSwitch.vdpid, portMap._2, host.mac)
    vHostOpt match {
      case Some(vHost) =>
        log.info("Host {} connected to Switch {} at (physPort: {}, vPort {})",
          host.mac, physSwitch.dpid, portMap._1, portMap._2)
        //Update tenantSwitchPortMap's last portMap entry with the just established Host:
        val newHostPortMap = (portMap._1, portMap._2, Some(host))
        val portMapIndex = tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()).indexWhere(t => t._1 == portMap._1 && t._2 == portMap._2)
        tenantSwitchPortMap = tenantSwitchPortMap +
          ((tenant, physSwitch) -> tenantSwitchPortMap.getOrElse((tenant, physSwitch), List()).
            updated(portMapIndex, newHostPortMap))
        return Some(vHost)

      case None =>
        log.error("Host connection to Switch {} at (physPort: {}, vPort {}) failed!",
          host.mac, portMap._1, portMap._2)
        return None
    }
  }

  def startOVXNetwork(tenant: Tenant): Option[VirtualNetwork] = {
    if (tenantNetMap.keys.exists(_ == tenant) && tenantNetMap(tenant).isBooted.getOrElse(false)) {
      log.info("Virtual Network {} for tenant {} is already booted up.", tenantNetMap(tenant), tenant)
      return None
    }
    val netOpt = ovxConn.startNetwork(tenant.id)
    netOpt match{
      case Some(net)  =>
        log.info("Started Network for Tenant {} at OFC: {}:{}. Is Booted: {}",
          tenant.id, tenant.ofcIp, tenant.ofcPort, net.isBooted)
        tenantNetMap = tenantNetMap + (tenant -> net)
        return Some(net)

      case None          =>
        log.error("Network for Tenant {} at OFC: {}:{} was not started correctly!",
          tenant.id, tenant.ofcIp, tenant.ofcPort)
        return None
    }
  }
}

/**
 * Companion Object for OVXManager
 */
object OVXManager {
  def apply(ovxConn: OVXConnector) = new OVXManager(ovxConn)
}
