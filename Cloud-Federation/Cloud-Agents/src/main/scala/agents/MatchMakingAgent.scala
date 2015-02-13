package agents

import agents.cloudfederation.RemoteDependencyAgent
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import connectors.CloudConfigurator
import datatypes._
import messages._

import scala.concurrent.duration._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class MatchMakingAgent(cloudConfig: CloudConfigurator, 
                       federatorActorSel: ActorSelection, nraActorSel: ActorSelection)
	extends RemoteDependencyAgent(List(federatorActorSel, nraActorSel)) with ActorLogging
{
	
/* Values: */
/* ======= */

  val localGWSwitch = cloudConfig.cloudGateway

	
/* Variables: */
/* ========== */

	private var cloudDiscoveries: Vector[Subscription] = Vector()
	private var federationSubscriptions: Vector[(Subscription, ActorRef)] = Vector()
  private var federatedOvxInstance: Option[OvxInstance] = None

  /**
   * Known from a FederateableResourceReply from the NRA to this MMA, so that this MMA knows, which ResourceAllocs
   * are available to be auctioned to foreign MMAs. 
   */
  private var federateableResources: Map[(Host, ResourceAlloc), Boolean] = Map()
  /**
   * Used by the auctioneer side of this MMA: tells, which foreign MMAs have won auctioned ResourceAllocs at this MMA- 
   */
	private var auctionedResources: Map[ActorRef, ResourceAlloc] = Map()
  /**
   * Used by the bidder side of this MMA: tells, which foreign ResourceAllocs are won by this MMA. 
   */
  private var wonResources: Map[ResourceAlloc, ActorRef] = Map()
	
  
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

    // Only try to federated, if foreign clouds are known:
		if(cloudDiscoveries.size > 0){
			// First, add all previously discovered foreign MMAs into the list of outstanding requests for the
			// current ResourceAlloc that should be solved in a federation from the foreign cloud:
			fedResOutstanding = fedResOutstanding + (resourcesToGather -> cloudDiscoveries.map(_.actorRefMMA).toList)

      log.info("Sending ResourceFederationRequest to next MMA...")
      sendFederationRequestToNextMMA(tenant, localGWSwitch, resourcesToGather)
		}
	}

	/**
	 * Received from one of the foreign MMAs that want to start a Federation.
	 * The Federation starter MMA will be the master MMA then, this MMA will be the slave (some for NRA).
	 * @param tenant
	 * @param resourcesToAlloc
	 */
	def recvResourceFederationRequest(tenant: Tenant, foreignGWSwitch: OFSwitch, 
                                    resourcesToAlloc: ResourceAlloc, ovxInstance: OvxInstance): Unit = {
		log.info("Received ResourceFederationRequest (Tenant: {}, ResCount: {}) from {} at MatchMakingAgent.",
			tenant, resourcesToAlloc.resources.size, sender())

		if(auctionedResources.contains(sender())){
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
        nraActorSel ! ResourceFederationRequest(mapForeignToLocal(tenant), foreignGWSwitch, resourcesToAlloc, ovxInstance)

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
      federatedOvxInstance match{
          case Some(ovxInstance) =>
            log.info("Sending successful federation establishment as a ResourceFederationResult to local NRA...")
            nraActorSel ! ResourceFederationResult(tenant, foreignGWSwitch, federatedResources, ovxInstance)
            
          case None =>
            log.error("No OVX-F was available, on sending a ResourceFederationResult to local NRA for tenant {}!",
                      tenant.id)
      }
      
    }
    else{
      log.warning("Received ResourceFederationReply. " +
        "No federation was established with MMA {}, as no resources were allocated on the foreign cloud!",
        sender())
      log.info("Trying to send ResourceFederationRquest to next foreign MMA in outstanding MMA list...")
      sendFederationRequestToNextMMA(tenant, localGWSwitch, federatedResources)
    }
	}

  /**
   * Received from local NRA after this MMA send a FederateableResourceRequest to him.
   * @param fedDiscovery
   */
  def recvFederateableResourceDiscovery(fedDiscovery: Vector[(Host, ResourceAlloc)]) = {
    log.info("Received FederateableResourceDiscovery from {}", sender())
    for ((actHost, hostResAlloc) <- fedDiscovery ) {
      if(! federateableResources.contains((actHost, hostResAlloc))){
        log.info("Initiated federateableResource {}, received from NRA", (actHost, hostResAlloc))
        federateableResources = federateableResources + ((actHost, hostResAlloc) -> false)
      }
    }
  }


  /**
   * Received from foreign MMA that wants to subscribe with this MMA
   * @param subscription
   */
	def recvFederationInfoSubscription(subscription: Subscription): Unit = {
		log.info("Received FederationInfoSubscription from {}", sender())
		federationSubscriptions = federationSubscriptions :+ (subscription, sender())
    
    // Preparing any federeateableResources for the InfoPublication:
    val possibleFedAllocs = federateableResources.map(_._1).toVector
    log.info("Sending FederationInfoPublication {} back to subscriber.", possibleFedAllocs)
    sender() ! FederationInfoPublication(possibleFedAllocs)
	}

  /**
   * Received from foreign MMA that accepts subscription with this MMA and sends periodic information about foreign
   * federateable status 
   * 
   * An InfoPublication acts as a initiator of Resource-Auctioning for all possibleFederatedAllocs that is included
   * in the message. 
   * @param possibleFedAllocs
   */
	def recvFederationInfoPublication(possibleFedAllocs: Vector[(Host, ResourceAlloc)]): Unit = {
		log.info("Received FederationInfoPublication from {}", sender())
    
    // Only answer senders of a FederationInfoPublications, that were asked to act as subscribers via a FederationInfoSubscription first:
    if(cloudDiscoveries.exists(_.actorRefMMA == sender())) {
      log.info("FederationInfoPublication was received from previously discovered Cloud subscriber. Sending ResourceAuctionBid back...")
      //TODO: Implement iBundle - Shortcut implementation: For each ResAlloc, simply bid something on some ResourceAllocs:
      for ((actHost, hostResAlloc) <- possibleFedAllocs) {
        // Send a random amount of CLOUD_CREDITS as askPrice for given ResAlloc to auctioneer:
        sender() ! ResourceAuctionBid(actHost, hostResAlloc, Price(Math.random().toFloat, CloudCurrency.CLOUD_CREDIT))
      }
    }
	}

  /**
   * Received from foreign MMA that answers on a previous FederationInfoPublication, send by this MMA.
   * As the FederationInfoPublication indicates to start an auction for all ResourceAllocs, contained in the message,
   * all subscribed MMAs are allowed to reply with ResourceAuctionBids to it. This MMA has to decide on the ask Price,
   * which of the subscribed MMAs wins each federateable resource. 
   * @param resourceBid
   * @param askPrice
   * @return
   */
  def recvResourceAuctionBid(resourceHost: Host, resourceBid: ResourceAlloc, askPrice: Price) = {
    log.info("Received ResourceAuctionBid from {}. Bid on {} with askPrice: {}",
             sender(), resourceBid, askPrice)
    
    if(federationSubscriptions.exists(_._2 == sender())) {
      log.info("AuctionBid was received from known subscriber MMA: {}", sender())
      //TODO: Implement iBundle - Shortcut implementation: The first incoming Request wins.
      if (federateableResources.contains((resourceHost, resourceBid)) && federateableResources((resourceHost, resourceBid))) {
        sender() ! ResourceAuctionResult(resourceBid, won = true)
        auctionedResources = auctionedResources + (sender() -> resourceBid)
      }
      else
        sender() ! ResourceAuctionResult(resourceBid, won = false)
    }
  }

  /**
   * Received from foreign MMA that acts as the auctioneer for a previously initiated auction. This MMA has send a 
   * ResourceAuctionBid before and the foreign MMA replies with a ResourceAuctionResult to each received ResourceAuctionBid.
   * The message tells this MMA, whether he has won the bidded ResourceAlloc for the previously defined askPrice.
   * @param resourceBid
   * @param won
   * @return
   */
  def recvResourceAuctionResult(resourceBid: ResourceAlloc, won: Boolean) = {
    log.info("Received ResourceAuctionResult from {}. Won the ResAlloc? {}.", sender(), won)
    if(won) {
      log.info("Won ResourceAuction with {}!", resourceBid)
      wonResources = wonResources + (resourceBid -> sender())
    }
  }

  
  /**
   * The online receive-handle that needs to be implemented by the specified class, extending this RemoteDependencyAgent.
   * Contains the functionality, which will be executed by the Actor if all RemoteDependencies are solved and a message
   * comes into the mailbox, or there were stashed messages while the Actor was in its _offline state.
   * @return
   */
  override def receiveOnline: Receive = {
		case message: MMADiscoveryDest => message match {
			case DiscoveryPublication(cloudDiscovery)         => recvDiscoveryPublication(cloudDiscovery)
		}
		case message: MMAFederationDest => message match{
      case FederateableResourceDiscovery(fedResources)  => recvFederateableResourceDiscovery(fedResources)
			case FederationInfoSubscription(foreignCloudSLA) 	=> recvFederationInfoSubscription(foreignCloudSLA)
			case FederationInfoPublication(possibleAllocs) 		=> recvFederationInfoPublication(possibleAllocs)
      case ResourceAuctionBid(host, resBid, askPrice)   => recvResourceAuctionBid(host, resBid, askPrice)
      case ResourceAuctionResult(resourceBid, won)      => recvResourceAuctionResult(resourceBid, won)
      case OvxInstanceReply(ovxInstance) => log.error("OvxInstanceReply should not be received here!")
		}
		case message: MMAResourceDest	=> message match {
			case ResourceRequest(tenant, resources)
						=> recvResourceRequest(tenant, resources)
				
			case ResourceFederationRequest(tenant, foreignGWSwitch, resources, ovxInstance)
						=> recvResourceFederationRequest(tenant, foreignGWSwitch, resources, ovxInstance)
				
			case ResourceFederationReply(tenant, foreignGWSwitch, federatedResources, wasFederated)
						=> recvResourceFederationReply(tenant, foreignGWSwitch, federatedResources, wasFederated)

		}
		case message => log.error("Unknown message {} received!", message.toString)
	}
  
  override def becomeOnline = {
    //Import implicit ExecutionContext for onSuccess and onFailure. Uses default ThreadPool for ExecContext:
    import scala.concurrent.ExecutionContext.Implicits.global

    // Send an OVXInstanceRequest to the PubSub-Federator,
    // in order to receive an OvxInstanceReply here at the MMA:
    // (Do this only once! Only ove OVX-F is needed per Cloud)
    val localSubscr = Subscription(context.self, cloudConfig.cloudSLA,
      cloudConfig.cloudHosts.map(_.sla).toVector, cloudConfig.certFile)
    log.info("Asking the PubSub-Federator for an OVX Instance now, " +
      "that will host any outgoing federation where this cloud would be the federation master.")
    
    val futureOvxReply = federatorActorSel.ask(OvxInstanceRequest(localSubscr)) (Timeout(15 seconds).duration)
    futureOvxReply onSuccess  {
      case ovxReply: OvxInstanceReply =>
        log.info("Received asked OvxInstanceReply {} from Federator!",
                 ovxReply.ovxInstance)
        federatedOvxInstance = Some(ovxReply.ovxInstance)
    }
    
    futureOvxReply onFailure {
      case _ => log.error("No asked OvxInstanceReply could be received from the federator in an async future!")
    }
  }
  
  
  
/* Private Methods: */
/* ================ */

  private def mapForeignToLocal(foreignTenant: Tenant): Tenant = {
    // Use a random ID for the local mapping of the foreign Tenant
    // TODO: test if newly generated, local Tenant-ID already exists:
    val localTenant = Tenant((Math.random * Int.MaxValue).toInt, foreignTenant.subnet,
      foreignTenant.ofcIp, foreignTenant.ofcPort)
    foreignToLocalTenants = foreignToLocalTenants + (foreignTenant -> localTenant)
    return localTenant
  }
	
	private def sendFederationRequestToNextMMA(tenant: Tenant, gwSwitch: OFSwitch, 
                                             resourcesToGather: ResourceAlloc) = {
		// Find the next MMA from the local MMA's mapping:
		val outstandingMMAList = fedResOutstanding.getOrElse(resourcesToGather, List())
		val nextMMAOpt = outstandingMMAList.headOption
		
		nextMMAOpt match{
		    case Some(nextMMA) => 
          federatedOvxInstance match{
            case Some(ovxInstance) =>
              log.info("Sending Federation Request to next MMA in outstanding List. MMA: {}", nextMMA)
              nextMMA !  ResourceFederationRequest(tenant, gwSwitch, resourcesToGather, ovxInstance)
                // Remove the nextMMA that was just requested from the outstanding list:
              fedResOutstanding = fedResOutstanding + (resourcesToGather -> outstandingMMAList.filter(_ != nextMMA))
              
            case None => 
              log.error("No OVX-F was available, on sending a FederationRequest to next MMA {} for tenant {}!",
                        nextMMA, tenant.id)
          }
					
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
	def props(cloudConfig: CloudConfigurator, federatorActorSel: ActorSelection, nraActorSel: ActorSelection):
		Props = Props(new MatchMakingAgent(cloudConfig, federatorActorSel, nraActorSel))
}

