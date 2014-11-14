import agents.{CCFM, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory


object CloudAgentManagement extends App
{
	val config = ConfigFactory.load("localApplication.conf")
	val system = ActorSystem("CloudAgents", config.getConfig("cloudAgents").withFallback(config))

	//	Applied on the remote side of the PubSubSystem:
	val pubSubActorName = "remoteFederator"
	val pubSubActor = system.actorOf(Props[PubSubFederator], name=pubSubActorName)
	val pubSubActorSelection: ActorSelection = system.actorSelection("/user/"+pubSubActorName)

	val ccfmProps = Props(classOf[CCFM], pubSubActorSelection)
	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

  println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmAgent)
}
