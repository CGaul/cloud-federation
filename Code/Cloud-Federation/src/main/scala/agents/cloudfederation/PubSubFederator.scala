package agents

import akka.actor._
import messages.{DiscoveryPublication, DiscoverySubscription, PubSubFederatorReply}

/**
 * Created by costa on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

  private var subscriberRefs : Vector[ActorRef] = Vector()
  private var subscriberPaths : Vector[ActorPath] = Vector()
  private var subscriberCerts : Map[ActorRef, String] = Map()


/* Methods: */
/* ======== */

  def publish(message: Unit) = {
	 for (subscriber <- subscriberRefs){
		subscriber ! message
	 }
  }

  override def receive(): Receive = {
	 case message: PubSubFederatorReply	=> message match {
		case DiscoverySubscription(certificate)	=> recvDiscoverySubscription(certificate)
	 }
	 case _										=> log.error("Unknown message received!")
  }

  def recvDiscoverySubscription(certificate: String): Unit = {

	 //When a new DiscoverySubscription drops in, save that sender as a subscriber:
	 val subscriber: ActorRef = sender()
	 subscriberRefs = subscriberRefs :+ subscriber
	 subscriberPaths = subscriberPaths :+ subscriber.path
	 subscriberCerts = subscriberCerts + (subscriber -> certificate)
	 log.info("Received Cloud Subscription. Subscribed Sender.")
	 log.debug("Current subscriberRefs: "+ subscriberRefs)
	 log.debug("Current subscriberPaths: "+ subscriberPaths)

	 //and answer this discovery-Request with a DiscoveryPublication:
	 subscriber ! DiscoveryPublication(subscriberPaths)
  }


}
