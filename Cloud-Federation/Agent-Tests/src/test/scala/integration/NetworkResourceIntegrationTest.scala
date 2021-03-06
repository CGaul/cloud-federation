package integration

import java.io.File
import java.net.InetAddress

import agents.{DiscoveryState, NetworkResourceAgent}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import connectors.{FederationConfigurator, CloudConfigurator}
import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes._
import messages.{OvxInstanceReply, FederateableResourceDiscovery, ResourceRequest, TopologyDiscovery}
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class NetworkResourceIntegrationTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll with GivenWhenThen {

/* Global Values: */
/* ============== */

  private val cloudConfFile1 = new File ("Agent-Tests/src/test/resources/cloudconf1")
  private val cloudConfig1 = CloudConfigurator(cloudConfFile1)

  private val fedConfDir = new File("Agent-Tests/src/test/resources/federatorconf")
  private val fedConfig = new FederationConfigurator(fedConfDir)


/* AKKA Testing Environment: */
/* ========================= */

  val config = ConfigFactory.load("testApplication.conf")
  def this() = this(ActorSystem("cloudAgentSystem"))

  override def beforeAll(): Unit = {
    require(cloudConfFile1.isDirectory, "Directory cloudconf1 needs to exist in \"Agent-Tests/src/test/resources/\"!")
    require(fedConfDir.isDirectory, "Directory federatorconf needs to exist in \"Agent-Tests/src/test/resources/\"!")
  }

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


  /* The NRA Actor Generation with MMA TestProbe: */
  /* -------------------------------------------- */
  
  // Needed for the NRA to run, as MMA is bi-directionally linked from NRA <-> MMA:
  val localMMAProbe: TestProbe = TestProbe()

  // Finally, set up the NRA itself (a "real" TestActor):
	val nraProps:	Props = Props(classOf[NetworkResourceAgent], cloudConfig1, localMMAProbe.ref)
	val localNRATestActor = TestActorRef[NetworkResourceAgent](nraProps)


  /* Additional TestProbes for Test-Specifications: */
  /* ---------------------------------------------- */

  // As OVX is not expected to be running somewhere, while running this test, simulate NetworkDiscoveryAgent and its
  // TopologyDiscovery to the NRA, getting the NRA from INACTIVE to ACTIVE:
  val localNDAProbe: TestProbe = TestProbe()

  // test probe that sends ResourceRequests to the local NRA:
  val localCCFMProbe: TestProbe = TestProbe()
  
  

/* Test Specifications: */
/* ==================== */

  /* Resource Specifications: */
  /* ------------------------ */

  val ovxTestInstance = cloudConfig1.cloudOvx
  //NetworkResourceAgent Test-Topology:
  val testTopology = NetworkResourceIntegrationTest.prepareTestTopology

  // NetworkResourceAgent-Tests Resources:
  val (resAlloc1, resAlloc2, resAlloc3) = NetworkResourceIntegrationTest.prepareTestResources
  

  "A NetworkResourceAgent" should {
        
    "be OFFLINE, before TopologyDiscovery was received" in {
      localNRATestActor.underlyingActor.state should equal(DiscoveryState.OFFLINE)
    }
        
    "become ONLINE, if a TopologyDiscovery from its local NDA and a OvxInstanceReply from its local MMA is received" in {
      Given("a TopologyDiscovery was received at the NRA (from its child NDA)")
      localNDAProbe.send(localNRATestActor, TopologyDiscovery(ovxTestInstance, testTopology, List()))
      
      Then("the local MMA should receive a FederateableResourceDiscovery from the NRA")
      localMMAProbe.expectMsgClass(classOf[FederateableResourceDiscovery])
      
      Given("a OvxInstanceReply was received at the NRA (from its local MMA)")
      localMMAProbe.send(localNRATestActor, OvxInstanceReply(fedConfig.ovxInstances(0)))
      
      Then("the NRA should become ONLINE")
      localNRATestActor.underlyingActor.state should equal (DiscoveryState.ONLINE)
    }
    
  }
  
	"A NetworkResourceAgent's recv-methods" should {
        
		"answer with a ResourceReply, if ResourceRequests are locally fulfillable" in {
			Given(s"that the local NRA receives a ResourceRequest from the local CCFM, including resAlloc1: $resAlloc1")
      localCCFMProbe.send(localNRATestActor, ResourceRequest(cloudConfig1.cloudTenants(0), resAlloc1))
      
      Then("no message should be sended to the local MMA, as enough Host-Resources are available locally")
      localMMAProbe.expectNoMsg()
      // Only activate, if OVX is running on correct IP and API-Port, next to this test:
//      Then("the CCFM should get a ResourceReply immediately, as all Host-Resources could have been allocated locally")
//      localCCFMProbe.expectMsg(ResourceReply(resAlloc1))

      Given(s"that the local NRA receives another ResourceRequest from the local CCFM, including resAlloc2: $resAlloc2")
			localNRATestActor.receive(ResourceRequest(cloudConfig1.cloudTenants(0), resAlloc2))
      
      Then("no message should be sended to the local MMA, as enough Host-Resources are available locally")
      localMMAProbe.expectNoMsg()
      // Only activate, if OVX is running on correct IP and API-Port, next to this test:
//      Then("the CCFM should get a ResourceReply immediately, as all Host-Resources could have been allocated locally")
//      localCCFMProbe.expectMsg(ResourceReply(resAlloc2))
		}

    //TODO: this is more complicated now. Fix it:
//		"try to allocate Resources via a Federation, if ResourceRequests are NOT locally fulfillable" in{
//			Given(s"that the local NRA receives another ResourceRequest from the local CCFM, including resAlloc3: $resAlloc3")
//			localNRATestActor.receive(ResourceRequest(cloudConfig1.cloudTenants(0), resAlloc3))
//      
//      Then("a ResourceRequest should be sended to the local MMA, as the allocation could not be fullfilled locally")
//      localMMAProbe.expectMsgClass(classOf[ResourceRequest])
//		}
	}
}

/** 
 * Companion Object for NetworkResourceAgentTest
 */
object NetworkResourceIntegrationTest {
  def prepareTestResources: (ResourceAlloc, ResourceAlloc, ResourceAlloc) = {
    val res1 : Resource = Resource(	ResId(1), SMALL, ByteSize(4.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
    val res2 : Resource = Resource(	ResId(2), MEDIUM, ByteSize(8.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
    val res3 : Resource = Resource(	ResId(2), MEDIUM, ByteSize(8.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
    val res4 : Resource = Resource(	ResId(1), SMALL, ByteSize(8.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
    val res5 : Resource = Resource(	ResId(1), SMALL, ByteSize(4.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())

    val reqHostSLA1 = new HostSLA(0.90f, Vector(IMG, COW),
      Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3)))
    val reqHostSLA2 = new HostSLA(0.91f, Vector(IMG, CLOOP, BOCHS),
      Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 4)))
    val reqHostSLA3 = new HostSLA(0.95f, Vector(IMG, CLOOP, QCOW2),
      Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 2)))

    val resAlloc1 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](res1, res2), 	reqHostSLA1)
    val resAlloc2 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](res3),				reqHostSLA2)
    val resAlloc3 : ResourceAlloc = ResourceAlloc(2, Vector[Resource](res1, res2, res3, res4, res5), 	reqHostSLA3)
    
    return (resAlloc1, resAlloc2, resAlloc3)
  }
  
  def prepareTestTopology: List[OFSwitch] = {
    val ofSwitch1 = OFSwitch("00:00:00:00:00:01:10:00")
    val ofSwitch2 = OFSwitch("00:00:00:00:00:01:11:00")
    val ofSwitch3 = OFSwitch("00:00:00:00:00:01:12:00")
    
    // Bi-directionally link switch1:1 <-> switch2:1:
    ofSwitch1.remapPort(1, Endpoint("00:00:00:00:00:01:11:00", 1))
    ofSwitch2.remapPort(1, Endpoint("00:00:00:00:00:01:10:00", 1))

    // Bi-directionally link switch2:2 <-> switch3:1:
    ofSwitch2.remapPort(2, Endpoint("00:00:00:00:00:01:12:00", 1))
    ofSwitch2.remapPort(1, Endpoint("00:00:00:00:00:01:11:00", 2))
    
    return List(ofSwitch1, ofSwitch2, ofSwitch3)
  }
}
