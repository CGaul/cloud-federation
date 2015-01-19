package agents

import java.net.InetAddress

import akka.actor._
import datatypes._
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class MatchMakingAgent(cloudSLA: CloudSLA, nraSelection: ActorSelection) extends Actor with ActorLogging
{
	// TODO: Shortcut - Better handle in DB:
	var cloudDiscoveries: Vector[Subscription] = Vector()
	var federationSubscriptions: Vector[(ActorRef, CloudSLA)] = Vector()
	var auctionedResources: Map[ActorRef, ResourceAlloc] = Map()

/* Methods: */
/* ======== */

	def recvDiscoveryPublication(cloudDiscovery: Subscription): Unit = {
		log.info("MatchMakingAgent received DiscoveryPublication. " +
			"Subscribing on FederationInfo about that Cloud via other MMA...")
		cloudDiscoveries = cloudDiscoveries :+ cloudDiscovery
		cloudDiscovery.cloudMMA ! FederationInfoSubscription(cloudSLA)
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
	 * @param resourcesToGather The ResourceAllocation that the MMA needs to gather from its federated Clouds.
	 */
	def recvResourceRequest(tenant: Tenant, resourcesToGather: ResourceAlloc): Unit = {
		log.info("Received ResourceRequest (Tenant: {}, ResCount: {}) at MatchMakingAgent.",
						 tenant, resourcesToGather.resources.size)
		// TODO: Shortcut - Implement more specific:
		// Shortcut: Forward ResourceRequest to previously published Cloud:
		if(cloudDiscoveries.size > 0){
			// Send a ResourceFederationRequest to the other Cloud's MMA immediately:
			cloudDiscoveries(0).cloudMMA ! ResourceFederationRequest(tenant, resourcesToGather)
		}
	}

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Received from one of the foreign MatchMakingAgents
	 * that this Agent queried before.
	 * @param resourceAlloc
	 */
	def recvResourceReply(resourceAlloc: ResourceAlloc): Unit = ???


	def recvResourceFederationRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc): Unit = {
		log.info("Received ResourceFederationRequest (Tenant: {}, ResCount: {}) at MatchMakingAgent.",
			tenant, resourcesToAlloc.resources.size)
		// TODO: Shortcut - Implement more specific:
		if(auctionedResources.contains(sender())){
			val sendersAvailResources: ResourceAlloc = auctionedResources(sender())
			// Are the requested resources a subset (or the whole set) of the sender's available Resources?
			// This is fulfilled, if no resource is left in the resourceToAlloc, if filtered by its available Resources:
			val leftResToAlloc: Iterable[Resource] = resourcesToAlloc.resources.filter(sendersAvailResources.resources.contains)
			if(leftResToAlloc.size > 0){
				log.error("More Resources should be allocated over the Federation than available from won auctions!" +
									" ResToAlloc: %s, Sender's AuctionedResources: %s", resourcesToAlloc.resources, sendersAvailResources.resources)
			}
			else{
				// If Allocation Requirements are met, forward ResourceFederationRequest to NRA,
				// so that it will be mapped to the running OVX instance:
				nraSelection ! ResourceFederationRequest(tenant, resourcesToAlloc)
			}
		}
		// Shortcut: Forward ResourceFederationRequest to local NRA,
		// TODO: implement.
	}

	def recvResourceFederationReply(allocatedResources: Vector[(ActorRef, ResourceAlloc)]): Unit = ???


	def recvFederationInfoSubscription(otherCloudSLA: CloudSLA): Unit = {
		log.info("Received FederationInfoSubscription from {}", sender())
		federationSubscriptions = federationSubscriptions :+ (sender(), otherCloudSLA)
	}

	def recvFederationInfoPublication(possibleFederatedAllocs: Vector[(Host, Vector[ResourceAlloc])]): Unit = {
		log.info("Received FederationInfoPublication from {}", sender())
	}

	override def receive(): Receive = {
		case message: MMADiscoveryDest => message match {
			case DiscoveryPublication(cloudDiscovery) => recvDiscoveryPublication(cloudDiscovery)
		}
		case message: MMAFederationDest => message match{
			case FederationInfoSubscription(cloudSLA) 			=> recvFederationInfoSubscription(cloudSLA)
			case FederationInfoPublication(possibleAllocs) 	=> recvFederationInfoPublication(possibleAllocs)
		}
		case message: MMAResourceDest	=> message match {
			case ResourceRequest(tenant, resources)
						=> recvResourceRequest(tenant, resources)
				
			case ResourceReply(allocResources)
						=> recvResourceReply(allocResources)
				
			case ResourceFederationRequest(tenant, resources)
						=> recvResourceFederationRequest(tenant, resources)
				
			case ResourceFederationReply(allocatedResources)
						=> recvResourceFederationReply(allocatedResources)

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
	def props(cloudSLA: CloudSLA, nraSelection: ActorSelection):
		Props = Props(new MatchMakingAgent(cloudSLA, nraSelection))
}

