import java.io.File

import agents.PubSubFederator
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
 * @author Constantin Gaul, created on 6/23/14.
 */
object PubSubManagement extends App
{
  val appcfg = loadConfigs(args)

  val config = ConfigFactory.parseFileAnySyntax(appcfg)
  val system = ActorSystem("PubSubSystem", config.getConfig("pubSubSystem").withFallback(config))

  val pubSubActorName = "remoteFederator"
  val pubSubActor = system.actorOf(Props[PubSubFederator], name=pubSubActorName)



/* Private Methods: */
/* ================ */

  private def loadConfigs(args: Array[String]): File = {
    def exitOnParamError() = {
      System.err.println("At least one argument " +
        "(namely --appcfg application.conf) " +
        "has to be passed into this pubsub-system.jar!")
      System.exit(1)
    }
    def exitOnFileError(file: File) = {
      System.err.println(s"The File ``${file.getName}´´ specified as appcfg needs to be existent!")
      System.exit(1)
    }

    var appcfg: 	Option[File] = None

    if(args.length < 2) exitOnParamError()

    for (i <- 0 to (args.size - 1)) args(i) match{
      case "--appconf" 	=> if(i+1 < args.size) {appcfg 		= Option(new File(args(i+1)))}
      case "-a" 				=> if(i+1 < args.size) {appcfg 		= Option(new File(args(i+1)))}
      case _						=>
    }

    // Check whether appcfg and clouddir are existing Files:
    appcfg match {
      case Some(file) => if (!file.exists()) exitOnFileError(file)
      case None => exitOnParamError()
    }
    return appcfg.get
  }
}
