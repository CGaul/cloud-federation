package agents

import java.security.cert.Certificate

import akka.actor._
import datatypes.HostSLA
import messages.{DiscoveryPublication, DiscoverySubscription, PubSubFederatorReply}

/**
 * @author Constantin Gaul, created on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

  private var subscriberRefs : Vector[(ActorRef, Vector[HostSLA])] = Vector()
  private var subscriberPaths : Vector[ActorPath] = Vector()
  private var subscriberCerts : Vector[(ActorRef, Certificate)] = Vector()


/* Methods: */
/* ======== */

	//TODO: use?!
  def publish(message: Unit) = {
	 for (subscriber <- subscriberRefs){
		subscriber._1 ! message
	 }
  }

  override def receive(): Receive = {
	 case message: PubSubFederatorReply	=> message match {
     case DiscoverySubscription(slaList, cert)	=> recvDiscoverySubscription(slaList, cert)
	 }
	 case _										=> log.error("Unknown message received!")
  }

  def recvDiscoverySubscription(slaList: Vector[HostSLA], cert: Certificate): Unit = {

	 //When a new DiscoverySubscription drops in, save that sender as a subscriber:
	 val subscriber: ActorRef = sender()
	 subscriberRefs = subscriberRefs :+ (subscriber, slaList)
	 subscriberPaths = subscriberPaths :+ subscriber.path
	 subscriberCerts = subscriberCerts :+ (subscriber, cert)
	 log.info("Received Cloud Subscription. Subscribed Sender.")
	 log.debug("Current subscriberRefs: "+ subscriberRefs)
	 log.debug("Current subscriberPaths: "+ subscriberPaths)

	 //and answer this discovery-Request with a DiscoveryPublication:
	 subscriber ! DiscoveryPublication(subscriberPaths)
  }


}
