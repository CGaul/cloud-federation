package agents

import java.io.{File}
import java.net.InetAddress
import java.security.cert.{CertificateFactory, Certificate}

import akka.actor._
import datatypes._
import messages._


/**
 * @author Constantin Gaul, created on 5/27/14.
 */
class CCFM(pubSubActorSelection: ActorSelection) extends Actor with ActorLogging
{
	//TODO: replace by a XML File
	object CCFMConfig
	{
		def certFile 		= _certFile
		def ovxIP 			= _ovxIP
		def cloudHosts 	= _cloudHosts
		def cloudSLA 		= _cloudSLA


		//TODO: build security interfaces for a Certificate-Store
		val _certFile: File			= new File("cloudconf/cloud1.key")

		val _ovxIP: InetAddress 	= InetAddress.getLocalHost


		// Define the Cloud-Hosts from all files in the resources/cloudconf/hosts/ directory
		var _cloudHosts: Vector[Host] = Vector()
		val _cloudHostDir: File = new File("Cloud-Agents/src/main/resources/cloudconf/hosts")
		if(_cloudHostDir.listFiles() == null)
			log.error("Hosts need a defined .xml file in $PROJECT$/resources/cloudconf/hosts/ !")
		for (actHostFile <- _cloudHostDir.listFiles) {
			_cloudHosts = _cloudHosts :+ Host.loadFromXML(actHostFile)
		}

		// Define the Cloud-SLA from the CloudSLA.xml file in the resources/cloudconf/ directory
		val _cloudSLA  = CloudSLA.loadFromXML(new File ("Cloud-Agents/src/main/resources/cloudconf/CloudSLA.xml"))
	}


/* Values: */
/* ======= */

	// Akka Child-Actor spawning:
	val mMASelection: ActorSelection			= context.actorSelection("/user/"+"matchMakingAgent")
	val discoveryAgentProps: Props 				= Props(classOf[DiscoveryAgent],
																								pubSubActorSelection, mMASelection, CCFMConfig.certFile)
	val discoveryAgent: ActorRef 					= context.actorOf(discoveryAgentProps, name="discoveryAgent")

	
	val matchMakingAgentProps: Props 			= Props(classOf[MatchMakingAgent],
																								CCFMConfig.cloudSLA)
	val matchMakingAgent: ActorRef				= context.actorOf(matchMakingAgentProps, name="matchMakingAgent")

	
	val networkResourceAgentProps: Props 	= Props(classOf[NetworkResourceAgent],
																								CCFMConfig.cloudHosts, CCFMConfig.ovxIP, matchMakingAgent)
	val networkResourceAgent: ActorRef 		= context.actorOf(networkResourceAgentProps, name="networkResourceAgent")



/* Variables: */
/* ========== */

	var foreignDiscoveryActors: Vector[ActorPath] = Vector()
	var cloudFederationMatches: Vector[(ActorPath, ResourceAlloc)] = Vector()


/* Execution: */
/* ========== */

  	//Called on CCFM construction:
// val certFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
//	val fis 						= new FileInputStream(CCFMConfig.certFile)
//	val cert: Certificate 	= certFactory.generateCertificate(fis)
//	fis.close()

	//TODO: Maybe possible HostSLAs should differ from initial CloudHostSLAs:
	discoveryAgent ! FederationSLAs(CCFMConfig.cloudSLA, CCFMConfig.cloudHosts.map(_.hostSLA))
  	log.debug("Discovery-Init send to Discovery-Agent!")


/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------


	override def receive(): Receive = {
		case DiscoveryAck(status)		=> recvDiscoveryStatus(status)
		case DiscoveryError(status)	=> recvDiscoveryError(status)
		case "matchmakingMsg" 			=> recvMatchMakingMsg() //TODO: define MessageContainer in 0.3 - Federation-Agents
		case "authenticationMsg"		=> recvAuthenticationMsg() //TODO: define MessageContainer in 0.3 - Federation-Agents
		case message: NetworkResourceMessage	=> message match {
			case ResourceReply(allocResources)				=> recvResourceReply(allocResources)
			case ResourceFederationReply(allocResources) => recvResourceReply(allocResources)
		}
		case _								=> log.error("Unknown message received!")
	}

	def recvDiscoveryStatus(status: String) = {
		log.info("Discovery Status \""+ status + "\" received.")
	}

	def recvDiscoveryError(error: String) = {
		log.error("Discovery Error \""+ error + "\" received.")
	}


	def recvMatchMakingMsg() = ??? //TODO: Implement in 0.3 - Federation-Agents

	def recvAuthenticationMsg() = ??? //TODO: Implement in 0.3 - Federation-Agents

	def recvResourceReply(resources: ResourceAlloc): Unit = ??? //TODO: Implement in 0.2 Integrated Controllers

	def recvResourceReply(resources: Vector[(ActorRef, ResourceAlloc)]): Unit = ??? //TODO: Implement in 0.2 Integrated Controllers
}


/**
 * Companion Object of the CCFM-Agent,
 * in order to implement some default behaviours
 */
object CCFM
{
	/**
	 * props-method is used in the AKKA-Context, spawning a new Agent.
	 * In this case, to generate a new CCFM Agent, call
	 * 	val ccfmProps = Props(classOf[CCFM], args = pubSubServerAddr)
	 * 	val ccfmAgent = system.actorOf(ccfmProps, name="CCFM-x")
	 * @param pubSubServerAddr The akka.tcp connection, where the PubSub-Federator-Agents is listening.
	 * @return An Akka Properties-Object
	 */
	def props(pubSubServerAddr: ActorSelection):
		Props = Props(new CCFM(pubSubServerAddr))
}
