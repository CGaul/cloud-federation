package agents

import java.net.InetAddress

import akka.actor._
import datatypes.{Subscriber, Subscription}
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

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

  override def receive: Receive = {
	 case message: PubSubDiscoveryDest	=> message match {
     case DiscoverySubscription(subscription)	=> recvDiscoverySubscription(subscription)
		 case AuthenticationAnswer(solvedKey) 		=> recvAuthenticationAnswer(solvedKey)
     case OvxInstanceRequest(cloud1, cloud2)  => recvOVXInstanceRequest(cloud1, cloud2)
	 }
	 case _																	=> log.error("Unknown message received!")
  }

  /**
   * Received from DiscoveryAgent
   * @param newSubscription
   */
  def recvDiscoverySubscription(newSubscription: Subscription): Unit = {
		log.info("Received DiscoverySubscription from {}", sender())
	 	//When a new DiscoverySubscription drops in, save that sender as an unauthenticated subscriber:
		val newSubscriber = Subscriber(sender(), authenticated = false, List())
		if(subscribers.contains(newSubscriber)){
			log.warning("Subscriber {} is already registered at PubSub-Server", newSubscriber.actorRefDA)
			return
		}
		log.debug("Subscribers before discovery update: "+ subscribers)
		// Add the new, unauthenticated subscriber to the vector of all subscribers and the subscription Map:
		subscribers = subscribers :+ newSubscriber
		subscriptions = subscriptions + (newSubscriber.actorRefDA -> newSubscription)

		log.info("Received DiscoverySubscription. Pre-Registered {}.", newSubscriber)
		//log.info("Subscriptions: "+ subscriptions)
		log.debug("Subscribers after discovery update: "+ subscribers)

		val encrSecCheck = Math.random().toLong //TODO: Write out Shortcut implementation.
		log.info("Sending AuthenticationInquiry with encrypted security check: {}", encrSecCheck)
		// Begin with the authentication of the new subscriber:
		val authSubscriberQuestion = AuthenticationInquiry(encrSecCheck)
		newSubscriber.actorRefDA ! authSubscriberQuestion
	}

  /**
   * Received from DiscoveryAgent
   * @param solvedKey
   */
	def recvAuthenticationAnswer(solvedKey: Long): Unit = {
		log.info("Received AuthenticationAnswer from {}", sender())
		val subscriberToAuth: ActorRef = sender()
		val registeredSubscriber = subscribers.find(subscriber => subscriber.actorRefDA == subscriberToAuth)
		log.info("Found Pre-Reg. Subscriber: {}", registeredSubscriber)
		if(registeredSubscriber.isDefined){
			//TODO: check if inquiry key is correct:
			if(registeredSubscriber.get.authenticated){
				log.warning("Subscriber {} is already authenticated. No further authentication check needed.", registeredSubscriber.get)
				return
			}
			if(solvedKey == 0){ //TODO: Write out Shortcut implementation for solvedKey.
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				// Replace old subscriber in subscribers Vector with authenticated Subscriber:
				val authSubscriber = Subscriber(subscribers(index).actorRefDA, authenticated = true, List())
				log.debug("Subscribers before auth-update: "+ subscribers)
				subscribers = subscribers.updated(index, authSubscriber)
				log.info("Authentication for new {} was successful! Subscriber Registration completed.", subscribers(index))
				log.debug("Subscribers after auth-update: "+ subscribers)

				// After successful authentication, publish new Subscription to all subscribers:
				broadcastOneSubscription(authSubscriber, subscriptions(authSubscriber.actorRefDA))
				
				//Publish all Subscriptions from every authenticated Subscriber to the 
				// new initialized (and authenticated) Subscriber:
				publishAllSubscriptions(authSubscriber)
			}
			else{
				log.warning("Authentication for new {} was unsuccessful. " +
					"Dropping temporary Subscriber from registered Subscribers.", registeredSubscriber.get.actorRefDA)
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
   * Received from DiscoveryAgent
   * @param cloud1
   * @param cloud2
   */
  def recvOVXInstanceRequest(cloud1: Subscription, cloud2: Subscription) = {
    // After setting up an OVX instance for federation, tell both cloud MMA-Actors where the OVX-Instance is found:
    val (ovxIp, ovxApiPort, ovxCtrlPort) = startOVXInstance()
    cloud1.actorRefMMA ! OvxInstanceReply(ovxIp, ovxApiPort, ovxCtrlPort)
    cloud2.actorRefMMA ! OvxInstanceReply(ovxIp, ovxApiPort, ovxCtrlPort)

    // Set the asking DA as the federation master inside the Federator for the cloud2 subscription:
    val masterSubscriberOpt = subscribers.find(_.actorRefDA == sender())
    masterSubscriberOpt match{
      case Some(masterSubscriber) =>
        val updFederationList = masterSubscriber.masterOfFederations :+ (cloud1, cloud2)
        val updSubscriber = new Subscriber(masterSubscriber.actorRefDA, masterSubscriber.authenticated, updFederationList)
        subscribers = subscribers.updated(subscribers.indexOf(masterSubscriber), updSubscriber)
        log.info("Subscriber {} is the new master of the federation between {} and {}", sender(), cloud1, cloud2)
      case None                   =>
        log.error("No matching DA-ActorRef found as a registered Subscriber for {}!", sender())
    }
  }



  /* Private Methods: */
  /* ================ */

	/**
	 * Publishes a subscription to each authenticated Subscriber in the subscribers Vector.
	 * Does not publish a Subscription back to the originating Subscriber.
	 */
	private def broadcastOneSubscription(originator: Subscriber, subscription: Subscription) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: Vector[Subscriber] = subscribers.filter(_.authenticated).filter(_ != originator)

		if(authSubscribers.size > 0) {
			log.info("Broadcasting Subscription of {} to {} authenticated Subscribers", originator.actorRefDA, authSubscribers.size)
			for (actSubscriber <- authSubscribers) {
				actSubscriber.actorRefDA ! DiscoveryPublication(subscription)
			}
		}
	}
	
	private def publishAllSubscriptions(receiver: Subscriber) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: Vector[Subscriber] = subscribers.filter(_.authenticated).filter(_ != receiver)
		val authSubscriptions: Iterable[Subscription] = subscriptions.filterKeys(authSubscribers.map(_.actorRefDA).contains).map(_._2)

		if(authSubscriptions.size > 0){
			log.info("Initial Publication to {}", receiver.actorRefDA)
			for (actSubscription <-  authSubscriptions) {
				receiver.actorRefDA ! DiscoveryPublication(actSubscription)
			}
		}
	}


  private def startOVXInstance(): (InetAddress, Int, Int) = {
    //TODO: Implement shortcut: just read OVX config and start OVX manually

    val ovxIp = InetAddress.getLocalHost
    val ovxApiPort = 8080
    val ovxCtrlPort = 6633 //TODO: check in OVX Config

    return (ovxIp, ovxApiPort, ovxCtrlPort)
  }
}
