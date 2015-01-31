import java.io.File

import agents.CCFM
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import datatypes.ByteUnit._
import datatypes.CPUUnit.{CPUUnit, _}
import datatypes.ImgFormat._
import datatypes._
import messages.TenantRequest


object CloudAgentManagement extends App
{
	val (appcfg, clouddir) = loadConfigs(args)
	val config = ConfigFactory.parseFileAnySyntax(appcfg)

	val system = ActorSystem("cloudAgentSystem", config.getConfig("cloudagentsystem").withFallback(config))

	// Contacting the PubSubFederator via a static ActorSelection:
	val pubSubActorName = "remoteFederator"
  val federatorIP = "192.168.1.41"
  val federatorPort = 2550
	val pubSubActorSel  = system.actorSelection(s"akka.tcp://pubSubSystem@$federatorIP:$federatorPort/user/remoteFederator")// TODO: rewrite dynamically
//val pubSubActorSel  = system.actorSelection("/user/"+pubSubActorName)

	// Building up the CCFM via the local System:
	val ccfmProps = Props(classOf[CCFM], pubSubActorSel, clouddir)
	val ccfmActor = system.actorOf(ccfmProps, name="CCFM")

  println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmActor)


	// Lightweight Tenant Allocation Testing for 0.2 Integrated Controllers Milestone:
	// Copied from NetworkResourceAgentTest:

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
	val resAlloc2 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](res3), 				reqHostSLA2)
	val resAlloc3 : ResourceAlloc = ResourceAlloc(1, Vector[Resource](res4, res5), 	reqHostSLA3)

	Thread.sleep(10000)
	ccfmActor ! TenantRequest(resAlloc1)
	Thread.sleep(5000)
	ccfmActor ! TenantRequest(resAlloc2)
	Thread.sleep(5000)
	ccfmActor ! TenantRequest(resAlloc3)


/* Private Methods: */
/* ================ */

	private def loadConfigs(args: Array[String]): (File, File) = {
		def exitOnParamError() = {
			System.err.println("At least two arguments " +
				"(namely --appconf application.conf and --cloudconf cloudconfdir) " +
				"have to be passed into this cloud-agent.jar!")
			System.err.println(s"Number args: ${args.size} Values of args: ${args.mkString(" ")}")
			System.exit(1)
		}
		def exitOnFileError(file: File) = {
			System.err.println(s"The File ``${file.getName}´´ specified as appconf or cloudconf needs to be existent!")
			System.exit(1)
		}

		var appcfg: 	Option[File] = None
		var clouddir: Option[File] = None

		if(args.length < 4) exitOnParamError()

		for (i <- 0 to (args.size - 1)) args(i) match{
			case "--appconf" 		=> if(i+1 < args.size) {appcfg 		= Option(new File(args(i+1)))}
			case "-a" 					=> if(i+1 < args.size) {appcfg 		= Option(new File(args(i+1)))}
			case "--cloudconf" 	=> if(i+1 < args.size) {clouddir 	= Option(new File(args(i+1)))}
			case "-c" 					=> if(i+1 < args.size) {clouddir 	= Option(new File(args(i+1)))}
			case _							=>
		}

		// Check whether appcfg and clouddir are existing Files:
		for (actCfg <- Array(appcfg, clouddir)) {
			actCfg match{
				case Some(file)		=> if(!file.exists()) exitOnFileError(file)
				case None					=> exitOnParamError()
			}
		}
		return (appcfg.get, clouddir.get)
	}
}
