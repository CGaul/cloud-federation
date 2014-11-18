#!/bin/bash

#author:    Constantin.Gaul
#version:   0.10
#date:      July, 1th. 2014


######################################
##     Argument Variable setup:     ##
######################################

#The first two arguments are statically defined here:
hostAddr=$1
guestAddr=$2


## Program call-evaluation: ##
##############################

#Check if resourcePath is a directory and if vmName is not empty:
# if [ "${hostAddr}" !=  ] && [ "${vmName}" != "" ]; then
#     echo "User-Generation's resources are going to be injected into VM ${vmName}!"
#     echo "Resource Path: ${resourcePath}"
# else
#     echo "Relevant Parameters are not correctly set:"
#     if ! [ -d "${resourcePath}" ]; then
#         echo "Resource Path: ${resourcePath}. Not correct!"
#     fi
#     if ! [ -d "${vmName}" == "" ]; then
#         echo "VM-Name should not be empty!"
#     fi
#     exit 1
# fi

### Only runnable as root: ###
#----------------------------#
CURR_USER=$(id -un)
if ! [ "${CURR_USER}" = "root" ]; then
    echo
    echo "Application must be run as root! Current user is \"${CURR_USER}\". Aborting..."
    exit 1
fi


######################################
##      The Program Execution:      ##
######################################

echo "Changing iptables to NAT from ${hostAddr} to ${guestAddr} and back..."
iptables -t nat -A PREROUTING -d ${hostAddr} -j DNAT --to-destination ${guestAddr}
iptables -t nat -A POSTROUTING -s ${guestAddr} -j SNAT --to-source ${hostAddr}