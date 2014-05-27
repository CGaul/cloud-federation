package messages

import java.net.InetAddress


sealed abstract class DiscoveryMessage //TODO: implement common methods for Discovery-Messages here.

case class DiscoveryInit(pubSubServer : InetAddress, serverPort : Integer) extends DiscoveryMessage

case class DiscoverySubsription(certificate : String) extends DiscoveryMessage

case class DiscoveryPublication(cloudAddressList: List[InetAddress], cloudAddressPorts: List[Integer]) extends DiscoveryMessage
