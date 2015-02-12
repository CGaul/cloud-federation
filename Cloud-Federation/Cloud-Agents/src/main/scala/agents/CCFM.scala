package agents

import java.io.File

import akka.actor._
import connectors.CloudConfigurator
import datatypes._
import messages._


/**
 * @author Constantin Gaul, created on 5/27/14.
 */
class CCFM(pubSubActorSelection: ActorSelection, cloudConfDir: File) 
	extends Actor with ActorLogging
{

/* Values: */
/* ======= */

	// The cloudconfig as a dir of XML files:
	val cloudConfig: CloudConfigurator = CloudConfigurator(cloudConfDir)
	
	// Akka Child-Actor spawning:
	val mmaSelection: ActorSelection			= context.actorSelection("akka://cloudAgentSystem/user/CCFM/matchMakingAgent")
	val nraSelection: ActorSelection			= context.actorSelection("akka://cloudAgentSystem/user/CCFM/networkResourceAgent")
	val discoveryAgentProps: Props 				= Props(classOf[DiscoveryAgent], cloudConfig,
																								pubSubActorSelection, mmaSelection)
	val discoveryAgent: ActorRef 					= context.actorOf(discoveryAgentProps, name="discoveryAgent")
	log.info("DiscoveryAgent started at path: {}.", discoveryAgent.path)

	
	val matchMakingAgentProps: Props 			= Props(classOf[MatchMakingAgent],cloudConfig, 
                                                pubSubActorSelection, nraSelection)
	val matchMakingAgent: ActorRef				= context.actorOf(matchMakingAgentProps, name="matchMakingAgent")
	log.info("MatchMakingAgent started at path: {}.", matchMakingAgent.path)


	val networkResourceAgentProps: Props 	= Props(classOf[NetworkResourceAgent], cloudConfig,
																								matchMakingAgent)
	val networkResourceAgent: ActorRef 		= context.actorOf(networkResourceAgentProps, name="networkResourceAgent")
	log.info("NetworkResourceAgent started at path: {}.", networkResourceAgent.path)



/* Variables: */
/* ========== */

	var foreignDiscoveryActors: Vector[ActorPath] = Vector()
	var cloudFederationMatches: Vector[(ActorPath, ResourceAlloc)] = Vector()
  
  
/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

  /**
   * Received from Tenant side
   * @param resToAlloc
   */
	def recvTenantRequest(resToAlloc: ResourceAlloc): Unit = {
		log.info("CCFM Received TenantRequest from Tenant-ID {}. Forwarding to NRA, after Tenant is resolved...",
			resToAlloc.tenantID)

		log.info("Fetching Tenant from internal Config file...")
		val tenantOpt = cloudConfig.cloudTenants.find(_.id == resToAlloc.tenantID)
		tenantOpt match{
			case Some(tenant) =>
					log.info("Successfully resolved tenant {}. Forwarding TenantRequest to NRA as ResourceRequest...", tenant)
					networkResourceAgent ! ResourceRequest(tenant, resToAlloc)
			case None => log.error("Tenant was not resolved for Tenant-ID {} in TenantRequest!", resToAlloc.tenantID)
		}
	}

  /**
   * Received from Tenant side 
   * @param tenant
   * @param resToAlloc
   */
	def recvResourceRequest(tenant: Tenant, resToAlloc: ResourceAlloc): Unit = {
		log.info("CCFM Received ResourceRequest from Tenant {}. Forwarding to NRA...",
		resToAlloc.tenantID)

		networkResourceAgent ! ResourceRequest(tenant, resToAlloc)
	}

  /**
   * Received from NRA 
   * @param resources
   */
  def recvResourceReply(resources: ResourceAlloc): Unit = {
    log.info("ResourceReply received from {} for tenant {}.", sender(), resources.tenantID)
  }


	override def receive(): Receive = {
		case DiscoveryAck(status)		=> recvDiscoveryStatus(status)
		case DiscoveryError(status)	=> recvDiscoveryError(status)
		case message: CCFMResourceDest	=> message match {
			case TenantRequest(resToAlloc)						=> recvTenantRequest(resToAlloc)
			case ResourceRequest(tenant, resToAlloc) 	=> recvResourceRequest(tenant, resToAlloc)
			case ResourceReply(allocResources)				=> recvResourceReply(allocResources)
		}
		case _								=> log.error("Unknown message received!")
	}

	def recvDiscoveryStatus(status: String) = {
		log.info("Discovery Status \""+ status + "\" received.")
	}

	def recvDiscoveryError(error: String) = {
		log.error("Discovery Error \""+ error + "\" received.")
	}
}


/**
 * Companion Object of the CCFM-Agent,
 * in order to implement some default behaviours
 */
object CCFM
{
	/**
	 * props-method is used in the AKKA-Context, spawning a new Agent.
	 * In this case, to generate a new CCFM Agent, call
	 * 	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	 * 	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM-x")
	 * @param pubSubServerAddr The akka.tcp connection, where the PubSub-Federator-Agents is listening.
	 * @return An Akka Properties-Object
	 */
	def props(pubSubServerAddr: ActorSelection, cloudConfDir: File):
		Props = Props(new CCFM(pubSubServerAddr, cloudConfDir))
}
