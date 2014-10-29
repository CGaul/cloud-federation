package messages

import java.net.InetAddress

import akka.actor.ActorRef
import datatypes.ResourceAlloc

/**
 * Created by costa on 10/16/14.
 */
sealed trait NetworkResourceMessage

//TODO: needed or delete?
//case class ResourceInfo(totalResources: ResourceAlloc, availResources: ResourceAlloc) extends NetworkResourceMessage

case class ResourceRequest(resourcesToAlloc: ResourceAlloc, ofcIP: InetAddress) extends NetworkResourceMessage
case class ResourceReply(allocatedResources: ResourceAlloc) extends NetworkResourceMessage
case class ResourceFederationReply(allocatedResources: Vector[(ActorRef, ResourceAlloc)]) extends NetworkResourceMessage