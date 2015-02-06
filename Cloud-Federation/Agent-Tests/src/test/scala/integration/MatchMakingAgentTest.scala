package integration

import java.io.File

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
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class MatchMakingAgentTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll {

/* Global Values: */
/* ============== */

	private val cloudConfig1 = CloudConfigurator(new File ("Agent-Tests/src/test/resources/cloudconf1"))
	private val cloudConfig2 = CloudConfigurator(new File ("Agent-Tests/src/test/resources/cloudconf2"))
	


/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("cloudAgentSystem"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


// The MMA Actor Generation with NRA TestProbe:
// --------------------------------------------
  
  private val nraTestProbe = TestProbe()
  private val nraTestSelection = system.actorSelection(nraTestProbe.ref.path)
	private val mmaProps1: Props = Props(classOf[MatchMakingAgent], cloudConfig1, nraTestSelection)
	private val tActorRefMMA1 	= TestActorRef[MatchMakingAgent](mmaProps1, name = "matchMakingAgent")
  

/* Test Specifications: */
/* ==================== */

	"A MatchMakingAgent's recv-methods" should {
    // testProbe, registered at MMA_1 in order to test message handling from MMA_1 -> foreign MMA (testProbe)
    val mmaTestProbe: TestProbe = TestProbe()
    
    // MatchMakingAgent-Tests Resources:
    val (resAlloc1, tenant1) = MatchMakingAgentTest.prepareTestResources(cloudConfig1)
    
    // recvDiscoveryPublication:
		"subscribe with a discovered foreign MMA, after a DiscoveryPublication was received from its local DA" in {
			val subscription = Subscription(mmaTestProbe.ref, cloudConfig2.cloudSLA,
																			cloudConfig2.cloudHosts.map(_.sla).toVector, cloudConfig2.certFile)
			mmaTestProbe.send(tActorRefMMA1, DiscoveryPublication(subscription))
      mmaTestProbe.expectMsgClass(classOf[FederationInfoSubscription])
		}
    
    // recvResourceRequest
    "send a ResourceFederationRequest to a previously subscribed foreign MMA, " +
    "if a ResourceRequest from the local NRA was received" in {
      mmaTestProbe.send(tActorRefMMA1, ResourceRequest(tenant1, resAlloc1))
      mmaTestProbe.expectMsg(ResourceFederationRequest(tenant1,
                                          tActorRefMMA1.underlyingActor.localGWSwitch, resAlloc1))
    }
    
    // recvResourceFederationRequest #1 (no auctioned Resources for foreign MMA)
    "send an unsucessful ResourceFederationReply, if a ResourceFederationRequest was received from" +
      " a foreign MMA that has no auctionedResources at the local MMA" in {
      mmaTestProbe.send(tActorRefMMA1, ResourceFederationRequest(tenant1,
                                          cloudConfig2.cloudGateway, resAlloc1))
      mmaTestProbe.expectMsg(ResourceFederationReply(tenant1,
                                          tActorRefMMA1.underlyingActor.localGWSwitch, resAlloc1, wasFederated = false))
    }
    
    // recvResourceFederationReply #1 (federation successful)
    "send a ResourceFederationResult to the local NRA, " +
      "if a successfully federated ResourceFederationReply was received from a foreign MMA" in {
      mmaTestProbe.send(tActorRefMMA1, ResourceFederationReply(tenant1,
                                          cloudConfig2.cloudGateway, resAlloc1, wasFederated = true))
      nraTestProbe.expectMsg(ResourceFederationResult(tenant1,
                                          cloudConfig2.cloudGateway, resAlloc1))
    }

    // recvResourceFederationReply #2 (federation unsuccessful)
    "send a ResourceFederationRequest to the next outstanding MMA in list, " +
      "if an unsucessful ResourceFederationReply was received from a foreign MMA" in {
      mmaTestProbe.send(tActorRefMMA1, ResourceFederationReply(tenant1,
                                          tActorRefMMA1.underlyingActor.localGWSwitch, resAlloc1, wasFederated = false))
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
