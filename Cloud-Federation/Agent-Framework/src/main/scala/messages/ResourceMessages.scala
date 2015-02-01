package messages

import datatypes.{OFSwitch, ResourceAlloc, Tenant}

/**
 * @author Constantin Gaul, created on 10/16/14.
 */
sealed trait ResourceMessage

sealed trait DDAResourceDest extends DDADest
sealed trait MMAResourceDest extends MMADest
sealed trait NRAResourceDest extends NRADest
sealed trait CCFMResourceDest extends CCFMDest


case class TenantRequest(resourcesToAlloc: ResourceAlloc)
  extends ResourceMessage with CCFMResourceDest

case class ResourceRequest(tenant: Tenant, resourcesToAlloc: ResourceAlloc)
  extends ResourceMessage with CCFMResourceDest with NRAResourceDest with MMAResourceDest

case class ResourceReply(allocatedResources: ResourceAlloc)
  extends ResourceMessage with CCFMResourceDest


case class ResourceFederationRequest(tenant: Tenant, gwSwitch: OFSwitch, resourcesToAlloc: ResourceAlloc)
  extends ResourceMessage with NRAResourceDest with MMAResourceDest


case class ResourceFederationReply(tenant: Tenant, gwSwitch: OFSwitch, federatedResources: ResourceAlloc, wasFederated: Boolean)
  extends ResourceMessage with MMAResourceDest

case class ResourceFederationResult(tenant: Tenant, gwSwitch: OFSwitch, federatedResources: ResourceAlloc)
  extends ResourceMessage with NRAResourceDest