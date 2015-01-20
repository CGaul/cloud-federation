package messages

import datatypes.{OFSwitch}

/**
 * Created by costa on 1/20/15.
 */
sealed trait NetworkMessages

sealed trait NRANetworkDest extends NRADest

case class TopologyDiscovery(switches: List[OFSwitch])
  extends NRANetworkDest
