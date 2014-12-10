#!/bin/bash

NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGPATH=~/logs/floodlight
LOGFILE=${LOGPATH}/${NOW}.log

mkdir -p ${LOGPATH}


#Redirect a copy of stdout and errout into a logfile files:
#(from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-       itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)

echo "Starting floodlight-controller with config \"defaultctl_1\"..."
java -jar floodlight.jar -cf openvirtex-defaultctl_1.floodlight

# echo "Starting floodlight-controller-2..."
# java -jar floodlight.jar -cf openvirtex-defaultctl_1.floodlight > ./logs/ctrl2.log 2>&1 &

exit 0

