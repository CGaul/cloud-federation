#!/bin/bash

JARNAME="fed-broker_0.3-SNAPSHOT.fat.jar"

NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGFILE=logs/fedbroker/${NOW}.log


#Redirect a copy of stdout and errout into a logfile files:
#(from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)

echo "Starting Federation-Broker at time ${NOW}..." 
wget --auth-no-challenge --user='citcloud' --password='fneA48pcEno9WiW1nYipdoPQiY367ZwbIc4' -N http://lab.is-by.us:8080/job/Cloud-Federation/lastSuccessfulBuild/artifact/Cloud-Federation/Federation-Broker/target/scala-2.11/${JARNAME} -P ~/Development/


java -jar ~/Development/${JARNAME} --appconf ~/Development/federator_application.conf --fedconf ~/Development/federatorconf
