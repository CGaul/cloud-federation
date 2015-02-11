package integration

import java.io.File
import java.net.InetAddress

import agents.MatchMakingAgent
import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import connectors.CloudConfigurator
import datatypes.ByteUnit._
import datatypes.CPUUnit.{CPUUnit, _}
import datatypes.ImgFormat._
import datatypes._
import messages._
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class MatchMakingAgentTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll with GivenWhenThen {

/* Global Values: */
/* ============== */

  private val cloudConfFile1 = new File ("Agent-Tests/src/test/resources/cloudconf1")
  private val cloudConfFile2 = new File ("Agent-Tests/src/test/resources/cloudconf2")
	private val cloudConfig1 = CloudConfigurator(cloudConfFile1)
	private val cloudConfig2 = CloudConfigurator(cloudConfFile2)
	


/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("cloudAgentSystem"))

  override def beforeAll(): Unit = {
    require(cloudConfFile1.isDirectory, "Directory cloudconf1 needs to exist in \"Agent-Tests/src/test/resources/\"!")
    require(cloudConfFile2.isDirectory, "Directory cloudconf2 needs to exist in \"Agent-Tests/src/test/resources/\"!")
  }
  
	override def afterAll(): Unit = {
		TestKit.shutdownActorSystem(system)
	}


// The MMA Actor Generation with NRA TestProbe:
// --------------------------------------------

  // the local NRA that has a bidirectional connection from MMA <-> NRA:
  private val localNRAProbe = TestProbe()

  // global PubSub-Federator Probe, where each MMA and DA are connected to:
  private val federatorProbe: TestProbe = TestProbe()

  private val nraTestSel = system.actorSelection(localNRAProbe.ref.path)
  private val federatorTestSel = system.actorSelection(federatorProbe.ref.path)
	private val mmaProps1: Props = Props(classOf[MatchMakingAgent], cloudConfig1, federatorTestSel, nraTestSel)
	private val localMMATestActor 	= TestActorRef[MatchMakingAgent](mmaProps1, name = "matchMakingAgent")


/* Test Specifications: */
/* ==================== */

	"A MatchMakingAgent's recv-methods" should {
    // testProbe, registered at MMA_1 in order to test message handling from MMA_1 -> foreign MMA (testProbe):
    val foreignMMAProbe: TestProbe = TestProbe()
    
    // MatchMakingAgent-Tests Resources:
    val (resAlloc1, tenant1) = MatchMakingAgentTest.prepareTestResources(cloudConfig1)
    
    // Federator OVX-Instance:
    val ovxInstanceFed = OvxInstance(InetAddress.getLoopbackAddress, 1234, 5678, federator = true)
    
    
    // recvDiscoveryPublication:
		"subscribe with a discovered foreign MMA, after a DiscoveryPublication was received from its local DA" in {
			val subscription = Subscription(foreignMMAProbe.ref, cloudConfig2.cloudSLA,
																			cloudConfig2.cloudHosts.map(_.sla).toVector, cloudConfig2.certFile)
			foreignMMAProbe.send(localMMATestActor, DiscoveryPublication(subscription))
      foreignMMAProbe.expectMsgClass(classOf[FederationInfoSubscription])
		}
    
    // recvResourceRequest
    "send a ResourceFederationRequest to a previously subscribed foreign MMA, " +
    "if a ResourceRequest from the local NRA was received" in {
      Given("that the local MMA receives a ResourceRequest from the local NRA")
      localNRAProbe.send(localMMATestActor, ResourceRequest(tenant1, resAlloc1))
      
      When("the federator receives a valid OvxInstanceRequest from the local MMA and answers with a OvxInstanceReply")
      val expectedSubscription = Subscription(localMMATestActor, cloudConfig1.cloudSLA,
        cloudConfig1.cloudHosts.map(_.sla).toVector, cloudConfig1.certFile)
      federatorProbe.expectMsg(OvxInstanceRequest(expectedSubscription))
      federatorProbe.reply(OvxInstanceReply(ovxInstanceFed))
      
      Then("the local MMA should send a ResourceFederationRequest to the foreign MMA, including all information, " +
        "necessary to form a federation")
      foreignMMAProbe.expectMsg(ResourceFederationRequest(tenant1, localMMATestActor.underlyingActor.localGWSwitch, 
                                                          resAlloc1, ovxInstanceFed))
    }
    
    // recvResourceFederationRequest #1 (no auctioned Resources for foreign MMA)
    "send an unsucessful ResourceFederationReply, if a ResourceFederationRequest was received from" +
      " a foreign MMA that has no auctionedResources at the local MMA" in {
      When("a foreign MMA sends a ResourceFederationRequest to the local MMA and the local MMA has no saved auctioned Resources for the foreign MMA")
      foreignMMAProbe.send(localMMATestActor, ResourceFederationRequest(tenant1, 
                           cloudConfig2.cloudGateway, resAlloc1, ovxInstanceFed))
      
      Then("the localMMA should answer with a negative ResourceFederationReply (wasFederated = false)")
      foreignMMAProbe.expectMsg(ResourceFederationReply(tenant1,
                                          localMMATestActor.underlyingActor.localGWSwitch, resAlloc1, wasFederated = false))
    }
    
    // recvResourceFederationReply #1 (federation successful)
    "send a ResourceFederationResult to the local NRA, " +
      "if a successfully federated ResourceFederationReply was received from a foreign MMA" in {
      When("a foreign MMA sends a positive (wasFederated = true) ResourceFederationReply to the local MMA")
      foreignMMAProbe.send(localMMATestActor, ResourceFederationReply(tenant1,
                                          cloudConfig2.cloudGateway, resAlloc1, wasFederated = true))
      
      Then("the local MMA should send a ResourceFederationResult to the local NRA, including all information, " +
        "neccessary to form a federation on the Network Layer")
      localNRAProbe.expectMsg(ResourceFederationResult(tenant1,
                                          cloudConfig2.cloudGateway, resAlloc1, ovxInstanceFed))
    }

    // recvResourceFederationReply #2 (federation unsuccessful)
    "send a ResourceFederationRequest to the next outstanding MMA in list, " +
      "if an unsucessful ResourceFederationReply was received from a foreign MMA" in {
      When("a foreign MMA sends a negative (wasFederated = false) ResourceFederationReply to the local MMA")
      foreignMMAProbe.send(localMMATestActor, ResourceFederationReply(tenant1,
                                          localMMATestActor.underlyingActor.localGWSwitch, resAlloc1, wasFederated = false))
      
      Then("nothing should happen after that")
    }
    
    // recvFederationInfoSubscription
    //TODO
    
    //recvFederationInfoPublication
    //TODO
	}
}

/** 
 * Companion Object for MatchMakingAgentTest
 */
object MatchMakingAgentTest 
{
  def prepareTestResources(cloudConfig: CloudConfigurator): (ResourceAlloc, Tenant) = {
    // ResourceAlloc, used in test-cases:
    val res1 : Resource = Resource(	ResId(1), SMALL, ByteSize(4.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
    val res2 : Resource = Resource(	ResId(2), MEDIUM, ByteSize(8.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
  
    val reqHostSLA1 = new HostSLA(0.90f, Vector(IMG, COW),
      Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3)))
  
    val resAlloc1 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](res1, res2), 	reqHostSLA1)
  
    // Tenant, used in test-cases:
    val tenant1 = cloudConfig.cloudTenants(0)   
    
    return (resAlloc1, tenant1)
  }
}
