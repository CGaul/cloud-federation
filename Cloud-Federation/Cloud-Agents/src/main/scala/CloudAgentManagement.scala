import java.io.File

import agents.{CCFM, PubSubFederator}
import akka.actor.{ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory


object CloudAgentManagement extends App
{
	val (appcfg, clouddir) = loadConfigs(args)
	val config = ConfigFactory.parseFileAnySyntax(appcfg)

	val system = ActorSystem("cloudAgentSystem", config.getConfig("cloudagentsystem").withFallback(config))

	// Contacting the PubSubFederator via a static ActorSelection:
	val pubSubActorName = "remoteFederator"
	val pubSubActorSel  = system.actorSelection("akka.tcp://pubSubSystem@192.168.1.41:2550/user/remoteFederator")//(Props[PubSubFederator], name=pubSubActorName)
//val pubSubActorSel  = system.actorSelection("/user/"+pubSubActorName)

	// Building up the CCFM via the local System:
	val ccfmProps = Props(classOf[CCFM], pubSubActorSel, clouddir)
	val ccfmActor = system.actorOf(ccfmProps, name="CCFM")

  println("Starting AgentFederation. Initialized CCFM-Agent successful! CCFM Address: "+ ccfmActor)



/* Private Methods: */
/* ================ */

	private def loadConfigs(args: Array[String]): (File, File) = {
		def exitOnParamError() = {
			System.err.println("At least two arguments " +
				"(namely --appcfg application.conf and --clouddir cloudconfdir) " +
				"have to be passed into this cloud-agent.jar!")
			System.err.println(s"Number args: ${args.size} Values of args: ${args.mkString(" ")}")
			System.exit(1)
		}
		def exitOnFileError(file: File) = {
			System.err.println(s"The File ``${file.getName}´´ specified as appcfg or clouddir needs to be existent!")
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
