package messages

import datatypes.{Host, OvxInstance, ResourceAlloc, Subscription}

/**
 * @author Constantin Gaul, created on 11/16/14.
 */
sealed trait FederationMessages

sealed trait MMAFederationDest extends MMADest
sealed trait NRAFederationDest extends NRADest
sealed trait PubSubFederationDest extends PubSubDest

// TODO: finalize. Still in WIP phase.
case class FederationInfoSubscription(subscription: Subscription)
  extends FederationMessages with MMAFederationDest

// TODO: finalize. Still in WIP phase.
case class FederationInfoPublication(resourcePool: Vector[(Host, Vector[ResourceAlloc])])
  extends FederationMessages with MMAFederationDest



case class OvxInstanceRequest(subscription: Subscription)
  extends FederationMessages with PubSubFederationDest

/**
 * Only received through a Future of PubSub-Federator -> MMA
 * @param ovxInstance: The federated OVX Instance, managed by the PubSub-Federator
 */
case class OvxInstanceReply(ovxInstance: OvxInstance)
  extends FederationMessages with MMAFederationDest