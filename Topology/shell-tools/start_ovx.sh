#!/bin/bash

NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGDIR=~/logs/ovx
LOGFILE=${LOGDIR}/${NOW}.log

mkdir -p ${LOGDIR}

#Redirect a copy of stdout and errout into a logfile files:
#(from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)


echo "Starting OpenVirteX at ${NOW}..." 

sh ~/Development/openVirteX/scripts/ovx.sh
