import java.security.cert.Certificate

import org.junit.Test

import agents.{CCFM, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar

class CloudAgentTest extends AssertionsForJUnit with MockitoSugar{

	@Test
	def verifyCloudAgentsRunning(): Unit ={
		val config = ConfigFactory.load("localApplication.conf")
		val system = ActorSystem("CloudAgents", config.getConfig("cloudAgents").withFallback(config))

		val pubSubFederator = "remoteFederator"

		//	Applied on the remote side of the PubSubSystem:
		val pubSubServer = system.actorOf(Props[PubSubFederator], name=pubSubFederator)
		val pubSubServerAddr: ActorSelection = system.actorSelection("/user/"+pubSubFederator)


		val cloudCert = mock[Certificate]

		val ccfmProps = Props(classOf[CCFM], pubSubServerAddr, cloudCert)
		val ccfmAgent = system.actorOf(ccfmProps, name="CCFM")

		println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmAgent)
	}
}
