package unitspecs

import java.net.InetAddress

import connectors.OVXConnector
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}

/**
 * @author Constantin Gaul, created on 1/15/15.
 */
class OVXConnectorSpec extends FlatSpec with Matchers with GivenWhenThen
{
  /* connectors.OVXConnector-Class Unit-Spec */
  /* ============================ */
  
  behavior of "The OVXConnector"
  
  val ovxConn: OVXConnector = OVXConnector(InetAddress.getByName("192.168.1.40"))
  val tenantIds = ovxConn.listVirtualNetworks
  System.out.println(tenantIds.mkString(", "))
  val physicalHosts = ovxConn.getPhysicalHosts //Currently not working, due to "ipAddress" bug
  val physicalTopo = ovxConn.getPhysicalTopology
  System.out.println(physicalTopo)
}
