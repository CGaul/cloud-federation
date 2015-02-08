import sbt.Keys._

// "Agent-Framework"-Module:
// =========================

lazy val agentFramework: Project = project.in(file("Agent-Framework")).
  settings(
    name := "Agent-Framework",
    version := Common.prjVersion,
    scalaVersion := Common.scalaVersion,
    //Resolver Link for Akka Libraries:
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Logging (SLF4J + Logback):
    libraryDependencies ++= Common.Imports.loggerDependencies,
    //Scala XML Support:
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //JSON Support via PlayJson:
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.6",
    //Apache HTTP-Client for HTTP POST to OVX-Embedder:
    libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.6",
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
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Logging (SLF4J + Logback):
    libraryDependencies ++= Common.Imports.loggerDependencies,
    //Scala XML Support:
    //libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //JSON Support via PlayJson:
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.6",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  ).dependsOn(agentFramework)



// "Cloud-Agents"-Module:
// ======================

lazy val cloudAgents: Project = project.in(file("Cloud-Agents")).
  settings(
    name := "Cloud-Agents",
    version := Common.prjVersion,
    scalaVersion := Common.scalaVersion,
    //Resolver Link for Akka Libraries:
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "spray repo" at "http://repo.spray.io",
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Spray Library:
    libraryDependencies += "io.spray" %% "spray-can" % "1.3.2",
    //Logging (SLF4J + Logback):
    libraryDependencies ++= Common.Imports.loggerDependencies,
    //Scala XML Support:
    //libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //JSON Support via PlayJson:
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.6",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  ).dependsOn(agentFramework).dependsOn(pubSubSystem)



// "Testing"-Module:
// =================

lazy val agentTesting: Project = project.in(file("Agent-Tests")).
  settings(
    name := "Agent-Tests",
    version := Common.prjVersion,
    scalaVersion := Common.scalaVersion,
    //Resolver Link for Akka Libraries:
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Logging (SLF4J + Logback):
    libraryDependencies ++= Common.Imports.loggerDependencies,
    //Scala XML Support:
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  ).dependsOn(agentFramework % "test->compile")
  .dependsOn(pubSubSystem % "test->compile")
  .dependsOn(cloudAgents % "test->compile")
