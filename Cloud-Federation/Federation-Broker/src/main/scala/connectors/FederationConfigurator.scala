package connectors

import java.io.File

import datatypes._
import org.slf4j.LoggerFactory

/**
 * @author Constantin Gaul, created on 1/31/15.
 */
class FederationConfigurator(federationConfDir: File,
                        ovxFederatorsFileName: String = "ovx_federators.xml") {

/* Values: */
/* ======= */

  val log = LoggerFactory.getLogger(classOf[FederationConfigurator])


/* Getters: */
/* ======== */
  
  def ovxInstances = readOvxInstancesfromXML


/* Private Methods: */
/* ================ */

  private def readOvxInstancesfromXML: List[OvxInstance] = {
    // Define the Cloud-SLA from the CloudSLA.xml file in the cloudConfDir/ directory
    return OvxInstance.loadAllFromXML(new File(federationConfDir.getAbsolutePath +File.separator+ ovxFederatorsFileName))
  }
}

/** 
 * Companion Object for CloudConfigurator
 */
object FederationConfigurator {
  def apply(federationConfDir: File) = new FederationConfigurator(federationConfDir)

}
