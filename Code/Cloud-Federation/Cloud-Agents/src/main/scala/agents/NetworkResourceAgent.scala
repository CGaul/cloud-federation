package agents

import java.net.InetAddress

import akka.actor._
import util.control.Breaks._
import datatypes._
import messages.{ResourceRequest, ResourceFederationReply, NetworkResourceMessage}

/**
 * @author Constantin Gaul, created on 10/15/14.
 */
class NetworkResourceAgent(_initialHostAlloc: Vector[Host],
									_ovxIP: InetAddress) extends Actor with ActorLogging with Stash
{
/* Values: */
/* ======= */


/* Variables: */
/* ========== */

	var _cloudHosts: Vector[Host] = _initialHostAlloc


/* Public Methods: */
/* =============== */

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Receives ResourceInfos from the CCFM.
	 * <p>
	 *    Directly after the NetworkResourceAgent was started and everytime the topology changes from
	 *    outside circumstances, the CCFM sends a Topology update in the form of an abstracted combination
	 *    of total Resources of the whole Cloud (including the Host's, initial Power and their connections without load)
	 *    and the available Resources, which are excluding Resources that are currently completely assigned and/or
	 *    under load.
	 * </p>
	 * <p>
	 * 	When a ResourceInfo message is queued at the NetworkResourceAgent the first time, the internal _initialized value
	 * 	will be set to true, as the Agent is not able to function without these information.
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 *
	 * @param totalResources
	 * @param availResources
	 */
//	def recvResourceInfo(totalResources: ResourceAlloc): Unit = {
//		_totalResources	= _to
//		_availResources	= availResources
//
//		unstashAll()
//		context.become(receivedOnline())
//	}

	//TODO: Implement in 0.2 Integrated Controllers
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
	 * @param resourceAlloc
	 * @param address
	 */
	def recvResourceRequest(resourceAlloc: ResourceAlloc, address: InetAddress): Unit = {

		// Sort the potentialHosts as well as the resourceAlloc by their resources in descending Order:
		val sortedHosts			= _cloudHosts.sorted(RelativeHostByResOrdering)
		val sortedResAlloc	= ResourceAlloc(resourceAlloc.resources.sorted(RelativeResOrdering), resourceAlloc.requestedHostSLA)

		// Binpacking - First Fit Descending:
		// Fit each resourceToAlloc in the first potentialHost (bin)
		// that is fulfilling the resource & combined SLA requirements:
		var remainResAlloc: Option[ResourceAlloc]	= Option(sortedResAlloc)
		breakable {
			for (actHost <- sortedHosts) {
				log.debug("Host: {}", actHost)
				// Try to allocate the remaining ResourceAlloc to the actual Host:
				val (allocatedSome, allocSplit) = actHost.allocate(remainResAlloc.get)

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

		// If there is still a ResourceAlloc remaining, after the local cloudHosts tried to
		// allocate the whole ResourceAlloc-Request, send the remaining ResourceAlloc Split
		// to the MatchMakingAgent, in order to find a Federated Cloud that cares about the Resources:
		if(remainResAlloc.isDefined){
			//TODO: send to MatchMakingAgent
			log.info("ResourceRequest {} could not have been allocated completely on the local cloud. " +
				"Forwarding remaining ResourceAllocation {} to MatchMakingAgent!", resourceAlloc, remainResAlloc)
		}
		else log.info("ResourceRequest {} was completely allocated on the local cloud!", resourceAlloc)
	}

	//TODO: Implement in 0.2 Integrated Controllers
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
	 * @param tuples
	 */
	def recvResourceReply(tuples: Vector[(ActorRef, ResourceAlloc)]): Unit = {
	}


//	override def receive(): Receive = {
//		//case message: NetworkResourceMessage	=> message match {
//			//case ResourceInfo(totalRes, availRes)			=> recvResourceInfo(totalRes, availRes)
//
//		//}
//		case _														=> log.error("Unknown message received!")
//	}

	def receive(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceRequest(resourcesToAlloc, ofcIP)		=> recvResourceRequest(resourcesToAlloc, ofcIP)
			case ResourceFederationReply(resourcesAllocated) 	=> recvResourceReply(resourcesAllocated)
		}
		case _														=> log.error("Unknown message received!")
	}


/* Private Methods: */
/* ================ */

	//TODO: if not needed anymore, delete.
	private def stashMessage(): Unit = {
		log.debug("Received Message, before NetworkResourceAgent went online. Stashed message until being online.")
		try {
			stash()
		}
		catch {
			case e: StashOverflowException => log.error("Reached Stash buffer. Received message will be ignored."+
			  e.printStackTrace())
		}
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
	def props(initialHostAlloc: Vector[Host], ovxIP: InetAddress):
	Props = Props(new NetworkResourceAgent(initialHostAlloc, ovxIP))
}
