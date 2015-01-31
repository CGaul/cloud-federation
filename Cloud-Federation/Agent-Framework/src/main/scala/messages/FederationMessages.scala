package messages

import datatypes.{Host, ResourceAlloc, Subscription}

/**
 * @author Constantin Gaul, created on 11/16/14.
 */
sealed trait FederationMessages

sealed trait MMAFederationDest extends MMADest

// TODO: finalize. Still in WIP phase.
case class FederationInfoSubscription(subscription: Subscription)
  extends FederationMessages with MMAFederationDest

// TODO: finalize. Still in WIP phase.
case class FederationInfoPublication(resourcePool: Vector[(Host, Vector[ResourceAlloc])])
  extends FederationMessages with MMAFederationDest
