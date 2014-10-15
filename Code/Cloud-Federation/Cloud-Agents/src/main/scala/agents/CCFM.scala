package agents

import akka.actor._
import messages.{DiscoveryAck, DiscoveryError, DiscoveryInit}


/**
 * Created by costa on 5/27/14.
 */
class CCFM(pubSubServerAddr: ActorSelection) extends Actor with ActorLogging
{

/* Values: */
/* ======= */

  // Akka Child-Actor spawning:
  val discoveryAgentProps: Props 	= Props(classOf[DiscoveryAgent], args = pubSubServerAddr)
  val discoveryAgent: ActorRef 		= context.actorOf(discoveryAgentProps, name="discoveryAgent")
  log.info("Discovery-Agent established!")


/* Execution: */
/* ========= */

  //Called on CCFM construction:
  discoveryAgent ! DiscoveryInit()
  log.debug("Discovery-Init send to Discovery-Agent!")


/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receive(): Receive = {
		case DiscoveryAck(status)		=> recvDiscoveryStatus(status)
		case DiscoveryError(status)	=>	recvDiscoveryError(status)
		case "matchmakingMsg" 			=> recvMatchMakingMsg()
		case "authenticationMsg"		=> recvAuthenticationMsg()
		case _								=> log.error("Unknown message received!")
	}

	def recvDiscoveryStatus(status: String) = {
		log.info("Discovery Status \""+ status + "\" received.")
	}

	def recvDiscoveryError(error: String) = {
		log.error("Discovery Error \""+ error + "\" received.")
	}


	def recvMatchMakingMsg() = ???

	def recvAuthenticationMsg() = ???
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
	def props(pubSubServerAddr: ActorSelection): Props = Props(new CCFM(pubSubServerAddr))
}
