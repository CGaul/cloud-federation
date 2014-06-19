import agents.CCFM
import akka.actor.{ActorSelection, Props, ActorSystem}

object AgentFederation extends App
{
	val system = ActorSystem("Cloud-Federation")

	val pubSubServerAddr: ActorSelection = system.actorSelection("akka.tcp://actorSystemName@10.0.0.1:2552/user/actorName")

	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

	println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmAgent)
}
