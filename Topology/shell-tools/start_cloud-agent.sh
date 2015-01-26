#!/bin/bash

JARNAME="cloud-agents_0.2-SNAPSHOT.fat.jar"

NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGPATH=~/logs/cloud-agent
LOGFILE=${LOGPATH}/${NOW}.log

# Test if oracle java-8 is installed:
if ! [[ -x /usr/lib/jvm/java-8-oracle/jre/bin/java ]]; then
    echo "java-8-oracle needs to be installed for this program to run! \n"
    echo "Under a debian system, use ppa:"
    echo "sudo add-apt-repository ppa:webupd8team/java"
    echo "sudo apt-get update"
    echo "sudo apt-get install oracle-java8-installer"
fi

JAVA8_EXEC=/usr/lib/jvm/java-8-oracle/jre/bin/java

# Establish the logpath for the program:
mkdir -p ${LOGPATH}


# Redirect a copy of stdout and errout into a logfile files:
# (from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)


echo "Starting CCFM Cloud-Agent at ${NOW}..." 
wget --auth-no-challenge --user='' --password='' -N http://lab.is-by.us:8080/job/Cloud-Federation/lastSuccessfulBuild/artifact/Cloud-Federation/Cloud-Agents/target/scala-2.11/${JARNAME} -P ~/Development/


$JAVA8_EXEC -jar ~/Development/${JARNAME} --appconf ~/Development/cloudnet-1_application.conf --cloudconf ~/Development/cloudconf

exit 0
