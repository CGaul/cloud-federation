package agents

import java.net.InetAddress
import java.util.NoSuchElementException

import akka.actor._
import connectors.FederationConfigurator
import datatypes.{OvxInstance, Subscriber, Subscription}
import messages._

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class FederationBroker(fedConfig: FederationConfigurator) extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

  /**
   * Subscriptions are mapped from DA-ActorRef -> Subscription, including MMA-ActorRef
   */
  private var subscriptions : Map[ActorRef, Subscription] = Map()
  /**
   * List of DA-ActorRefs, including internal attributes (authenticated? and OVX-F)
   */
  private var subscribers : List[Subscriber] = List()

  /**
   * Includes already assigned OVXInstances, so that it is clear to see, which OVX-F's are
   * free for incoming OvxInstanceRequests.
   */
  private var assignedOvxInstances: List[OvxInstance] = List()


/* Methods: */
/* ======== */

  override def receive: Receive = {
	 case message: PubSubDiscoveryDest	=> message match {
     case DiscoverySubscription(subscription) => recvDiscoverySubscription(subscription)
     case AuthenticationAnswer(solvedKey)     => recvAuthenticationAnswer(solvedKey)
   }
     case message: PubSubFederationDest => message match {
     case OvxInstanceRequest(subscription)  => recvOVXInstanceRequest(subscription)
	 }
	 case _	=> log.error("Unknown message received!")
  }

  /**
   * Received from DiscoveryAgent
   * @param newSubscription
   */
  def recvDiscoverySubscription(newSubscription: Subscription): Unit = {
		log.info("Received DiscoverySubscription from {}", sender())
	 	//When a new DiscoverySubscription drops in, save that sender as an unauthenticated subscriber:
		val newSubscriber = Subscriber(sender(), authenticated = false, None)
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

		val encrSecCheck = Math.random().toLong //FIXME: Write out Shortcut implementation.
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
			if(solvedKey == 0){ //FIXME: Write out Shortcut implementation for solvedKey.
				val index: Int = subscribers.indexOf(registeredSubscriber.get)
				// Replace old subscriber in subscribers List with authenticated Subscriber:
				val authSubscriber = Subscriber(subscribers(index).actorRefDA, authenticated = true, None)
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
   * Received from MatchMakingAgent
   * @param subscription
   */
  def recvOVXInstanceRequest(subscription: Subscription) = {
    // After setting up an OVX instance for federation, tell both cloud MMA-Actors where the OVX-Instance is found:
    val ovxInstance = startOVXInstance()
    sender() ! OvxInstanceReply(ovxInstance)

    val masterSubscriptionOpt = subscriptions.find(subscr => subscr._2.actorRefMMA == sender())
    masterSubscriptionOpt match{
        case Some(masterSubscr) =>
          // IMPLICIT: If an entry in the subscriptions-map exist, whose mapping is ActorRefDA -> Subscription,
          // A subscriber is registered in the subscribers list:
          val newOvxSubscriber = subscribers.find(masterSubscr._1 == _.actorRefDA).get
          
          //Update the newOvxSubscriber with the newly assigned OVX-F instance in the subscribers list:
          val updSubscriber = new Subscriber(newOvxSubscriber.actorRefDA, newOvxSubscriber.authenticated, Some(ovxInstance))
          subscribers = subscribers.updated(subscribers.indexOf(newOvxSubscriber), updSubscriber)
          log.info("Subscriber {} is the new master of a federation and owns federated {}", sender(), ovxInstance)
          
        case None =>
          log.error("OvxInstanceRequest was received before DiscvorySubscription was completed for {}!", sender())
    }
  }



  /* Private Methods: */
  /* ================ */

	/**
	 * Publishes a subscription to each authenticated Subscriber in the subscribers List.
	 * Does not publish a Subscription back to the originating Subscriber.
	 */
	private def broadcastOneSubscription(originator: Subscriber, subscription: Subscription) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: List[Subscriber] = subscribers.filter(_.authenticated).filter(_ != originator)

		if(authSubscribers.size > 0) {
			log.info("Broadcasting Subscription of {} to {} authenticated Subscribers", originator.actorRefDA, authSubscribers.size)
			for (actSubscriber <- authSubscribers) {
				actSubscriber.actorRefDA ! DiscoveryPublication(subscription)
			}
		}
	}
	
	private def publishAllSubscriptions(receiver: Subscriber) = {
		// Filter all authenticated Subscribers without the originated subscriber:
		val authSubscribers: List[Subscriber] = subscribers.filter(_.authenticated).filter(_ != receiver)
		val authSubscriptions: Iterable[Subscription] = subscriptions.filterKeys(authSubscribers.map(_.actorRefDA).contains).map(_._2)

		if(authSubscriptions.size > 0){
			log.info("Initial Publication to {}", receiver.actorRefDA)
			for (actSubscription <-  authSubscriptions) {
				receiver.actorRefDA ! DiscoveryPublication(actSubscription)
			}
		}
	}


  private def startOVXInstance(): OvxInstance = {
    //FIXME change shortcut Implementation: Currently just read OVX config and start OVX manually. Better: Start OVX programmatically

    // Get all unassigned OVX-Instances from the federationConfig:
    val unassignedOvxInstances = fedConfig.ovxInstances.filterNot(assignedOvxInstances.contains)
    
    // Assign the first OVX-Instance for the current OVX-startup request:
		try {
			val startedOvx = unassignedOvxInstances.head
			assignedOvxInstances = assignedOvxInstances :+ startedOvx
			return startedOvx
		}
		catch {
			case e: NoSuchElementException =>
				log.warning("No unassigned OVX Instance could be found, starting new one...")
				return OvxInstance(InetAddress.getLocalHost, 8080, 6633, federator = false)
		}
  }
}

/** 
 * Companion Object for PubSubFederator with Akka Actor spawning context
 */
object FederationBroker {
    /**
     * props-method is used in the AKKA-Context, spawning a new Actor.
     * @return An Akka Properties-Object
     */
    def props(fedConfig: FederationConfigurator):
    Props = Props(new FederationBroker(fedConfig))
}


