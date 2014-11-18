import sbt._
import Keys._

object Common {
  def prjVersion = "0.2-SNAPSHOT"
  def scalaVersion = "2.11.4"

  object Imports{
    def akkaDependencies = Seq(
      "com.typesafe.akka" %% "akka-actor"     % "2.3.6",
      "com.typesafe.akka" %% "akka-remote"    % "2.3.6",
      "com.typesafe.akka" %% "akka-slf4j"     % "2.3.6",
      "com.typesafe.akka" %% "akka-testkit"   % "2.3.6")

    def testDependencies = Seq(
      "org.scalatest"     %% "scalatest"      % "2.2.1" % "test",
      "junit"             % "junit"           % "4.11"  % "test",
      "com.novocode"      % "junit-interface" % "0.11"  % "test",
      "org.mockito"       % "mockito-core"    % "1.9.5" % "test")
  }

  object Resolvers {
    def akkaTypeSafeRepo = "Typesafe Repository" at
                           "http://repo.typesafe.com/typesafe/releases/"
  }
}