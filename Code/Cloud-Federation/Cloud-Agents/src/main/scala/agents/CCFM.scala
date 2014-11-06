package agents

import java.io.{FileInputStream, File}
import java.net.InetAddress
import java.security.cert.{CertificateFactory, Certificate}

import akka.actor._
import datatypes.CPUUnit.CPUUnit
import datatypes.ImgFormat.ImgFormat
import datatypes._
import messages._


/**
 * @author Constantin Gaul, created on 5/27/14.
 */
class CCFM(pubSubServerAddr: ActorSelection, cloudCert: Certificate) extends Actor with ActorLogging
{
	//TODO: replace by a XML File
	object CCFMConfig
	{
		//TODO: build security interfaces for a Certificate-Store
		val certFile: File			= new File("filename")

		val ovxIP: InetAddress 	= InetAddress.getLocalHost

		val hostSLA: HostSLA		= new HostSLA(
														relOnlineTime 		= 0.8f,
													 	_supportedImgFormats = Vector[ImgFormat](ImgFormat.QCOW2),
													 	_maxResPerCPU 			= Vector[(CPUUnit, Int)](
																						(CPUUnit.SMALL, 2), (CPUUnit.MEDIUM, 2),
																						(CPUUnit.LARGE, 3), (CPUUnit.XLARGE, 4)))

		val host1 : Host = Host(Resource(NodeID(1), CPUUnit.MEDIUM, ByteSize(16.0, ByteUnit.GiB),
			ByteSize(320.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
			10.0f, Vector[NodeID]()), _hostSLA = hostSLA)
		val host2 : Host = Host(Resource(NodeID(2), CPUUnit.LARGE, ByteSize(32.0, ByteUnit.GiB),
			ByteSize(500.0, ByteUnit.GiB), ByteSize(50.0, ByteUnit.MiB),
			10.0f, Vector[NodeID]()), _hostSLA = hostSLA)

		val cloudHosts = Vector(host1, host2)

		val softSLAs: CloudSLA	= new CloudSLA(
														priceRangePerCPU 	= Vector[(CPUUnit, Price, Price)](
															 										(CPUUnit.SMALL,
														  												Price(0.01f, CloudCurrency.CLOUD_CREDIT),
														  												Price(0.05f, CloudCurrency.CLOUD_CREDIT)),
																									(CPUUnit.MEDIUM,
																									  	Price(0.05f, CloudCurrency.CLOUD_CREDIT),
																		  								Price(0.10f, CloudCurrency.CLOUD_CREDIT)),
																									(CPUUnit.LARGE,
																									  	Price(0.10f, CloudCurrency.CLOUD_CREDIT),
																		  								Price(0.15f, CloudCurrency.CLOUD_CREDIT)),
																									(CPUUnit.XLARGE,
																									  	Price(0.20f, CloudCurrency.CLOUD_CREDIT),
																		  								Price(0.50f, CloudCurrency.CLOUD_CREDIT))),
														priceRangePerRAM		= (new ByteSize(1, ByteUnit.GB),
														  												Price(0.02f, CloudCurrency.CLOUD_CREDIT),
														  												Price(0.05f, CloudCurrency.CLOUD_CREDIT)),
														priceRangePerStorage= (new ByteSize(1, ByteUnit.GB),
																									  	Price(0.02f, CloudCurrency.CLOUD_CREDIT),
																									  	Price(0.05f, CloudCurrency.CLOUD_CREDIT))
														)

	}


/* Values: */
/* ======= */

	// Akka Child-Actor spawning:
	val discoveryAgentProps: Props 				= Props(classOf[DiscoveryAgent],
																								pubSubServerAddr, cloudCert)
	val discoveryAgent: ActorRef 					= context.actorOf(discoveryAgentProps, 				name="discoveryAgent")

	val matchMakingAgentProps: Props 			= Props(classOf[MatchMakingAgent],
																								CCFMConfig.softSLAs)
	val matchMakingAgent: ActorRef				= context.actorOf(matchMakingAgentProps, 			name="matchMakinAgent")

	val networkResourceAgentProps: Props 	= Props(classOf[NetworkResourceAgent],
																								CCFMConfig.cloudHosts, CCFMConfig.ovxIP, matchMakingAgent)
	val networkResourceAgent: ActorRef 		= context.actorOf(networkResourceAgentProps, 	name="networkResourceAgent")



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

	//TODO: DiscoveryInit really needed? Yes -> for every update of SLA, send a new one and forward to PubSubFederator.
	discoveryAgent ! FederationSLAs(Vector[HostSLA](CCFMConfig.hostSLA))
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
	def props(pubSubServerAddr: ActorSelection, cloudCert: Certificate):
		Props = Props(new CCFM(pubSubServerAddr, cloudCert))
}
