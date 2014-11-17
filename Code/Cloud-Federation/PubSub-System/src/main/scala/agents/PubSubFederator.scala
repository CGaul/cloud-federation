package agents

import java.io.File
import java.security.cert.Certificate

import akka.actor._
import datatypes.{Subscription, Subscriber, CloudSLA, HostSLA}
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

	//TODO: change cert type to "Certificate"
  private var subscriptions : Map[ActorRef, Subscription] = Map()
  private var subscribers : Vector[Subscriber] = Vector()


/* Methods: */
/* ======== */

	//TODO: use?!
  def publish(message: Unit) = {
	 for (subscriber <- subscriptions){
		subscriber._1 ! message
	 }
  }

  override def receive(): Receive = {
	 case message: PubSubDiscoveryDest	=> message match {
     case DiscoverySubscription(subscription)	=> recvDiscoverySubscription(subscription)
		 case AuthenticationAnswer(solvedKey) 		=> recvAuthenticationAnswer(solvedKey)
	 }
	 case _																	=> log.error("Unknown message received!")
  }

	//TODO: change cert type to "Certificate"
  def recvDiscoverySubscription(newSubscription: Subscription): Unit = {

	 	//When a new DiscoverySubscription drops in, save that sender as an unauthenticated subscriber:
		val newSubscriber = Subscriber(sender(), authenticated = false)

		// Add the new, unauthenticated subscriber to the vector of all subscribers and the subscription Map:
		subscribers = subscribers :+ newSubscriber
		subscriptions = subscriptions + (newSubscriber.actorRef -> newSubscription)

		log.info("Received DiscoverySubscription. Pre-Registered Subscriber: {}.", newSubscriber)
		log.debug("Current subscriptions: "+ subscriptions)
		log.debug("Current subscribers: "+ subscribers)

		val encrSecCheck = Math.random().toLong //TODO: Write out Shortcut implementation.
		log.info("Sending AuthenticationInquiry with encrypted security check: {}", encrSecCheck)
		// Begin with the authentication of the new subscriber:
		val authSubscriberQuestion = AuthenticationInquiry(encrSecCheck)
		newSubscriber.actorRef ! authSubscriberQuestion
	}

	def recvAuthenticationAnswer(solvedKey: Long): Unit = {
		val subscriberToAuth: ActorRef = sender()
		val registeredSubscriber = subscribers.find(subscriber => subscriber.actorRef == subscriberToAuth)
		if(registeredSubscriber.isDefined){
			//TODO: check if inquiry key is correct:
			if(registeredSubscriber.get.authenticated){
				log.warning("Subscriber {} is already authenticated. No further authentication check needed.", registeredSubscriber.get)
				return
			}
			if(solvedKey == 0){ //TODO: Write out Shortcut implementation for solvedKey.
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				val authSubscriber = Subscriber(subscribers(index).actorRef, authenticated = true)
				subscribers = subscribers.updated(index, authSubscriber)
				log.info("Authentication for new Subscriber {} was successful! Subscriber Registration completed.", registeredSubscriber.get)
				// replace subscriber in subscriber list with authenticated = true:


				// After successful authentication, publish new Subscription to all subscribers:
				publishSubscrition(authSubscriber, subscriptions(authSubscriber.actorRef))
			}
			else{
				log.warning("Authentication for new Subscriber {} was unsuccessful. " +
					"Dropping temporary Subscriber from registered Subscribers.", registeredSubscriber.get.actorRef)
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				subscribers = subscribers.drop(index)
			}
		}
		else{
			log.warning("Received a AuthenticationAnswer of {}, which is not actually registered as a potential subscriber!",
			subscriberToAuth)
		}
	}


	/**
	 * Publishes a subscription to each authenticated Subscriber in the subscribers Vector.
	 * Does not publish a Subscription back to the originating Subscriber.
	 */
	def publishSubscrition(originator: Subscriber, subscription: Subscription) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: Vector[Subscriber] = subscribers.filter(_.authenticated).filter(_ != originator)

		for (actSubscriber <- authSubscribers) {
			actSubscriber.actorRef ! DiscoveryPublication(subscription)
		}
	}
}
