import agents.CCFM
import akka.actor.{ActorSelection, Props, ActorSystem}

object AgentFederation extends App
{
	val system = ActorSystem("Cloud-Federation")

	val pubSubServerAddr: ActorSelection = system.actorSelection("/user/pubSubFederator")

	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

	println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmAgent)
}
