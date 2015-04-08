## Code Repo of "Agent-Based Cloud-Federation"

### SBT Environment for Cloud-Federation Project

#### Install SBT & Oracle Java 8
* [Installation Tutorial on the SBT Website](www.scala-sbt.org/release/tutorial/index.html)
* Under Ubuntu, install the DEB package via apt:
    * `echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list`
    * `sudo apt-get update`
    * `sudo apt-get install sbt`
* Install the SBT-Idea Plugin (only required if project integration in IntelliJ needed):
    * [SBT-Idea GitHub Website](https://github.com/mpeltonen/sbt-idea)
    * As mentioned there, add the following lines to ~/.sbt/0.13/plugins/build.sbt
        * `addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")`
    * Also, install the Scala-Plugin from IntelliJ's Plugin Manager
* Install Oracle Java 8 (via ppa on Ubuntu):
    * `sudo add-apt-repository ppa:webupd8team/java`
    * `sudo apt-get update`
    * `sudo apt-get install oracle-java8-installer`


### Code-Environment under IntelliJ
* Under "Code/Cloud-Federation", open a Terminal
    * Type `sbt`
    * Wait until all sbt / sbt-idea updates are Downloaded successfully
    * Type `gen-idea`
    * Wait until all dependencies have been downloaded
    * `exit` the sbt-command-prompt and close the terminal
* Start IntelliJ
    * Open the currently build Project "Cloud-Federation"
    * Wait for the Indexing and Downloading to complete
    * Press "F4" when selecting the Project "Cloud-Federation" and see, if there are any errors
        * In the Platform Settings -> Global Libraries, make sure that JavaDocs are available for the scala-library
            * If not, add the JavaDoc Path: http://www.scala-lang.org/api/current/
    * Run `Code/Cloud-Federation/Cloud-Agents/src/main/scala/CloudAgentManagement` for the Agent Cloud-Stack
    * Run `Code/Cloud-Federation/Federation-Broker/src/main/scala/FedBrokerManagement` for the Federation Broker
    

### Fat .jar Assembly via SBT for all associated Projects
* Under "Code/Cloud-Federation", open a Terminal
* run `sbt assembly`
* Wait until all compilations and the testing phase is complete (takes up to a minute)
* In `Code/Cloud-Federation/Cloud-Agents/target/scala-2.11/` find a `cloud-agents_0.4-FINAL.fat.jar`
* In `Code/Cloud-Federation/Federation-Broker/target/scala-211` find a `fed-broker_0.4-FINAL.fat.jar`
* Run both jars with a simple `java -jar` command.
