package agents

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
										 fedBrokerActorSel: ActorSelection, mmaActorSel: ActorSelection)
								extends RemoteDependencyAgent(List(fedBrokerActorSel, mmaActorSel)) with ActorLogging
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

  /**
   * The online receive-handle that needs to be implemented by the specified class, extending this RemoteDependencyAgent.
   * Contains the functionality, which will be executed by the Actor if all RemoteDependencies are solved and a message
   * comes into the mailbox, or there were stashed messages while the Actor was in its _offline state.
   * @return
   */
	override def receiveOnline: Receive = {
		case message: DDADiscoveryDest	=> message match {
			case AuthenticationInquiry(hashKey)							=> recvAuthenticationInquiry(hashKey)
			case DiscoveryPublication(cloudDiscovery)				=> recvDiscoveryPublication(cloudDiscovery)
		}
		case Kill									=> recvCCFMShutdown()
		case _										=> log.error("Unknown message received!")
  }
  
  override def becomeOnline: Unit = {
    log.info("DiscoveryAgent became online, sending Subscription to FedBroker now...")
    sendSubscriptionToFedBroker(cloudConfig.cloudSLA, cloudConfig.cloudHosts.map(_.sla))
  }

  /**
   * Received from local CCFM
   * @param cloudSLA
   * @param possibleHostSLAs
   */
	private def sendSubscriptionToFedBroker(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA] ) = {
		log.info("Received FederationSLAs from CCFM.")

		//See if the MMA Actor is already resolved by the RemoteDependencyAgent:
		val resolvedMMASelIndex = resolvedActorSelects.find(_._2 == mmaActorSel).map(_._1).getOrElse(-1)
    val mmaActorOpt = resolvedActorRefs.find(_._1 == resolvedMMASelIndex).map(_._2)
		
		mmaActorOpt match{
		    case Some(mmaActor)	=>
					fedBrokerActorSel ! DiscoverySubscription(Subscription(mmaActor, cloudSLA, 
																																		possibleHostSLAs, cloudConfig.certFile))
					log.info("Sended subscription request to Federation-Broker.")
					
		    case None          	=> 
					log.error("Subscription request can't be send to Federation-Broker, as no mmaActor-Ref was resolved!")
		}
		
	}

  /**
   * Received from Federation-Broker
   * @param hashKey
   */
	def recvAuthenticationInquiry(hashKey: Long) = {
		log.info("Received AuthenticationInquiry from Federation-Broker.")
		//TODO: decrypt hashKey with own private key:
		val solvedKey = 0

		// send decrypted inquiry back to Federation-Broker as
		val authAnswer: AuthenticationAnswer = AuthenticationAnswer(solvedKey)
		fedBrokerActorSel ! authAnswer
	}

  /**
   * Received from Federation-Broker
   * @param cloudDiscovery
   */
	//TODO: change cert type to "Certificate"
	def recvDiscoveryPublication(cloudDiscovery: Subscription) = {
		log.info("Received DiscoveryPublication from Federation-Broker. Other MMA: {}", cloudDiscovery.actorRefMMA)
		// Forward this Publication to the MMA:
		log.info("Trying to contact MMA at {}", mmaActorSel)
		mmaActorSel ! DiscoveryPublication(cloudDiscovery)

//	  	this.discoveryActors = discoveryActors #TODO: filter uninteresting publications.
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
	def props(cloudConfig: CloudConfigurator, fedBrokerActorSel: ActorSelection, mmaActorSel: ActorSelection):
		Props = Props(new DiscoveryAgent(cloudConfig, fedBrokerActorSel, mmaActorSel))
}