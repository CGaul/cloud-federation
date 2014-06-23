import agents.{CCFM, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object CloudAgentManagement extends App
{
  	val config = ConfigFactory.load("remoteApplication.conf")
	val system = ActorSystem("CloudAgents", config.getConfig("cloudAgents").withFallback(config))

  	val pubSubFederator = "remoteFederator"
//	Applied on the remote side of the PubSubSystem:
//	val pubSubServer = system.actorOf(Props[PubSubFederator], name=pubSubFederator)
	val pubSubServerAddr: ActorSelection = system.actorSelection("/user/"+pubSubFederator)


	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

	println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmAgent)
}
