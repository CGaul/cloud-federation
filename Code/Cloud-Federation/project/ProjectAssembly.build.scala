import sbt._
import sbt.Keys._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._


//
//object ProjectAssembly{
//  val setSBTAssembly = taskKey[Unit]("An assembly method, applied to each Module, " +
//    "setting the SBT-Assembly build-params")
//
//  def setSBTAssembly(moduleName: String, mainClass: String) = {
//    assemblySettings
//
//    jarName in assembly := moduleName + "_" + Common.prjVersion + ".fat.jar"
//
//    mainClass in assembly := Some(mainClass)
//
//    mergeStrategy in assembly <<= (mergeStrategy in assembly) {
//      (old) => {
//        case PathList("cloudconf", "cloud1.key") => MergeStrategy.discard
//        case "localApplication.conf" => MergeStrategy.discard
//        case "remoteApplication.conf" => MergeStrategy.discard
//        case "cloudnet-1VM_application.conf" => MergeStrategy.discard
//        case "cloudnet-2VM_application.conf" => MergeStrategy.discard
//        case "federatorVM_application.conf" => MergeStrategy.discard
//        case x => old(x)
//      }
//    }
//  }
//}
