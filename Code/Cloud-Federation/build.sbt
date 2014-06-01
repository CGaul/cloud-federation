name := "Cloud-Federation"

version := "0.1"

scalaVersion := "2.11.1"


// Library Dependencies (MAIN)
// ===========================

//Resolver Link for Akka Libraries:
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

//Akka Libraries:
libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.3.3",
	"com.typesafe.akka" %% "akka-remote" % "2.3.3"
)



// Library Dependencies (TEST)
// ===========================

libraryDependencies +=
  "org.scalatest" %% "scalatest" % "2.1.6" % "test"