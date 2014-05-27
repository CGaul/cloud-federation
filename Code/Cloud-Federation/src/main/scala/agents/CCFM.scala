package agents

import akka.actor.{Props, Actor}
import akka.event.Logging
import java.net.InetAddress
import messages.DiscoveryInit


/**
 * Created by costa on 5/27/14.
 */
class CCFM extends Actor
{

/* Global Values: */
/* ============== */

	val log = Logging(context.system, this)

	//Pre-Defined Values:
	val pubSubServerAddr : InetAddress 	= InetAddress.getLocalHost
	val pubSubServerPort : Integer 		= 13

	// Akka Child-Actor spawning:
	val discoveryAgentProps = Props(classOf[DiscoveryAgent], args = pubSubServerAddr)
	val discoveryAgent = context.actorOf(discoveryAgentProps, name="discoveryAgent")


/* Methods & Execution: */
/* ==================== */

	//Called on CCFM construction:
	discoveryAgent ! DiscoveryInit(pubSubServerAddr, pubSubServerPort)


	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receive: Receive = {
		case "discoveryMsg" 			=> recvDiscoveryMsg()
		case "matchmakingMsg" 		=> recvMatchMakingMsg()
		case "authenticationMsg"	=> recvAuthenticationMsg()
		case _                		=> log.error("Unknown message received!")
	}

	def recvDiscoveryMsg(): Unit = ???

	def recvMatchMakingMsg(): Unit = ???

	def recvAuthenticationMsg(): Unit = ???
}
