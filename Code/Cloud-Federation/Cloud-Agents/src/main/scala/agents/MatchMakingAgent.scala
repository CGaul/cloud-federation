package agents

import java.net.InetAddress

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import datatypes.{Host, ResourceAlloc}
import messages.{ResourceReply, ResourceFederationReply, ResourceRequest, NetworkResourceMessage}

/**
 * @author Constantin Gaul, created on 5/31/14.
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
	 * @param resourceAlloc
	 */
	def recvResourceRequest(resourceAlloc: ResourceAlloc, address: InetAddress): Unit = {
		log.info("Received ResourceRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at MatchMakingAgent.",
						 resourceAlloc.tenantID, resourceAlloc.resources.size, address)
	}

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Received from one of the foreign MatchMakingAgents
	 * that this Agent queried before.
	 * @param resourceAlloc
	 */
	def recvResourceReply(resourceAlloc: ResourceAlloc): Unit = ???


	override def receive(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceRequest(resources, ofcIP)		=> recvResourceRequest(resources, ofcIP)
			case ResourceReply(allocResources) 			=> recvResourceReply(allocResources)
		}
		case _										=> log.error("Unknown message received!")
	}
}

/**
 * Companion Object of the MatchMaking-Agent,
 * in order to implement some default behaviours
 */
object MatchMakingAgent
{
	/**
	 * props-method is used in the AKKA-Context, spawning a new Agent.
	 * In this case, to generate a new NetworkResource Agent, call
	 * 	val ccfmProps = Props(classOf[NetworkResourceAgent], args = ovxIP)
	 * 	val ccfmAgent = system.actorOf(ccfmProps, name="NetworkResourceAgent-x")
	 * @param ovxIP The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
	 * @return An Akka Properties-Object
	 */
	def props(initialHostAlloc: Vector[Host], ovxIP: InetAddress, matchMakingAgent: ActorRef):
	Props = Props(new MatchMakingAgent(initialHostAlloc, ovxIP, matchMakingAgent))
}

