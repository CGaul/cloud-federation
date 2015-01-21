package integration

import java.io.File
import java.net.InetAddress

import agents.{DiscoveryAgent, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes.CloudCurrency._
import datatypes.ImgFormat._
import datatypes._
import messages.{AuthenticationAnswer, AuthenticationInquiry, DiscoveryPublication, DiscoverySubscription}
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class PubSubSystemTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll with GivenWhenThen {

/* Global Values: */
/* ============== */

	val cloudSLA = new CloudSLA(Vector((SMALL, Price(1, CLOUD_CREDIT), Price(3, CLOUD_CREDIT))),
		(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)),
		(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)))

	val hostSLA = new HostSLA(0.95f, Vector(IMG, COW, CLOOP, BOCHS, QCOW2),
									Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 10)))

	//General Medium Node
	val res1 = Resource(ResId(1), MEDIUM,
							ByteSize(16, GiB), ByteSize(320, GiB),
							ByteSize(50, MB), 10, Vector())

	//General Large Node
	val res2= Resource(ResId(2), LARGE,
							ByteSize(32, GiB), ByteSize(500, GiB),
							ByteSize(50, MB), 10, Vector())
	
	val host1 : Host = Host(res1, Endpoint("00:00:00:00:00:01:11:00", 1), InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA)
	val host2 : Host = Host(res2, Endpoint("00:00:00:00:00:02:11:00", 1), InetAddress.getByName("192.168.1.1"), "00:00:00:02", Vector(), hostSLA)

	val cloudHosts: Vector[Host] = Vector(host1, host2)

	val ovxIP = InetAddress.getLocalHost



/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("pubSubSystem"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


	val testMMAActorSel0: ActorSelection = system.actorSelection("/user/testMMA0")
	val testMMAActorSel1: ActorSelection = system.actorSelection("/user/testMMA1")
	val testMMAActorSel2: ActorSelection = system.actorSelection("/user/testMMA2")
	val discoveryAgent1Props: Props 		 = Props(classOf[DiscoveryAgent],
																								system.actorSelection("/user/remoteFederator"),
																								testMMAActorSel1, new File("Certificate1"))
	val discoveryAgent2Props: Props 		 = Props(classOf[DiscoveryAgent],
																								system.actorSelection("/user/remoteFederator"),
																								testMMAActorSel2, new File("Certificate2"))

	val testActor_PubSub 	= TestActorRef[PubSubFederator](Props[PubSubFederator], name="remoteFederator")
	val testActor_DA1 		= TestActorRef[DiscoveryAgent](discoveryAgent1Props, name="discoveryAgent1")
	val testActor_DA2 		= TestActorRef[DiscoveryAgent](discoveryAgent2Props, name="discoveryAgent2")

	val subscription0 = Subscription(testMMAActorSel0, cloudSLA, cloudHosts.map(_.sla).distinct, new File("Certificate0"))
	val subscription1 = Subscription(testMMAActorSel1, cloudSLA, cloudHosts.map(_.sla).distinct, new File("Certificate1"))
	val subscription2 = Subscription(testMMAActorSel2, cloudSLA, cloudHosts.map(_.sla).distinct, new File("Certificate2"))



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
		"receive a DiscoveryPublication on a second DiscoverySubscription, from another Agent" in {
			Given("Second Subscriber: DiscoveryAgent 1")
			Thread.sleep(100)
			When("Subscriber 2 subscribed at PubSubFederator")
			testActor_PubSub.tell(DiscoverySubscription(subscription1), testActor_DA1)
			//testActor_PubSub.tell(AuthenticationAnswer(0), testActor_DA1)
			Then("Subscription1 should be received as DiscoveryPublication on first Subscriber: TestActor")
			expectMsg(DiscoveryPublication(subscription1))
		}
		"on receive a DiscoveryPublication on both previously subscribed Senders, " +
			"on a third DiscoverySubscription, from another Agent" in {
			Given("Third Subscriber: DiscoveryAgent 2")
			Thread.sleep(100)
			testActor_PubSub.tell(DiscoverySubscription(subscription2), testActor_DA2)
			//testActor_PubSub.tell(AuthenticationAnswer(0), testActor_DA2)
			expectMsg(DiscoveryPublication(subscription2))
		}

	}



}
