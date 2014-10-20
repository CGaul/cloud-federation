package agents

import java.io.{FileInputStream, File}
import java.security.cert.{CertificateFactory, Certificate}

import akka.actor._
import datatypes.CPU_Unit.CPU_Unit
import datatypes.Img_Format.Img_Format
import datatypes._
import messages._


/**
 * Created by costa on 5/27/14.
 */
class CCFM(pubSubServerAddr: ActorSelection, cloudCert: Certificate) extends Actor with ActorLogging
{
	//TODO: replace by a XML File
	object CCFMConfig
	{
		//TODO: build security interfaces for a Certificate-Store
		val certFile: File			= new File("filename")

		val lowProfileSLA: SLA		= new SLA(relOnlineTime 		= 0.8f,
														 supportedImgFormats = Vector[Img_Format](Img_Format.QCOW2),
														 maxVMsPerCPU 			= Vector[(CPU_Unit, Integer)]
														 											(	(CPU_Unit.SMALL, 2), (CPU_Unit.MEDIUM, 2),
																										(CPU_Unit.LARGE, 3), (CPU_Unit.XLARGE, 4)),
														 priceRangePerCPU 	= Vector[(CPU_Unit, Price, Price)]
														 											((CPU_Unit.SMALL,
														  												Price(0.01f, CloudCurrency.CLOUD_CREDIT),
														  												Price(0.05f, CloudCurrency.CLOUD_CREDIT)),
																									(CPU_Unit.MEDIUM,
																									  	Price(0.05f, CloudCurrency.CLOUD_CREDIT),
																		  								Price(0.10f, CloudCurrency.CLOUD_CREDIT)),
																									(CPU_Unit.LARGE,
																									  	Price(0.10f, CloudCurrency.CLOUD_CREDIT),
																		  								Price(0.15f, CloudCurrency.CLOUD_CREDIT)),
																									(CPU_Unit.XLARGE,
																									  	Price(0.20f, CloudCurrency.CLOUD_CREDIT),
																		  								Price(0.50f, CloudCurrency.CLOUD_CREDIT))),
														 priceRangePerRAM		= (new ByteSize(1, Byte_Unit.GB),
														  												Price(0.02f, CloudCurrency.CLOUD_CREDIT),
														  												Price(0.05f, CloudCurrency.CLOUD_CREDIT)),
														 priceRangePerStorage= (new ByteSize(1, Byte_Unit.GB),
																									  	Price(0.02f, CloudCurrency.CLOUD_CREDIT),
																									  	Price(0.05f, CloudCurrency.CLOUD_CREDIT))
														)

	}


/* Values: */
/* ======= */

  // Akka Child-Actor Instantiation-Preparation:
	val discoveryAgentProps:			Props = Props(classOf[DiscoveryAgent], pubSubServerAddr, cloudCert)
	val matchMakingAgentProps:			Props = Props(classOf[MatchMakingAgent])
	val networkResourceAgentProps:	Props = Props(classOf[NetworkResourceAgent])

	// Akka Child-Actor spawning:
	val discoveryAgent: ActorRef 			= context.actorOf(discoveryAgentProps, 		name="discoveryAgent")
	val networkResourceAgent: ActorRef 	= context.actorOf(networkResourceAgentProps, name="networkResourceAgent")

/* Variables: */
/* ========== */

	var foreignDiscoveryActors: Vector[ActorPath] = Vector()
	var cloudFederationMatches: Vector[(ActorPath, Resources)] = Vector()


/* Execution: */
/* ========== */

  	//Called on CCFM construction:
// val certFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
//	val fis 						= new FileInputStream(CCFMConfig.certFile)
//	val cert: Certificate 	= certFactory.generateCertificate(fis)
//	fis.close()

	discoveryAgent ! DiscoveryInit(Vector[SLA](CCFMConfig.lowProfileSLA))
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

	def recvResourceReply(resources: Resources): Unit = ??? //TODO: Implement in 0.2 Integrated Controllers

	def recvResourceReply(resources: Vector[(ActorRef, Resources)]): Unit = ??? //TODO: Implement in 0.2 Integrated Controllers
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
