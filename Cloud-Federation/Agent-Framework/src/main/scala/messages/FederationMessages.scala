package messages

import java.net.InetAddress

import datatypes.{OvxInstance, Host, ResourceAlloc, Subscription}

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
case class OvxInstanceReply(ovxInstance: OvxInstance)
  extends FederationMessages with MMAFederationDest