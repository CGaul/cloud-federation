package agents

import java.net.InetAddress

import akka.actor._
import util.control.Breaks._
import datatypes._
import messages._

/**
 * @author Constantin Gaul, created on 10/15/14.
 */
class NetworkResourceAgent(_cloudSwitches: Vector[Switch], _cloudHosts: Vector[Host],
													 _ovxIP: InetAddress, matchMakingAgent: ActorRef)
													extends Actor with ActorLogging with Stash
{
/* Values: */
/* ======= */


/* Variables: */
/* ========== */


/* Public Methods: */
/* =============== */

	def receive(): Receive = {
		case message: NRAResourceDest	=> message match {
			case ResourceRequest(resourcesToAlloc, ofcIP)		=> recvResourceRequest(resourcesToAlloc, ofcIP)
			case ResourceFederationRequest(resourcesToAlloc, ofcIP) => recvResourceFederationRequest(resourcesToAlloc, ofcIP)
			case ResourceFederationReply(resourcesAllocated) 	=> recvResourceFederationReply(resourcesAllocated)
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
	 * @param resourceAlloc
	 * @param ofcIP
	 */
	private def recvResourceRequest(resourceAlloc: ResourceAlloc, ofcIP: InetAddress): Unit = {

		log.info("Received ResourceRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NetworkResourceAgent.",
			resourceAlloc.tenantID, resourceAlloc.resources.size, ofcIP)

		val (allocationsPerHost, remainResToAlloc) = allocateLocally(resourceAlloc)

		// Prepare the locally fulfilled allocations as a JSON-Message
		// that will be send to the OVX embedder:
		prepareOVXJsonAllocation(allocationsPerHost, ofcIP)

		// If there is still a ResourceAlloc remaining, after the local cloudHosts tried to
		// allocate the whole ResourceAlloc-Request, send the remaining ResourceAlloc Split
		// to the MatchMakingAgent, in order to find a Federated Cloud that cares about the Resources:
		if(remainResToAlloc.isDefined){
			log.info("ResourceRequest {} could not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceAlloc, remainResToAlloc)
			matchMakingAgent ! ResourceRequest(remainResToAlloc.get, ofcIP)
		}
		else log.info("ResourceRequest {} was completely allocated on the local cloud!", resourceAlloc)
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
	private def recvResourceFederationRequest(resourcesToAlloc: ResourceAlloc, ofcIP: InetAddress): Unit = {
		log.info("Received ResourceFederationRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at NetworkResourceAgent.",
			resourcesToAlloc.tenantID, resourcesToAlloc.resources.size, ofcIP)

		val hostResourceMap: Map[Host, ResourceAlloc] = mapResourcesToHosts(resourcesToAlloc)
		prepareOVXJsonAllocation(hostResourceMap, ofcIP)
	}


/* Private Helper Methods: */
/* ======================= */

	private def allocateLocally(resourceAlloc: ResourceAlloc): (Map[Host, ResourceAlloc], Option[ResourceAlloc]) = {
		// Will be filled with each allocation per Host that happened in this local allocation call:
		var allocationPerHost: Map[Host, ResourceAlloc] = Map()

		// Sort the potentialHosts as well as the resourceAlloc by their resources in descending Order:
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
	private def mapResourcesToHosts(resourcesToAlloc: ResourceAlloc): Map[Host, ResourceAlloc] = {
		val hostResourceMap: Map[Host, ResourceAlloc] = Map()
		// TODO: if no single Host could be found to allocate all Resources on, split and allocate on multiple Hosts.
		// TODO: Use previously defined FederationPool for this, as the NRA should know its free slots at this point.

		return hostResourceMap
	}

	//TODO: Implement in 0.2 Integrated Controllers
	private def prepareOVXJsonAllocation(allocationsPerHost: Map[Host, ResourceAlloc], ofcIP: InetAddress) = {

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
	 * @param ovxIP The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
	 * @return An Akka Properties-Object
	 */
	def props(cloudSwitches: Vector[Switch], cloudHosts: Vector[Host], ovxIP: InetAddress, matchMakingAgent: ActorRef):
	Props = Props(new NetworkResourceAgent(cloudSwitches, cloudHosts, ovxIP, matchMakingAgent))
}
