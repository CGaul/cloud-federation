import agents.PubSubFederator
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
 * @author Constantin Gaul, created on 6/23/14.
 */
object PubSubManagement extends App
{
  val config = ConfigFactory.load("localApplication.conf")
  val system = ActorSystem("PubSubSystem", config.getConfig("pubSubSystem").withFallback(config))

  val pubSubFederator = "remoteFederator"
  val pubSubServer = system.actorOf(Props[PubSubFederator], name=pubSubFederator)
}
