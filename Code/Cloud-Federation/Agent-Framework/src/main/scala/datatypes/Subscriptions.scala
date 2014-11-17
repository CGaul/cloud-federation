package datatypes

import java.io.File

import akka.actor.{ActorSelection, ActorRef}



case class Subscriber(actorRef: ActorRef, authenticated: Boolean)
case class Subscription(cloudMMA: ActorSelection, cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA], cert: File)