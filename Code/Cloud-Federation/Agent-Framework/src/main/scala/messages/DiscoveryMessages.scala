package messages

import java.security.cert.Certificate

import akka.actor.ActorPath
import datatypes.HardSLA

sealed trait DiscoveryMessage

sealed trait DiscoveryAgentReply

sealed trait PubSubFederatorReply



case class DiscoveryInit(slaList: Vector[HardSLA])	extends DiscoveryMessage
									with DiscoveryAgentReply

case class DiscoveryAck(status: String)	extends DiscoveryMessage

case class DiscoveryError(error: String)	extends DiscoveryMessage


/**
 * <p>
 * 	A DiscoverySubscription is send from the Discovery-Agent to the PubSubFederator
 * 	in order to subscribe on a Cloud-Federation for other Clouds.
 * </p><p>
 *    The answer of a DiscoverySubscription will be a DiscoveryPublication from the PubSubFederator
 *    to the Discovery-Agent
 * </p>
 * @param slaList
 * @param cert
 */
case class DiscoverySubscription(slaList: Vector[HardSLA], cert : Certificate)	extends DiscoveryMessage
																			with PubSubFederatorReply


/**
 * <p>
 *    A DiscoveryPublication contains a List of all akka.tcp connections to Discovery-Agents
 *    from other clouds.
 * </p><p>
 *    This message will be send from the PubSubFederator to the pre-subscribed Discovery-Agent
 *    so that this Agent is able to get into contact with all Discovery-Agents in that list.
 * </p>
 * @param federatedDiscoveryActors A list of akka.tcp connections, each belonging to another Discovery-Agent from
 *                         possible matches of a Cloud-Federation
 */
case class DiscoveryPublication(federatedDiscoveryActors: Vector[ActorPath]) 	extends DiscoveryMessage 
																										with DiscoveryAgentReply
