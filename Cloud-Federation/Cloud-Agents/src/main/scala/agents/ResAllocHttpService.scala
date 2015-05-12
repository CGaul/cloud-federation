package agents

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import datatypes.ResourceAlloc
import messages.TenantRequest
import org.apache.http.protocol.HttpService
import spray.can.Http
import spray.routing.SimpleRoutingApp
import spray.http.{Uri, HttpRequest}
import spray.http.HttpMethods._

/**
 * Created by costa on 5/12/15.
 */
class ResAllocHttpService(ccfm: ActorRef, httpServiceAddr: String, httpServicePort: Int) extends Actor with ActorLogging {
//  import context.dispatcher // ExcutionContext for the futures and scheduler

  override def receive: Receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(POST, Uri.Path("/resalloc"), headers, entity, _) =>
      log.info(s"Received ResourceAlloc over http://$httpServiceAddr/resalloc with data: ${entity.data.asString}")
      val resAlloc = ResourceAlloc.loadFromXML(entity.data.asString)
      ccfm ! TenantRequest(resAlloc)
  }
}

object ResAllocHttpService
{
  /**
   * props-method is used in the AKKA-Context, spawning a new Agent.
   * @return An Akka Properties-Object
   */
  def props(ccfm: ActorRef, httpServiceAddr: String, httpServicePort: Int):
  Props = Props(new ResAllocHttpService(ccfm, httpServiceAddr, httpServicePort))
}
