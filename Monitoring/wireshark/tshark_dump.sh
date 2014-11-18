#!/bin/bash


######################################
##         Global Constants:        ##
######################################

TIME_NOW=$(date +"%y-%m-%d_%H-%M-%S")
CURR_USER=$(id -un)



######################################
##       Function Parameters:       ##
######################################

IFACE=$1



######################################
##        Program Execution:        ##
######################################

LOGFILE_DIR=~/logs/tshark
LOGFILE_NAME="${TIME_NOW}.${IFACE}.$(hostname).pcap"

## Program call-evaluation: ##
##############################

if ! [ "${CURR_USER}" = "root" ]; then
	echo "Application must be run as root! Current user is \"${CURR_USER}\". Aborting script..."
	exit 1
fi


if [[ -x `which tshark` ]]; then
	
	if [[ ! -f ${LOGFILE_DIR} ]]; then
		echo "Creating ${LOGFILE_DIR} directory..."
		su -c "mkdir -p ${LOGFILE_DIR}" ${SUDO_USER}
	fi
	echo "Running tshark dump on Interface ${IFACE} as root."
	echo "Logfile: ${LOGFILE_DIR}/${LOGFILE_NAME}"
	su -c "touch ${LOGFILE_DIR}/${LOGFILE_NAME}" ${SUDO_USER}
	chmod o+rw ${LOGFILE_DIR}/${LOGFILE_NAME}
	tshark -i ${IFACE} -w ${LOGFILE_DIR}/${LOGFILE_NAME}
	exit 0
else
	echo "tshark not found!"
	exit 2
fi
