package messages


sealed trait MessageDestination
trait DDADest extends MessageDestination
trait PubSubDest extends MessageDestination

trait CCFMDest extends MessageDestination
trait NRADest extends MessageDestination
trait MMADest extends MessageDestination
