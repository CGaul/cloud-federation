import java.io.File
import java.net.InetAddress

import agents.PubSubFederator
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes.CloudCurrency._
import datatypes.ImgFormat._
import datatypes._
import messages.{DiscoverySubscription}
import org.scalatest.{FlatSpec, Matchers}

/**
 * @author Constantin Gaul, created on 10/29/14.
 */
class PubSubFederatorSpec extends FlatSpec with Matchers{

	val cloudSLA = new CloudSLA(Vector((SMALL, Price(1, CLOUD_CREDIT), Price(3, CLOUD_CREDIT))),
		(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)),
		(ByteSize(1, GB), Price(0.3f, CLOUD_CREDIT), Price(0.8f, CLOUD_CREDIT)))
	
	val hostSLA = new HostSLA(0.95f, Vector(IMG, COW, CLOOP, BOCHS, QCOW2),
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

	val cloudHosts: Vector[Host] = Vector(host1, host2)

	val ovxIP = InetAddress.getLocalHost


//	val testAgentSystem = AgentTestSystem[NetworkResourceAgent]("testApplication.conf", "CloudAgents")
//	val networkResourceAgentProps:	Props = Props(classOf[NetworkResourceAgent], initialResAlloc, ovxIP)
//	val (nraRef, nraActor) = testAgentSystem.prepareAgentTestSystem(networkResourceAgentProps)

	val config = ConfigFactory.load("testApplication.conf")
	implicit val system = ActorSystem("pubSubSystem", config.getConfig("pubSubSystem").withFallback(config))

	val testActor_PubSub = TestActorRef[PubSubFederator](Props[PubSubFederator], name="remoteFederator")

	testActor_PubSub.receive(DiscoverySubscription(cloudSLA, cloudHosts.map(_.hostSLA).distinct, new File("Certificate")))

}
