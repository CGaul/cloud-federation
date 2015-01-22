package unitspecs

import java.io.File
import java.net.InetAddress

import datatypes.ByteUnit._
import datatypes.CPUUnit.CPUUnit
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes._
import org.scalatest.{Inspectors, GivenWhenThen, Matchers, FlatSpec}

/**
 * @author Constantin Gaul, created on 1/22/15.
 */
class NetworkComponentSpec extends FlatSpec with Matchers with GivenWhenThen with Inspectors
{

  // Create the resources-dir in Agent-Framework Module,
  // if not already existent:
  val resDir = new File("Agent-Framework/src/test/resources/")
  if(! resDir.exists()){
    resDir.mkdirs()
  }

  //General Small Node
  val res1 = new Resource(ResId(1), SMALL,
    ByteSize(8, GiB), ByteSize(50, GiB),
    ByteSize(10, MB), 10, Vector())

  //General Medium Node
  val res2= new Resource(ResId(2), MEDIUM,
    ByteSize(16, GiB), ByteSize(100, GiB),
    ByteSize(10, MB), 10, Vector())

  //Equals res2, but with lower latency
  val res3 = new Resource(ResId(3), MEDIUM,
    ByteSize(16, GiB), ByteSize(100, GiB),
    ByteSize(10, MB), 5, Vector())

  //General Large Node
  val res4 = new Resource(ResId(3), LARGE,
    ByteSize(24, GiB), ByteSize(200, GiB),
    ByteSize(50, MB), 10, Vector())



  /* Switch-Class Unit-Spec */
  /* ====================== */

  behavior of "A Switch"

  // All Hosts are starting with SLAs that allow 10 SMALL, 5 MEDIUM and 2 LARGE VMs at the beginning:

  val switchDPID1 = DPID("00:00:00:00:00:00:01:00")
  val switchDPID2 = DPID("00:00:00:00:00:00:02:00")
  val switchDPID3 = DPID("00:00:00:00:00:00:03:00")

  val switch1 	= new OFSwitch(switchDPID1, Map(1.toShort->Endpoint(switchDPID2, 1), 2.toShort->Endpoint(switchDPID3, 2)))
  val switch2 	= new OFSwitch(switchDPID2, Map(1.toShort->Endpoint(switchDPID1, 1), 2.toShort->Endpoint(switchDPID3, 2)))
  val switch3 	= new OFSwitch(switchDPID3, Map(1.toShort->Endpoint(switchDPID1, 1), 2.toShort->Endpoint(switchDPID2, 2)))

  it should "be equal equal to itself" in{
    switch1 should equal(switch1)
  }

  it should "be equal to another Switch with the same values" in{
    val switch1Comp = new OFSwitch(switchDPID1, Map(1.toShort->Endpoint(switchDPID1, 1), 2.toShort->Endpoint(switchDPID2, 2)))
    switch1 should equal(switch1Comp)
  }

  it should "be fully serializable to and deserializable from XML" in{
    val xmlSerialSwitch1 = OFSwitch.toXML(switch1)
    val xmlSerialSwitch2 = OFSwitch.toXML(switch2)
    val xmlSerialSwitch3 = OFSwitch.toXML(switch3)

    println("serialized Switch1 = " + xmlSerialSwitch1)
    println("serialized Switch2 = " + xmlSerialSwitch2)
    println("serialized Switch3 = " + xmlSerialSwitch3)

    val xmlDeserialRes1 = OFSwitch.fromXML(xmlSerialSwitch1)
    val xmlDeserialRes2 = OFSwitch.fromXML(xmlSerialSwitch2)
    val xmlDeserialRes3 = OFSwitch.fromXML(xmlSerialSwitch3)

    switch1 shouldEqual xmlDeserialRes1
    switch2 shouldEqual xmlDeserialRes2
    switch3 shouldEqual xmlDeserialRes3
    switch3 should not equal xmlDeserialRes1
  }

  it should "be loadable from and saveable to a XML file" in{
    val xmlFile1 = new File(resDir.getAbsolutePath +"/Switch1.xml")
    val xmlFile2 = new File(resDir.getAbsolutePath +"/Switch2.xml")
    val xmlFile3 = new File(resDir.getAbsolutePath +"/Switch3.xml")
    OFSwitch.saveToXML(xmlFile1, switch1)
    OFSwitch.saveToXML(xmlFile2, switch2)
    OFSwitch.saveToXML(xmlFile3, switch3)

    val loadedSwitch1 = OFSwitch.loadFromXML(xmlFile1)
    val loadedSwitch2 = OFSwitch.loadFromXML(xmlFile2)
    val loadedSwitch3 = OFSwitch.loadFromXML(xmlFile3)

    println("switch1 = " + loadedSwitch1)
    println("switch2 = " + loadedSwitch2)
    println("switch3 = " + loadedSwitch3)

    switch1 should equal (loadedSwitch1)
    switch2 should equal (loadedSwitch2)
    switch3 should equal (loadedSwitch3)
  }



  /* Host-Class Unit-Spec */
  /* ==================== */

  behavior of "A Host"

  // All Hosts are starting with SLAs that allow 10 SMALL, 5 MEDIUM and 2 LARGE VMs at the beginning:

  val hostSLA1 	= new HostSLA(0.91f, Vector(IMG, DMG, CLOOP, QCOW2),
    Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 5), (LARGE, 2)))

  val hostSLA2 	= new HostSLA(0.91f, Vector(IMG, CLOOP, BOCHS),
    Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 5), (LARGE, 2)))

  val hostSLA3 	= new HostSLA(0.99f, Vector(IMG, DMG, CLOOP, BOCHS, QCOW2),
    Vector[(CPUUnit, Int)]((SMALL, 10), (MEDIUM, 5), (LARGE, 2)))

  // The required SLAs for the resource-allocation are mapped as follows:
  // host1 can only allocate by the resourceAlloc with reqSLA1
  // host2 can only allocate by the resourceAlloc with reqSLA2
  // host3 isres2

  val reqSLA1		= new HostSLA(0.90f, Vector(IMG, DMG),
    Vector[(CPUUnit, Int)]((SMALL, 3), (MEDIUM, 2)))
  val reqSLA2		= new HostSLA(0.91f, Vector(IMG, CLOOP, BOCHS),
    Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 1)))
  val reqSLA3		= new HostSLA(0.99f, Vector(IMG, CLOOP, QCOW2),
    Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 1)))

  val resAlloc1 = new ResourceAlloc(1, Vector(res1), reqSLA1)
  val resAlloc2 = new ResourceAlloc(1, Vector(res2), reqSLA2)
  val resAlloc3 = new ResourceAlloc(1, Vector(res3), reqSLA3)


  val host1 		= new Host(res2, Endpoint("00:00:00:00:00:01:11:00", 1), InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA1)
  val host2 		= new Host(res3, Endpoint("00:00:00:00:00:01:12:00", 1), InetAddress.getByName("192.168.1.2"), "00:00:00:02", Vector(), hostSLA2)
  val host3 		= new Host(res4, Endpoint("00:00:00:00:00:01:13:00", 1), InetAddress.getByName("192.168.1.3"), "00:00:00:03", Vector(), hostSLA3)



  /* Test-Specs */
  /* ========== */

  it should "be equal to itself, independent from the object creation (applied or instantiated)" in{
    When("host1 is directly compared to itself")
    Then("host1.equals(host1) should be true")
    host1.equals(host1) should be (right = true)
    host1 == host1 should be (right = true)

    Given("A Host with the host1 footprint, instantiated statically via apply()")
    val staticHost1 = Host(res2, Endpoint("00:00:00:00:00:01:11:00", 1), InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA1)

    When("host1 is compared with the statically applied staticHost1 copy")
    Then("host1.equals(staticHost1) should be true")
    host1.equals(staticHost1) should be (right = true)
    staticHost1.equals(host1) should be (right = true)
    host1 == staticHost1 should be (right = true)
    staticHost1 == host1 should be (right = true)
  }

  it should "be equal to another Host with the same Host footprint (even with different Resource Allocations)" in{

    Given("A Respource with the host1 footprint, with additional link descriptions")
    val equalHost1 = Host(res2, Endpoint("00:00:00:00:00:01:11:00", 1), InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(resAlloc1), hostSLA1)


    When("host1 is compared with the resourceAlloc-different equalHost1")
    Then("host1.equals(equalHost1) should be true")
    host1.equals(equalHost1) should be (right = true)
    equalHost1.equals(host1) should be (right = true)
    host1 == equalHost1 should be (right = true)
    equalHost1 == host1 should be (right = true)
    info("Equality tests completed!")
  }

  it should "be unequal to Hosts with a different Host footprint than the origin" in{
    When("Two unequal Hosts are compared")
    Then("equals should be false")
    host1.equals(host2) should be (right = false)
    host2.equals(host1) should be (right = false)
    host2.equals(host3) should be (right = false)
    host1 == host3 should be (right = false)
    host3 == host2 should be (right = false)
    host3 == host1 should be (right = false)
    info("Unequality tests completed!")
  }

  it should "be comparable with another Host in a relative ordering" in{
    When("host1 is compared to itself")
    Then("RelativeHostByResOrdering.compare(host1, host1) should be == 0")
    (RelativeHostByResOrdering.compare(host1, host1) == 0) should be (right = true)

    When("smaller host1 is compared to medium host2")
    Then("RelativeHostByResOrdering.compare(host1, host2) should be < 0")
    (RelativeHostByResOrdering.compare(host1, host2) < 0) should be (right = true)

    When("medium host2 is compared to smaller host1")
    Then("RelativeHostByResOrdering.compare(host2, host1) should be > 0")
    (RelativeHostByResOrdering.compare(host2, host1) > 0) should be (right = true)
    info("Comparison tests completed!")
  }

  it should "allocate new ResourceAllocations, when allocation is acceptable" in{
    // For Host1:
    When("resAlloc1 is allocated to host1")
    Then("host1.allocate(resAlloc1) should be true")
    And ("resAlloc1 should be in host1's allocatedResources")
    host1.allocate(resAlloc1)._1 should be (right = true)
    host1.allocatedResources.size should be (1)
    host1.allocatedResources.contains(resAlloc1) should be (right = true)
    host2.allocatedResources.contains(resAlloc2) should be (right = false)
    host2.allocatedResources.contains(resAlloc3) should be (right = false)

    When("resAlloc2 is allocated to host1")
    Then("host1.allocate(resAlloc2) should be false, as the requested resAlloc2's image-formats are not supported")
    And("resAlloc1 should still be in host1's allocatedResources")
    host1.allocate(resAlloc2)._1 should be (right = false)
    host1.allocatedResources.size should be (1)
    host1.allocatedResources.contains(resAlloc1) should be (right = true)
    host1.allocatedResources.contains(resAlloc2) should be (right = false)
    host1.allocatedResources.contains(resAlloc3) should be (right = false)

    When("resAlloc3 is allocated to host1")
    Then("host1.allocate(resAlloc3) should be false, as the resAlloc3 SLA uptime limit is above Host1's uptime possibility")
    And("resAlloc1 should still be in host1's allocatedResources")
    host1.allocate(resAlloc3)._1 should be (right = false)
    host1.allocatedResources.size should be (1)
    host1.allocatedResources.contains(resAlloc1) should be (right = true)
    host1.allocatedResources.contains(resAlloc2) should be (right = false)
    host1.allocatedResources.contains(resAlloc3) should be (right = false)


    // For Host2:
    When("resAlloc1 is allocated to host2")
    Then("host2.allocate(resAlloc1) should be false, as the requested resAlloc1's image-formats are not supported")
    And ("resAlloc1 should not be in host2's allocatedResources")
    host2.allocate(resAlloc1)._1 should be (right = false)
    host2.allocatedResources.size should be (0)
    host2.allocatedResources.contains(resAlloc1) should be (right = false)
    host2.allocatedResources.contains(resAlloc2) should be (right = false)
    host2.allocatedResources.contains(resAlloc3) should be (right = false)

    When("resAlloc2 is allocated to host2")
    Then("host2.allocate(resAlloc2) should be true")
    And("resAlloc2 should be in host2's allocatedResources")
    host2.allocate(resAlloc2)._1 should be (right = true)
    host2.allocatedResources.size should be (1)
    host2.allocatedResources.contains(resAlloc1) should be (right = false)
    host2.allocatedResources.contains(resAlloc2) should be (right = true)
    host2.allocatedResources.contains(resAlloc3) should be (right = false)

    When("resAlloc3 is allocated to host2")
    Then("host2.allocate(resAlloc3) should be false, as the resAlloc3 SLA uptime limit is above host2's uptime possibility")
    And("resAlloc1 should still be in host2's allocatedResources")
    host2.allocate(resAlloc3)._1 should be (right = false)
    host2.allocatedResources.size should be (1)
    host2.allocatedResources.contains(resAlloc1) should be (right = false)
    host2.allocatedResources.contains(resAlloc2) should be (right = true)
    host2.allocatedResources.contains(resAlloc3) should be (right = false)


    // For Host3:
    When("resAlloc1 is allocated to host3")
    Then("host3.allocate(resAlloc1) should be true")
    And ("resAlloc1 should be in host3's allocatedResources")
    host3.allocate(resAlloc1)._1 should be (right = true)
    host3.allocatedResources.size should be (1)
    host3.allocatedResources.contains(resAlloc1) should be (right = true)
    host3.allocatedResources.contains(resAlloc2) should be (right = false)
    host3.allocatedResources.contains(resAlloc3) should be (right = false)

    When("resAlloc2 is allocated to host3")
    Then("host3.allocate(resAlloc2) should be true, as there is only a SMALL Node in it")
    And("resAlloc2 should be in host3's allocatedResources")
    host3.allocate(resAlloc2)._1 should be (right = true)
    host3.allocatedResources.size should be (2)
    host3.allocatedResources.contains(resAlloc1) should be (right = true)
    host3.allocatedResources.contains(resAlloc2) should be (right = true)
    host3.allocatedResources.contains(resAlloc3) should be (right = false)

    When("resAlloc3 is allocated to host3")
    Then("host3.allocate(resAlloc3) should be false, as the current SLA prohibits to have more than one MEDIUM Node on this host")
    And("resAlloc3 should not be in host3's allocatedResources, but the other two should be")
    host3.allocate(resAlloc3)._1 should be (right = false)
    host3.allocatedResources.size should be (2)
    host3.allocatedResources.contains(resAlloc1) should be (right = true)
    host3.allocatedResources.contains(resAlloc2) should be (right = true)
    host3.allocatedResources.contains(resAlloc3) should be (right = false)
  }

  it should "be able to split ResourceAllocations, if the allocation would violate the" +
    "QoS, defined by the combined SLA" in {
    pending
    //TODO: check allocation split via host.allocate()
  }

  it should "be fully serializable to and deserializable from XML" in{

    val host1 		= new Host(res2, Endpoint("00:00:00:00:00:01:11:00", 1), InetAddress.getByName("192.168.1.1"), "00:00:00:01", Vector(), hostSLA1)
    val host2 		= new Host(res3, Endpoint("00:00:00:00:00:01:12:00", 1), InetAddress.getByName("192.168.1.2"), "00:00:00:02", Vector(resAlloc1), hostSLA2)
    val host3 		= new Host(res4, Endpoint("00:00:00:00:00:01:13:00", 1), InetAddress.getByName("192.168.1.3"), "00:00:00:03", Vector(resAlloc2, resAlloc3), hostSLA3)

    val xmlSerialHost1 = Host.toXML(host1)
    val xmlSerialHost2 = Host.toXML(host2)
    val xmlSerialHost3 = Host.toXML(host3)

    println("serialized Host1 = " + xmlSerialHost1)
    println("serialized Host2 = " + xmlSerialHost2)
    println("serialized Host3 = " + xmlSerialHost2)

    val xmlDeserialHost1 = Host.fromXML(xmlSerialHost1)
    val xmlDeserialHost2 = Host.fromXML(xmlSerialHost2)
    val xmlDeserialHost3 = Host.fromXML(xmlSerialHost3)

    host1 shouldEqual xmlDeserialHost1
    host2 shouldEqual xmlDeserialHost2
    host3 shouldEqual xmlDeserialHost3
    host3 should not equal xmlDeserialHost1
  }

  it should "be loadable from and saveable to a XML file" in{
    val xmlFile1 = new File(resDir.getAbsolutePath +"/Host1.xml")
    val xmlFile2 = new File(resDir.getAbsolutePath +"/Host2.xml")
    val xmlFile3 = new File(resDir.getAbsolutePath +"/Host3.xml")
    Host.saveToXML(xmlFile1, host1)
    Host.saveToXML(xmlFile2, host2)
    Host.saveToXML(xmlFile3, host3)

    val loadedHost1 = Host.loadFromXML(xmlFile1)
    val loadedHost2 = Host.loadFromXML(xmlFile2)
    val loadedHost3 = Host.loadFromXML(xmlFile3)

    println("host1 = " + loadedHost1)
    println("host2 = " + loadedHost2)
    println("host3 = " + loadedHost3)

    host1 should equal (loadedHost1)
    host2 should equal (loadedHost2)
    host3 should equal (loadedHost3)
  }



  /* Tenant-Class Unit-Spec */
  /* ====================== */

  behavior of "A Tenant"

  val tenant1 = new Tenant(1, ("10.0.1.1", 16), InetAddress.getByName("192.168.1.41"), 10000)
  val tenant2 = new Tenant(2, ("10.0.2.1", 16), InetAddress.getByName("192.168.1.42"), 10000)
  val tenant3 = Tenant(3, ("10.0.3.1", 16), InetAddress.getLocalHost, 10000)


  /* Test-Specs */
  /* ========== */

  it should "be equal to itself, independent from the object creation (applied or instantiated)" in{
    When("tenant1 is directly compared to itself")
    Then("tenant1.equals(tenant1) should be true")
    tenant1.equals(tenant1) should be (right = true)
    tenant1 == tenant1 should be (right = true)

    Given("A Tenant with the tenant1 footprint, instantiated statically via apply()")
    val staticTenant1 = Tenant(1, ("10.0.1.1", 16), InetAddress.getByName("192.168.1.41"), 10000)

    When("tenant1 is compared with the statically applied staticTenant1 copy")
    Then("tenant1.equals(staticTenant1) should be true")
    tenant1.equals(staticTenant1) should be (right = true)
    staticTenant1.equals(tenant1) should be (right = true)
    tenant1 == staticTenant1 should be (right = true)
    staticTenant1 == tenant1 should be (right = true)
  }

  it should "be unequal to other Tenants" in{

    Given("Two Tenants with different parameters")
    When("tenant1 is compared to tenant2")
    Then("tenant1.equals(equalTenant1) should be false (and the other way around)")
    tenant1.equals(tenant2) should be (right = false)
    tenant2.equals(tenant1) should be (right = false)
    tenant1 == tenant2 should be (right = false)
    tenant2 == tenant1 should be (right = false)
    info("Equality tests completed!")
  }

  it should "be fully serializable to and deserializable from XML" in{
    val xmlSerialTenant1 = Tenant.toXML(tenant1)
    val xmlSerialTenant2 = Tenant.toXML(tenant2)
    val xmlSerialTenant3 = Tenant.toXML(tenant3)

    println("serialized Tenant1 = " + xmlSerialTenant1)
    println("serialized Tenant2 = " + xmlSerialTenant2)
    println("serialized Tenant3 = " + xmlSerialTenant2)

    val xmlDeserialTenant1 = Tenant.fromXML(xmlSerialTenant1)
    val xmlDeserialTenant2 = Tenant.fromXML(xmlSerialTenant2)
    val xmlDeserialTenant3 = Tenant.fromXML(xmlSerialTenant3)

    tenant1 shouldEqual xmlDeserialTenant1
    tenant2 shouldEqual xmlDeserialTenant2
    tenant3 shouldEqual xmlDeserialTenant3
    tenant3 should not equal xmlDeserialTenant1
  }

  it should "be loadable from and saveable to a XML file" in{
    val xmlFile1 = new File(resDir.getAbsolutePath +"/Tenant1.xml")
    val xmlFile2 = new File(resDir.getAbsolutePath +"/Tenant2.xml")
    val xmlFile3 = new File(resDir.getAbsolutePath +"/Tenant3.xml")
    Tenant.saveToXML(xmlFile1, tenant1)
    Tenant.saveToXML(xmlFile2, tenant2)
    Tenant.saveToXML(xmlFile3, tenant3)

    val loadedTenant1 = Tenant.loadFromXML(xmlFile1)
    val loadedTenant2 = Tenant.loadFromXML(xmlFile2)
    val loadedTenant3 = Tenant.loadFromXML(xmlFile3)

    println("tenant1 = " + loadedTenant1)
    println("tenant2 = " + loadedTenant2)
    println("tenant3 = " + loadedTenant3)

    tenant1 should equal (loadedTenant1)
    tenant2 should equal (loadedTenant2)
    tenant3 should equal (loadedTenant3)
  }
}
