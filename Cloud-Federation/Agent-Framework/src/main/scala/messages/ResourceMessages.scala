package messages

import akka.actor.ActorRef
import datatypes.{ResourceAlloc, Tenant}

/**
 * @author Constantin Gaul, created on 10/16/14.
 */
sealed trait ResourceMessage

sealed trait DDAResourceDest extends DDADest
sealed trait MMAResourceDest extends MMADest
sealed trait NRAResourceDest extends NRADest
sealed trait CCFMResourceDest extends CCFMDest


case class ResourceRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc)
  extends ResourceMessage with CCFMResourceDest with NRAResourceDest with MMAResourceDest

case class ResourceReply(allocatedResources: ResourceAlloc)
  extends ResourceMessage with CCFMResourceDest with MMAResourceDest


case class ResourceFederationRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc)
  extends ResourceMessage with NRAResourceDest with MMAResourceDest


case class ResourceFederationReply(allocatedResources: Vector[(ActorRef, ResourceAlloc)])
  extends ResourceMessage with NRAResourceDest with MMAResourceDest