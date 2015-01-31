package connectors

import java.io.File
import java.net.InetAddress

import datatypes.{CloudSLA, Host, Tenant}
import org.slf4j.LoggerFactory

/**
 * @author Constantin Gaul, created on 1/31/15.
 */
class CloudConfigurator(cloudConfDir: File, 
                        hostDirName: String = "hosts", 
                        tenantsDirName: String = "tenants", 
                        cloudSLAFileName: String = "CloudSLA.xml") {

/* Values: */
/* ======= */

  val log = LoggerFactory.getLogger(classOf[OVXConnector])


/* Getters: */
/* ======== */

  def certFile 			= _certFile
  def ovxIp 				= _ovxIP
  def ovxApiPort		= _ovxApiPort
  def cloudHosts 		= readCloudHostsfromXML
  def cloudTenants  = readCloudTenantsfromXML
  def cloudSLA 			= readCloudSLAfromXML


  //TODO: build security interfaces for a Certificate-Store
  private val _certFile: File			= new File(cloudConfDir.getAbsolutePath +"/cloud1.key")
  private val _ovxIP: InetAddress 	= InetAddress.getLocalHost
  private val _ovxApiPort: Int			= 8080


/* Private Methods: */
/* ================ */
  
  private def readCloudHostsfromXML: Vector[Host] = {
    // Define the Cloud-Hosts from all files in the cloudConfDir/hosts/ directory
    var cloudHosts: Vector[Host] = Vector()
    val hostFiles: File = new File(cloudConfDir.getAbsolutePath +"/hosts")
    if(hostFiles.listFiles() == null)
      log.error("Hosts need at least one defined .xml file in {}/hosts/ !", cloudConfDir.getAbsolutePath)
    for (actHostFile <- hostFiles.listFiles) {
      cloudHosts = cloudHosts :+ Host.loadFromXML(actHostFile)
    }
    return cloudHosts
  }
  
  private def readCloudTenantsfromXML: Vector[Tenant] = {
    // Define the Cloud-Tenants from all files in the cloudConfDir/hosts/ directory
    var cloudTenants: Vector[Tenant] = Vector()
    val tenantFiles: File = new File(cloudConfDir.getAbsolutePath +"/tenants")
    if(tenantFiles.listFiles() == null)
      log.error("Tenants need at least one defined .xml file in {}/tenants/ !", cloudConfDir.getAbsolutePath)
    for (actTenantFile <- tenantFiles.listFiles) {
      cloudTenants = cloudTenants :+ Tenant.loadFromXML(actTenantFile)
    }
    return cloudTenants
  }
  
  private def readCloudSLAfromXML: CloudSLA = {
    // Define the Cloud-SLA from the CloudSLA.xml file in the cloudConfDir/ directory
    return CloudSLA.loadFromXML(new File(cloudConfDir.getAbsolutePath +"/CloudSLA.xml"))
  }
}

/** 
 * Companion Object for CloudConfigurator
 */
object CloudConfigurator {
  def apply(cloudConfDir: File) = new CloudConfigurator(cloudConfDir)

}
