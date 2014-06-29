#!/bin/bash

#author:    Constantin.Gaul
#version:   0.10
#date:      June, 25th. 2014


######################################
##      The Program Execution:      ##
######################################

### Only runnable as root: ###
#----------------------------#
CURR_USER=$(id -un)
if ! [ "${CURR_USER}" = "root" ]; then
    echo
	echo "Application must be run as root! Current user is \"${CURR_USER}\". Aborting..."
	exit 1
fi

### Script Execution ###
#----------------------#

while [ $# -gt 0 ]; do 	# Until you run out of parameters
    vmName=$1
    if [ -d "~/.vmbuilder/VMs/${vmName}" ]; then
        virsh -c qemu:///system destroy ${vmName}
        rm -r ~/.vmbuilder/VMs/${vmName}
        sleep 10
    else
        echo "No directory found for ${vmName}!"
    fi
	shift	# Check next set of parameters.
done


