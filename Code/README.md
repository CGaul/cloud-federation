## Code Repo of "Agent-Based Cloud-Federation"

### Setting up the Akka-Scala Development Environment in IntelliJ
*Preparations:*
* Install SBT
* [Download SBT from the Scala-Website](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)
* Install the SBT-Idea Plugin
** [SBT-Idea GitHub Website](https://github.com/mpeltonen/sbt-idea)
** As mentioned there, add the following lines to ~/.sbt/0.13/plugins/build.sbt
*** `addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")`
** Also, install the Scala-Plugin from IntelliJ's Plugin Manager

*Code-Environment*
* Clone this Repository
* Under "Code/Cloud-Federation", open a Terminal
** Type `sbt`
** Wait until all sbt / sbt-idea updates are Downloaded successfully
** Type `gen-idea`
** Wait until all dependencies have been downloaded
** `exit` the sbt-command-prompt and close the terminal
* Start IntelliJ
** Open the currently build Project "Cloud-Federation"
** Wait for the Indexing and Downloading to complete
** Press "F4" when selecting the Project "Cloud-Federation" and see, if there are any errors
*** In the Platform Settings -> Global Libraries, make sure that JavaDocs are available for the scala-library
**** If not, add the JavaDoc Path: http://www.scala-lang.org/api/current/
** Run /src/main/scala/AgentFederation