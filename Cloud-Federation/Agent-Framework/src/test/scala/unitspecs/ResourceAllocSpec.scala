package unitspecs

import java.io.File

import datatypes.ByteUnit._
import datatypes.CPUUnit._
import datatypes.ImgFormat._
import datatypes._
import org.scalatest.{FlatSpec, GivenWhenThen, ShouldMatchers}

/**
 * @author Constantin Gaul, created onc 10/28/14.
 */
class ResourceAllocSpec extends FlatSpec with ShouldMatchers with GivenWhenThen
{

  // Create the resources-dir in Agent-Framework Module,
  // if not already existent:
  val resDir = new File("Agent-Framework/src/test/resources/")
  if(! resDir.exists()){
    resDir.mkdirs()
  }


/* ResourceAlloc-Class Unit-Spec */
/* ============================= */

  behavior of "A ResourceAlloc"

  val resources1  = ResourceAllocSpec.prepareTestResources1._1
  val hostSLA1    = ResourceAllocSpec.prepareTestResources1._2
  val resources2  = ResourceAllocSpec.prepareTestResources2._1
  val hostSLA2    = ResourceAllocSpec.prepareTestResources2._2

  val resAlloc1   = new ResourceAlloc(1, resources1, hostSLA1)
  val resAlloc2   = new ResourceAlloc(2, resources1, hostSLA1)
  val resAlloc3   = new ResourceAlloc(2, resources2, hostSLA1)
  val resAlloc4   = new ResourceAlloc(2, resources2, hostSLA2)


  /* Test-Specs */
  /* ========== */

  it should "be equal to itself, independent from the object creation (applied or instantiated)" in {
    When("res1 is directly compared to itself")
    Then("res1.equals(res1) should be true")
    resAlloc1 should equal(resAlloc1)
    resAlloc1 == resAlloc1 should be (right = true)


    Given("A Resource with the resAlloc1 footprint, instantiated statically via apply()")
    val staticResAlloc1 = ResourceAlloc(1, resources1, hostSLA1)

    When("resAlloc1 is compared with the statically applied staticResAlloc1 copy")
    Then("resAlloc1.equals(staticResAlloc1) should be true")
    resAlloc1.equals(staticResAlloc1) should be (right = true)
    staticResAlloc1.equals(resAlloc1) should be (right = true)
    resAlloc1 == staticResAlloc1 should be (right = true)
    staticResAlloc1 == resAlloc1 should be (right = true)
  }

  it should "be unequal to Resources with a different Resource footprint than the origin" in{
    When("Two unequal resources are compared")
    Then("equals should be false")
    resAlloc1.equals(resAlloc2) should be (right = false)
    resAlloc2.equals(resAlloc1) should be (right = false)
    resAlloc3.equals(resAlloc2) should be (right = false)
    resAlloc4.equals(resAlloc3) should be (right = false)
    resAlloc1 == resAlloc3 should be (right = false)
    resAlloc3 == resAlloc4 should be (right = false)
    resAlloc3 == resAlloc1 should be (right = false)
    info("Unequality tests completed!")
  }

  it should "be fully serializable to and deserializable from XML" in{
    val xmlSerialRes1 = ResourceAlloc.toXML(resAlloc1)
    val xmlSerialRes2 = ResourceAlloc.toXML(resAlloc2)
    val xmlSerialRes3 = ResourceAlloc.toXML(resAlloc3)

    println("serialized Res1 = " + xmlSerialRes1)
    println("serialized Res2 = " + xmlSerialRes2)
    println("serialized Res3 = " + xmlSerialRes3)

    val xmlDeserialRes1 = ResourceAlloc.fromXML(xmlSerialRes1)
    val xmlDeserialRes2 = ResourceAlloc.fromXML(xmlSerialRes2)
    val xmlDeserialRes3 = ResourceAlloc.fromXML(xmlSerialRes3)

    resAlloc1 shouldEqual xmlDeserialRes1
    resAlloc2 shouldEqual xmlDeserialRes2
    resAlloc3 shouldEqual xmlDeserialRes3
    resAlloc3 should not equal xmlDeserialRes1
  }

  it should "be loadable from and saveable to a XML file" in{
    val xmlFile1 = new File(resDir.getAbsolutePath +"/ResourceAlloc1.xml")
    val xmlFile2 = new File(resDir.getAbsolutePath +"/ResourceAlloc2.xml")
    val xmlFile3 = new File(resDir.getAbsolutePath +"/ResourceAlloc3.xml")
    ResourceAlloc.saveToXML(xmlFile1, resAlloc1)
    ResourceAlloc.saveToXML(xmlFile2, resAlloc2)
    ResourceAlloc.saveToXML(xmlFile3, resAlloc3)

    val loadedResourceAlloc1 = ResourceAlloc.loadFromXML(xmlFile1)
    val loadedResourceAlloc2 = ResourceAlloc.loadFromXML(xmlFile2)
    val loadedResourceAlloc3 = ResourceAlloc.loadFromXML(xmlFile3)

    println("resAlloc1 = " + loadedResourceAlloc1)
    println("resAlloc2 = " + loadedResourceAlloc2)
    println("resAlloc3 = " + loadedResourceAlloc3)

    resAlloc1 should equal (loadedResourceAlloc1)
    resAlloc2 should equal (loadedResourceAlloc2)
    resAlloc3 should equal (loadedResourceAlloc3)
  }
}


object ResourceAllocSpec {
  def prepareTestResources1: (Vector[Resource], HostSLA) = {
    // ResourceAlloc, used in test-cases:
    val res1 : Resource = Resource(	ResId(1), SMALL, ByteSize(4.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId](ResId(2)))
    val res2 : Resource = Resource(	ResId(2), MEDIUM, ByteSize(8.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId](ResId(1)))

    val reqHostSLA = new HostSLA(0.90f, Vector(IMG, COW),
      Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3)))

    return (Vector[Resource](res1, res2), reqHostSLA)
  }

  def prepareTestResources2: (Vector[Resource], HostSLA) = {
    // ResourceAlloc, used in test-cases:
    val res1 : Resource = Resource(	ResId(1), SMALL, ByteSize(8.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())
    val res2 : Resource = Resource(	ResId(2), MEDIUM, ByteSize(16.0, GiB),
      ByteSize(50.0, GiB), ByteSize(50.0, MiB),
      20.0f, Vector[ResId]())

    val reqHostSLA = new HostSLA(0.93f, Vector(IMG, COW, QCOW),
      Vector[(CPUUnit, Int)]((SMALL, 2), (MEDIUM, 3)))

    return (Vector[Resource](res1, res2), reqHostSLA)
  }
}