//package agents
//
//import akka.actor.Actor.Receive
//import akka.actor.{Actor, ActorLogging}
//import org.apache.http.protocol.HttpService
//import spray.can.Http
//import spray.routing.SimpleRoutingApp
//import spray.http.{Uri, HttpRequest}
//import spray.http.HttpMethods._
//
///**
// * Created by costa on 5/12/15.
// */
//object ResAllocHttpService extends Actor with ActorLogging with SimpleRoutingApp {
//  import context.dispatcher // ExcutionContext for the futures and scheduler
//
//  override def receive: Receive = {
//    // when a new connection comes in we register ourselves as the connection handler
//    case _: Http.Connected => sender ! Http.Register(self)
//
//    case HttpRequest(POST, Uri.Path("/resalloc"), _, _, _) =>
//
//      sender ! index
//  }
//}

//trait MyHttpService extends HttpService {
//  val sprayRoute = {
//    path("entity") {
//      post {
//        entity(as[JObject]) { someObject =>
//          doCreate(someObject)
//        }
//      }
//  }
//}
