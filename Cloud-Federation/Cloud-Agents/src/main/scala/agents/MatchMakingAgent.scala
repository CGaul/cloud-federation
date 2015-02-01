package agents

import akka.actor._
import connectors.CloudConfigurator
import datatypes._
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class MatchMakingAgent(cloudConfig: CloudConfigurator, nraSelection: ActorSelection) 
	extends Actor with ActorLogging
{
	
/* Values: */
/* ======= */
	
/* Variables: */
/* ========== */

	private var cloudDiscoveries: Vector[Subscription] = Vector()
	private var federationSubscriptions: Vector[Subscription] = Vector()

	private var auctionedResources: Map[ActorRef, ResourceAlloc] = Map() //TODO: use
	
	// Each ResourceAlloc has exactly one Cloud that manages it (locally or inside a federation):
	/**
	 * Outstanding Mapping of ResourceAlloc to a list of ActorSelections, that are tried one after the other
	 * until a Cloud was found that is able to allocate the ResourceAlloc-Key in a federation with this cloud.
	 * Once this happens, the Key -> Value Mapping can be deleted from fedResOutstanding, as the final
	 * result will be found in fedResCloudAssigns then. 
	 */
	private var fedResOutstanding: Map[ResourceAlloc, List[ActorRef]] = Map()
	/**
	 * Finally Assigned Mapping of ResourceAlloc to the foreign (slave) MMA that was willing to accept
	 * the FederationRequest made from this MMA with the ResourceAlloc-Key.
	 */
	private var fedResCloudAssigns: Map[ResourceAlloc, ActorRef] = Map()
	private var foreignToLocalTenants: Map[Tenant, Tenant] = Map()


/* Methods: */
/* ======== */

	def recvDiscoveryPublication(cloudDiscovery: Subscription): Unit = {
		log.info("MatchMakingAgent received DiscoveryPublication from {}. " +
			"Adding CloudDiscovery to known Clouds...",
      sender())
		cloudDiscoveries = cloudDiscoveries :+ cloudDiscovery
		log.info("Sending own FederationInfoSubscription to foreign MMA...")
		val ownSubscription = Subscription(context.self, cloudConfig.cloudSLA,
																			 cloudConfig.cloudHosts.map(_.sla).toVector, cloudConfig.certFile)
		cloudDiscovery.actorRefMMA ! FederationInfoSubscription(ownSubscription)
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
		log.info("Received ResourceRequest (Tenant: {}, ResCount: {}) from {} at MatchMakingAgent.",
						 tenant, resourcesToGather.resources.size, sender())
		
		if(cloudDiscoveries.size > 0){
			// First, add all previously discovered foreign MMAs into the list of outstanding requests for the
			// current ResourceAlloc that should be solved in a federation from the foreign cloud:
			fedResOutstanding = fedResOutstanding + (resourcesToGather -> cloudDiscoveries.map(_.actorRefMMA).toList)
			sendFederationRequestToNextMMA(tenant, localGWSwitch, resourcesToGather)
		}
	}

	/**
	 * Received from one of the foreign MMAs that want to start a Federation.
	 * The Federation starter MMA will be the master MMA then, this MMA will be the slave (some for NRA).
	 * @param tenant
	 * @param resourcesToAlloc
	 */
	def recvResourceFederationRequest(tenant: Tenant, foreignGWSwitch: OFSwitch, resourcesToAlloc: ResourceAlloc): Unit = {
		log.info("Received ResourceFederationRequest (Tenant: {}, ResCount: {}) from {} at MatchMakingAgent.",
			tenant, resourcesToAlloc.resources.size, sender())

		if(auctionedResources.contains(sender())){
			//TODO: auctionedResources need to be set somewhere before..
			val sendersAvailResources: ResourceAlloc = auctionedResources(sender())
			// Are the requested resources a subset (or the whole set) of the sender's available Resources?
			// This is fulfilled, if no resource is left in the resourceToAlloc, if filtered by its available Resources:
			val leftResToAlloc: Iterable[Resource] = resourcesToAlloc.resources.filter(sendersAvailResources.resources.contains)
			if(leftResToAlloc.size == 0){
        log.info("Federation queried by MMA {}, including ResToAlloc: {} is fully allocateable locally. " +
                 "NRA will allocate resources.",
          sender(), resourcesToAlloc.resources)
        // If Allocation Requirements are met, forward ResourceFederationRequest to NRA,
        // so that it will be mapped to the running OVX instance:
        nraSelection ! ResourceFederationRequest(mapForeignToLocal(tenant), foreignGWSwitch, resourcesToAlloc)

        // If the Resources are allocateable locally, send a ResourceFederationReply back to foreign (master) MMA
        // including local (slave) MMA ActorRef and resourcesToAlloc that will be allocated by (slave) NRA locally.
        sender ! ResourceFederationReply(tenant, localGWSwitch, resourcesToAlloc, wasFederated = true)
			}
			else{
				log.warning("More Resources should be allocated over the Federation than available from won auctions! " +
                    "ResToAlloc: {}, MMA's AuctionedResources: {}", 
									resourcesToAlloc.resources, sendersAvailResources.resources)
				
				// TODO: possibility to only allocate resourcesToAlloc partially.
				// If resourcesToAlloc are not completely allocateable locally, drop the FederationRequest,
				// replying with an empty body in the ResourceFederationReply back to the foreign MMA.
				sender ! ResourceFederationReply(tenant, localGWSwitch, resourcesToAlloc, wasFederated = false)
			}
		}
    else {
      log.warning("No auctionedResources found for foreign MMA {}! Can't allocate any federated resources here.",
                  sender())
      sender ! ResourceFederationReply(tenant, localGWSwitch, resourcesToAlloc, wasFederated = false)
    }
	}

  /**
   * Received from the previously requested foreign (slave) MMA in a possible federation scenario.
   * FederatedResources are equal to the requested resources in the previous ResourceFederationRequest in any case.
   *  
   * @param tenant
   * @param federatedResources The same resources that were sent as a Request to the foreign MMA
   *                           are included in the Reply for correct answer remapping.
   * @param wasFederated
   */
	def recvResourceFederationReply(tenant: Tenant, foreignGWSwitch: OFSwitch, federatedResources: ResourceAlloc, wasFederated: Boolean): Unit = {

		// If no resources were federated at the foreign cloud, simply log and return:
    if(wasFederated){
      // If fedResources were federated, add the federatedResources to the fedResCloudAssigns-Map and send a Federation
      log.info("Received ResourceFederationReply. " +
        "Successfully established a federation with MMA {}. FederatedResources: {}",
        sender(), federatedResources)
      // IMPLICIT: federatedResources is the same ResourceAlloc that was forwarded from this MMA to the foreign MMA before:
      fedResCloudAssigns = fedResCloudAssigns + (federatedResources -> sender())
      nraSelection ! ResourceFederationResult(tenant, foreignGWSwitch, federatedResources)
    }
    else{
      log.warning("Received ResourceFederationReply. " +
        "No federation was established with MMA {}, as no resources were allocated on the foreign cloud!",
        sender())
      log.info("Trying to send ResourceFederationRquest to next foreign MMA in outstanding MMA list...")
      sendFederationRequestToNextMMA(tenant, localGWSwitch, federatedResources)
    }
	}


	def recvFederationInfoSubscription(subscription: Subscription): Unit = {
		log.info("Received FederationInfoSubscription from {}", sender())
		federationSubscriptions = federationSubscriptions :+ subscription
    //TODO: Implement subscription handling
	}

	def recvFederationInfoPublication(possibleFederatedAllocs: Vector[(Host, Vector[ResourceAlloc])]): Unit = {
		log.info("Received FederationInfoPublication from {}", sender())
    // TODO: Implement publication handling
	}

	override def receive: Receive = {
		case message: MMADiscoveryDest => message match {
			case DiscoveryPublication(cloudDiscovery) => recvDiscoveryPublication(cloudDiscovery)
		}
		case message: MMAFederationDest => message match{
			case FederationInfoSubscription(foreignCloudSLA) 	=> recvFederationInfoSubscription(foreignCloudSLA)
			case FederationInfoPublication(possibleAllocs) 		=> recvFederationInfoPublication(possibleAllocs)
		}
		case message: MMAResourceDest	=> message match {
			case ResourceRequest(tenant, resources)
						=> recvResourceRequest(tenant, resources)
				
			case ResourceFederationRequest(tenant, foreignGWSwitch, resources)
						=> recvResourceFederationRequest(tenant, foreignGWSwitch, resources)
				
			case ResourceFederationReply(tenant, foreignGWSwitch, federatedResources, wasFederated)
						=> recvResourceFederationReply(tenant, foreignGWSwitch, federatedResources, wasFederated)

		}
		case _ => log.error("Unknown message received!")
	}

  private def mapForeignToLocal(foreignTenant: Tenant): Tenant = {
    // Use a random ID for the local mapping of the foreign Tenant
    // TODO: test if Tenant-ID already exists:
    val localTenant = Tenant((Math.random * Int.MaxValue).toInt, foreignTenant.subnet,
      foreignTenant.ofcIp, foreignTenant.ofcPort)
    foreignToLocalTenants = foreignToLocalTenants + (foreignTenant -> localTenant)
    return localTenant
  }
	
	private def sendFederationRequestToNextMMA(tenant: Tenant, gwSwitch: OFSwitch, resourcesToGather: ResourceAlloc) = {
		// Find the next MMA from the local MMA's mapping:
		val outstandingMMAList = fedResOutstanding.getOrElse(resourcesToGather, List())
		val nextMMAOpt = outstandingMMAList.headOption
		
		nextMMAOpt match{
		    case Some(nextMMA) => 
					log.info("Sending Federation Request to next MMA in outstanding List. MMA: {}", nextMMA)
									 nextMMA !  ResourceFederationRequest(tenant, gwSwitch, resourcesToGather)
					// Remove the nextMMA that was just requested from the outstanding list:
					fedResOutstanding = fedResOutstanding + (resourcesToGather -> outstandingMMAList.filter(_ != nextMMA))
					
		    case None          =>  
					log.warning("No next MMA is outstanding for the ResourceFederationRequest ({}, {}). " +
											"No FederationRequest will be send!",
											tenant, resourcesToGather.resources)
		}
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
	 * @param cloudConfig The CloudConfigurator that manages all XML-configs for the local Cloud
	 * @return An Akka Properties-Object
	 */
	def props(cloudConfig: CloudConfigurator, nraSelection: ActorSelection):
		Props = Props(new MatchMakingAgent(cloudConfig, nraSelection))
}

