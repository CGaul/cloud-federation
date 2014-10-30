package unitspecs.agents

import java.net.InetAddress

import agents.NetworkResourceAgent
import akka.actor.Props
import datatypes.CPUUnit._
import datatypes._
import messages.ResourceRequest
import org.scalatest.{ShouldMatchers, FlatSpec}

/**
 * Created by costa on 10/29/14.
 */
class NetworkResourceAgentSpec extends FlatSpec with ShouldMatchers{

	val hostSLA = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW, ImgFormat.CLOOP, ImgFormat.BOCHS, ImgFormat.QCOW2),
									Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 10), (CPUUnit.MEDIUM, 10)))

	val host1 : Host = Host(Resource(NodeID(1), CPUUnit.MEDIUM, ByteSize(16.0, ByteUnit.GiB),
														ByteSize(320.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														10.0f, Vector[NodeID]()), hostSLA)
	val host2 : Host = Host(Resource(NodeID(2), CPUUnit.LARGE, ByteSize(32.0, ByteUnit.GiB),
														ByteSize(500.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														10.0f, Vector[NodeID]()), hostSLA)

	val hostAlloc1 : Resource = Resource(	NodeID(1), CPUUnit.SMALL, ByteSize(4.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val hostAlloc2 : Resource = Resource(	NodeID(2), CPUUnit.MEDIUM, ByteSize(8.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val hostAlloc3 : Resource = Resource(	NodeID(2), CPUUnit.MEDIUM, ByteSize(8.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())

	val resToAlloc1 : Resource = Resource(	NodeID(1), CPUUnit.SMALL, ByteSize(8.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())
	val resToAlloc2 : Resource = Resource(	NodeID(1), CPUUnit.SMALL, ByteSize(4.0, ByteUnit.GiB),
														ByteSize(50.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
														20.0f, Vector[NodeID]())

	val reqHostSLA1 = new HostSLA(0.95f, Vector(ImgFormat.IMG, ImgFormat.COW),
											Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 3)))
	val reqHostSLA2 = new HostSLA(0.91f, Vector(ImgFormat.IMG, ImgFormat.CLOOP, ImgFormat.BOCHS),
											Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 4)))
	val reqHostSLA3 = new HostSLA(0.99f, Vector(ImgFormat.IMG, ImgFormat.CLOOP, ImgFormat.QCOW2),
											Vector[(CPU_Unit, Integer)]((CPUUnit.SMALL, 1), (CPUUnit.MEDIUM, 2)))

	val initResAlloc1 : ResourceAlloc = ResourceAlloc(Vector[Resource](hostAlloc1), reqHostSLA1)
	val initResAlloc2 : ResourceAlloc = ResourceAlloc(Vector[Resource](hostAlloc2), reqHostSLA2)
	val initResAlloc3 : ResourceAlloc = ResourceAlloc(Vector[Resource](hostAlloc3), reqHostSLA3)
	val newResAlloc 	: ResourceAlloc = ResourceAlloc(Vector[Resource](resToAlloc1, resToAlloc2), reqHostSLA3)

	var initialResAlloc : Map[Host, Vector[ResourceAlloc]] = Map()
	initialResAlloc += (host1 -> Vector(), host2 -> Vector(initResAlloc2, initResAlloc3))

	val ovxIP = InetAddress.getLocalHost


	val testAgentSystem = AgentTestSystem[NetworkResourceAgent]("testApplication.conf", "CloudAgents")
	val networkResourceAgentProps:	Props = Props(classOf[NetworkResourceAgent], initialResAlloc, ovxIP)
	val (nraRef, nraActor) = testAgentSystem.prepareAgentTestSystem(networkResourceAgentProps)

	nraRef.receive(ResourceRequest(newResAlloc, ovxIP))

}
