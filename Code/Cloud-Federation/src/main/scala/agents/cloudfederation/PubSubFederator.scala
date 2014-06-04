package agents

import akka.actor.{ActorLogging, ActorRef, Actor}
import messages.DiscoverySubscription
import akka.event.Logging

/**
 * Created by costa on 5/31/14.
 */
class PubSubFederator extends Actor with ActorLogging
{

/* Variables: */
/* ========== */

  private var subscriberList : List[ActorRef] = List()


/* Methods: */
/* ======== */

  def publish(message: Unit) = {
	 for (subscriber <- subscriberList){
		subscriber ! message
	 }
  }

  override def receive: Receive = {
	 case DiscoverySubscription(certificate)	=> recvDiscoverySubscription(certificate)
	 case _												=> log.error("Unknown message received!")
  }

  def recvDiscoverySubscription(certificate: String): Unit = {
	 val subscriber: ActorRef = sender()
	 subscriberList = subscriber :: subscriberList
	 log.info("Received Cloud Subscription. Subscribed Sender.")
  }


}
