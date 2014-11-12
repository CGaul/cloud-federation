package datatypes

/**
 * Created by costa on 11/10/14.
 */
object TupleSerializer {

/* XML-Serialization: */
/* ================== */

  def tupleToXML(t2: (Any, Any)): xml.Node =
    <t2>{t2._1.toString},{t2._2.toString}</t2>

  def tupleToXML(t3: (Any, Any, Any)): xml.Node =
    <t3>{t3._1.toString},{t3._2.toString},{t3._3.toString}</t3>
  
  def tupleToXML(t4: (Any, Any, Any, Any)): xml.Node =
    <t4>{t4._1.toString},{t4._2.toString},{t4._3.toString},{t4._4.toString}</t4>
  
  def tupleToXML(t5: (Any, Any, Any, Any, Any)): xml.Node =
    <t5>{t5._1.toString},{t5._2.toString},{t5._3.toString},{t5._4.toString},{t5._5}</t5>
  
  def tupleToXML(t6: (Any, Any, Any, Any, Any, Any)): xml.Node =
    <t6>{t6._1.toString},{t6._2.toString},{t6._3.toString},{t6._4.toString},{t6._5},{t6._6}</t6>



/* XML-Deserialization: */
/* ==================== */

  def xmlToTuple2Vector(node: xml.NodeSeq): Vector[(String, String)] = {

    var tupleVector: Vector[(String, String)] = Vector()

    for (actNode <- node \ "t2") {
      actNode match{
        case <t2>{val1},{val2}</t2> => tupleVector = tupleVector :+ (val1.toString(), val2.toString())
        case _                      => tupleVector = tupleVector :+ ("", "")
      }
    }

    return tupleVector
  }

  def xmlToTuple3Vector(node: xml.NodeSeq): Vector[Option[(String, String, String)]] = {

    var tupleVector: Vector[Option[(String, String, String)]] = Vector()

    for (actNode <- node \ "t3") {
      actNode match{
        case <t>{val1},{val2},{val3}</t>    => tupleVector = tupleVector :+ Option((val1.toString(), val2.toString(), val3.toString()))
        case <t3>{val1},{val2},{val3}</t3>  => tupleVector = tupleVector :+ Option((val1.toString(), val2.toString(), val3.toString()))
        case _                              => tupleVector = tupleVector :+ {
                                                val tupleExtract = tryDirectExtraction(actNode, 3)
                                                if(tupleExtract.isDefined) {Option(tupleExtract.get(0), tupleExtract.get(1), tupleExtract.get(2))} else None
                                               }
      }
    }

    return tupleVector
  }

  def xmlToTuple4Vector(node: xml.NodeSeq): Vector[(String, String, String, String)] = {

    var tupleVector: Vector[(String, String, String, String)] = Vector()

    for (actNode <- node \ "t4") {
      actNode match{
        case <t4>{val1},{val2},{val3},{val4}</t4> => tupleVector = tupleVector :+ (val1.toString(), val2.toString(), val3.toString(), val4.toString())
        case _                                    => tupleVector = tupleVector :+ ("", "", "", "")
      }
    }

    return tupleVector
  }

  def xmlToTuple5Vector(node: xml.NodeSeq): Vector[(String, String, String, String, String)] = {

    var tupleVector: Vector[(String, String, String, String, String)] = Vector()

    for (actNode <- node \ "t5") {
      actNode match{
        case <t5>{val1},{val2},{val3},{val4},{val5}</t5>  => tupleVector = tupleVector :+ (val1.toString(), val2.toString(), val3.toString(), val4.toString(), val5.toString())
        case _                                            => tupleVector = tupleVector :+ ("", "", "", "", "")
      }
    }

    return tupleVector
  }

  def xmlToTuple6Vector(node: xml.NodeSeq): Vector[(String, String, String, String, String, String)] = {

    var tupleVector: Vector[(String, String, String, String, String, String)] = Vector()

    for (actNode <- node \ "t6") {
      actNode match{
        case <t6>{val1},{val2},{val3},{val4},{val5},{val6}</t6> => tupleVector = tupleVector :+ (val1.toString(), val2.toString(), val3.toString(), val4.toString(), val5.toString(), val6.toString())
        case _                                                  => tupleVector = tupleVector :+ ("", "", "", "", "", "")
      }
    }

    return tupleVector
  }



/*  XML-Deserialization:  */
/* (Convenience Methods)  */
/* =====================  */

  def xmlToTuple2(node: xml.NodeSeq): (String, String) = {
    val xmlTupleVector = xmlToTuple2Vector(node)
    return xmlTupleVector(0)
  }

  def xmlToTuple3(node: xml.NodeSeq): Option[(String, String, String)] ={
    val xmlTupleVector = xmlToTuple3Vector(node)
    return xmlTupleVector(0)
  }

  def xmlToTuple4(node: xml.NodeSeq): (String, String, String, String) ={
    val xmlTupleVector = xmlToTuple4Vector(node)
    return xmlTupleVector(0)
  }

  def xmlToTuple5(node: xml.NodeSeq): (String, String, String, String, String) ={
    val xmlTupleVector = xmlToTuple5Vector(node)
    return xmlTupleVector(0)
  }

  def xmlToTuple6(node: xml.NodeSeq): (String, String, String, String, String, String) ={
    val xmlTupleVector = xmlToTuple6Vector(node)
    return xmlTupleVector(0)
  }



/* Private Methods: */
/* ================ */

  private def tryDirectExtraction(node: xml.NodeSeq, tupleLen: Int): Option[Vector[String]] ={
    if(node.text != ""){
      val tupleVals = node.text.trim.split(",").toVector
      if(tupleVals.length == tupleLen)
        return Option(tupleVals)
      else return None
    }
    else return None
  }
}
