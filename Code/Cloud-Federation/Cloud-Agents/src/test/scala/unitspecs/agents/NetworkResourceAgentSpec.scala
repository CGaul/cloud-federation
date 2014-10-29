package unitspecs.agents

import java.net.InetAddress

import agents.NetworkResourceAgent
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import datatypes.CPU_Unit._
import datatypes._
import messages.ResourceRequest
import org.scalatest.{ShouldMatchers, FlatSpec}

/**
 * Created by costa on 10/29/14.
 */
class NetworkResourceAgentSpec extends FlatSpec with ShouldMatchers{
	val hostRes1 : Resource = Resource(	NodeID(1), CPU_Unit.MEDIUM, ByteSize(16.0, Byte_Unit.GiB),
														ByteSize(320.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
														10.0f, Vector[NodeID]())
	val hostRes2 : Resource = Resource(	NodeID(1), CPU_Unit.LARGE, ByteSize(32.0, Byte_Unit.GiB),
														ByteSize(500.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
														10.0f, Vector[NodeID]())

	val hostAlloc1 : Resource = Resource(	NodeID(1), CPU_Unit.SMALL, ByteSize(4.0, Byte_Unit.GiB),
														ByteSize(50.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
														20.0f, Vector[NodeID]())
	val hostAlloc2 : Resource = Resource(	NodeID(2), CPU_Unit.MEDIUM, ByteSize(8.0, Byte_Unit.GiB),
														ByteSize(50.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
														20.0f, Vector[NodeID]())
	val hostAlloc3 : Resource = Resource(	NodeID(2), CPU_Unit.MEDIUM, ByteSize(8.0, Byte_Unit.GiB),
														ByteSize(50.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
														20.0f, Vector[NodeID]())

	val resToAlloc1 : Resource = Resource(	NodeID(1), CPU_Unit.SMALL, ByteSize(8.0, Byte_Unit.GiB),
												ByteSize(50.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
												20.0f, Vector[NodeID]())
	val resToAlloc2 : Resource = Resource(	NodeID(1), CPU_Unit.SMALL, ByteSize(4.0, Byte_Unit.GiB),
												ByteSize(50.0, Byte_Unit.GiB), ByteSize(50.0, Byte_Unit.MiB),
												20.0f, Vector[NodeID]())
	val hardSLA1 = new HardSLA(0.95f, Vector(Img_Format.IMG, Img_Format.COW),
										Vector[(CPU_Unit, Integer)]((CPU_Unit.SMALL, 2), (CPU_Unit.MEDIUM, 3)))
	val hardSLA2 = new HardSLA(0.91f, Vector(Img_Format.IMG, Img_Format.CLOOP, Img_Format.BOCHS),
										Vector[(CPU_Unit, Integer)]((CPU_Unit.SMALL, 1), (CPU_Unit.MEDIUM, 4)))
	val hardSLA3 = new HardSLA(0.99f, Vector(Img_Format.IMG, Img_Format.CLOOP, Img_Format.QCOW2),
										Vector[(CPU_Unit, Integer)]((CPU_Unit.SMALL, 1), (CPU_Unit.MEDIUM, 2)))

	
	val initResAlloc1 : ResourceAlloc = ResourceAlloc(Vector[Resource](hostAlloc1), hardSLA1)
	val initResAlloc2 : ResourceAlloc = ResourceAlloc(Vector[Resource](hostAlloc2), hardSLA1)
	val initResAlloc3 : ResourceAlloc = ResourceAlloc(Vector[Resource](hostAlloc3), hardSLA2)
	val newResAlloc 	: ResourceAlloc = ResourceAlloc(Vector[Resource](resToAlloc1, resToAlloc2), hardSLA3)

	var initialResAlloc : Map[Resource, Vector[ResourceAlloc]] = Map()
	initialResAlloc += (hostRes1 -> Vector(), hostRes2 -> Vector(initResAlloc2, initResAlloc3))

	val ovxIP = InetAddress.getLocalHost


	val config = ConfigFactory.load("testApplication.conf")
	implicit val system = ActorSystem("CloudAgents", config.getConfig("cloudAgents").withFallback(config))
	val networkResourceAgentProps:	Props = Props(classOf[NetworkResourceAgent], initialResAlloc, ovxIP)
	val nraRef : TestActorRef[NetworkResourceAgent] = TestActorRef[NetworkResourceAgent](networkResourceAgentProps)
	//val nraActor = nraRef.underlyingActor

	nraRef.receive(ResourceRequest(newResAlloc, ovxIP))

}
