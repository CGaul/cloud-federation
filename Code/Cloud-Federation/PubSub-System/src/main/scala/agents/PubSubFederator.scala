package agents

import java.io.File
import java.security.cert.Certificate

import akka.actor._
import akka.pattern.ask
import datatypes.{CloudSLA, HostSLA}
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

	//TODO: change cert type to "Certificate"
  private var subscriptions 	: Vector[(ActorRef, CloudSLA, Vector[HostSLA], File)] = Vector()
  private var subscribers : Vector[(ActorRef, Boolean)] = Vector()


/* Methods: */
/* ======== */

	//TODO: use?!
  def publish(message: Unit) = {
	 for (subscriber <- subscriptions){
		subscriber._1 ! message
	 }
  }

  override def receive(): Receive = {
	 case message: PubSubFederatorDestination	=> message match {
     case DiscoverySubscription
			 (cloudSLA, possibleHostSLAs, cert)	=> recvDiscoverySubscription(cloudSLA, possibleHostSLAs, cert)
		 case AuthenticationAnswer(solvedKey) => recvAuthenticationAnswer(solvedKey)
	 }
	 case _																	=> log.error("Unknown message received!")
  }

	//TODO: change cert type to "Certificate"
  def recvDiscoverySubscription(cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA], cert: File): Unit = {

	 	//When a new DiscoverySubscription drops in, save that sender as a subscriber:
	 	val subscriber: ActorRef = sender()
		val newSubscription = (subscriber, cloudSLA, possibleHostSLAs, cert)
	 	subscriptions = subscriptions :+ newSubscription
	 	log.info("Received Cloud Subscription. Subscribed Sender.")
	 	log.debug("Current subscriptions: "+ subscriptions)
	 	log.debug("Current subscribers: "+ subscribers)

		val authSubscriberQuestion = AuthenticationInquiry(Math.random().toLong)
		val currentSubscribers = subscribers
		subscriber ! authSubscriberQuestion
		//Add the new subscriber to the vector of all subscribers:
		subscribers = subscribers :+ (subscriber, false)

		// Send new publication to all current subscribers that are already authenticated:
		for (actSubscriber <- currentSubscribers.filter(_._2).map(_._1)) {
			actSubscriber ! DiscoveryPublication(newSubscription)
		}
	}

	def recvAuthenticationAnswer(solvedKey: Long): Unit = {
		val subscriberToAuth: ActorRef = sender()
		val registeredSubscriber = subscribers.find(_._1 == subscriberToAuth)
		if(registeredSubscriber.isDefined){
			//TODO: check if inquiry key is correct:
			if(registeredSubscriber.get._2){
				log.warning("Subscriber {} is already authenticated. No further authentication check needed.", registeredSubscriber.get._1)
				return
			}
			if(solvedKey == 0){
				log.info("Authentication for new Subscriber {} was successful! Subscriber Registration completed.", registeredSubscriber.get._1)
				// replace subscriber in subscriber list with authenticated = true:
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				subscribers = subscribers.updated(index, (registeredSubscriber.get._1, true))

				// After successful authentication, send new subscriber all current subscriptions:
				publishSubscritions(registeredSubscriber.get._1)
			}
			else{
				log.warning("Authentication for new Subscriber {} was unsuccessful. " +
					"Dropping temporary Subscriber from registered Subscribers.", registeredSubscriber.get._1)
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				subscribers = subscribers.drop(index)
			}
		}
		else{
			log.warning("Received a AuthenticationAnswer of {}, which is not actually registered as a potential subscriber!",
			subscriberToAuth)
		}
	}


	def publishSubscritions(subscriber: ActorRef) = {
		val authSubscribers: Vector[ActorRef] = subscribers.filter(_._2).map(_._1)
		val authSubscriptions = subscriptions.filter(subscr => authSubscribers.contains(subscr._1))

		for (actSubscription <- authSubscriptions) {
			subscriber ! DiscoveryPublication(actSubscription)
		}
	}


}
