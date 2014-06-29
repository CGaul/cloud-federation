#!/bin/bash

#author:    Constantin.Gaul
#version:   0.10
#date:      June, 25th. 2014


######################################
##      The Program Execution:      ##
######################################

### Command argument evaluation & exec: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
    virsh -c qemu:///system start $1
    sleep 10
	shift	# Check next set of parameters.
done


