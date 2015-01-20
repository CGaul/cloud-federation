package agents

import java.net._

import akka.actor._
import connectors.{Network, OVXConnector}
import datatypes._
import messages._

import scala.util.control.Breaks._

/**
 * @author Constantin Gaul, created on 10/15/14.
 */
class NetworkResourceAgent(ovxIp: InetAddress, ovxApiPort: Int,
													 matchMakingAgent: ActorRef)
													extends Actor with ActorLogging with Stash
{
/* Values: */
/* ======= */
	
		val _ovxConn = OVXConnector(ovxIp, ovxApiPort)
	

/* Variables: */
/* ========== */

		var _tenantNetMap: Map[Tenant, Network] = Map()
		var _hostSwitchesMap: Map[Host, List[OFSwitch]] = Map()

	//TODO: delete
// 	var cloudSwitches = _cloudSwitches
//	var cloudHosts = _cloudHosts

//		private var _ovxSubnetID: Int = 1
//		private var _ovxSubnetAddress: InetAddress = InetAddress.getByName("10.10.1.0")

/* Public Methods: */
/* =============== */

	def receive(): Receive = {
		case message: NRAResourceDest	=> message match {
			case ResourceRequest(tenant, resourcesToAlloc)
						=> recvResourceRequest(tenant, resourcesToAlloc)

			case ResourceFederationRequest(tenant, resourcesToAlloc)
						=> recvResourceFederationRequest(tenant, resourcesToAlloc)

			case ResourceFederationReply(resourcesAllocated)
						=> recvResourceFederationReply(resourcesAllocated)
		}
		case _														=> log.error("Unknown message received!")
	}


/* Private Receiving Methods: */
/* ========================== */

	/**
	 * Receives ResourceRequests from the CCFM.
	 * <p>
	 * 	Either local Cloud's Resources are sufficient, then the Request could be
	 * 	assigned to local resources only. If the local Resources are insufficient,
	 * 	split the Request, using all locally available resources and forward the
	 * 	federation part of the splitted Request to the MatchMakingAgent,
	 * 	who should then send a ResourceReply back to this NetworkResourceAgent,
	 * 	if all Federation attempts are finished,
	 * 	or all Resources could have been allocated.
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * Implemented in 0.2 Integrated Controllers
	 * @param tenant
	 * @param resourceToAlloc
	 */
	private def recvResourceRequest(tenant: Tenant, resourceToAlloc: ResourceAlloc): Unit = {

		log.info("Received ResourceRequest (Tenant: {}, ResCount: {}) at NetworkResourceAgent.",
			tenant, resourceToAlloc.resources.size)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourceToAlloc)

		// Prepare the locally fulfilled allocations that will be send to OVX via
		// the own OVXConnector-API:
		val hostList = allocationsPerHost.map(_._1).toList
		mapAllocOnOVX(tenant, hostList)
//		log.info("Send json-Query {} to OVX Hypervisor", jsonQuery) TODO: delete

		// If there is still a ResourceAlloc remaining, after the local cloudHosts tried to
		// allocate the whole ResourceAlloc-Request, send the remaining ResourceAlloc Split
		// to the MatchMakingAgent, in order to find a Federated Cloud that cares about the Resources:
		if(remainResToAlloc.isDefined){
			log.info("ResourceRequest {} could	 not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceToAlloc, remainResToAlloc)
			matchMakingAgent ! ResourceRequest(tenant, remainResToAlloc.get)
		}
		else log.info("ResourceRequest {} was completely allocated on the local cloud!", resourceToAlloc)
	}

	//TODO: Shortcut Implementation in 0.2 Integrated Controllers
	/**
	 * Receives a ResourceFederationReply from the MatchMakingAgent.
	 * <p>
	 * 	If a ResourceRequest could not have been processed locally,
	 * 	the NetworkFederationAgent has asked the MatchMakingAgent
	 * 	for Federation-Resources.
	 * 	All results are included in such ResourceFederationReply,
	 * 	stating the allocated Resources per foreign Cloud
	 * 	(the ActorRef is the foreign MatchMakingAgent)
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * @param federationResAllocs
	 */
	private def recvResourceFederationReply(federationResAllocs: Vector[(ActorRef, ResourceAlloc)]): Unit = {
	}


	//TODO: Shortcut Implementation in 0.2 Integrated Controllers
	/**
	 * Received from local MMA.
	 *
	 * @param resourcesToAlloc
	 * @param tenant
	 */
	private def recvResourceFederationRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc): Unit = {
		log.info("Received ResourceFederationRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NetworkResourceAgent.",
			resourcesToAlloc.tenantID, resourcesToAlloc.resources.size, tenant.ofcIp)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourcesToAlloc)
		// Prepare the locally fulfilled allocations as a JSON-Message
		// that will be send to the OVX embedder:
		val hostList = allocationsPerHost.map(_._1).toList
		mapAllocOnOVX(tenant, hostList)

//		log.info("Send json-Query {} to OVX Hypervisor", jsonQuery) TODO: delete

		if(remainResToAlloc.size > 0){
			// TODO: send Information about remaing Resources to Allocate back to the sender.
		}
	}


/* Private Helper Methods: */
/* ======================= */

	private def allocateLocally(resourceAlloc: ResourceAlloc): (Map[Host, ResourceAlloc], Option[ResourceAlloc]) = {
		// Will be filled with each allocation per Host that happened in this local allocation call:
		var allocationPerHost: Map[Host, ResourceAlloc] = Map()

		// Sort the potentialHosts as well as the resourceToAlloc by their resources in descending Order:
		val sortedHosts			= _cloudHosts.sorted(RelativeHostByResOrdering)
		val sortedResAlloc	= ResourceAlloc(resourceAlloc.tenantID,
			resourceAlloc.resources.sorted(RelativeResOrdering),
			resourceAlloc.requestedHostSLA)

		// Binpacking - First Fit Descending:
		// Fit each resourceToAlloc in the first potentialHost (bin)
		// that is fulfilling the resource & combined SLA requirements:
		var remainResAlloc: Option[ResourceAlloc]	= Option(sortedResAlloc)
		breakable {
			for (actHost <- sortedHosts) {
				// Try to allocate the remaining ResourceAlloc to the actual Host:
				val (allocatedSome, allocSplit, allocation) = actHost.allocate(remainResAlloc.get)

				// If an allocation took place, save this in the allocationPerHost-Map:
				if(allocatedSome && allocation.isDefined){
					allocationPerHost += (actHost -> allocation.get)
				}
				// If the actual ResourceAlloc could be allocated completely to the actHost,
				// set the remaining ResourceAlloc to None and break out of the loop.
				if (allocatedSome && allocSplit.isEmpty) {
					remainResAlloc = None
					break
				}

				// If not the whole ResourceAlloc could be allocated to the actHost,
				// the remainResAlloc for the next iteration is the allocSplit of this iteration:
				if (allocSplit.isDefined)  {
					remainResAlloc = allocSplit
				}
			}
		}
		return (allocationPerHost, remainResAlloc)
	}

	//TODO: Implement in 0.2 Integrated Controllers
	private def mapAllocOnOVX(tenant: Tenant, hosts: List[Host]) = {

		// If the tenant does not have an OVX tenant-network until now:
		if(! _tenantNetMap.keys.exists(_ == tenant)){
			val net = _ovxConn.createNetwork(List(s"tcp:${tenant.ofcIp}:${tenant.ofcPort}"), tenant.subnet._1, tenant.subnet._2)
			if(net.isDefined)
				_tenantNetMap = _tenantNetMap + (tenant -> net.get)
		}
		// In general, this allocation includes all physical Switches, that are needed to connect all hosts with each other.
		// first only get the physical Switches, directly connected to the hosts:
		// (get a flat list of each host -> List[Switch] mapping)
		val physSwitches = hosts.flatMap(_hostSwitchesMap).distinct
		// Afterwards, try to solve a path from each host to all other hosts, using all
		// currently discovered physical Switches as direct gateway to them:
		// TODO: path discovery
		
		// Once all Switches in this allocation are discovered,
		for (actPhysSwitch <- physSwitches) {
			// Create the virtual Switch as a direct mapping from phys -> virt
			val vSwitch = _ovxConn.createSwitch(tenant.tenantId, List(actPhysSwitch.dpid))

			// Add all known physical Ports to the virtual Switch:
			//TODO: add Ports to vSwitch

			// Add each Host that is connected directly to the current physical Switch to the
			// virtual Switch also:
			for (actHost <- hosts) {
				if(_hostSwitchesMap(actHost).contains(actPhysSwitch)){
					// TODO: connect Host to vSwitch
				}
			}
		}

		//TODO: delete:
//		// Prepare the Host-List for the Json-Query.
//		// Each Host entry is defined by connected Switch DPID, Host MAC and Switch Port
//		var hostsList: Seq[JsValue] = Seq()
//		for (actSwitch <- allocatedSwitches) {
//			// Find all allocated hosts that are connected to the actual switch:
//			val actHosts: Iterable[Host] = allocatedHosts.filter(h => actSwitch.links.values.exists(_ == h.compID))
//
//			for (actHost <- actHosts) {
//				// Find the Port at the Switch that connects the actual Host:
//				val port: Option[(Int, CompID)] = actSwitch.links.find(_._2 == actHost.compID)
//				if(port.isDefined){
//					hostsList = hostsList :+ Json.toJson(Map("dpid" -> Json.toJson(actSwitch.dpid), "mac" -> Json.toJson(actHost.mac), "port" -> Json.toJson(port.get._1)))
//				}
//				else{
//					log.error("Host {} is not connected to a Port in the Switch {}! Aborting allocation into OVX-Network!",
//										actHost, actSwitch)
//				}
//			}
//		}
//		val ofcQuery: JsValue = Json.toJson(Map(
//			"ctrls" -> Json.toJson(Seq(Json.toJson(
//				"tcp:"+ ofcIP.getHostAddress+":"+ofcPort))),
//			"type" -> Json.toJson("custom")))
//
//		val jsonQuery: JsValue = Json.toJson(
//			Map(
//				"id" -> Json.toJson(_ovxSubnetID.toString),
//				"jsonrpc" -> Json.toJson("2.0"),
//				"method" -> Json.toJson("createNetwork"),
//				"params" -> Json.toJson(Map(
//					"network" -> Json.toJson(Map(
//						"controller" -> ofcQuery,
//						"hosts" -> Json.toJson(hostsList),
//						"routing" -> Json.toJson(Map("algorithm" -> Json.toJson("spf"), "backup_num" -> Json.toJson(1))),
//						"subnet" -> Json.toJson(_ovxSubnetAddress.getHostAddress +"/24"),
//						"type" -> Json.toJson("physical")
//					))
//				))
//			)
//		)
//		// Save the jsonQuery to file:
//		val out: FileWriter = new FileWriter(new File("ovx_subnet-"+_ovxSubnetID+".json"))
//		out.write(Json.stringify(jsonQuery))
//		out.close()
//
//		// Prepare (increase) SubnetID and SubnetAddress for the next OVX-Network allocation:
//		_ovxSubnetID += 1
//		val newSubnetRange: Int = _ovxSubnetAddress.getHostAddress.substring(3,5).toInt + 1
//		val newAddress: String = _ovxSubnetAddress.getHostAddress.substring(0,3) +
//														 newSubnetRange + _ovxSubnetAddress.getHostAddress.substring(5)
//		_ovxSubnetAddress = InetAddress.getByName(newAddress)

	}
}


/**
 * Companion Object of the NetworkResource-Agent,
 * in order to implement some default behaviours
 */
object NetworkResourceAgent
{
	/**
	 * props-method is used in the AKKA-Context, spawning a new Agent.
	 * In this case, to generate a new NetworkResource Agent, call
	 * 	val ccfmProps = Props(classOf[NetworkResourceAgent], args = ovxIP)
	 * 	val ccfmAgent = system.actorOf(ccfmProps, name="NetworkResourceAgent-x")
	 * @param ovxIp The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
	 * @return An Akka Properties-Object
	 */
	def props(ovxIp: InetAddress, ovxApiPort: Int, matchMakingAgent: ActorRef):
	Props = Props(new NetworkResourceAgent(ovxIp, ovxApiPort, matchMakingAgent))
}