package messages

import java.net.InetAddress

import akka.actor.ActorRef
import datatypes.Resources

/**
 * Created by costa on 10/16/14.
 */
sealed trait NetworkResourceMessage

case class ResourceRequest(resources: Resources, ofcIP: InetAddress) extends NetworkResourceMessage
case class ResourceReply(allocatedResources: Resources) extends NetworkResourceMessage
case class ResourceFederationReply(allocatedResources: Vector[(ActorRef, Resources)]) extends NetworkResourceMessage