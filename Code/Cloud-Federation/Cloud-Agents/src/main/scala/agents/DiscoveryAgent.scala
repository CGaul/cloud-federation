package agents

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
class DiscoveryAgent(pubSubServer: ActorSelection, cert: Certificate)
									extends RemoteDependencyAgent(Vector(pubSubServer)) with ActorLogging
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

	override def receivedOnline(): Receive = {
	  	case KillNotifier()						=> super.recv_offlineNotifier()

	  	case message: DiscoveryAgentDestination	=> message match {
			case FederationSLAs(cloudSLA, possibleHostSLAs)	=> recvDiscoveryInit(cloudSLA, possibleHostSLAs)
			case DiscoveryPublication(discoveries)					=> recvDiscoveryPublication(discoveries)
		}
		case Kill									=> recvCCFMShutdown()
		case _										=> log.error("Unknown message received!")
	}

	private def recvDiscoveryInit(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA] ) = {
		log.info("Received Discovery-Init Call from CCFM.")
		log.info("Sending subscription request to PubSub-Federator...")

		pubSubServer ! DiscoverySubscription(cloudSLA, possibleHostSLAs, cert)

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

	def recvDiscoveryPublication(discoveryActors: Vector[(ActorPath, CloudSLA, Vector[HostSLA])]) = {
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
	def props(pubSubServerAddr: ActorSelection, cert: Certificate):
		Props = Props(new DiscoveryAgent(pubSubServerAddr, cert))
}