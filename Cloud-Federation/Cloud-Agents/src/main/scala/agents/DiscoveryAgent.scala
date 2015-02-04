package agents

import agents.cloudfederation.RemoteDependencyAgent
import akka.actor._
import connectors.CloudConfigurator
import datatypes.{CloudSLA, HostSLA, Subscription}
import messages._


/**
 * This agent is able to connect to the Discovery-Subscription-Service
 * and establishes a communication with the CCFM as its Supervisor.
 * @author Constantin Gaul created on 5/27/14.
 */
class DiscoveryAgent(cloudConfig: CloudConfigurator, 
										 pubSubActorSelection: ActorSelection, matchMakingActorSelection: ActorSelection) 
								extends RemoteDependencyAgent(List(pubSubActorSelection, matchMakingActorSelection)) with ActorLogging
									//TODO: change cert type to "Certificate"
									//extends RemoteDependencyAgent(Vector(pubSubActorSelection, matchMakingActorSelection))
									//with ActorLogging
{

/* Variables: */
/* ========== */ 
	
	var discoveryActors: Vector[ActorPath] = Vector()


/* Execution: */
/* ========== */


/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receiveOnline: Receive = {
		case message: DDADiscoveryDest	=> message match {
			case FederationSLAs(cloudSLA, possibleHostSLAs)	=> revcFederationSLAs(cloudSLA, possibleHostSLAs)
			case AuthenticationInquiry(hashKey)							=> recvAuthenticationInquiry(hashKey)
			case DiscoveryPublication(cloudDiscovery)				=> recvDiscoveryPublication(cloudDiscovery)
      case OvxInstanceRequest(cloud1, cloud2)         => recvOvxInstanceRequest(cloud1, cloud2)
		}
		case Kill									=> recvCCFMShutdown()
		case _										=> log.error("Unknown message received!")
}

  /**
   * Received from local CCFM
   * @param cloudSLA
   * @param possibleHostSLAs
   */
	private def revcFederationSLAs(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA] ) = {
		log.info("Received FederationSLAs from CCFM.")

    val mmaActorOpt = dependentActors.filter(_.isDefined).map(_.get).find(_.path.name == "matchMakingAgent")
		
		mmaActorOpt match{
		    case Some(mmaActor)	=>
					pubSubActorSelection ! DiscoverySubscription(Subscription(mmaActor, cloudSLA, 
																																		possibleHostSLAs, cloudConfig.certFile))
					log.info("Sended subscription request to Federator.")
					
		    case None          	=> 
					log.error("Subscription request can't be send to Federator, as no mmaActor-Ref was resolved!")
		}
		
	}

  /**
   * Received from PubSub-Federator
   * @param hashKey
   */
	def recvAuthenticationInquiry(hashKey: Long) = {
		log.info("Received AuthenticationInquiry from Federator.")
		//TODO: decrypt hashKey with own private key:
		val solvedKey = 0

		// send decrypted inquiry back to PubSubFederator as
		val authAnswer: AuthenticationAnswer = AuthenticationAnswer(solvedKey)
		pubSubActorSelection ! authAnswer
	}

  /**
   * Received from PubSub-Federator
   * @param cloudDiscovery
   */
	//TODO: change cert type to "Certificate"
	def recvDiscoveryPublication(cloudDiscovery: Subscription) = {
		log.info("Received DiscoveryPublication from PubSubFederator. Other MMA: {}", cloudDiscovery.actorRefMMA)
		// Forward this Publication to the MMA:
		log.info("Trying to contact MMA at {}", matchMakingActorSelection)
		matchMakingActorSelection ! DiscoveryPublication(cloudDiscovery)

//	  	this.discoveryActors = discoveryActors #TODO: filter interesting publications.
	}

  /**
   * Received from local MMA
   * @param cloud1
   * @param cloud2
   */
  def recvOvxInstanceRequest(cloud1: Subscription, cloud2: Subscription) = {
    log.info("Received OvxInstanceRequest from {}. Forwarding to PubSub-Federator...", sender())
    pubSubActorSelection ! OvxInstanceRequest(cloud1, cloud2)
  }


	private def recvCCFMShutdown() = {
		log.info("Received Shutdown Call from CCFM.")
	}
}



/**
 * Companion Object of the DiscoveryAgent, 
 * in order to implement some default behaviours
 */
object DiscoveryAgent
{
	//TODO: change cert type to "Certificate"
	def props(cloudConfig: CloudConfigurator, pubSubActorSelection: ActorSelection, matchMakingAgentSelection: ActorSelection):
		Props = Props(new DiscoveryAgent(cloudConfig, pubSubActorSelection, matchMakingAgentSelection))
}