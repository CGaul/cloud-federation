import agents.CCFM
import akka.actor.{Props, ActorSystem}

object AgentFederation extends App {
	print("Hello World!")

	val system = ActorSystem("Cloud-Federation")
	val ccfmAgent = system.actorOf(Props[CCFM], name="CCFM")

	print("CCFM Agent established!")
}
