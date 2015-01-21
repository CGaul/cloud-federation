package integration

import java.net.InetAddress

import agents.{NetworkDiscoveryAgent, MatchMakingAgent, NetworkResourceAgent}
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes.CloudCurrency._
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

	val cloudSLA = new CloudSLA(Vector((SMALL, Price(1, CLOUD_CREDIT), Price(3, CLOUD_CREDIT))),
		(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)),
		(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)))

	val hostSLA = new HostSLA(0.95f, Vector(IMG, COW, CLOOP, BOCHS, QCOW2),
									Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 10)))

	//General Medium Node
	val hwspec1 = Resource(ResId(11), MEDIUM,
							ByteSize(16, GiB), ByteSize(320, GiB),
							ByteSize(50, MB), 10, Vector())

	//General Large Node
	val hwspec2= Resource(ResId(12), LARGE,
							ByteSize(32, GiB), ByteSize(500, GiB),
							ByteSize(50, MB), 10, Vector())

	val host1 : Host = Host(hwspec1, Endpoint("00:00:00:00:00:01:11:00", 1),
													InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA)
	val host2 : Host = Host(hwspec2, Endpoint("00:00:00:00:00:02:11:00", 1),
													InetAddress.getByName("192.168.1.2"), "00:00:00:02", Vector(), hostSLA)

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

	val cloudHosts: Vector[Host] = Vector(host1, host2)

	val cloudSwitches: Vector[OFSwitch] = Vector(OFSwitch("00:00:10:00"))

	val ovxIp = InetAddress.getLocalHost
	val ovxApiPort = 8080
	
	val tenant1: Tenant = Tenant(1, ("10.1.1.10", 16), InetAddress.getByName("192.168.1.42"), 10000)
	


/* AKKA Testing Environment: */
/* ========================= */

	val config = ConfigFactory.load("testApplication.conf")
	def this() = this(ActorSystem("cloudAgentSystem"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


	//val mmaSelection: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/CCFM/matchMakingAgent")
	val nraSelection: ActorSelection = system.actorSelection("akka://cloudAgentSystem/user/CCFM/networkResourceAgent")

	val mmaProps: Props = Props(classOf[MatchMakingAgent], cloudSLA, nraSelection)
	val tActorRefMMA 	= TestActorRef[MatchMakingAgent](mmaProps)

	val nraProps:	Props = Props(classOf[NetworkResourceAgent],
															ovxIp, ovxApiPort, cloudHosts.toList,
															tActorRefMMA)
	val tActorRefNRA 	= TestActorRef[NetworkResourceAgent](nraProps)
	
	val ndaProps: Props = Props(classOf[NetworkDiscoveryAgent], ovxIp, ovxApiPort, tActorRefNRA)
	val tActorRefNDA 	= TestActorRef[NetworkDiscoveryAgent](ndaProps)
	



	/* Test Specifications: */
	/* ==================== */

	"A NetworkDiscoveryAgent" should {
		"be able to discover the physical Topology" in{
			tActorRefNDA.receive("start")
		}
	}
	"A NetworkResourceAgent" should {
		"answer with a ResourceReply, if ResourceRequests are locally fulfillable" in {
			//TODO: Write out Shortcut implementation:
			tActorRefNRA.receive(ResourceRequest(tenant1, resAlloc1))
			tActorRefNRA.receive(ResourceRequest(tenant1, resAlloc2))
		}

		"try to allocate Resources via a Federation, if ResourceRequests are NOT locally fulfillable" in{
			//TODO: Write out Shortcut implementation:
			tActorRefNRA.receive(ResourceRequest(tenant1, resAlloc3))
		}
	}
}
