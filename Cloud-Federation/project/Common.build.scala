import sbt._

object Common {
  def prjVersion = "0.3-SNAPSHOT"
  def scalaVersion = "2.11.5"

  object Imports{
    /**
     * Using the Simple Logging Facade for Java (SLF4J) as a
     * generic logging abstraction and Logback as the
     * underlying logging framework.
     * 
     * @see The SLF4J Homepage:
     *      http://www.slf4j.org/index.html
     * @see The Logback Homepage:
     *      http://logback.qos.ch/index.html
     *      
     * @see How to use Java-style logging in Scala with SLF4J:
     *      http://alvinalexander.com/scala/how-to-use-java-style-logging-slf4j-scala
     * @see Specify Logback via SBT: 
     *      http://stackoverflow.com/questions/10511690/how-to-specify-logback-as-project-dependency
     * 
     * @see Use SLF4J as the default Akka-Logger:
     *      http://doc.akka.io/docs/akka/snapshot/java/logging.html
     */
    def loggerDependencies = Seq(
      "org.slf4j"         % "slf4j-api"       % "1.7.10",
      //"org.slf4j"         % "slf4j-simple"    % "1.7.10",
      "ch.qos.logback"    % "logback-classic" % "1.1.2")
    
    def akkaDependencies = Seq(
      "com.typesafe.akka" %% "akka-actor"     % "2.3.9",
      "com.typesafe.akka" %% "akka-remote"    % "2.3.9",
      "com.typesafe.akka" %% "akka-slf4j"     % "2.3.9",
      "com.typesafe.akka" %% "akka-testkit"   % "2.3.9")

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
