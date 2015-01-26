#!/bin/bash

NOW=$(date +"%y-%m-%d_%H-%M-%S")
LOGDIR=~/logs/ovx-dev
LOGFILE=${LOGDIR}/${NOW}.log

mkdir -p ${LOGDIR}

#Redirect a copy of stdout and errout into a logfile files:
#(from: http://stackoverflow.com/questions/3173131/redirect-copy-of-stdout-to-log-file-from-within-bash-script-itself)
exec >  >(tee -a ${LOGFILE})
exec 2> >(tee -a ${LOGFILE} >&2)

echo "Cleaning up OpenVirteX-dev build and pull new version from git..."
cd ~/Development/openVirteX-dev/
rm -r target
git pull
cd ~

echo "Starting OpenVirteX-dev at ${NOW}..." 

sh ~/Development/openVirteX-dev/scripts/ovx.sh
