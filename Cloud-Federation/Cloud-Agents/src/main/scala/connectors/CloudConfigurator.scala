package connectors

import java.io.{IOException, File}
import java.net.InetAddress

import datatypes._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

/**
 * @author Constantin Gaul, created on 1/31/15.
 */
class CloudConfigurator(_cloudConfDir: File,
                        _hostDirName: String = "hosts", 
                        _tenantsDirName: String = "tenants",
                        _gatewayFileName: String = "CloudGateway.xml",
                        _cloudSLAFileName: String = "CloudSLA.xml",
                        _certFileName: String = "cloud1.key",
                        _cloudOvx: OvxInstance = OvxInstance(InetAddress.getLocalHost, 8080, 6633, false)) {

/* Values: */
/* ======= */

  val log = LoggerFactory.getLogger(classOf[CloudConfigurator])


/* Getters: */
/* ======== */
  def confDir       = _cloudConfDir
  def certFile 			= _certFile
  def cloudOvx      = _cloudOvx
  def cloudHosts 		= readCloudHostsfromXML
  def cloudTenants  = readCloudTenantsfromXML
  def cloudSLA 			= readCloudSLAfromXML
  def cloudGateway  = readGatewayfromXML


  //TODO: build security interfaces for a Certificate-Store
  private val _certFile: File			  = new File(_cloudConfDir.getAbsolutePath +File.separator+ _certFileName)


/* Private Methods: */
/* ================ */
  
  private def readCloudHostsfromXML: Vector[Host] = {
    // Define the Cloud-Hosts from all files in the cloudConfDir/hosts/ directory
    var cloudHosts: Vector[Host] = Vector()
    val hostFiles: File = new File(_cloudConfDir.getAbsolutePath +File.separator+ _hostDirName)
    if(hostFiles.listFiles() == null)
      log.error("Hosts need at least one defined .xml file in {} !", 
                _cloudConfDir.getAbsolutePath +File.separator+ _hostDirName)
    for (actHostFile <- hostFiles.listFiles) {
      cloudHosts = cloudHosts :+ Host.loadFromXML(actHostFile)
    }
    return cloudHosts
  }
  
  private def readCloudTenantsfromXML: Vector[Tenant] = {
    // Define the Cloud-Tenants from all files in the cloudConfDir/hosts/ directory
    var cloudTenants: Vector[Tenant] = Vector()
    val tenantFiles: File = new File(_cloudConfDir.getAbsolutePath +File.separator+ _tenantsDirName)
    if(tenantFiles.listFiles() == null)
      log.error("Tenants need at least one defined .xml file in {}/tenants/ !", 
                _cloudConfDir.getAbsolutePath +File.separator+ _tenantsDirName)
    for (actTenantFile <- tenantFiles.listFiles) {
      cloudTenants = cloudTenants :+ Tenant.loadFromXML(actTenantFile)
    }
    return cloudTenants
  }
  
  private def readCloudSLAfromXML: CloudSLA = {
    // Define the Cloud-SLA from the CloudSLA.xml file in the cloudConfDir/ directory
    return CloudSLA.loadFromXML(new File(_cloudConfDir.getAbsolutePath +File.separator+ _cloudSLAFileName))
  }

  private def readGatewayfromXML: OFSwitch = {
    // Define the Cloud's Gateway OF-Switch from the CloudGateway.xml file in the cloudConfDir/ directory
    return OFSwitch.loadFromXML(new File(_cloudConfDir.getAbsolutePath +File.separator+ _gatewayFileName))
  }
}

/** 
 * Companion Object for CloudConfigurator
 */
object CloudConfigurator {
  def apply(cloudConfDir: File) = new CloudConfigurator(cloudConfDir)

  def copyConfig(cloudConfDir: File, copiedCloudConfDir: File) = {
    try {
      FileUtils.copyDirectory(cloudConfDir, copiedCloudConfDir)
    } catch{
      case e: IOException => e.printStackTrace()
    }
  }
}
