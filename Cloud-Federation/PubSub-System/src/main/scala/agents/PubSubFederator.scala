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
		log.info("Received DiscoverySubscription from {}", sender())
	 	//When a new DiscoverySubscription drops in, save that sender as an unauthenticated subscriber:
		val newSubscriber = Subscriber(sender(), authenticated = false)
		if(subscribers.contains(newSubscriber)){
			log.warning("Subscriber {} is already registered at PubSub-Server", newSubscriber.actorRef)
			return
		}
		// Add the new, unauthenticated subscriber to the vector of all subscribers and the subscription Map:
		subscribers = subscribers :+ newSubscriber
		subscriptions = subscriptions + (newSubscriber.actorRef -> newSubscription)

		log.info("Received DiscoverySubscription. Pre-Registered {}.", newSubscriber)
		log.info("Subscriptions: "+ subscriptions)
		log.info("Subscribers: "+ subscribers)

		val encrSecCheck = Math.random().toLong //TODO: Write out Shortcut implementation.
		log.info("Sending AuthenticationInquiry with encrypted security check: {}", encrSecCheck)
		// Begin with the authentication of the new subscriber:
		val authSubscriberQuestion = AuthenticationInquiry(encrSecCheck)
		newSubscriber.actorRef ! authSubscriberQuestion
	}

	def recvAuthenticationAnswer(solvedKey: Long): Unit = {
		log.info("Received AuthenticationAnswer from {}", sender())
		val subscriberToAuth: ActorRef = sender()
		val registeredSubscriber = subscribers.find(subscriber => subscriber.actorRef == subscriberToAuth)
		log.info("Found Pre-Reg. Subscriber: {}", registeredSubscriber)
		log.info("Subscriptions: "+ subscriptions)
		if(registeredSubscriber.isDefined){
			//TODO: check if inquiry key is correct:
			if(registeredSubscriber.get.authenticated){
				log.warning("Subscriber {} is already authenticated. No further authentication check needed.", registeredSubscriber.get)
				return
			}
			if(solvedKey == 0){ //TODO: Write out Shortcut implementation for solvedKey.
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				// Replace old subscriber in subscribers Vector with authenticated Subscriber:
				val authSubscriber = Subscriber(subscribers(index).actorRef, authenticated = true)
				subscribers = subscribers.updated(index, authSubscriber)
				log.info("Authentication for new {} was successful! Subscriber Registration completed.", subscribers(index))
				log.info("Position of Subscriber update: {}", index)
				log.info("Subscriptions: "+ subscriptions)

				log.info("Broadcast and Publish Subscriptions for authSubscriber: {}", authSubscriber.actorRef)
				// After successful authentication, publish new Subscription to all subscribers:
				broadcastOneSubscription(authSubscriber, subscriptions(authSubscriber.actorRef))
				
				//Publish all Subscriptions from every authenticated Subscriber to the 
				// new initialized (and authenticated) Subscriber:
				publishAllSubscriptions(authSubscriber)
			}
			else{
				log.warning("Authentication for new {} was unsuccessful. " +
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
	def broadcastOneSubscription(originator: Subscriber, subscription: Subscription) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: Vector[Subscriber] = subscribers.filter(_.authenticated).filter(_ != originator)

		for (actSubscriber <- authSubscribers) {
			log.info("Broadcasting Subscription of {} to {}", originator.actorRef, actSubscriber.actorRef)
			actSubscriber.actorRef ! DiscoveryPublication(subscription)
		}
	}
	
	def publishAllSubscriptions(receiver: Subscriber) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: Vector[Subscriber] = subscribers.filter(_.authenticated).filter(_ != receiver)
		val authSubscriptions: Iterable[Subscription] = subscriptions.filterKeys(authSubscribers.map(_.actorRef).contains).map(_._2)
		if(authSubscriptions.size > 0)
			log.info("Initial Publication to {}", receiver.actorRef)

		for (actSubscription <-  authSubscriptions) {
			receiver.actorRef ! DiscoveryPublication(actSubscription)
		}
	}
}
