package agents

import akka.actor._

import scala.collection.mutable.ArrayBuffer


/* Enums: */
/* ====== */

object DependencyState extends Enumeration {
  type DependencyState = Value
  val ONLINE, OFFLINE = Value
}


/* Classes: */
/* ======== */

/**
 * A simple Actor which has two states: OFFLINE and ONLINE.
 * If all remote dependencies {Vector[ActorSelection]} are answering to a identityRequest, the Actor will go ONLINE,
 * otherwise the state defaults to OFFLINE.
 * @author Constantin Gaul, created on 6/3/14.
 */
//TODO: maybe use Akka FSM for the Implementation of the RemoteDependencyAgent
abstract class RemoteDependencyAgent(remoteDependencies: List[ActorSelection]) extends Actor with ActorLogging with Stash
{

/* Variables: */
/* ========== */
	
  var state = DependencyState.OFFLINE
  
	private var _shouldRun = true

	private var _resolvedActorRefs: Map[Int, ActorRef] = Map()
	private var _resolvedActorSelects: Map[Int, ActorSelection] = Map()

	private var _unresolvedActors: Map[Int, ActorSelection] = Map()
	for (n <- 0 to (remoteDependencies.size - 1)) yield {
		_unresolvedActors += (n -> remoteDependencies(n))
	}


/* Getters: */
/* ======== */

  def resolvedActorRefs: List[(Int, ActorRef)] = _resolvedActorRefs.toList
  def resolvedActorSelects: List[(Int, ActorSelection)] = _resolvedActorSelects.toList
  def unresolvedActors: List[(Int, ActorSelection)] = _unresolvedActors.toList


/* Execution: */
/* ========== */

	val _workingThread = new Thread(new Runnable{
		override def run(): Unit = {
			log.info("Trying to solve remote Dependencies of other Agents in {}...", context.self)
			while(_shouldRun) {
			  val unresolvedBefore = _unresolvedActors.size
				sendIdentityRequests()
        
        Thread.sleep(1 * 1000) //sleep 1 seconds between each discovery
        val unresolvedAfter = _unresolvedActors.size
        log.info("Resolved {}/{} Actor dependencies in current iteration.",
          unresolvedBefore - unresolvedAfter, unresolvedBefore)
        Thread.sleep(1 * 1000) //sleep 1 seconds between each discovery
			}
		}
	})



/* Methods: */
/* ======== */
	
	initActor()

	private def initActor() = {
		_workingThread.start()		
	}
	private def sendIdentityRequests() = {
		for ((actorId, actorRef) <- _unresolvedActors){
      log.info("Send Identify request with ID {} to Actor {}", actorId, actorRef)
			actorRef ! Identify(actorId)
		}
	}

	// Akka Actor Send & Receive method-handling:
	// ------------------------------------------

  override def receive: Receive = _offline()

  private def _offline(): Receive = {
		case ActorIdentity(actorID, actorRef)	=> recv_onlineNotifier(actorID.asInstanceOf[Int], actorRef)
		case _											=> stashMessage()
	}

  /**
	* The online receive-handle that needs to be implemented by the specified class, extending this RemoteDependencyAgent.
	* Contains the functionality, which will be executed by the Actor if all RemoteDependencies are solved and a message
	* comes into the mailbox, or there were stashed messages while the Actor was in its _offline state.
	* @return
	*/
	def receiveOnline: Receive

  /**
   * Event that will be queried, when the RemoteDependencyAgent switches its internal state from OFFLINE to ONLINE.
   * Implement method calls here, that should be handled as an ONLINE state initializer.
   * 
   * This Event is called just after the state change via context.become(receiveOnline) has happened.
   */
  def becomeOnline: Unit = {}

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

  private def recv_onlineNotifier(actorID: Int, actorRef: Option[ActorRef]) = {
		if(actorRef.isDefined){
      val resolvedActorSel = _unresolvedActors.find(_._1 == actorID)
      if(resolvedActorSel.isDefined){
        _resolvedActorSelects += (actorID -> resolvedActorSel.get._2)
			  _resolvedActorRefs += (actorID -> actorRef.get)
        _unresolvedActors -= resolvedActorSel.get._1
      }

		}

		//Search for any unresolved ActorRefs:
		val unresolved = _unresolvedActors.size

		//If no unresolved ActorRefs are left, become online:
		if(unresolved == 0){
		 	unstashAll()
			_shouldRun = false
			context.become(receiveOnline)
      state = DependencyState.ONLINE
      becomeOnline
			log.info("All ActorRef dependencies solved. RemoteDependencyActor is now ONLINE.")
		}
	 	else {
		  log.debug("Some ActorRef dependencies are currently unsolved. RemoteDependencyActor stays OFFLINE. " +
			 "Resolved Actors: "+ _resolvedActorRefs)
		}
	}

  // TODO: use
  private def recv_offlineNotifier() = {
	 //Find the actorRef that notifies this actor of being killed in the actorDependency,
	 //remove it and make this actor _offline again:
	 val killedActorSel = _resolvedActorSelects.find(_._2 == sender())
	 if (killedActorSel.isDefined){
		 //unresolve this actor
     _resolvedActorSelects -= killedActorSel.get._1
     _resolvedActorRefs -= killedActorSel.get._1
		 _unresolvedActors += (killedActorSel.get._1 -> killedActorSel.get._2)
		 context.become(_offline())
     state = DependencyState.OFFLINE
		 log.debug("Actor "+ sender().toString() +" has send a KillNotifier. RemoteDependencyActor is now OFFLINE.")
	 }
  }
}