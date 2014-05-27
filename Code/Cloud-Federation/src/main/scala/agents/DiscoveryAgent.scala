package agents

import akka.actor.{Props, Actor}
import akka.event.Logging
import java.net.InetAddress
import messages.DiscoveryInit

/**
 * Created by costa on 5/27/14.
 *
 * This agent is able to connect to the Discovery-Subscription-Service
 * and establishes a communication with the CCFM as its Supervisor.
 */
class DiscoveryAgent(pubSubServerAddr: InetAddress) extends Actor
{

/* Global Values: */
/* ============== */

	val log = Logging(context.system, this)



/* Methods & Execution: */
/* ==================== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	override def receive: Receive = {
		case DiscoveryInit   => recvDiscoveryInit()
		case "CCFMShutdown"  => recvCCFMShutdown()
		case _                => log.error("Unknown message received!")
	}

	private def recvDiscoveryInit(): Unit = {
		log.info("Received Init Call from CCFM")
	}

	private def recvCCFMShutdown(): Unit = {
		log.info("Received Shutdown Call from CCFM")
	}
}



/**
 * Companion Object of the DiscoveryAgent, 
 * in order to implement some default behaviours
 */
object DiscoveryAgent
{
	def props(pubSubServerAddr: InetAddress): Props = Props(new DiscoveryAgent(pubSubServerAddr))
}