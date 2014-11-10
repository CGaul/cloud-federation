package datatypes

/**
 * Created by costa on 11/10/14.
 */
object TupleSerializer {

/* XML-Serialization: */
/* ================== */

  def tupleToXML(t2: (Any, Any)): xml.Node =
    <t2>{t2._1},{t2._2}</t2>

  def tupleToXML(t3: (Any, Any, Any)): xml.Node =
    <t3>{t3._1},{t3._2},{t3._3}</t3>
  
  def tupleToXML(t4: (Any, Any, Any, Any)): xml.Node =
    <t4>{t4._1},{t4._2},{t4._3},{t4._4}</t4>
  
  def tupleToXML(t5: (Any, Any, Any, Any, Any)): xml.Node =
    <t5>{t5._1},{t5._2},{t5._3},{t5._4},{t5._5}</t5>
  
  def tupleToXML(t6: (Any, Any, Any, Any, Any, Any)): xml.Node =
    <t6>{t6._1},{t6._2},{t6._3},{t6._4},{t6._5},{t6._6}</t6>



/* XML-Deserialization: */
/* ==================== */

  def xmlToTuple2Vector(node: xml.NodeSeq): Vector[(String, String)] = {

    var tupleVector: Vector[(String, String)] = Vector()

    for (actNode <- node \ "t2") {
      tupleVector = tupleVector :+ xmlToTuple2(actNode)
    }

    return tupleVector
  }

  def xmlToTuple2(node: xml.NodeSeq): (String, String) = node match{
    case <t2>{val1},{val2}</t2> => (val1.toString(), val2.toString())
    case _                      => ("", "")
  }


  def xmlToTuple3(node: xml.NodeSeq): (String, String, String) = node match{
    case <t3>{val1},{val2},{val3}</t3>  => (val1.toString(), val2.toString(), val3.toString())
    case _                              => ("", "", "")
  }

  def xmlToTuple4(node: xml.NodeSeq): (String, String, String, String) = node match{
    case <t4>{val1},{val2},{val3},{val4}</t4> => (val1.toString(), val2.toString(), val3.toString(), val4.toString())
    case _                                    => ("", "", "", "")
  }

  def xmlToTuple5(node: xml.NodeSeq): (String, String, String, String, String) = node match{
    case <t5>{val1},{val2},{val3},{val4},{val5}</t5>  => (val1.toString(), val2.toString(), val3.toString(), val4.toString(), val5.toString())
    case _                                            => ("", "", "", "", "")
  }

  def xmlToTuple6(node: xml.NodeSeq): (String, String, String, String, String, String) = node match{
    case <t6>{val1},{val2},{val3},{val4},{val5},{val6}</t6> => (val1.toString(), val2.toString(), val3.toString(), val4.toString(), val5.toString(), val6.toString())
    case _                                                  => ("", "", "", "", "", "")
  }

}
