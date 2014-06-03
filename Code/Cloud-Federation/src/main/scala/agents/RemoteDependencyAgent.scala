package agents.cloudfederation

import akka.actor._
import scala.collection.mutable.ArrayBuffer
import akka.actor.Identify

/**
 * Created by costa on 6/3/14.
 */
class RemoteDependencyAgent(remoteDependencies: Vector[ActorSelection]) extends Actor with ActorLogging
{
	//Initializes an internal boolean vector with false for every ActorSelection in remoteDependencies:
	private val resolvedActors: ArrayBuffer[Option[ActorRef]] = ArrayBuffer.fill(remoteDependencies.length)(None)


	def online: Receive = ???
	def offline: Receive = ???

	def sendIdentityRequests(actors: List[ActorSelection]) = {

		var actorID: Integer = 0
		for (actor <- actors){
			actor ! Identify(actorID)
			actorID += 1
		}
	}

	def recvActorIdentity(actorID: Any, actorRef: Option[ActorRef]): Unit = {
		if(actorRef != None){
			resolvedActors.update(actorID.asInstanceOf[Integer], actorRef)
		}

		//Search for any unresolved ActorRefs:
		val unresolved = resolvedActors.find(_.equals(None))

		//If no unresolved ActorRefs are left, become online:
		if(unresolved == None){
			context.become(online)
			log.info("All ActorRef dependencies solved. Actor becomes ONLINE.")
		}
		//Else become offline (needed, as dependent Actors could be shutdown on runtime,
		// which draws child actors offline again.
		else{
			context.become(offline)
			log.info("Some ActorRef dependencies unsolved. Actor becomes OFFLINE.")
		}
	}

	override def receive: Receive = {
		case ActorIdentity(actorID, actorRef) => recvActorIdentity(actorID, actorRef)
	}
}
