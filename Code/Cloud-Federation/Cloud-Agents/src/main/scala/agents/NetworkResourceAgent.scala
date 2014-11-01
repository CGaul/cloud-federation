package agents

import java.net.InetAddress

import akka.actor._
import datatypes._
import messages.{ResourceRequest, ResourceFederationReply, NetworkResourceMessage}

import scala.util.Sorting

/**
 * Created by costa on 10/15/14.
 */
class NetworkResourceAgent(_initialResAlloc: Map[Host, Vector[ResourceAlloc]],
									_ovxIP: InetAddress) extends Actor with ActorLogging with Stash
{
/* Values: */
/* ======= */


/* Variables: */
/* ========== */

	var _totalResources: Map[Host, Vector[ResourceAlloc]] = _initialResAlloc


/* Public Methods: */
/* =============== */

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Receives ResourceInfos from the CCFM.
	 * <p>
	 *    Directly after the NetworkResourceAgent was started and everytime the topology changes from
	 *    outside circumstances, the CCFM sends a Topology update in the form of an abstracted combination
	 *    of total Resources of the whole Cloud (including the Host's, initial Power and their connections without load)
	 *    and the available Resources, which are excluding Resources that are currently completely assigned and/or
	 *    under load.
	 * </p>
	 * <p>
	 * 	When a ResourceInfo message is queued at the NetworkResourceAgent the first time, the internal _initialized value
	 * 	will be set to true, as the Agent is not able to function without these information.
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 *
	 * @param totalResources
	 * @param availResources
	 */
//	def recvResourceInfo(totalResources: ResourceAlloc): Unit = {
//		_totalResources	= _to
//		_availResources	= availResources
//
//		unstashAll()
//		context.become(receivedOnline())
//	}

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Receives ResourceRequests from the CCFM.
	 * <p>
	 * 	Either local Cloud's Resources are sufficient, then the Request could be
	 * 	assigned to local resources only. If the local Resources are insufficient,
	 * 	split the Request, using all locally available resources and forward the
	 * 	federation part of the splitted Request to the MatchMakingAgent,
	 * 	who should then send a ResourceReply back to this NetworkResourceAgent,
	 * 	if all Federation attempts are finished,
	 * 	or all Resources could have been allocated.
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * @param resourceAlloc
	 * @param address
	 */
	def recvResourceRequest(resourceAlloc: ResourceAlloc, address: InetAddress): Unit = {
		// Filter all Cloud-Hosts by their SLAs. Each host's SLA has to fulfill the QoS of the requestedSLA:
		val hostFilteredQoSMap : Map[Host, Vector[ResourceAlloc]] = _totalResources.filter(t => t._1.hostSLA.fulfillsQoS(resourceAlloc.requestedHostSLA))
		
		// Afterwards, filter all remaining, QoS matching Cloud-Hosts by their free resources:
		var potentialHosts: Vector[Host] 		= Vector()
		var resourcesToAlloc: Vector[Resource] = Vector()
		for (actResource <- resourceAlloc.resources) {
			val hostFilteredResMap: Map[Host, Vector[ResourceAlloc]] = hostFilteredQoSMap.filter(t => RelativeResOrdering.compare(t._1.hardwareSpec, actResource) >= 0)
			potentialHosts 	= potentialHosts ++ hostFilteredResMap.map(t => t._1).toVector
			resourcesToAlloc 	= resourcesToAlloc :+ actResource
		}
		// Filter all double elements from the potentialHosts-Vector:
		potentialHosts		= potentialHosts.distinct

		// Sort the potentialHosts as well as the resourceAlloc by their resources:
		potentialHosts		= potentialHosts.sorted(RelativeHostByResOrdering)
		resourcesToAlloc	= resourcesToAlloc.sorted(RelativeResOrdering)

		// At last: Binpacking - First Fit Descending:
		// Fit each resourceToAlloc in the first potentialHost (bin) that is fulfilling the resource & combined SLA requirements:
		for (actResToAlloc <- resourcesToAlloc) {
			val potentialHostIndex = potentialHosts.indexWhere(h => RelativeResOrdering.compare(h.hardwareSpec, actResToAlloc) > 0)
			if(potentialHostIndex != -1){
				System.out.println("Pre-Allocation done.")
				//TODO: Check if the potentialHost is still fulfilling SLA with attached resToAlloc

				//TODO: finally attach resToAlloc to potentialHost and remove resToAlloc & potentialHost from lists

				//TODO: update potentialHost's SLA
			}
		}

		//TODO: For any left resToAlloc in list, build ResourceAlloc split and inform matchMakingAgent
	}

	//TODO: Implement in 0.2 Integrated Controllers
	/**
	 * Receives a ResourceFederationReply from the MatchMakingAgent.
	 * <p>
	 * 	If a ResourceRequest could not have been processed locally,
	 * 	the NetworkFederationAgent has asked the MatchMakingAgent
	 * 	for Federation-Resources.
	 * 	All results are included in such ResourceFederationReply,
	 * 	stating the allocated Resources per foreign Cloud
	 * 	(the ActorRef is the foreign MatchMakingAgent)
	 * </p>
	 * Jira: CITMASTER-28 - Develop NetworkResourceAgent
	 * @param tuples
	 */
	def recvResourceReply(tuples: Vector[(ActorRef, ResourceAlloc)]): Unit = {
	}


//	override def receive(): Receive = {
//		//case message: NetworkResourceMessage	=> message match {
//			//case ResourceInfo(totalRes, availRes)			=> recvResourceInfo(totalRes, availRes)
//
//		//}
//		case _														=> log.error("Unknown message received!")
//	}

	def receive(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceRequest(resourcesToAlloc, ofcIP)		=> recvResourceRequest(resourcesToAlloc, ofcIP)
			case ResourceFederationReply(resourcesAllocated) 	=> recvResourceReply(resourcesAllocated)
		}
		case _														=> log.error("Unknown message received!")
	}


/* Private Methods: */
/* ================ */

	//TODO: if not needed anymore, delete.
	private def stashMessage(): Unit = {
		log.debug("Received Message, before NetworkResourceAgent went online. Stashed message until being online.")
		try {
			stash()
		}
		catch {
			case e: StashOverflowException => log.error("Reached Stash buffer. Received message will be ignored."+
			  e.printStackTrace())
		}
	}
	
	private def retrieveHostSLAs(): Map[Host, Option[HostSLA]] ={
		//Init hostSLAMap with all Nodes (Resources) from _totalResources and map None to it:
		var hostSLAMap : Map[Host, Option[HostSLA]] = Map()
		_totalResources.foreach(t => hostSLAMap += (t._1 -> None))


		for (actNode: Host <- _totalResources.keys ) {
			val actResAlloc : Vector[ResourceAlloc]	= _totalResources.get(actNode).get

			if(actResAlloc.size > 0){
				//List all hostSLAs for the actual ResourceAlloc:
				var hostSLAs : Vector[HostSLA]	= Vector()
				actResAlloc.foreach(t => hostSLAs = hostSLAs :+ t.requestedHostSLA)

				//Reduce the Vector of hostSLAs to a single hardestSLA:
				val hardestSLA : HostSLA = hostSLAs.reduce(_ combineToAmplifiedSLA _)
				hostSLAMap += (actNode -> Option(hardestSLA))
			}
		}
		return hostSLAMap
	}

	private def countTotalResAlloc(): Int ={
		var resAllocCount = 0
		_totalResources.foreach(resAllocCount += _._2.size)
		return resAllocCount
	}

}

/**
 * Companion Object of the NetworkResource-Agent,
 * in order to implement some default behaviours
 */
object NetworkResourceAgent
{
	/**
	 * props-method is used in the AKKA-Context, spawning a new Agent.
	 * In this case, to generate a new NetworkResource Agent, call
	 * 	val ccfmProps = Props(classOf[NetworkResourceAgent], args = ovxIP)
	 * 	val ccfmAgent = system.actorOf(ccfmProps, name="NetworkResourceAgent-x")
	 * @param ovxIP The InetAddress, where the OpenVirteX OpenFlow hypervisor is listening.
	 * @return An Akka Properties-Object
	 */
	def props(initialResAlloc: Map[Host, Vector[ResourceAlloc]], ovxIP: InetAddress):
	Props = Props(new NetworkResourceAgent(initialResAlloc, ovxIP))
}
