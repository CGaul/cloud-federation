import java.io.File

import agents.FederationBroker
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import connectors.FederationConfigurator

/**
 * @author Constantin Gaul, created on 6/23/14.
 */
object FedBrokerManagement extends App
{
  val appCfg = loadAkkaConfig(args)
  val fedconfDir = loadFederatorConfDir(args)

  val config = ConfigFactory.parseFileAnySyntax(appCfg)
  val system = ActorSystem("fedBroker", config.getConfig("fedbroker").withFallback(config))

  val fedBrokerProps = Props(classOf[FederationBroker], FederationConfigurator(fedconfDir))
  val fedBroker = system.actorOf(fedBrokerProps, name="federationBroker")

  println(s"Starting Federation-Broker successfully at $fedBroker!")



/* Private Methods: */
/* ================ */

  private def loadFederatorConfDir(args: Array[String]): File = {
    def exitOnParamError() = {
      System.err.println("No --fedconf federatorconf-dir could have been found as commandline arg, " +
        "passed into this fed-broker.jar!")
      System.err.println(s"Number args: ${args.size} Values of args: ${args.mkString(" ")}")
      System.exit(1)
    }

    def exitOnDirError(dir: File) = {
      System.err.println(s"The Dir ``${dir.getName}´´ specified as cloudconfdir needs to be existent and a directory!")
      System.exit(1)
    }

    var clouddir: Option[File] = None

    for (i <- 0 to (args.size - 1)) args(i) match{
      case "--fedconf" 	  => if(i+1 < args.size) {clouddir 	= Option(new File(args(i+1)))}
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
        "passed into this fed-broker.jar!")
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
