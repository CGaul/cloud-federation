package messages

import java.net.InetAddress

import akka.actor.ActorRef
import datatypes.ResourceAlloc

/**
 * @author Constantin Gaul, created on 10/16/14.
 */
sealed trait ResourceMessage

sealed trait DDAResourceDest extends DDADest
sealed trait MMAResourceDest extends MMADest
sealed trait NRAResourceDest extends NRADest
sealed trait CCFMResourceDest extends CCFMDest


case class ResourceRequest(resourcesToAlloc: ResourceAlloc, ofcIP: InetAddress, ofcPort: Int)
  extends ResourceMessage with CCFMResourceDest with NRAResourceDest with MMAResourceDest

case class ResourceReply(allocatedResources: ResourceAlloc)
  extends ResourceMessage with CCFMResourceDest with MMAResourceDest


case class ResourceFederationRequest(resourcesToAlloc: ResourceAlloc, ofcIP: InetAddress, ofcPort: Int)
  extends ResourceMessage with NRAResourceDest with MMAResourceDest


case class ResourceFederationReply(allocatedResources: Vector[(ActorRef, ResourceAlloc)])
  extends ResourceMessage with NRAResourceDest with MMAResourceDest