package agents

import java.net.InetAddress

import akka.actor._
import datatypes.Resources
import messages.{ResourceInfo, ResourceRequest, ResourceFederationReply, NetworkResourceMessage}

/**
 * Created by costa on 10/15/14.
 */
class NetworkResourceAgent(_ovxIP: InetAddress) extends Actor with ActorLogging with Stash
{
/* Values: */
/* ======= */


/* Variables: */
/* ========== */

	var _totalResources : Resources = new Resources()
	var _availResources : Resources = new Resources()


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
	def recvResourceInfo(totalResources: Resources, availResources: Resources): Unit = {
		_totalResources	= totalResources
		_availResources	= availResources

		unstashAll()
		context.become(receivedOnline())
	}

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
	 * @param resources
	 * @param address
	 */
	def recvResourceRequest(resources: Resources, address: InetAddress): Unit = {
		/* Check if local Cloud's Resources could be sufficient:
		 * As the compareTo(..) method is only a lightweight sufficiency check
		 * Try to solve the binpacking problem here via an _availresource.allocate(resources).
		 * This allocation will not be adopted to the available resources, as long as it is not clear if the
		 * allocation will succeed. */
		if(_availResources.compareTo(resources) > 0){
			_availResources.allocate(resources)
		}
		/* Inform the MatchMakingAgent about necessary federated Resources.
		 * If compareTo fails, it is guaranteed, that the local available resources are not sufficient. */
		else{
			//TODO: split resources and fulfill as much as possible locally.
			//TODO: inform matchMakingAgent.
		}
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
	def recvResourceReply(tuples: Vector[(ActorRef, Resources)]): Unit = {
	}


	override def receive(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceInfo(totalRes, availRes)			=> recvResourceInfo(totalRes, availRes)
		}
		case _														=> log.error("Unknown message received!")
	}

	def receivedOnline(): Receive = {
		case message: NetworkResourceMessage	=> message match {
			case ResourceRequest(resources, ofcIP)			=> recvResourceRequest(resources, ofcIP)
			case ResourceFederationReply(allocResources) => recvResourceReply(allocResources)
		}
		case _														=> stashMessage()
	}


/* Private Methods: */
/* ================ */

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
	def props(ovxIP: InetAddress):
	Props = Props(new NetworkResourceAgent(ovxIP))
}
