package messages

import datatypes.{OvxInstance, OFSwitch}

/**
 * @author Constantin Gaul, created on 1/20/15.
 */
sealed trait NetworkMessages

sealed trait NDANetworkDest extends NDADest
sealed trait NRANetworkDest extends NRADest


/**
 * Send from NRA to NDA.
 * For each incoming DiscoveryRequest, NDA has to reply with a TopologyDiscovery.
 */
case class DiscoveryRequest()
  extends NDANetworkDest

/**
 * Sends a TopologyDiscovery from the NDA to the NRA.
 * Contains the ovxInstance were the discovery ran on and the switches, that were discovered.
 * @param ovxInstance
 * @param newSwitches
 */
case class TopologyDiscovery(ovxInstance: OvxInstance, newSwitches: List[OFSwitch], removedSwitches: List[OFSwitch])
  extends NRANetworkDest
