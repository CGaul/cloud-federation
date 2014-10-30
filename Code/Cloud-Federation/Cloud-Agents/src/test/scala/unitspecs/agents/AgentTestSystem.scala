package unitspecs.agents

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory

/**
 * Created by costa on 10/30/14.
 */
case class AgentTestSystem[T](confName: String, actorSysname: String) {
	def prepareAgentTestSystem(testAgentProps: Props): (TestActorRef[T], T) = {
		val config 							= ConfigFactory.load("testApplication.conf")
		implicit val system 				= ActorSystem("CloudAgents", config.getConfig("cloudAgents").withFallback(config))

		val nraRef : TestActorRef[T] 	= TestActorRef[T](testAgentProps)
		val nraActor: T 					= nraRef.underlyingActor

		return (nraRef, nraActor)
	}

}
