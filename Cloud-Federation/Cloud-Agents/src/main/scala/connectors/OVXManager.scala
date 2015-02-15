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
  private var tenantPhysSwitchMap: Map[Tenant, List[OFSwitch]] = Map()
  private var switchPortMap: Map[OFSwitch, List[(Short, Short, Option[NetworkComponent])]] = Map()

  // Virtual Mappings:
  private var tenantToOVXTenantId: Map[Tenant, Int] = Map()
  private var tenantNetMap: Map[Tenant, VirtualNetwork] = Map()
  private var tenantVirtSwitchMap: Map[Tenant, List[VirtualSwitch]] = Map()



/* Public Methods: */
/* =============== */

  /**
   * Creates an OVXNetwork with the given Tenant-OFC and the predefined OVX-F instance as the Network-Controllers
   * @param tenant
   * @return
   */
  def createOVXNetwork(tenant: Tenant, foreignOvxInstance: Option[OvxInstance] = None): Option[VirtualNetwork] = {
    var netOpt: Option[VirtualNetwork] = None
    foreignOvxInstance match{
      case Some(ovxInst) =>
        log.info("Creating Network with additional OFC connection to foreign OVX-Instance {}...", ovxInst)
        netOpt = ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp.getHostAddress}:${tenant.ofcPort}," +
          s"tcp:${ovxInst.ovxIp.getHostAddress}:${ovxInst.ovxCtrlPort}"),
          tenant.subnet._1, tenant.subnet._2)

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
    val portMapOpt = ovxConn.createPort(tenantToOVXTenantId(tenant), physSwitch.dpid.toString, physPort)
    portMapOpt match{
      case Some(portMap)  =>
        val physPort = portMapOpt.get._1
        val virtPort = portMapOpt.get._2
        assert(physPort == physPort, s"Associated Physical Port $physPort after Port creation " +
          s"on Switch ${physSwitch.dpid} ist not equal to requested physical Source Port $physPort!")

        log.info(s"Created Port (phys: {} virt: {}) at Switch {} for other Switch in Tenant-Network {}/(${tenantToOVXTenantId(tenant)})",
          physPort, virtPort, physSwitch.dpid.toString, tenant.id)

        //Append a new value to switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
        switchPortMap = switchPortMap + (physSwitch -> (switchPortMap.getOrElse(physSwitch, List()) :+ (physPort, virtPort, None)))
        return Some(portMap)

      case None          	=>
        log.error(s"Port creation failed for Switch {} on physical Port {}!",
          physSwitch.dpid, physPort)
        return None
    }
  }

  def connectOVXSwitches(tenant: Tenant,
                                  physSrcSwitch: OFSwitch, physSrcPort: Short, virtSrcSwitch: VirtualSwitch, virtSrcPort: Short,
                                  physDstSwitch: OFSwitch, physDstPort: Short, virtDstSwitch: VirtualSwitch, virtDstPort: Short):
  Option[VirtualLink] = {

    val vLinkOpt = ovxConn.connectLink(tenantToOVXTenantId(tenant),
      virtSrcSwitch.vdpid, virtSrcPort,
      virtDstSwitch.vdpid, virtDstPort, "spf", 1)
    vLinkOpt match {
      case Some(vLink) =>
        log.info(s"Link connection between Switches " +
          s"(${physSrcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
          s"- ${physDstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) suceeded!")

        // If virtual link was established successfully, update srcPortMapping in switchPortMap with physDstSwitch:
        val newSrcPortMap = (physSrcPort, virtSrcPort, Some(physDstSwitch))
        val srcPortMapIndex = switchPortMap.getOrElse(physSrcSwitch, List()).
          indexWhere(t => t._1 == physSrcPort && t._2 == virtSrcPort)

        switchPortMap = switchPortMap +
          (physSrcSwitch -> switchPortMap.getOrElse(physSrcSwitch, List()).updated(srcPortMapIndex, newSrcPortMap))
        return Some(vLink)

      case None =>
        log.error(s"Link connection between Switches " +
          s"(${physSrcSwitch.dpid}:$physSrcPort(${virtSrcSwitch.vdpid}:$virtSrcPort) " +
          s"- ${physDstSwitch.dpid}:$physDstPort(${virtDstSwitch.vdpid}:$virtDstPort)) failed!")
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

        //Append a new value to switchPortMap, which is either a new list (if no value was found for key), or a new entry in the list:
        val hostPortMap = (portMap._1, portMap._2, None)
        switchPortMap = switchPortMap + (physSwitch -> (switchPortMap.getOrElse(physSwitch, List()) :+ hostPortMap))
        return Some((portMap._1, portMap._2))

      case None 					=>
        log.error(s"Port creation failed for Switch {} on physical Port {}!",
          physSwitch.dpid, host.endpoint.port)
        return None
    }
  }

  def connectOVXHost(tenant: Tenant, physSwitch: OFSwitch, virtSwitch: VirtualSwitch,
                              host: Host, portMap: (Short, Short)): Option[VirtualHost] = {

    val vHostOpt = ovxConn.connectHost(tenant.id, virtSwitch.vdpid, portMap._2, host.mac)
    vHostOpt match {
      case Some(vHost) =>
        log.info("Host {} connected to Switch {} at (physPort: {}, vPort {})",
          host.mac, physSwitch.dpid, portMap._1, portMap._2)
        //Update switchPortMap's last portMap entry with the just established Host:
        val newHostPortMap = (portMap._1, portMap._2, Some(host))
        val portMapIndex = switchPortMap.getOrElse(physSwitch, List()).indexWhere(t => t._1 == portMap._1 && t._2 == portMap._2)
        switchPortMap = switchPortMap +
          (physSwitch -> switchPortMap.getOrElse(physSwitch, List()).
            updated(portMapIndex, newHostPortMap))
        return Some(vHost)

      case None =>
        log.error("Host connection to Switch {} at (physPort: {}, vPort {}) failed!",
          host.mac, portMap._1, portMap._2)
        return None
    }
  }

  def startOVXNetwork(tenant: Tenant): Option[VirtualNetwork] = {
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
