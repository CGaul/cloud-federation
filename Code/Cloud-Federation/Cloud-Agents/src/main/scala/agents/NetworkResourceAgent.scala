package agents

import java.net.InetAddress

import akka.actor.{ActorRef, ActorLogging, Actor}
import datatypes.Resources
import messages.{ResourceRequest, ResourceFederationReply, NetworkResourceMessage}

/**
 * Created by costa on 10/15/14.
 */
class NetworkResourceAgent extends Actor with ActorLogging
{

	/* Methods: */
	/* ======== */

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Receives ResourceRequests from CCFM.
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
	 * @param resources
	 * @param address
	 */
	def recvResourceRequest(resources: Resources, address: InetAddress): Unit = {

	}

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Receives a ResourceFederationReply from the MatchMakingAgent.
	 * <p>
	 * 	If a ResourceRequest could not have been processed locally,
	 * 	the NetworkFederationAgent asked the MatchMakingAgent
	 * 	for Federation-Resources.
	 * 	All results are included in such ResourceFederationReply,
	 * 	stating the allocated Resources per foreign Cloud
	 * 	(the ActorRef is the foreign MatchMakingAgent)
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * @param tuples
	 */
	def recvResourceReply(tuples: Vector[(ActorRef, Resources)]): Unit = {

	}


	override def receive(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceRequest(resources, ofcIP)			=> recvResourceRequest(resources, ofcIP)
			case ResourceFederationReply(allocResources) => recvResourceReply(allocResources)
		}
		case _										=> log.error("Unknown message received!")
	}
}
