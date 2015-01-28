package datatypes

import java.io.File

import akka.actor.{ActorRef, ActorSelection}



case class Subscriber(actorRefDA: ActorRef, authenticated: Boolean)
case class Subscription(actorSelMMA: ActorSelection, cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA], cert: File)