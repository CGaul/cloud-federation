package messages

import datatypes.{Subscription}

sealed trait DiscoveryMessage

sealed trait DDADiscoveryDest extends DDADest
sealed trait MMADiscoveryDest extends PubSubDest
sealed trait PubSubDiscoveryDest extends PubSubDest


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
 */
//TODO: change cert type to "Certificate"
case class DiscoverySubscription(cloudSubscription: Subscription)
	extends DiscoveryMessage with PubSubDiscoveryDest

case class AuthenticationInquiry(hashedKey: Long)
	extends DiscoveryMessage with DDADiscoveryDest

case class AuthenticationAnswer(solvedKey: Long)
	extends DiscoveryMessage with PubSubDiscoveryDest
/**
 * <p>
 *    A DiscoveryPublication contains a List of all akka.tcp connections to Discovery-Agents
 *    from other clouds.
 * </p><p>
 *    This message will be send from the PubSubFederator to the pre-subscribed Discovery-Agent
 *    so that this Agent is able to get into contact with all Discovery-Agents in that list.
 * </p>
 * @param cloudDiscovery Describes a newly subscribed Cloud to all other, currently authenticated subscribers.
 */
//TODO: change cert type to "Certificate"
case class DiscoveryPublication(cloudDiscovery: Subscription)
	extends DiscoveryMessage with DDADiscoveryDest with MMADiscoveryDest