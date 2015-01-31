package agents.cloudfederation

import akka.actor._
import messages.TopologyDiscovery

import scala.collection.mutable.ArrayBuffer

/**
 * A simple Actor which has two states: _offline and online.
 * If all remote dependencies {Vector[ActorSelection]} are answering to a identityRequest, the Actor will go online,
 * otherwise the state defaults to _offline.
 * @author Constantin Gaul, created on 6/3/14.
 */
abstract class RemoteDependencyAgent(remoteDependencies: List[ActorSelection]) extends Actor with ActorLogging with Stash
{
	
/* Values: */
/* ======= */

	//Initializes an internal boolean vector with false for every ActorSelection in remoteDependencies:
	val dependentActors: ArrayBuffer[Option[ActorRef]] = ArrayBuffer.fill(remoteDependencies.length)(None)
	
/* Variables: */
/* ========== */
	
	private var _shouldRun = true
	private var unresolvedActors: List[ActorSelection] = remoteDependencies


/* Execution: */
/* ========== */

	val _workingThread = new Thread(new Runnable{
		override def run(): Unit = {
			log.info("Trying to solve remote Dependencies of other Agents in {}...", context.self)
			val unresolvedBefore = unresolvedActors.length
			while(_shouldRun) {
				sendIdentityRequests()
			}
			
			Thread.sleep(1 * 1000) //sleep 1 seconds between each discovery
			val unresolvedAfter = unresolvedActors.length
			log.info("Resolved {}/{} Actor dependencies in current iteration.", 
							 unresolvedBefore - unresolvedAfter, dependentActors.length)
			Thread.sleep(1 * 1000) //sleep 1 seconds between each discovery
		}
	})



/* Methods: */
/* ======== */
	
	initActor()

	private def initActor() = {
		_workingThread.start()		
	}
	private def sendIdentityRequests() = {

		var actorID: Int = 0
		for (actor <- unresolvedActors){
			actor ! Identify(actorID)
			actorID += 1
		}
	}

	// Akka Actor Send & Receive method-handling:
	// ------------------------------------------

  override def receive: Receive = _offline()

  private def _offline(): Receive = {
		case ActorIdentity(actorID, actorRef)	=> recv_onlineNotifier(actorID, actorRef)
		case _											=> stashMessage()
	}

  /**
	* The online receive-handle that needs to be implemented by the specified class, extending this RemoteDependencyAgent.
	* Contains the functionality, which will be executed by the Actor if all RemoteDependencies are solved and a message
	* comes into the mailbox, or there were stashed messages while the Actor was in its _offline state.
	* @return
	*/
	def receiveOnline: Receive


  private def stashMessage() = {
	 log.debug("Received Message, before RemoteDependencyAgent went online. Stashed message until being online.")
	 try {
		stash()
	 }
	 catch {
		case e: StashOverflowException => log.error("Reached Stash buffer. Received message will be ignored."+
		  e.printStackTrace())
	 }
  }

  private def recv_onlineNotifier(actorID: Any, actorRef: Option[ActorRef]) = {
		if(actorRef != None){
			dependentActors.update(actorID.asInstanceOf[Int], actorRef)
			unresolvedActors = unresolvedActors.filterNot(_ == context.actorSelection(actorRef.get.path))
		}

		//Search for any unresolved ActorRefs:
		val unresolved = dependentActors.count(_ == None)

		//If no unresolved ActorRefs are left, become online:
		if(unresolved == 0){
		 	unstashAll()
			_shouldRun = false
			context.become(receiveOnline(())
			log.debug("All ActorRef dependencies solved. RemoteDependencyActor is now ONLINE.")
		}
	 	else {
		  log.debug("Some ActorRef dependencies are currently unsolved. RemoteDependencyActor stays OFFLINE. " +
			 "Dependent Actors: "+ dependentActors)
		}
	}

  private def recv_offlineNotifier() = {
	 //Find the actorRef that notifies this actor of being killed in the actorDependency,
	 //remove it and make this actor _offline again:
	 val killedActorIndex = dependentActors.indexOf(sender())
	 if (killedActorIndex != -1){
		dependentActors.update(killedActorIndex, None)
		context.become(_offline())
		log.debug("Actor "+ sender().toString() +" has send a KillNotifier. RemoteDependencyActor is now OFFLINE.")
	 }
  }
}