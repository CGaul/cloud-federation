package agents

import java.io.File
import java.net.InetAddress

import akka.actor._
import akka.io.IO
import datatypes._
import messages._
import spray.can.Http


/**
 * @author Constantin Gaul, created on 5/27/14.
 */
class CCFM(pubSubActorSelection: ActorSelection, cloudConfDir: File,
					 httpServiceAddr: String, httpServicePort: Int) extends Actor with ActorLogging
{
	//TODO: replace by a XML File
	object CCFMConfig
	{
		def certFile 			= _certFile
		def ovxIp 				= _ovxIP
		def ovxApiPort		= _ovxApiPort
		def cloudHosts 		= _cloudHosts
		def cloudTenants  = _cloudTenants
		def cloudSLA 			= _cloudSLA


		//TODO: build security interfaces for a Certificate-Store
		val _certFile: File			= new File(cloudConfDir.getAbsolutePath +"/cloud1.key")

		val _ovxIP: InetAddress 	= InetAddress.getLocalHost
		val _ovxApiPort: Int			= 8080


		// Define the Cloud-Hosts from all files in the cloudConfDir/hosts/ directory
		var _cloudHosts: Vector[Host] = Vector()
		val _hostFiles: File = new File(cloudConfDir.getAbsolutePath +"/hosts")
		if(_hostFiles.listFiles() == null)
			log.error("Hosts need at least one defined .xml file in {}/hosts/ !", cloudConfDir.getAbsolutePath)
		for (actHostFile <- _hostFiles.listFiles) {
			_cloudHosts = _cloudHosts :+ Host.loadFromXML(actHostFile)
		}

		// Define the Cloud-Tenants from all files in the cloudConfDir/hosts/ directory
		var _cloudTenants: Vector[Tenant] = Vector()
		val _tenantFiles: File = new File(cloudConfDir.getAbsolutePath +"/tenants")
		if(_tenantFiles.listFiles() == null)
			log.error("Tenants need at least one defined .xml file in {}/tenants/ !", cloudConfDir.getAbsolutePath)
		for (actTenantFile <- _tenantFiles.listFiles) {
			_cloudTenants = _cloudTenants :+ Tenant.loadFromXML(actTenantFile)
		}
		// Define the Cloud-SLA from the CloudSLA.xml file in the cloudConfDir/ directory
		val _cloudSLA  = CloudSLA.loadFromXML(new File(cloudConfDir.getAbsolutePath +"/CloudSLA.xml"))
	}


/* Values: */
/* ======= */

	// Akka Child-Actor spawning:

	// The HTTP-Service Actor:
	implicit val system = context.system
	val httpHandlerProps = Props(classOf[ResAllocHttpService], this.self, httpServiceAddr, httpServicePort)
	val resAllocHttpHandler = context.actorOf(httpHandlerProps, name="resAllocHTTPService")
	IO(Http) ! Http.Bind(resAllocHttpHandler, interface = httpServiceAddr, port = httpServicePort)

	// The Federation Actors:
	val mmaSelection: ActorSelection			= context.actorSelection("akka://cloudAgentSystem/user/CCFM/matchMakingAgent")
	val nraSelection: ActorSelection			= context.actorSelection("akka://cloudAgentSystem/user/CCFM/networkResourceAgent")
	val discoveryAgentProps: Props 				= Props(classOf[DiscoveryAgent],
																								pubSubActorSelection, mmaSelection, CCFMConfig.certFile)
	val discoveryAgent: ActorRef 					= context.actorOf(discoveryAgentProps, name="discoveryAgent")
	log.info("DiscoveryAgent started at path: {}.", discoveryAgent.path)

	
	val matchMakingAgentProps: Props 			= Props(classOf[MatchMakingAgent],
																								CCFMConfig.cloudSLA, nraSelection)
	val matchMakingAgent: ActorRef				= context.actorOf(matchMakingAgentProps, name="matchMakingAgent")
	log.info("MatchMakingAgent started at path: {}.", matchMakingAgent.path)


	val networkResourceAgentProps: Props 	= Props(classOf[NetworkResourceAgent],
																								CCFMConfig.ovxIp, CCFMConfig.ovxApiPort, 
																								CCFMConfig.cloudHosts.toList,
																								matchMakingAgent)
	val networkResourceAgent: ActorRef 		= context.actorOf(networkResourceAgentProps, name="networkResourceAgent")
	log.info("NetworkResourceAgent started at path: {}.", networkResourceAgent.path)



/* Variables: */
/* ========== */

	var foreignDiscoveryActors: Vector[ActorPath] = Vector()
	var cloudFederationMatches: Vector[(ActorPath, ResourceAlloc)] = Vector()


/* Execution: */
/* ========== */

  	//Called on CCFM construction:
// val certFactory: CertificateFactory =  CertificateFactory.getInstance("X.509")
//	val fis 						= new FileInputStream(CCFMConfig.certFile)
//	val cert: Certificate 	= certFactory.generateCertificate(fis)
//	fis.close()

	//TODO: Maybe possible HostSLAs should differ from initial CloudHostSLAs:
	discoveryAgent ! FederationSLAs(CCFMConfig.cloudSLA, CCFMConfig.cloudHosts.map(_.sla))
  	log.debug("Discovery-Init send to Discovery-Agent!")


/* Methods: */
/* ======== */

	// Akka Actor Receive method-handling:
	// -----------------------------------

	def recvTenantRequest(resToAlloc: ResourceAlloc): Unit = {
		log.info("CCFM Received TenantRequest from Tenant-ID {}. Forwarding to NRA, after Tenant is resolved...",
			resToAlloc.tenantID)

		log.info("Fetching Tenant from internal Config file...")
		val tenantOpt = CCFMConfig.cloudTenants.find(_.id == resToAlloc.tenantID)
		tenantOpt match{
			case Some(tenant) =>
					log.info("Successfully resolved tenant {}. Forwarding TenantRequest to NRA as ResourceRequest...", tenant)
					networkResourceAgent ! ResourceRequest(tenant, resToAlloc)
			case None => log.error("Tenant was not resolved for Tenant-ID {} in TenantRequest!", resToAlloc.tenantID)
		}
	}

	def recvResourceRequest(tenant: Tenant, resToAlloc: ResourceAlloc): Unit = {
		log.info("CCFM Received ResourceRequest from Tenant {}. Forwarding to NRA...",
		resToAlloc.tenantID)

		networkResourceAgent ! ResourceRequest(tenant, resToAlloc)
	}


	override def receive(): Receive = {
		case DiscoveryAck(status)		=> recvDiscoveryStatus(status)
		case DiscoveryError(status)	=> recvDiscoveryError(status)
		case "matchmakingMsg" 			=> recvMatchMakingMsg() //TODO: define MessageContainer in 0.3 - Federation-Agents
		case "authenticationMsg"		=> recvAuthenticationMsg() //TODO: define MessageContainer in 0.3 - Federation-Agents
		case message: CCFMResourceDest	=> message match {
			case TenantRequest(resToAlloc)						=> recvTenantRequest(resToAlloc)
			case ResourceRequest(tenant, resToAlloc) 	=> recvResourceRequest(tenant, resToAlloc)
			case ResourceReply(allocResources)				=> recvResourceReply(allocResources)
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
	def props(pubSubServerAddr: ActorSelection, cloudConfDir: File, httpServiceAddr: String, httpServicePort: Int):
		Props = Props(new CCFM(pubSubServerAddr, cloudConfDir, httpServiceAddr, httpServicePort))
}
