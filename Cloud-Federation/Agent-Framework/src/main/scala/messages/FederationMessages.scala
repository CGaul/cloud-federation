package messages

import datatypes._

/**
 * @author Constantin Gaul, created on 11/16/14.
 */
sealed trait FederationMessages

sealed trait MMAFederationDest extends MMADest
sealed trait NRAFederationDest extends NRADest
sealed trait PubSubFederationDest extends PubSubDest


/**
 * Send from local NRA back to local MMA as a reply to the FederateableResourceRequest-message.
 * Tells the MMA which Resources in the NRAs Cloud-Topology are marked as "federateable".
 * @param federateableResources
 */
case class FederateableResourceDiscovery(federateableResources: Vector[(Host, ResourceAlloc)])
  extends FederationMessages with MMAFederationDest


/**
 * Send from one MMA to another MMA in order to tell the receiving MMA that the originating MMA wants to subscribe at him.
 * FederationInfoSubscription is needed to receive periodic FederationInfoPublications from receiving MMA afterwards.
 * @param subscription
 */
case class FederationInfoSubscription(subscription: Subscription)
  extends FederationMessages with MMAFederationDest

/**
 * After a MMA is subscribed at the publishing MMA, the subscriber will regularly receive FederationInfoPublications from
 * the publishing MMA.
 * Each FederationInfoPublication is an invitation to a ResourceAuctionBid that may be sent for each available ResourceAlloc
 * in the resourcePool of this message, or for a subset of ResourceAllocs. Each ResourceAuctionBid is part of the auction
 * that will be managed by the publishing MMA afterwards.
 * @param resourcePool
 */
case class FederationInfoPublication(resourcePool: Vector[(Host, ResourceAlloc)])
  extends FederationMessages with MMAFederationDest


/**
  * Used after a FederationInfoPublication between two MMAs to bid on a a previously advertised resourceAlloc
  * that the asking MMA wants to win in a following auction for the given askPrice. The askPrice is obligatory:
  * Once the bidder won the auction, it has to allocate the Resource for the given price or at least has to pay
  * a certain violation fee after a given time of unallocation. However, the complete askPrice only has to be paid,
  * if the ResourceAlloc is allocated by the subscriber MMA.
  * @param resourceHost
  * @param resourceBid
  * @param askPrice
  */
case class ResourceAuctionBid(resourceHost: Host, resourceBid: ResourceAlloc, askPrice: Price)
  extends FederationMessages with MMAFederationDest

/**
  * Follows on a ResourceAuctionBid and is send from the receiving MMA to the originated MMA. 
  * Auctioneer MMA tells bidding MMA wether it has won the resourceAlloc or not.
  * @param resourceBid
  * @param won
  */
case class ResourceAuctionResult(resourceBid: ResourceAlloc, won: Boolean)
  extends FederationMessages with MMAFederationDest


case class OvxInstanceRequest(subscription: Subscription)
  extends FederationMessages with PubSubFederationDest

/**
  * Only received through a Future of PubSub-Federator -> MMA
  * @param ovxInstance: The federated OVX Instance, managed by the PubSub-Federator
  */
case class OvxInstanceReply(ovxInstance: OvxInstance)
  extends FederationMessages with MMAFederationDest