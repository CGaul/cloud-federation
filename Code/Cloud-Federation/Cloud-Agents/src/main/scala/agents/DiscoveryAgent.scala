package agents

import java.io.File
import java.security.cert.Certificate

import agents.cloudfederation.RemoteDependencyAgent
import akka.actor._
import datatypes.{CloudSLA, HostSLA}
import messages._


/**
 * This agent is able to connect to the Discovery-Subscription-Service
 * and establishes a communication with the CCFM as its Supervisor.
 * @author Constantin Gaul created on 5/27/14.
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

	  	case message: DiscoveryAgentDestination	=> message match {
			case FederationSLAs(cloudSLA, possibleHostSLAs)	=> recvDiscoveryInit(cloudSLA, possibleHostSLAs)
			case AuthenticationInquiry(hashKey)							=> log.debug("Received Authentication Inquiry from PubSubFederator!")
			case DiscoveryPublication(discoveredActor)					=> recvDiscoveryPublication(discoveredActor)
		}
		case Kill									=> recvCCFMShutdown()
		case _										=> log.error("Unknown message received!")
	}

	private def recvDiscoveryInit(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA] ) = {
		log.info("Received Discovery-Init Call from CCFM.")
		log.info("Sending subscription request to PubSub-Federator...")

		pubSubActorSelection ! DiscoverySubscription(cloudSLA, possibleHostSLAs, cert)

//		try{
//			implicit val timeout = Timeout(5 seconds) //will be implicitely used in "ask" below
//			val pubSubReply: Future[DiscoveryAck] = (pubSubServerAddr.ask(DiscoverySubscription("this is my cert!"))(timeout)).mapTo[DiscoveryAck]
//			//val ident: ActorIdentity = Await.result(pubSubReply, Duration.apply(5 seconds))
//
//		  //TODO: go on here.
//			if(pubSubReply.) {
//				sender() ! DiscoveryAck("Subscribed at PubSubServer")
//			}
//			else{
//				sender() ! DiscoveryError("PubSubServer is not available!")
//
//				//TODO: close Agent.
//			}
//		}
//		catch{
//			case timeoutError: AskTimeoutException =>
//				sender() ! DiscoveryError("PubSubServer timeout!")
//				throw timeoutError
//		}


	}

	//TODO: change cert type to "Certificate"
	def recvDiscoveryPublication(discoveredActor: (ActorRef, CloudSLA, Vector[HostSLA], File)) = {
		log.info("Received Publication Call from PubSubFederator.")
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