assemblyJarName in assembly := "cloud-agents_"+Common.prjVersion+".fat.jar"

mainClass in assembly := Some("CloudAgentManagement")

assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) =>
  {
    case PathList("cloudconf", "cloud1.key") => MergeStrategy.discard
    case "localApplication.conf" => MergeStrategy.discard
    case "remoteApplication.conf" => MergeStrategy.discard
    case "cloudnet-1VM_application.conf" => MergeStrategy.discard
    case "cloudnet-2VM_application.conf" => MergeStrategy.discard
    case "federatorVM_application.conf" => MergeStrategy.discard
    case x => old(x)
  }
}

test in assembly := {}
