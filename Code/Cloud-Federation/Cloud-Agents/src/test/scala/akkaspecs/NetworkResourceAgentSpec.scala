package akkaspecs

import java.net.InetAddress

import agents.NetworkResourceAgent
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes._
import messages.ResourceRequest
import org.scalatest.{FlatSpec, Matchers}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class NetworkResourceAgentSpec extends FlatSpec with Matchers{

	val hostSLA = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW, ImgFormat.CLOOP, ImgFormat.BOCHS, ImgFormat.QCOW2),
									Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 10)))

	//General Medium Node
	val res1 = Resource(NodeID(1), MEDIUM,
							ByteSize(16, GiB), ByteSize(320, GiB),
							ByteSize(50, MB), 10, Vector())

	//General Large Node
	val res2= Resource(NodeID(2), LARGE,
							ByteSize(32, GiB), ByteSize(500, GiB),
							ByteSize(50, MB), 10, Vector())
	
	val host1 : Host = Host(res1, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA)
	val host2 : Host = Host(res2, InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA)

	val resToAlloc1 : Resource = Resource(	NodeID(1), SMALL, ByteSize(4.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc2 : Resource = Resource(	NodeID(2), MEDIUM, ByteSize(8.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc3 : Resource = Resource(	NodeID(2), MEDIUM, ByteSize(8.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc4 : Resource = Resource(	NodeID(1), SMALL, ByteSize(8.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc5 : Resource = Resource(	NodeID(1), SMALL, ByteSize(4.0, GiB),
														ByteSize(50.0, GiB), ByteSize(50.0, MiB),
														20.0f, Vector[NodeID]())

	val reqHostSLA1 = new HostSLA(0.90f, Vector(ImgFormat.IMG, ImgFormat.COW),
											Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3)))
	val reqHostSLA2 = new HostSLA(0.91f, Vector(ImgFormat.IMG, ImgFormat.CLOOP, ImgFormat.BOCHS),
											Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 4)))
	val reqHostSLA3 = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.CLOOP, ImgFormat.QCOW2),
											Vector[(CPUUnit, Int)]((SMALL, 1), (MEDIUM, 2)))

	val resAlloc1 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](resToAlloc1, resToAlloc2), reqHostSLA1)
	val resAlloc2 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](resToAlloc3), 							reqHostSLA2)
	val resAlloc3 : ResourceAlloc = ResourceAlloc(2, Vector[Resource](resToAlloc4, resToAlloc5), reqHostSLA3)

	val cloudHosts: Vector[Host] = Vector(host1, host2)

	val ovxIP = InetAddress.getLocalHost


//	val testAgentSystem = AgentTestSystem[NetworkResourceAgent]("testApplication.conf", "CloudAgents")
//	val networkResourceAgentProps:	Props = Props(classOf[NetworkResourceAgent], initialResAlloc, ovxIP)
//	val (nraRef, nraActor) = testAgentSystem.prepareAgentTestSystem(networkResourceAgentProps)

	val config = ConfigFactory.load("testApplication.conf")
	implicit val system = ActorSystem("CloudAgents", config.getConfig("cloudAgents").withFallback(config))
	val networkResourceAgentProps:	Props = Props(classOf[NetworkResourceAgent], cloudHosts, ovxIP)
	val nraRef : TestActorRef[NetworkResourceAgent] = TestActorRef[NetworkResourceAgent](networkResourceAgentProps)
	//val nraActor = nraRef.underlyingActor

	nraRef.receive(ResourceRequest(resAlloc1, ovxIP))
	nraRef.receive(ResourceRequest(resAlloc2, ovxIP))
	nraRef.receive(ResourceRequest(resAlloc3, ovxIP))

}
