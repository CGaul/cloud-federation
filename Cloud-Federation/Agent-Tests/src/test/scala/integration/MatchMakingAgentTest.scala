package integration

import java.io.File

import agents.MatchMakingAgent
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import connectors.CloudConfigurator
import datatypes.Subscription
import messages.DiscoveryPublication
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class MatchMakingAgentTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll {

/* Global Values: */
/* ============== */

	val cloudConfig1 = CloudConfigurator(new File ("Agent-Tests/src/test/resources/cloudconf1"))
	val cloudConfig2 = CloudConfigurator(new File ("Agent-Tests/src/test/resources/cloudconf2"))
	


/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("cloudAgentSystem"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}

	
// ActorSelections and Naming
// --------------------------
	
	val mmaName1 = "matchMakingAgent_1"
	val mmaName2 = "matchMakingAgent_2"
	val mmaSelection1: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+mmaName1)
	val mmaSelection2: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+mmaName2)
	val nraName1 = "networkResourceAgent_1"
	val nraName2 = "networkResourceAgent_2"
	val nraSelection1: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+nraName1)
	val nraSelection2: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+nraName2)
	
	
// The MMA Actor Generation:
// -------------------------
	
	val mmaProps1: Props = Props(classOf[MatchMakingAgent], cloudConfig1, nraSelection1)
	val tActorRefMMA1 	= TestActorRef[MatchMakingAgent](mmaProps1, name = mmaName1)

	val mmaProps2: Props = Props(classOf[MatchMakingAgent], cloudConfig2, nraSelection2)
	val tActorRefMMA2 	= TestActorRef[MatchMakingAgent](mmaProps2, name = mmaName2)
	
	
// The NRA Actor Generation:
// -------------------------	
	
//	val nraProps1:	Props = Props(classOf[NetworkResourceAgent], cloudConfig1.ovxIp, cloudConfig1.ovxApiPort,
//																cloudConfig1.cloudHosts.toList, tActorRefMMA1)
//	val tActorRefNRA1 	= TestActorRef[NetworkResourceAgent](nraProps1, nraName1)
//
//	val nraProps2:	Props = Props(classOf[NetworkResourceAgent], cloudConfig2.ovxIp, cloudConfig2.ovxApiPort,
//																cloudConfig2.cloudHosts.toList, tActorRefMMA2)
//	val tActorRefNRA2 	= TestActorRef[NetworkResourceAgent](nraProps1, nraName2)
	
//	Thread.sleep(10000) // Wait for 10 seconds (this should be enough for the first two NDA TopologyDiscoveries)



/* Test Specifications: */
/* ==================== */

	"A MatchMakingAgent" should {
		"be able to receive a DiscoveryPublication from its local DA and subscribe with the foreign MMA afterwards" in {
			val subscription = Subscription(tActorRefMMA2, cloudConfig2.cloudSLA,
																			cloudConfig2.cloudHosts.map(_.sla).toVector, cloudConfig2.certFile)
			tActorRefMMA1.receive(DiscoveryPublication(subscription))

		}
	}
}
