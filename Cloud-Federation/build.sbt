import sbt.Keys._
import AssemblyKeys._


// "Agent-Framework"-Module:
// =========================

lazy val agentFramework: Project = project.in(file("Agent-Framework")).
  settings(
    name := "Agent-Framework",
    version := Common.prjVersion,
    scalaVersion := Common.scalaVersion,
    //Resolver Link for Akka Libraries:
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Logging (SLF4J):
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4",
    //Scala XML Support:
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  )



// "PubSub-System"-Module:
// =======================

lazy val pubSubSystem: Project = project.in(file("PubSub-System")).
  settings(
    name := "PubSub-System",
    version := Common.prjVersion,
    scalaVersion := Common.scalaVersion,
    //Resolver Link for Akka Libraries:
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Logging (SLF4J):
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4",
    //Scala XML Support:
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  ).dependsOn(agentFramework)//.dependsOn(cloudAgents % "test->compile")



// "Cloud-Agents"-Module:
// ======================

lazy val cloudAgents: Project = project.in(file("Cloud-Agents")).
  settings(
    name := "Cloud-Agents",
    version := Common.prjVersion,
    scalaVersion := Common.scalaVersion,
    //Resolver Link for Akka Libraries:
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Logging (SLF4J):
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4",
    //Scala XML Support:
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  ).dependsOn(agentFramework).dependsOn(pubSubSystem)
