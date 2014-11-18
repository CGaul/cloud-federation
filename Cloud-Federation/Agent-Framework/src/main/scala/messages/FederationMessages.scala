package messages

import datatypes.{ResourceAlloc, Host, CloudSLA}

/**
 * @author Constantin Gaul, created on 11/16/14.
 */
sealed trait FederationMessages


// TODO: finalize. Still in WIP phase.
case class FederationInfoSubscription(cloudSLA: CloudSLA)
  extends FederationMessages with MMADest

// TODO: finalize. Still in WIP phase.
case class FederationInfoPublication(resourcePool: Vector[(Host, Vector[ResourceAlloc])])
  extends FederationMessages with MMADest
