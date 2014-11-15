package akkaspecs

import java.net.InetAddress

import agents.NetworkResourceAgent
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import datatypes.CPUUnit._
import datatypes._
import messages.ResourceRequest
import org.scalatest.{FlatSpec, Matchers}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class NetworkResourceAgentSpec extends FlatSpec with Matchers{

	val hostSLA = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW, ImgFormat.CLOOP, ImgFormat.BOCHS, ImgFormat.QCOW2),
									Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 10), (CPUUnit.MEDIUM, 10)))

	val host1 : Host = Host(Resource(NodeID(1), CPUUnit.MEDIUM, ByteSize(16.0, ByteUnit.GiB),
														ByteSize(320.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														10.0f, Vector[NodeID]()), _hostSLA = hostSLA)
	val host2 : Host = Host(Resource(NodeID(2), CPUUnit.LARGE, ByteSize(32.0, ByteUnit.GiB),
														ByteSize(500.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														10.0f, Vector[NodeID]()), _hostSLA = hostSLA)

	val resToAlloc1 : Resource = Resource(	NodeID(1), CPUUnit.SMALL, ByteSize(4.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc2 : Resource = Resource(	NodeID(2), CPUUnit.MEDIUM, ByteSize(8.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc3 : Resource = Resource(	NodeID(2), CPUUnit.MEDIUM, ByteSize(8.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc4 : Resource = Resource(	NodeID(1), CPUUnit.SMALL, ByteSize(8.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc5 : Resource = Resource(	NodeID(1), CPUUnit.SMALL, ByteSize(4.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())

	val reqHostSLA1 = new HostSLA(0.90f, Vector(ImgFormat.IMG, ImgFormat.COW),
											Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 3)))
	val reqHostSLA2 = new HostSLA(0.91f, Vector(ImgFormat.IMG, ImgFormat.CLOOP, ImgFormat.BOCHS),
											Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 4)))
	val reqHostSLA3 = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.CLOOP, ImgFormat.QCOW2),
											Vector[(CPUUnit, Int)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 2)))

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