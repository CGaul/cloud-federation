package messages

import datatypes.OFSwitch

/**
 * @author Constantin Gaul, created on 1/20/15.
 */
sealed trait NetworkMessages

sealed trait NRANetworkDest extends NRADest

case class TopologyDiscovery(switches: List[OFSwitch])
  extends NRANetworkDest
