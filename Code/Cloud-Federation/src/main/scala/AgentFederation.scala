import agents.CCFM
import akka.actor.{Props, ActorSystem}

object AgentFederation extends App
{
	val system = ActorSystem("Cloud-Federation")

	val pubSubServerAddr = "akka.tcp://actorSystemName@10.0.0.1:2552/user/actorName"

	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

	println("CCFM-Agent established!")
}
