package integration

import java.net.InetAddress

import agents.{MatchMakingAgent, NetworkResourceAgent}
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
	val hwspec1 = Resource(CompID(11), MEDIUM,
							ByteSize(16, GiB), ByteSize(320, GiB),
							ByteSize(50, MB), 10, Vector())

	//General Large Node
	val hwspec2= Resource(CompID(12), LARGE,
							ByteSize(32, GiB), ByteSize(500, GiB),
							ByteSize(50, MB), 10, Vector())

	val host1 : Host = Host(hwspec1, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA)
	val host2 : Host = Host(hwspec2, InetAddress.getByName("192.168.1.2"), "00:00:00:02", Vector(), hostSLA)

	val res1 : Resource = Resource(	CompID(1), SMALL, ByteSize(4.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[CompID]())
	val res2 : Resource = Resource(	CompID(2), MEDIUM, ByteSize(8.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[CompID]())
	val res3 : Resource = Resource(	CompID(2), MEDIUM, ByteSize(8.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[CompID]())
	val res4 : Resource = Resource(	CompID(1), SMALL, ByteSize(8.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[CompID]())
	val res5 : Resource = Resource(	CompID(1), SMALL, ByteSize(4.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[CompID]())

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

	val cloudSwitches: Vector[Switch] = Vector(Switch(CompID(1), "00:00:10:00", Map(1->CompID(11), 2->CompID(12))))

	val ovxIP = InetAddress.getLocalHost
	val ovxPort = 10000

	val embedderIP = InetAddress.getByName("127.0.1.1")
	val embedderPort = 8000


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
	val testActor_MMA 	= TestActorRef[MatchMakingAgent](mmaProps)

	val nraProps:	Props = Props(classOf[NetworkResourceAgent], cloudSwitches, cloudHosts,
															ovxIP, ovxPort,
															embedderIP, embedderPort,
															testActor_MMA)
	val testActor_NRA 	= TestActorRef[NetworkResourceAgent](nraProps)



	/* Test Specifications: */
	/* ==================== */

	"A NetworkResourceAgent" should {
		"answer with a ResourceReply, if ResourceRequests are locally fulfillable" in {
			//TODO: Write out Shortcut implementation:
			testActor_NRA.receive(ResourceRequest(resAlloc1, ovxIP, ovxPort))
			testActor_NRA.receive(ResourceRequest(resAlloc2, ovxIP, ovxPort))
		}

		"try to allocate Resources via a Federation, if ResourceRequests are NOT locally fulfillable" in{
			//TODO: Write out Shortcut implementation:
			testActor_NRA.receive(ResourceRequest(resAlloc3, ovxIP, ovxPort))
		}
	}



}
