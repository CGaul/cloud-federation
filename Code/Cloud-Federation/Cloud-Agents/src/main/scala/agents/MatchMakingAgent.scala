package agents

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging}
import datatypes.ResourceAlloc
import messages.{ResourceReply, ResourceFederationReply, ResourceRequest, NetworkResourceMessage}

/**
 * Created by costa on 5/31/14.
 */
class MatchMakingAgent extends Actor with ActorLogging
{

/* Methods: */
/* ======== */

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Received from NetworkResourceAgent.
	 * <p>
	 *    When a ResourceReply arrives, the MatchMakingAgent has to gather
	 *    the requested resources from the currently known Federation-Clouds.
	 *    Two possibilities here:
	 *    <ul>
	 *       <li>Proactive: A bargaining was done before, so that
	 *    		 the MatchMakingAgent clearly knows to which federated Clouds to connect</li>
	 *    	<li>Reactive: The bargaining only occurs when needed, say when
	 *    		 a ResourceReply drops in.	This causes the Reply to be delayed,
	 *    		 however the bargaining is more accurate, as only the really needed
	 *    		 Resources are on the table. </li>
	 *    <ul>
	 * </p>
	 * @param resources
	 */
	def recvResourceRequest(resources: ResourceAlloc, address: InetAddress): Unit = ???

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Received from one of the foreign MatchMakingAgents
	 * that this Agent queried before.
	 * @param resources
	 */
	def recvResourceReply(resources: ResourceAlloc): Unit = ???


	override def receive(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceRequest(resources, ofcIP)		=> recvResourceRequest(resources, ofcIP)
			case ResourceReply(allocResources) 			=> recvResourceReply(allocResources)
		}
		case _										=> log.error("Unknown message received!")
	}
}
