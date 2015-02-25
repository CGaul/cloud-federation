package integration

import java.io.File

import agents.FederationBroker
import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import connectors.{CloudConfigurator, FederationConfigurator}
import datatypes.Subscription
import messages.{AuthenticationAnswer, AuthenticationInquiry, DiscoveryPublication, DiscoverySubscription}
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class FedBrokerIntegrationTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll with GivenWhenThen {

  /* Global Values: */
  /* ============== */

  private val federatorConfFile = new File ("Agent-Tests/src/test/resources/federatorconf")
  private val federatorConfig = FederationConfigurator(federatorConfFile)

  private val cloudConfFile1 = new File ("Agent-Tests/src/test/resources/cloudconf1")
  private val cloudConfig1 = CloudConfigurator(cloudConfFile1)
  private val cloudConfFile2 = new File ("Agent-Tests/src/test/resources/cloudconf2")
  private val cloudConfig2 = CloudConfigurator(cloudConfFile2)



/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("fedBroker"))

  override def beforeAll(): Unit = {
    require(federatorConfFile.isDirectory, "Directory federatorconf needs to exist in \"Agent-Tests/src/test/resources/\"!")
    require(cloudConfFile1.isDirectory, "Directory cloudconf1 needs to exist in \"Agent-Tests/src/test/resources/\"!")
    require(cloudConfFile2.isDirectory, "Directory cloudconf2 needs to exist in \"Agent-Tests/src/test/resources/\"!")
  }
  
	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}

  
  
  /* The FederationBroker Generation: */
  /* -------------------------------- */

  // Finally, set up the FederationBroker itself (a "real" TestActor):
  private val fedBrokerProps:	Props = Props(classOf[FederationBroker], federatorConfig)
  private val fedBrokerTestActor = TestActorRef[FederationBroker](fedBrokerProps)

  
  /* Additional TestProbes for Test-Specifications: */
  /* ---------------------------------------------- */

  // TestProbes for three different Discovery-Agents, both subscribing at the FederationBroker:
  // (The MMA-Probes are needed as they are included in the DiscoverySubscription as an ActorRef) 
  val daProbe1 = TestProbe()
  val mmaProbe1 = TestProbe()

  val daProbe2 = TestProbe()
  val mmaProbe2 = TestProbe()

  // TODO: Add Cloudconf 3 before this probes are needed.
  //    val daProbe3 = TestProbe()
  //    val mmaProbe3 = TestProbe()



/* Test Specifications: */
/* ==================== */

  /* Resource Specifications: */
  /* ------------------------ */

  val cloud1HostSLAs = cloudConfig1.cloudHosts.map(_.sla)
  val da1Subscription = Subscription(mmaProbe1.ref, cloudConfig1.cloudSLA, cloud1HostSLAs, cloudConfig1.certFile)

  val cloud2HostSLAs = cloudConfig2.cloudHosts.map(_.sla)
  val da2Subscription = Subscription(mmaProbe2.ref, cloudConfig2.cloudSLA, cloud2HostSLAs, cloudConfig2.certFile)
  
  
	"A Federation-Broker's recv-methods" should {

    "answer with an AuthenticationInquiry, if a DiscoverySubscription was received" in {
      // Discovery-Subscription -> AuthenticationInquiry for DA-1
      Given("First Subscriber: Discovery-Agent 1 (DA-1)")
      
			When("DA-1 subscribed at Federation-Broker with DiscoverySubscription")
      daProbe1.send(fedBrokerTestActor, DiscoverySubscription(da1Subscription))
      
      Then("FedBroker should answer with an AuthenticationInquiry back to DA-1")
			daProbe1.expectMsgClass(classOf[AuthenticationInquiry])

      
      // Discovery-Subscription -> AuthenticationInquiry for DA-2
      Given("Second Subscriber: Discovery-Agent 2 (DA-2)")

      When("DA-2 subscribed at Federation-Broker with DiscoverySubscription")
      daProbe2.send(fedBrokerTestActor, DiscoverySubscription(da2Subscription))

      Then("FedBroker should answer with an AuthenticationInquiry back to DA-2")
      daProbe2.expectMsgClass(classOf[AuthenticationInquiry])
		}
    
    
    "answer with a DiscoveryPublication, if a correct AuthenticationAnswer was received from a previously registered DA" in {
      // AuthenticationAnswer -> DiscoveryPublication for DA-2
      Given("First Subscriber: Discovery-Agent 1 (DA-1)")
      daProbe1.send(fedBrokerTestActor, AuthenticationAnswer(0)) //shortcut impl. for correct key
      
      Then("FedBroker should not answer with a DiscoveryPublication, as no other DA is completely registered at that time.")
      daProbe1.expectNoMsg()
      daProbe2.expectNoMsg()
//      daProbe3.expectNoMsg()


      // AuthenticationAnswer -> DiscoveryPublication for DA-2
      Given("Second Subscriber: Discovery-Agent 2 (DA-2)")

      When("DA-2 authenticated itself at Federation-Broker with AuthenticationAnswer")
      daProbe2.send(fedBrokerTestActor, AuthenticationAnswer(0)) //shortcut impl. for correct key

      Then("FedBroker should not answer with a DiscoveryPublication, as no other DA is completely registered at that time.")
      
      daProbe1.expectMsg(DiscoveryPublication(da2Subscription))
      daProbe2.expectMsg(DiscoveryPublication(da1Subscription))
//      daProbe3.expectNoMsg()
    }

	}
}
