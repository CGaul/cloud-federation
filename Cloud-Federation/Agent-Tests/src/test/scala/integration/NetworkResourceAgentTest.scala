package integration

import java.io.File

import agents.{MatchMakingAgent, NetworkResourceAgent}
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import connectors.CloudConfigurator
import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes._
import messages.ResourceRequest
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class NetworkResourceAgentTest (_system: ActorSystem) extends TestKit(_system)
	with WordSpecLike with Matchers with BeforeAndAfterAll {

/* Global Values: */
/* ============== */

	val cloudConfig1 = CloudConfigurator(new File ("Agent-Tests/src/test/resources/cloudconf1"))

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
	val resAlloc3 : ResourceAlloc = ResourceAlloc(2, Vector[Resource](res4, res5), 	reqHostSLA3)

	val cloudHosts: Vector[Host] = Vector(cloudConfig1.cloudHosts(0), cloudConfig1.cloudHosts(1))

	val cloudSwitches: Vector[OFSwitch] = Vector(OFSwitch("00:00:10:00"))


/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("cloudAgentSystem"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


	//val mmaSelection: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/matchMakingAgent")
	val nraSelection: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/networkResourceAgent")

	val mmaProps: Props = Props(classOf[MatchMakingAgent], cloudConfig1, nraSelection)
	val tActorRefMMA 	= TestActorRef[MatchMakingAgent](mmaProps)

	val nraProps:	Props = Props(classOf[NetworkResourceAgent], cloudConfig1, tActorRefMMA)
	val tActorRefNRA 	= TestActorRef[NetworkResourceAgent](nraProps)
	
//	val ndaProps: Props = Props(classOf[NetworkDiscoveryAgent], ovxIp, ovxApiPort, tActorRefNRA)
//	val tActorRefNDA 	= TestActorRef[NetworkDiscoveryAgent](ndaProps)
	
	Thread.sleep(10000) // Wait for 10 seconds (this should be enough for the first two NDA TopologyDiscoveries)



	/* Test Specifications: */
	/* ==================== */

//	"A NetworkDiscoveryAgent" should {
//		"be able to discover the physical Topology" in{
//			tActorRefNDA.receive("start")
//		}
//	}
	"A NetworkResourceAgent" should {
		"answer with a ResourceReply, if ResourceRequests are locally fulfillable" in {
			//TODO: Write out Shortcut implementation:
			tActorRefNRA.receive(ResourceRequest(cloudConfig1.cloudTenants(0), resAlloc1))
			tActorRefNRA.receive(ResourceRequest(cloudConfig1.cloudTenants(0), resAlloc2))
		}

		"try to allocate Resources via a Federation, if ResourceRequests are NOT locally fulfillable" in{
			//TODO: Write out Shortcut implementation:
			tActorRefNRA.receive(ResourceRequest(cloudConfig1.cloudTenants(0), resAlloc3))
		}
	}
}
