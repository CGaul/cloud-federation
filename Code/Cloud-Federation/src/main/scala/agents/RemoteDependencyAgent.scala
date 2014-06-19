package agents.cloudfederation

import akka.actor._
import akka.actor.Stash
import messages.KillNotifier
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorIdentity
import akka.actor.Identify

/**
 * Created by costa on 6/3/14.
 */
abstract class RemoteDependencyAgent(remoteDependencies: Vector[ActorSelection]) extends Actor with ActorLogging with Stash
{
/* Values: */
/* ======= */

	//Initializes an internal boolean vector with false for every ActorSelection in remoteDependencies:
	private val dependentActors: ArrayBuffer[Option[ActorRef]] = ArrayBuffer.fill(remoteDependencies.length)(None)


/* Execution: */
/* ========= */

	//RemoteDependencyAgent starts in offline context.
	this.sendIdentityRequests(remoteDependencies)


/* Auxillary Constructors: */
/* ======================= */


/* Methods: */
/* ======== */

	// Akka Actor Send & Receive method-handling:
	// ------------------------------------------

	def sendIdentityRequests(actors: Vector[ActorSelection]) = {

		var actorID: Integer = 0
		for (actor <- actors){
			actor ! Identify(actorID)
			actorID += 1
		}
	}

  override def receive: Receive = offline

  def offline: Receive = {
		case ActorIdentity(actorID, actorRef)	=> recv_onlineNotifier(actorID, actorRef)
		case _											=> stashMessage
	}

  /**
	* The online receive-handle that needs to be implemented by the specified class, extending this RemoteDependencyAgent.
	* Contains the functionality, which will be executed by the Actor if all RemoteDependencies are solved and a message
	* comes into the mailbox, or there were stashed messages while the Actor was in its offline state.
	* @return
	*/
	def online: Receive


  def stashMessage = {
	 log.debug("Received Message, before RemoteDependencyAgent went online. Stashed message until being online.")
	 try {
		stash()
	 }
	 catch {
		case e: StashOverflowException => log.error("Reached Stash buffer. Received message will be ignored."+
		  e.printStackTrace())
	 }
  }

  def recv_onlineNotifier(actorID: Any, actorRef: Option[ActorRef]) = {
		if(actorRef != None){
			dependentActors.update(actorID.asInstanceOf[Integer], actorRef)
		}

		//Search for any unresolved ActorRefs:
		val unresolved = dependentActors.find(_ == None)

		//If no unresolved ActorRefs are left, become online:
		if(unresolved == None){
			context.become({
			  case KillNotifier()	=> recv_offlineNotifier
			  case _						=> unstashAll()
												online
			})
			log.info("All ActorRef dependencies solved. RemoteDependencyActor is now ONLINE.")
		}
	 	else {
		  log.info("Some ActorRef dependencies are currently unsolved. RemoteDependencyActor stays OFFLINE. " +
			 "Dependent Actors: "+ dependentActors)
		}
	}

  	def recv_offlineNotifier = {
	 //Find the actorRef that notifies this actor of being killed in the actorDependency,
	 //remove it and make this actor offline again:
	 val killedActorIndex = dependentActors.indexOf(sender)
	 if (killedActorIndex != -1){
		dependentActors.update(killedActorIndex, None)
		context.become(offline)
		log.info("Actor "+ sender.toString() +" has send a KillNotifier. RemoteDependencyActor is now OFFLINE.")
	 }
  }
}