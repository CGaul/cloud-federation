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

lazy val fedBroker: Project = project.in(file("Federation-Broker")).
  settings(
    name := "Federation-Broker",
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
    // Resolvers:
    // ----------
    resolvers += Common.Resolvers.akkaTypeSafeRepo,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "spray repo" at "http://repo.spray.io",
    // Compile Dependencies:
    // ---------------------
    //Akka Libraries:
    libraryDependencies ++= Common.Imports.akkaDependencies,
    //Spray Library:
    libraryDependencies += "io.spray" %% "spray-can" % "1.3.2",
    //JSON Support via PlayJson:
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.6",
    //Apache Commons IO for comfortable file operations (in CloudConfig)
    libraryDependencies += "commons-io" % "commons-io" % "2.4",
    // Test & Logging Dependencies:
    // ----------------------------
    //Logging (SLF4J + Logback):
    libraryDependencies ++= Common.Imports.loggerDependencies,
    //Scala XML Support:
    //libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    //JSON Support via PlayJson:
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.6",
    //Testing Scope:
    libraryDependencies ++= Common.Imports.testDependencies
  ).dependsOn(agentFramework).dependsOn(fedBroker)



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
  .dependsOn(fedBroker % "test->compile")
  .dependsOn(cloudAgents % "test->compile")
