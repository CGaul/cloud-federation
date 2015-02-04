package datatypes

import java.io.File

import akka.actor.ActorRef


case class Subscriber(actorRefDA: ActorRef, authenticated: Boolean, masterOfFederations: List[(Subscription, Subscription)])
case class Subscription(actorRefMMA: ActorRef, cloudSLA: CloudSLA, possibleHostSLAs: Vector[HostSLA], cert: File)