package agents.cloudfederation

import akka.actor._
import scala.collection.mutable.ArrayBuffer
import akka.actor.Identify

/**
 * Created by costa on 6/3/14.
 */
abstract class RemoteDependencyAgent(remoteDependencies: Vector[ActorSelection]) extends Actor with ActorLogging
{
/* Values: */
/* ======= */

	//Initializes an internal boolean vector with false for every ActorSelection in remoteDependencies:
	private val resolvedActors: ArrayBuffer[Option[ActorRef]] = ArrayBuffer.fill(remoteDependencies.length)(None)


/* Execution: */
/* ======== */

	//RemoteDependencyAgent starts in offline context.
	context.become(offline)
	this.sendIdentityRequests(remoteDependencies)


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


	def offline: Receive = {
		case ActorIdentity(actorID, actorRef)	=> recvActorIdentity(actorID, actorRef)
		case _											=> log.error("Offline RemoteDependencyAgents should not receive anything!")
	}

	def recvActorIdentity(actorID: Any, actorRef: Option[ActorRef]): Unit = {
		if(actorRef != None){
			resolvedActors.update(actorID.asInstanceOf[Integer], actorRef)
		}

		//Search for any unresolved ActorRefs:
		val unresolved = resolvedActors.find(_.equals(None))

		//If no unresolved ActorRefs are left, become online:
		if(unresolved == None){
			context.unbecome()
			log.info("All ActorRef dependencies solved. Actor is now ONLINE (using regular receive() method).")
		}
		//Else become offline (needed, as dependent Actors could be shutdown on runtime,
		// which draws child actors offline again.
		else{
			context.become(offline)
			log.info("Some ActorRef dependencies unsolved. Actor becomes OFFLINE.")
		}
	}
}
