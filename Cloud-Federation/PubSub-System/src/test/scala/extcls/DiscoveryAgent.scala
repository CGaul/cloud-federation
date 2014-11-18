package extcls

import java.io.File

import akka.actor._
import datatypes.{CloudSLA, HostSLA, Subscription}
import messages._


/**
 * This agent is able to connect to the Discovery-Subscription-Service
 * and establishes a communication with the CCFM as its Supervisor.
 * @author Constantin Gaul created on 5/27/14.
 *
 * Static Reference to the Cloud-Agent's DiscoveryAgent. Only needed for testing purposes.
 * If tests, using this Agent should always be as actual as possible, this external class link
 * needs to be updated manually by copying the Cloud-Agent's DiscoveryAgent! (ln -s does not work).
 */
class DiscoveryAgent(pubSubActorSelection: ActorSelection, matchMakingActorSelection: ActorSelection,
										 cert: File) extends Actor with ActorLogging
									//TODO: change cert type to "Certificate"
									//extends RemoteDependencyAgent(Vector(pubSubActorSelection, matchMakingActorSelection))
									//with ActorLogging
{

/* Values: */
/* ======= */


/* Variables: */
/* ========== */

  	var discoveryActors: Vector[ActorPath] = Vector()


/* Execution: */
/* ========== */


/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receive(): Receive = {
	  	//case KillNotifier()						=> super.recv_offlineNotifier()

		case message: DDADiscoveryDest	=> message match {
			case FederationSLAs(cloudSLA, possibleHostSLAs)	=> revcFederationSLAs(cloudSLA, possibleHostSLAs)
			case AuthenticationInquiry(hashKey)							=> recvAuthenticationInquiry(hashKey)
			case DiscoveryPublication(cloudDiscovery)				=> recvDiscoveryPublication(cloudDiscovery)
		}
		case Kill									=> recvCCFMShutdown()
		case _										=> log.error("Unknown message received!")
}

	private def revcFederationSLAs(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA] ) = {
		log.info("Received FederationSLAs from CCFM.")

		pubSubActorSelection ! DiscoverySubscription(Subscription(matchMakingActorSelection, cloudSLA, possibleHostSLAs, cert))
		log.info("Sended subscription request to PubSub-Federator.")
	}

	def recvAuthenticationInquiry(hashKey: Long) = {
		//TODO: decrypt hashKey with own private key:
		val solvedKey = 0

		// send decrypted inquiry back to PubSubFederator as
		val authAnswer: AuthenticationAnswer = AuthenticationAnswer(solvedKey)
		pubSubActorSelection ! authAnswer
	}

	//TODO: change cert type to "Certificate"
	def recvDiscoveryPublication(cloudDiscovery: Subscription) = {
		log.info("Received DiscoveryPublication from PubSubFederator. Other MMA: {}", cloudDiscovery.cloudMMA)
		// Forward this Publication to the MMA:
		matchMakingActorSelection ! DiscoveryPublication(cloudDiscovery)

//	  	this.discoveryActors = discoveryActors #TODO: filter interesting publications.
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
	def props(pubSubActorSelection: ActorSelection, matchMakingAgentSelection: ActorSelection, cert: File):
		Props = Props(new DiscoveryAgent(pubSubActorSelection, matchMakingAgentSelection, cert))
}