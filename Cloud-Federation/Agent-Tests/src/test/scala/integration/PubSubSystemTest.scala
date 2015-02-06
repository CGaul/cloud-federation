package integration

import java.io.File

import agents.{DiscoveryAgent, MatchMakingAgent, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import connectors.CloudConfigurator
import datatypes._
import messages.{AuthenticationAnswer, AuthenticationInquiry, DiscoverySubscription}
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class PubSubSystemTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll with GivenWhenThen {

/* Global Values: */
/* ============== */

	val cloudConfig = CloudConfigurator(new File ("Agent-Tests/src/test/resources/cloudconf1"))



/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("pubSubSystem"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


//	val testMMAActorSel0: ActorSelection = system.actorSelection("/user/testMMA0")
//	val testMMAActorSel1: ActorSelection = system.actorSelection("/user/testMMA1")
//	val testMMAActorSel2: ActorSelection = system.actorSelection("/user/testMMA2")

// ActorSelections and Naming
// --------------------------
	
  //Federator:
  val pubSubName = "remoteFederator"
  val pubSubSelection: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+pubSubName)
  
  //MMA:
	val mmaName1 = "matchMakingAgent_1"
	val mmaName2 = "matchMakingAgent_2"
	val mmaName3 = "matchMakingAgent_3"
	val mmaSelection1: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+mmaName1)
	val mmaSelection2: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+mmaName2)
	val mmaSelection3: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+mmaName3)

  //NRA:
	val nraName1 = "networkResourceAgent_1"
	val nraName2 = "networkResourceAgent_2"
	val nraName3 = "networkResourceAgent_3"
	val nraSelection1: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+nraName1)
	val nraSelection2: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+nraName2)
	val nraSelection3: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/"+nraName3)
	
	
// The MMA Actor Generation:
// -------------------------
	
	val mmaProps1 = Props(classOf[MatchMakingAgent], cloudConfig, nraSelection1)
	val mmaProps2 = Props(classOf[MatchMakingAgent], cloudConfig, nraSelection2)
	val mmaProps3 = Props(classOf[MatchMakingAgent], cloudConfig, nraSelection3)
	val mmaActor1 = TestActorRef[MatchMakingAgent](mmaProps1, name = mmaName1)
	val mmaActor2 = TestActorRef[MatchMakingAgent](mmaProps2, name = mmaName2)
	val mmaActor3 = TestActorRef[MatchMakingAgent](mmaProps3, name = mmaName3)
	
	val discoveryAgent1Props: Props 		 = Props(classOf[DiscoveryAgent], cloudConfig,
																								pubSubSelection, mmaSelection1)
	val discoveryAgent2Props: Props 		 = Props(classOf[DiscoveryAgent], cloudConfig,
																								pubSubSelection, mmaSelection2)

	val testActor_PubSub 	= TestActorRef[PubSubFederator](Props[PubSubFederator], name=pubSubName)
	val testActor_DA1 		= TestActorRef[DiscoveryAgent](discoveryAgent1Props, name="discoveryAgent1")
	val testActor_DA2 		= TestActorRef[DiscoveryAgent](discoveryAgent2Props, name="discoveryAgent2")

	val subscription0 = Subscription(mmaActor1, cloudConfig.cloudSLA, cloudConfig.cloudHosts.map(_.sla).distinct, new File("Certificate0"))
	val subscription1 = Subscription(mmaActor2, cloudConfig.cloudSLA, cloudConfig.cloudHosts.map(_.sla).distinct, new File("Certificate1"))
	val subscription2 = Subscription(mmaActor3, cloudConfig.cloudSLA, cloudConfig.cloudHosts.map(_.sla).distinct, new File("Certificate2"))



/* Test Specifications: */
/* ==================== */

	"A PubSubFederator" should {
		"send an AuthenticationInquiry back, when a DiscoverySubscription drops in" in{
			Given("First Subscriber: TestActor")
			Thread.sleep(100)
			When("Subscriber 1 subscribed at PubSubFederator")
			testActor_PubSub.tell(DiscoverySubscription(subscription0), testActor)
			expectMsgClass(classOf[AuthenticationInquiry])
			testActor_PubSub.tell(AuthenticationAnswer(0), testActor)
			Then("Nothing should happen")
		}
//    TODO: rewrite
//		"receive a DiscoveryPublication on a second DiscoverySubscription, from another Agent" in {
//			Given("Second Subscriber: DiscoveryAgent 1")
//			Thread.sleep(100)
//			When("Subscriber 2 subscribed at PubSubFederator")
//			testActor_PubSub.tell(DiscoverySubscription(subscription1), testActor_DA1)
//			//testActor_PubSub.tell(AuthenticationAnswer(0), testActor_DA1)
//			Then("Subscription1 should be received as DiscoveryPublication on first Subscriber: TestActor")
//			expectMsg(DiscoveryPublication(subscription1))
//		}
//		"on receive a DiscoveryPublication on both previously subscribed Senders, " +
//			"on a third DiscoverySubscription, from another Agent" in {
//			Given("Third Subscriber: DiscoveryAgent 2")
//			Thread.sleep(100)
//			testActor_PubSub.tell(DiscoverySubscription(subscription2), testActor_DA2)
//			//testActor_PubSub.tell(AuthenticationAnswer(0), testActor_DA2)
//			expectMsg(DiscoveryPublication(subscription2))
//		}

	}



}
