package agents

import akka.actor.{ActorRef, Props, Actor}
import akka.event.Logging
import java.net.InetAddress
import messages.{DiscoveryError, DiscoveryMessage, DiscoveryAck, DiscoveryInit}


/**
 * Created by costa on 5/27/14.
 */
class CCFM(pubSubServerAddr: String) extends Actor
{

/* Global Values: */
/* ============== */

	val log = Logging(context.system, this)

	// Akka Child-Actor spawning:
	val discoveryAgentProps: Props 	= Props(classOf[DiscoveryAgent], args = pubSubServerAddr)
	val discoveryAgent: ActorRef 		= context.actorOf(discoveryAgentProps, name="discoveryAgent")
	println("Discovery-Agent established!")


/* Methods & Execution: */
/* ==================== */

	//Called on CCFM construction:
	discoveryAgent ! DiscoveryInit()


	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receive: Receive = {
		case DiscoveryAck(status)		=> recvDiscoveryStatus(status)
		case DiscoveryError(status)	=>	recvDiscoveryError(status)
		case "matchmakingMsg" 			=> recvMatchMakingMsg()
		case "authenticationMsg"		=> recvAuthenticationMsg()
		case _                			=> log.error("Unknown message received!")
	}

	def recvDiscoveryStatus(status: String): Unit = {
		log.info("Discovery Status "+ status + " received.")
	}

	def recvDiscoveryError(error: String): Unit = {
		log.error("Discovery Error "+ error + " received.")
	}


	def recvMatchMakingMsg(): Unit = ???

	def recvAuthenticationMsg(): Unit = ???
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
	def props(pubSubServerAddr: String): Props = Props(new CCFM(pubSubServerAddr))
}
