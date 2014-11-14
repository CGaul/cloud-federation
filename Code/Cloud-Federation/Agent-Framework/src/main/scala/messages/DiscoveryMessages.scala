package messages

import java.io.File
import java.security.cert.Certificate

import akka.actor.{ActorRef}
import datatypes.{CloudSLA, HostSLA}

sealed trait DiscoveryMessage

sealed trait DiscoveryAgentDestination

sealed trait PubSubFederatorDestination


/**
 * Sent from CCFM to DiscoveryAgent
 * @param cloudSLA
 * @param possibleHostSLAs
 */
case class FederationSLAs(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA])
	extends DiscoveryMessage with DiscoveryAgentDestination

case class DiscoveryAck(status: String)
	extends DiscoveryMessage

case class DiscoveryError(error: String)
	extends DiscoveryMessage


/**
 * <p>
 * 	A DiscoverySubscription is send from the Discovery-Agent to the PubSubFederator
 * 	in order to subscribe on a Cloud-Federation for other Clouds.
 * </p><p>
 *    The answer of a DiscoverySubscription will be a DiscoveryPublication from the PubSubFederator
 *    to the Discovery-Agent
 * </p>
 * @param cloudSLA
 * @param possibleHostSLAs
 * @param cert
 */
//TODO: change cert type to "Certificate"
case class DiscoverySubscription(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA], cert: File)
	extends DiscoveryMessage with PubSubFederatorDestination

case class AuthenticationInquiry(hashedKey: Long)
	extends DiscoveryMessage with DiscoveryAgentDestination

case class AuthenticationAnswer(solvedKey: Long)
	extends DiscoveryMessage with PubSubFederatorDestination
/**
 * <p>
 *    A DiscoveryPublication contains a List of all akka.tcp connections to Discovery-Agents
 *    from other clouds.
 * </p><p>
 *    This message will be send from the PubSubFederator to the pre-subscribed Discovery-Agent
 *    so that this Agent is able to get into contact with all Discovery-Agents in that list.
 * </p>
 * @param discoveredActor Describes a newly subscribed actor to all other, current subscribers.
 */
//TODO: change cert type to "Certificate"
case class DiscoveryPublication(discoveredActor: (ActorRef, CloudSLA, Vector[HostSLA], File))
	extends DiscoveryMessage with DiscoveryAgentDestination
