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
	val appCfg = loadAkkaConfig(args)
  val cloudConfDir = loadCloudConfDir(args)
	val config = ConfigFactory.parseFileAnySyntax(appCfg)

	val system = ActorSystem("cloudAgentSystem", config.getConfig("cloudagentsystem").withFallback(config))

	// Contacting the PubSubFederator via a static ActorSelection:
  // TODO: get from config:
	val fedBrokerActorName = "federationBroker"
  val federatorIP = "192.168.1.41"
  val federatorPort = 2550
	val fedBrokerActorSel  = system.actorSelection(s"akka.tcp://fedBroker@$federatorIP:$federatorPort/user/$fedBrokerActorName")
//val pubSubActorSel  = system.actorSelection("/user/"+pubSubActorName)

	// Building up the CCFM via the local System:
	val ccfmProps = Props(classOf[CCFM], fedBrokerActorSel, cloudConfDir)
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

  private def loadCloudConfDir(args: Array[String]): File = {
    def exitOnParamError() = {
      System.err.println("No --cloudconf cloudconf-dir could have been found as commandline arg, " +
        "passed into this cloud-agent.jar!")
      System.err.println(s"Number args: ${args.size} Values of args: ${args.mkString(" ")}")
      System.exit(1)
    }

    def exitOnDirError(dir: File) = {
      System.err.println(s"The Dir ``${dir.getName}´´ specified as cloudconfdir needs to be existent and a directory!")
      System.exit(1)
    }

    var clouddir: Option[File] = None

    for (i <- 0 to (args.size - 1)) args(i) match{
      case "--cloudconf" 	=> if(i+1 < args.size) {clouddir 	= Option(new File(args(i+1)))}
      case "-c" 					=> if(i+1 < args.size) {clouddir 	= Option(new File(args(i+1)))}
      case _              =>
    }

    // Check cloudconfdir is an existing dir:
    clouddir match{
        case Some(file) =>
          if(!file.isDirectory) {
            exitOnDirError(file)
          }
          
        case None	=> 
          exitOnParamError()
      }
    
    return clouddir.get
  }
  
  
  private def loadAkkaConfig(args: Array[String]): File = {
    def exitOnParamError() = {
      System.err.println("No --appconf application.conf could have been found as commandline arg, " +
        "passed into this cloud-agent.jar!")
      System.err.println(s"Number args: ${args.size} Values of args: ${args.mkString(" ")}")
      System.exit(1)
    }

    def exitOnFileError(file: File) = {
      System.err.println(s"The File ``${file.getName}´´ specified as application.conf needs to be existent!")
      System.exit(1)
    }

    var appcfg: 	Option[File] = None

    for (i <- 0 to (args.size - 1)) args(i) match{
      case "--appconf" 		=> if(i+1 < args.size) {appcfg 		= Option(new File(args(i+1)))}
      case "-a" 					=> if(i+1 < args.size) {appcfg 		= Option(new File(args(i+1)))}
      case _              =>
    }

    // Check application.conf is an existing file:
    appcfg match{
      case Some(file) =>
        if(!file.isFile) {
          exitOnFileError(file)
        }

      case None	=>
        exitOnParamError()
    }
    
    return appcfg.get
  }
}
