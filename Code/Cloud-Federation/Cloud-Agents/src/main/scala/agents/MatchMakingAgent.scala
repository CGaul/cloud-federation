package agents

import java.io.File
import java.net.InetAddress

import akka.actor.{Props, ActorRef, Actor, ActorLogging}
import datatypes.{HostSLA, CloudSLA, Host, ResourceAlloc}
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class MatchMakingAgent(cloudSLA: CloudSLA) extends Actor with ActorLogging
{
	// TODO: Shortcut - Better handle in DB:
	var discoveredClouds: Vector[(ActorRef, CloudSLA, Vector[HostSLA], File)] = Vector()

/* Methods: */
/* ======== */

	def recvDiscoveryPublication(discoveredCloud: (ActorRef, CloudSLA, Vector[HostSLA], File)): Unit = {
		log.info("MatchMakingAgent received DiscoveryPublication. " +
			"Subscribing on FederationInfo about that Cloud soon.")
		discoveredClouds = discoveredClouds :+ discoveredCloud
	}

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
		// TODO: Shortcut - Implement more specific:
		// Shortcut: Forward ResourceRequest to previously published Cloud:
		if(discoveredClouds.size > 0){
			// Send a ResourceFederationRequest to the other Cloud's MMA immediately:
			discoveredClouds(0)._1 ! ResourceFederationRequest(resourceAlloc, address)
		}
	}

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Received from one of the foreign MatchMakingAgents
	 * that this Agent queried before.
	 * @param resourceAlloc
	 */
	def recvResourceReply(resourceAlloc: ResourceAlloc): Unit = ???


	def recvResourceFederationRequest(resourceAlloc: ResourceAlloc, address: InetAddress): Unit = {
		log.info("Received ResourceFederationRequest (TenantID: {}, ResCount: {}, OFC-IP: {}) at MatchMakingAgent.",
			resourceAlloc.tenantID, resourceAlloc.resources.size, address)
		// TODO: Shortcut - Implement more specific:
		// Shortcut: Forward ResourceFederationRequest to local NRA,
		// so that it will be mapped to the running OVX instance.:
		// TODO: implement.
	}


	override def receive(): Receive = {
		case message: MMADiscoveryDest => message match {
			case DiscoveryPublication(discoveredCloud) => recvDiscoveryPublication(discoveredCloud)
		}
		case message: MMAResourceDest	=> message match {
			case ResourceRequest(resources, ofcIP)	=> recvResourceRequest(resources, ofcIP)
			case ResourceReply(allocResources) 			=> recvResourceReply(allocResources)
			case ResourceFederationRequest(resources, ofcIP)	=> recvResourceFederationRequest(resources, ofcIP)

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
	 * @param cloudSLA The CloudSLA that is managed by the Cloud's CCFM.
	 * @return An Akka Properties-Object
	 */
	def props(cloudSLA: CloudSLA):
		Props = Props(new MatchMakingAgent(cloudSLA))
}

