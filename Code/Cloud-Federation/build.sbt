name := "Cloud-Federation"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.2"


// Library Dependencies (MAIN)
// ===========================

//Resolver Link for Akka Libraries:
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

//Akka Libraries:
libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
	"com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT",
  	"com.typesafe.akka" %% "akka-slf4j" % "2.4-SNAPSHOT",
	"com.typesafe.akka" %% "akka-testkit" % "2.4-SNAPSHOT"
)

//Logging (SLF4J):
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"



// Library Dependencies (TEST)
// ===========================

libraryDependencies +=
  "org.scalatest" %% "scalatest" % "2.1.6" % "test"