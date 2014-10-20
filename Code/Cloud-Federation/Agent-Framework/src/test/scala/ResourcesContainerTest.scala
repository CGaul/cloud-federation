import datatypes.CPU_Unit.CPU_Unit
import datatypes._
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test

/**
 * Created by costa on 10/20/14.
 */
class ResourcesContainerTest extends AssertionsForJUnit
{
	@Test
	def compareResourceContainer(): Unit ={
		val nodeIDs1 			= Vector[NodeID](new NodeID(1),new NodeID(2))
		val res1 : Resources = new Resources(	nodeIDs1,
															Vector[(NodeID, CPU_Unit)]((nodeIDs1(0), CPU_Unit.SMALL), (nodeIDs1(1), CPU_Unit.LARGE)),
															Vector[(NodeID, ByteSize)]((nodeIDs1(0), new ByteSize(4, Byte_Unit.GB)), (nodeIDs1(1), new ByteSize(16, Byte_Unit.GB))),
															Vector[(NodeID, ByteSize)]((nodeIDs1(0), new ByteSize(50, Byte_Unit.GB)), (nodeIDs1(1), new ByteSize(50, Byte_Unit.GB))),
															Vector[(NodeID, ByteSize)]((nodeIDs1(0), new ByteSize(10, Byte_Unit.MB)), (nodeIDs1(1), new ByteSize(50, Byte_Unit.MB))),
															Vector[(NodeID, Float)]((nodeIDs1(0), 200), (nodeIDs1(1), 100)))

		val nodeIDs2			= Vector[NodeID](new NodeID(1),new NodeID(2))
		val res2 : Resources = new Resources(	nodeIDs2,
			Vector[(NodeID, CPU_Unit)]((nodeIDs2(0), CPU_Unit.SMALL), (nodeIDs2(1), CPU_Unit.LARGE)),
			Vector[(NodeID, ByteSize)]((nodeIDs2(0), new ByteSize(4, Byte_Unit.GB)), (nodeIDs2(1), new ByteSize(16, Byte_Unit.GB))),
			Vector[(NodeID, ByteSize)]((nodeIDs2(0), new ByteSize(50, Byte_Unit.GB)), (nodeIDs2(1), new ByteSize(50, Byte_Unit.GB))),
			Vector[(NodeID, ByteSize)]((nodeIDs2(0), new ByteSize(10, Byte_Unit.MB)), (nodeIDs2(1), new ByteSize(50, Byte_Unit.MB))),
			Vector[(NodeID, Float)]((nodeIDs2(0), 200), (nodeIDs2(1), 100)))

		res1.compareTo(res2)
	}
}
