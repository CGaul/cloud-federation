import agents.{CCFM, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}

object AgentFederation extends App
{
	val system = ActorSystem("Cloud-Federation")

  	val pubSubFederator = "localFederator"
  	val pubSubServer = system.actorOf(Props[PubSubFederator], name=pubSubFederator)
	val pubSubServerAddr: ActorSelection = system.actorSelection("/user/"+pubSubFederator)


	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

	println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmAgent)
}
