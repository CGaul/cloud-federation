import AssemblyKeys._

assemblySettings

jarName in assembly := "pubsub-system_0.2-SNAPSHOT.fat.jar"

mainClass in assembly := Some("PubSubManagement")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
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


name := "pubSubSystem"

version := "0.2-SNAPSHOT"

scalaVersion := "2.11.2"


// Library Dependencies (MAIN)
// ===========================

//Resolver Link for Akka Libraries:
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

//Akka Libraries:
libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.3.6",
	"com.typesafe.akka" %% "akka-remote" % "2.3.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
	"com.typesafe.akka" %% "akka-testkit" % "2.3.6"
)

//Logging (SLF4J):
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

//Scala XML Support:
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"



// Library Dependencies (TEST)
// ===========================

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "2.2.1" % "test",
	"junit" % "junit" % "4.11" % "test",
	"com.novocode" % "junit-interface" % "0.11" % "test",
	"org.mockito" % "mockito-core" % "1.9.5" % "test"
)
