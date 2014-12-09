package agents

import java.net.InetAddress

import akka.actor._
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import util.control.Breaks._
import datatypes._
import messages._

/**
 * @author Constantin Gaul, created on 10/15/14.
 */
class NetworkResourceAgent(var _cloudSwitches: Vector[Switch], var _cloudHosts: Vector[Host],
													 var _ovxIP: InetAddress, var _ovxPort: Int,
													 matchMakingAgent: ActorRef)
													extends Actor with ActorLogging with Stash
{
///* Values: */
///* ======= */
//
//
///* Variables: */
///* ========== */
//
//	var cloudSwitches = _cloudSwitches
//	var cloudHosts = _cloudHosts

		private var _ovxSubnetID: Int = 1
		private var _ovxSubnetAddress: InetAddress = InetAddress.getByName("10.10.1.0")
		private var _ovxSwitchPorts: Map[Switch, Int]


/* Public Methods: */
/* =============== */

	def receive(): Receive = {
		case message: NRAResourceDest	=> message match {
			case ResourceRequest(resourcesToAlloc, ofcIP, ofcPort)
						=> recvResourceRequest(resourcesToAlloc, ofcIP, ofcPort)

			case ResourceFederationRequest(resourcesToAlloc, ofcIP, ofcPort)
						=> recvResourceFederationRequest(resourcesToAlloc, ofcIP, ofcPort)

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
	 * @param resourceToAlloc
	 * @param ofcIP
	 */
	private def recvResourceRequest(resourceToAlloc: ResourceAlloc, ofcIP: InetAddress, ofcPort: Int): Unit = {

		log.info("Received ResourceRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NetworkResourceAgent.",
			resourceToAlloc.tenantID, resourceToAlloc.resources.size, ofcIP)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourceToAlloc)

		// Prepare the locally fulfilled allocations as a JSON-Message
		// that will be send to the OVX embedder:
		val jsonQuery: JsValue = allocateOVXNetwork(allocationsPerHost, ofcIP, ofcPort)
		log.info("Send json-Query {} to OVX Hypervisor", jsonQuery)

		// If there is still a ResourceAlloc remaining, after the local cloudHosts tried to
		// allocate the whole ResourceAlloc-Request, send the remaining ResourceAlloc Split
		// to the MatchMakingAgent, in order to find a Federated Cloud that cares about the Resources:
		if(remainResToAlloc.isDefined){
			log.info("ResourceRequest {} could not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceToAlloc, remainResToAlloc)
			matchMakingAgent ! ResourceRequest(remainResToAlloc.get, ofcIP, ofcPort)
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
	 * @param ofcIP
	 */
	private def recvResourceFederationRequest(resourcesToAlloc: ResourceAlloc, ofcIP: InetAddress, ofcPort: Int): Unit = {
		log.info("Received ResourceFederationRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NetworkResourceAgent.",
			resourcesToAlloc.tenantID, resourcesToAlloc.resources.size, ofcIP)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourcesToAlloc)
		// Prepare the locally fulfilled allocations as a JSON-Message
		// that will be send to the OVX embedder:
		val jsonQuery: JsValue = allocateOVXNetwork(allocationsPerHost, ofcIP, ofcPort)

		log.info("Send json-Query {} to OVX Hypervisor", jsonQuery)

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
	private def allocateOVXNetwork(allocationsPerHost: Map[Host, ResourceAlloc],
																 ofcIP: InetAddress, ofcPort: Int): JsValue = {

		// Extract all allocated Hosts:
		val allocatedHosts = allocationsPerHost.map(_._1)

		// Find out the Switches that are connecting the Hosts with each other:
		var allocatedSwitches: Vector[Switch] = Vector()
		for (actHost <- allocatedHosts ) {
			 allocatedSwitches = allocatedSwitches ++ _cloudSwitches.filter(_.hostLinks.contains(actHost.compID))
		}

		// Prepare the Host-List for the Json-Query.
		// Each Host entry is defined by connected Switch DPID, Host MAC and Switch Port
		var hostsList: Seq[Map[String, String]] = Seq()
		for (actSwitch <- allocatedSwitches) {
			// Find all allocated hosts that are connected to the actual switch:
			val actHosts: Iterable[Host] = allocatedHosts.filter(h => actSwitch.hostLinks.contains(h.compID))

			// Begin with 0 and increase port +1 per new Host connection to Switch:
			var port = 0
			for (actHost <- actHosts) {
				hostsList = hostsList :+ Map("dpid" -> actSwitch.dpid, "mac" -> actHost.mac, "port" -> port.toString)
				port += 1
			}
		}

		val ofcQuery: JsValue = Json.toJson(Map(
			"ctrls" -> Json.toJson(Seq(Json.toJson(
				"tcp:"+ ofcIP.getHostAddress+":"+ofcPort))),
			"type" -> Json.toJson("custom")))

		val jsonQuery: JsValue = Json.toJson(
			Map(
				"id" -> Json.toJson(_ovxSubnetID),
				"jsonrpc" -> Json.toJson("2.0"),
				"method" -> Json.toJson("createNetwork"),
				"params" -> Json.toJson(Map(
					"controller" -> ofcQuery,
					"hosts" -> Json.toJson(hostsList),
					"routing" -> Json.toJson(Map("algorithm" -> "spf", "backup_num" -> "1")),
					"subnet" -> Json.toJson(_ovxSubnetAddress.getHostAddress +"/24"),
					"type" -> Json.toJson("physical")))
			)
		)

		// Prepare (increase) SubnetID and SubnetAddress for the next OVX-Network allocation:
		_ovxSubnetID += 1
		val newSubnetRange: Int = _ovxSubnetAddress.getHostAddress.substring(3,5).toInt + 1
		val newAddress: String = _ovxSubnetAddress.getHostAddress.substring(0,3) +
														 newSubnetRange + _ovxSubnetAddress.getHostAddress.substring(5)
		_ovxSubnetAddress = InetAddress.getByName(newAddress)

		// TODO: send jsonQuery to OVX-Embedder.

		return jsonQuery
	}
}

//{
//"id": "1",
//"jsonrpc": "2.0",
//"method": "createNetwork",
//"params": {
//	"network": {
//		"controller": {
//			"ctrls": [
//				"tcp:localhost:10000"
//			],
//			"type": "custom"
//			},
//		"hosts": [
//{
//"dpid": "00:00:00:00:00:00:02:00",
//"mac": "00:00:00:00:02:01",
//"port": 1
//},
//{
//"dpid": "00:00:00:00:00:00:05:00",
//"mac": "00:00:00:00:05:02",
//"port": 2
//},
//{
//"dpid": "00:00:00:00:00:00:06:00",
//"mac": "00:00:00:00:06:03",
//"port": 3
//},
//{
//"dpid": "00:00:00:00:00:00:09:00",
//"mac": "00:00:00:00:09:04",
//"port": 4
//}
//],
//"routing": {
//"algorithm": "spf",
//"backup_num": 1
//},
//"subnet": "192.168.0.0/24",
//"type": "bigswitch"
//}
//}
//}



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
	 * @param ovxIP The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
	 * @return An Akka Properties-Object
	 */
	def props(cloudSwitches: Vector[Switch], cloudHosts: Vector[Host],
						ovxIP: InetAddress, ovxPort: Int,
						matchMakingAgent: ActorRef):
	Props = Props(new NetworkResourceAgent(cloudSwitches, cloudHosts, ovxIP, ovxPort, matchMakingAgent))
}
