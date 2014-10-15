package messages

/**
 * Created by costa on 6/19/14.
 */
sealed abstract class AgentMessages

case class KillNotifier() extends AgentMessages
