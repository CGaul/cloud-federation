#!/bin/bash


##################
## Description: ##
##################

# Prepares a federateable Network from OVX-2 on OFC-1. 
# Awaits a call of OVX-1 on OFC-1 from the other cloudnet-1 via federated-net1-1.ovxctl.sh


##################
## Parameters:  ##
##################

ofc_ip=""
ofc_port=10000

### Command argument evaluation: ###
while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Required Parameters:
        --ofc_ip)         ofc_ip=$2 ;;
        --ofc_port)       ofc_port=$2 ;;
	#Printing the Help:
		#-[hH]|-help|--help)	printHelp ;;
	esac
	shift	# Check next set of parameters.
done

if [[ ${ofc_ip} == "" ]]; then
	echo "--ofc_ip is a required parameter!"
	echo "possible calls:"
	echo "bash ./federated-net1.ovxctl.sh --ofc_ip 192.168.1.100 (defaults to port 10.000)"
	echo "bash ./federated-net1.ovxctl.sh --ofc_ip 192.168.1.100 --ofc_port 12345"
	exit 1
fi


##################
##  Execution:  ##
## (via ovxctl) ##
##################

# Per default create Network with OpenFlowPort 10.000 (Floodlight-Controller listens on that port)
# and handed over ofc_ip, as a required argument
# with own virtual address 10.0.5.0.
python2 ovxctl.py -n createNetwork tcp:${ofc_ip}:${ofc_port} 10.10.0.0 16

#echo "Establishing GW-1 Switch..."
#python2 ovxctl.py -n createSwitch 1 00:00:00:00:00:01:10:00 #:01

echo "Establishing GW-2 Switch..."
python2 ovxctl.py -n createSwitch 1 00:00:00:00:00:02:10:00 #:01

echo "Establishing internal Switches 2.1 ..."
#python2 ovxctl.py -n createSwitch 1 00:00:00:00:00:01:11:00 #:03
python2 ovxctl.py -n createSwitch 1 00:00:00:00:00:02:11:00 #:02


# Create ports for each Switch. 
# Physical and virtual Port mapping is the same here, too - except the Gateway, 
# where Port 1 is the Port that will be connected to the foreign GW:

#echo "Establishing Ports (1,2) for GW-1 Switch..."
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:01:10:00 2
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:01:10:00 3

echo "Establishing Ports (1,2) for GW-2 Switch..."
python2 ovxctl.py -n createPort 1 00:00:00:00:00:02:10:00 2
python2 ovxctl.py -n createPort 1 00:00:00:00:00:02:10:00 3

#echo "Creating Ports (1,2,3) for SWITCH-1.1..."
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 1
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 2
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 3
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 4

echo "Creating Ports (1,2,3) for SWITCH-2.1..."
python2 ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 1
python2 ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 2
python2 ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 3
#python2 ovxctl.py -n createPort 1 00:00:00:00:00:02:11:00 4


# Connect Switches with each other:
# GW 1 <-> GW2:
#echo "Connecting Link: (GW-1, Port 1) <-> (GW-2, Port 1)..."
#python2 ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 2 00:a4:23:05:00:00:00:02 2 spf 1

# GW 1 <-> Switch 1.1:
#echo "Connecting Link: (GW-1, Port 2) <-> (SWITCH-1.1, Port 3)..."
#python2 ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:03 3 spf 1

# GW 2 <-> Switch 2.1:
echo "Connecting Link: (GW-2, Port 2) <-> (SWITCH-2.1, Port 3)..."
python2 ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 1 00:a4:23:05:00:00:00:02 3 spf 1



# Connect Hosts with Switches:
# GW 1 ([Port 1: Host DHCP])
#echo "Connecting Host: hdhcp <-> GW..."
#python2 ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:01:01

# Switch 1.1 ([Port 1: Host 1], [Port 2: Host 2]):
#echo "Connecting Host: h1_1_1 <-> SWITCH-1.1..."
#python2 ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 1 00:00:00:00:01:11
#echo "Connecting Host: h1_1_2 <-> SWITCH-1.1..."
#python2 ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 2 00:00:00:00:01:12

# Switch 2.1 ([Port 1: Host 1], [Port 2: Host 2]):
echo "Connecting Host: h2_1_1 <-> SWITCH-2.1..."
python2 ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 1 00:00:00:00:02:11
#echo "Connecting Host: h2_1_2 <-> SWITCH-2.1..."
#python2 ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:04 2 00:00:00:00:02:12
