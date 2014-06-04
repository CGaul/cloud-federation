package agents

import akka.actor._
import akka.event.Logging
import java.net.InetAddress
import messages.{DiscoveryError, DiscoveryMessage, DiscoveryAck, DiscoveryInit}
import messages.DiscoveryInit
import messages.DiscoveryError
import messages.DiscoveryAck


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
  println("Discovery-Agent established!")


/* Execution: */
/* ========= */

  //Called on CCFM construction:
  discoveryAgent ! DiscoveryInit()


/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receive: Receive = {
		case DiscoveryAck(status)		=> recvDiscoveryStatus(status)
		case DiscoveryError(status)	=>	recvDiscoveryError(status)
		case "matchmakingMsg" 			=> recvMatchMakingMsg()
		case "authenticationMsg"		=> recvAuthenticationMsg()
		case _								=> log.error("Unknown message received!")
	}

	def recvDiscoveryStatus(status: String): Unit = {
		log.info("Discovery Status \""+ status + "\" received.")
	}

	def recvDiscoveryError(error: String): Unit = {
		log.error("Discovery Error \""+ error + "\" received.")
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
	def props(pubSubServerAddr: ActorSelection): Props = Props(new CCFM(pubSubServerAddr))
}
