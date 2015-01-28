package agents

import akka.actor._
import datatypes._
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class MatchMakingAgent(cloudSLA: CloudSLA, nraSelection: ActorSelection) extends Actor with ActorLogging
{
/* Variables: */
/* ========== */

	private var cloudDiscoveries: Vector[Subscription] = Vector()
	private var federationSubscriptions: Vector[(ActorRef, CloudSLA)] = Vector()

	private var auctionedResources: Map[ActorRef, ResourceAlloc] = Map()

	private var assignedFedResources: Map[ActorRef, ResourceAlloc] = Map()
	private var outstandingFedAnswers: Map[ActorRef, Boolean] = Map()


/* Methods: */
/* ======== */

	def recvDiscoveryPublication(cloudDiscovery: Subscription): Unit = {
		log.info("MatchMakingAgent received DiscoveryPublication. " +
			"Subscribing on FederationInfo about that Cloud via other MMA...")
		cloudDiscoveries = cloudDiscoveries :+ cloudDiscovery
		cloudDiscovery.actorSelMMA ! FederationInfoSubscription(cloudSLA)
	}

	/**
	 * Received from NetworkResourceAgent.
	 * <p>
	 *    When a ResourceRequest arrives, the MatchMakingAgent has to gather
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
			val lastMMA = cloudDiscoveries(0).actorSelMMA
			cloudDiscoveries(0).actorSelMMA ! ResourceFederationRequest(tenant, resourcesToGather)
			outstandingFedAnswers = outstandingFedAnswers + (lastMMA -> false)
		}
	}

//
//	//TODO: Implement or Delete in 0.3 - Federation-Agents
//	/**
//	 * Received from one of the foreign MatchMakingAgents
//	 * that this Agent queried before.
//	 * @param resourceAlloc
//	 */
//	def recvResourceReply(resourceAlloc: ResourceAlloc): Unit = ???


	/**
	 * Received from one of the foreign MMAs that want to start a Federation.
	 * The Federation starter MMA will be the master MMA then, this MMA will be the slave (some for NRA).
	 * @param tenant
	 * @param resourcesToAlloc
	 */
	def recvResourceFederationRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc): Unit = {
		log.info("Received ResourceFederationRequest (Tenant: {}, ResCount: {}) at MatchMakingAgent.",
			tenant, resourcesToAlloc.resources.size)

		if(auctionedResources.contains(sender())){
			//TODO: auctionedResources need to be set somewhere before..
			val sendersAvailResources: ResourceAlloc = auctionedResources(sender())
			// Are the requested resources a subset (or the whole set) of the sender's available Resources?
			// This is fulfilled, if no resource is left in the resourceToAlloc, if filtered by its available Resources:
			val leftResToAlloc: Iterable[Resource] = resourcesToAlloc.resources.filter(sendersAvailResources.resources.contains)
			if(leftResToAlloc.size > 0){
				log.warning("More Resources should be allocated over the Federation than available from won auctions!" +
									" ResToAlloc: {}, MMA's AuctionedResources: {}", 
									resourcesToAlloc.resources, sendersAvailResources.resources)
				
				// TODO: possibility to only allocate resourcesToAlloc partially.
				// If resourcesToAlloc are not completely allocateably locally, drop the FederationRequest,
				// replying with an empty body in the ResourceFederationReply back to the foreign MMA.
				sender ! ResourceFederationReply(context.self, None)
			}
			else{
				log.info("Federation queried by MMA {}, including ResToAlloc: {} will be allocated locally.", 
								 sender(), resourcesToAlloc.resources)
				// If Allocation Requirements are met, forward ResourceFederationRequest to NRA,
				// so that it will be mapped to the running OVX instance:
				nraSelection ! ResourceFederationRequest(tenant, resourcesToAlloc)
				
				// If the Resources are allocateable locally, send a ResourceFederationReply back to foreign (master) MMA
				// including local (slave) MMA ActorRef and resourcesToAlloc that will be allocated by (slave) NRA locally.
				sender ! ResourceFederationReply(context.self, Some(resourcesToAlloc))
			}
		}
	}

	def recvResourceFederationReply(slaveMMA: ActorRef, federatedResourcesOpt: Option[ResourceAlloc]): Unit = {

		// If no resources were federated at the foreign cloud, simply log and return:
		federatedResourcesOpt match{
			case Some(fedResources)	=>
				// If fedResources were federated, add the federatedResources to the assignedFedResources-Map and send a Federation
				log.info("Received ResourceFederationReply:" +
					"Successfully established a federation with MMA {}. FederatedResources: {}",
					slaveMMA, fedResources)
				assignedFedResources = assignedFedResources + (slaveMMA -> fedResources)
				
			case None								=>
				log.warning("Received ResourceFederationReply: " +
					"No federation was established with MMA {}, as no resources were allocated on the foreign cloud!",
					slaveMMA)
		}

		// Update the outstandingFedAnswers (slaveMMA answered -> true) and send a FederationSummary back to the NRA,
		// if all outstandingAnswers were answered by the foreign MMAs. If so, clear the outstandingFedAnswers-Map afterwards.
		outstandingFedAnswers = outstandingFedAnswers + (slaveMMA -> true)

		if(! outstandingFedAnswers.exists(_._2 == false)){
			log.info("All outstanding Federation Answers were Replied. Sending FederationSummary back to NRA..")
			nraSelection ! ResourceFederationSummary(assignedFedResources)

			outstandingFedAnswers = outstandingFedAnswers.empty
		}
	}


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

				//TODO: implement or delete
//			case ResourceReply(allocResources)
//						=> recvResourceReply(allocResources)
				
			case ResourceFederationRequest(tenant, resources)
						=> recvResourceFederationRequest(tenant, resources)
				
			case ResourceFederationReply(slaveMMA, slaveResources)
						=> recvResourceFederationReply(slaveMMA, slaveResources)

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

