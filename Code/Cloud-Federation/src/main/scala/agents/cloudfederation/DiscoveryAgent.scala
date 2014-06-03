package agents

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.actor._
import akka.event.Logging
import akka.pattern.{AskTimeoutException, ask, pipe}

import messages._
import akka.util.Timeout
import messages.DiscoveryInit
import messages.DiscoveryPublication
import messages.DiscoverySubscription
import messages.DiscoveryAck
import messages.DiscoveryInit
import messages.DiscoveryPublication
import messages.DiscoveryError
import messages.DiscoverySubscription
import messages.DiscoveryAck


/**
 * Created by costa on 5/27/14.
 *
 * This agent is able to connect to the Discovery-Subscription-Service
 * and establishes a communication with the CCFM as its Supervisor.
 */
class DiscoveryAgent(pubSubServerAddr: String) extends Actor with ActorLogging
{

/* Global Values: */
/* ============== */

	val pubSubFederator = context.actorSelection(pubSubServerAddr)



/* Methods & Execution: */
/* ==================== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	def offline: Receive = {

	}

	override def receive: Receive = {
		case DiscoveryInit()								=> recvDiscoveryInit()
		case DiscoveryPublication(discoveryList)	=> recvDiscoveryPublication(discoveryList)

		case "CCFMShutdown" 								=> recvCCFMShutdown()
		case _												=> log.error("Unknown message received!")
	}

	private def recvDiscoveryInit(): Unit = {
		log.info("Received Init Call from CCFM.")
		log.info("Sending Identification Request to the PubSub-Federator...")
		pubSubFederator ! Identify
		log.info("Sending async subscription request to PubSub-Federator...")
		try{
			implicit val timeout = Timeout(5 seconds) //will be implicitely used in "ask" below
			val pubSubReply: Future[DiscoveryAck] = (pubSubFederator.ask(DiscoverySubscription("this is my cert!"))(timeout)).mapTo[DiscoveryAck]
			//val ident: ActorIdentity = Await.result(pubSubReply, Duration.apply(5 seconds))
			if(pubSubReply) {
				sender() ! DiscoveryAck("Subscribed at PubSubServer")
			}
			else{
				sender() ! DiscoveryError("PubSubServer is not available!")

				//TODO: close Agent.
			}
		}
		catch{
			case timeoutError: AskTimeoutException =>
				sender() ! DiscoveryError("PubSubServer timeout!")
				throw timeoutError
		}


	}

	def recvDiscoveryPublication(discoveryList: List[String]): Unit = {
		log.info("Received Publication Call from PubSubFederator.")
	}

	private def recvCCFMShutdown(): Unit = {
		log.info("Received Shutdown Call from CCFM.")
	}
}



/**
 * Companion Object of the DiscoveryAgent, 
 * in order to implement some default behaviours
 */
object DiscoveryAgent
{
	def props(pubSubServerAddr: String): Props = Props(new DiscoveryAgent(pubSubServerAddr))
}