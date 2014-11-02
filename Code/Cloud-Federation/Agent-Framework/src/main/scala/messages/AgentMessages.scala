package messages

/**
 * @author Constantin Gaul, created on 6/19/14.
 */
sealed abstract class AgentMessages

case class KillNotifier() extends AgentMessages
