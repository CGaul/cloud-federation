package agents

/**
 * @author Constantin Gaul, created on 2/22/15.
 */


/* Enums: */
/* ====== */

object ActorMode extends Enumeration {
  type ActorMode = Value
  val MANUAL, AUTO = Value
}

object DiscoveryState extends Enumeration {
  type DiscoveryState = Value
  val ONLINE, OFFLINE = Value
}
