package agents

import java.io.File
import java.security.cert.Certificate

import akka.actor._
import datatypes.{CloudSLA, HostSLA}
import messages.{DiscoveryPublication, DiscoverySubscription, PubSubFederatorDestination}

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

	//TODO: change cert type to "Certificate"
  private var subscriptions 	: Vector[(ActorRef, CloudSLA, Vector[HostSLA], File)] = Vector()
  private var subscribers : Vector[ActorRef] = Vector()


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

		// Send new publication to all current subscribers:
		for (actSubscriber <- subscribers) {
			actSubscriber ! DiscoveryPublication(newSubscription)
		}

		//Finally add the new subscriber to the vector of current subscribers:
		subscribers = subscribers :+ subscriber
	}


}
