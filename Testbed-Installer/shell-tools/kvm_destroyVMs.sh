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
    vmDir=~/.vmbuilder/VMs/${vmName}
    if [ -d "${vmDir}" ]; then
        echo "Stopping VM if it is running via \"virsh destroy ${vmName}\"..."
        virsh -c qemu:///system destroy ${vmName}
        echo "Deleting VM via \"virsh undefine ${vmName}\" in KVM..."
        virsh -c qemu:///system undefine ${vmName}
        echo "Deleting VM-dir in ${vmDir}..."
        rm -r ~/.vmbuilder/VMs/${vmName}
        sleep 1
    else
        echo "No directory ${vmDir} found!"
    fi
	shift	# Check next set of parameters.
done


