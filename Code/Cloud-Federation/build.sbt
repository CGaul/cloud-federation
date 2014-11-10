name := "cloudFederation"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

lazy val agentFramework = project.in(file("Agent-Framework"))
lazy val pubSubSystem = project.in(file("PubSub-System")).dependsOn(agentFramework)
lazy val cloudAgents = project.in(file("Cloud-Agents")).dependsOn(agentFramework, pubSubSystem)