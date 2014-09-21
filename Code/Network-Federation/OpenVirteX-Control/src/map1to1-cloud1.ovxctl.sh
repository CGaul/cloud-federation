#!/bin/bash

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
	echo "bash ./map1to1-cloud1.ovxctl.sh --ofc_ip 192.168.1.100 (defaults to port 10.000)"
	echo "bash ./map1to1-cloud1.ovxctl.sh --ofc_ip 192.168.1.100 --ofc_port 12345"
	exit 1
fi

# Per default create Network with OpenFlowPort 10.000 (Floodlight-Controller listens on that port)
# and handed over ofc_ip, as a required argument
# with own virtual address 10.0.1.0.
python ovxctl.py -n createNetwork tcp:${ofc_ip}:${ofc_port} 10.0.1.0 16

# For each physical Switch create a virtual switch:
# Home Gateway Switch:
echo "Creating Gateway-1 Switch..."
python ovxctl.py -n createSwitch 1 00:00:00:00:00:01:10:00

# Local (inner) Switches:
echo "Creating internal Switches 1-3..."
python ovxctl.py -n createSwitch 1 00:00:00:00:00:01:11:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:01:12:00
python ovxctl.py -n createSwitch 1 00:00:00:00:00:01:13:00

# Foreign Gateway Switch:
#echo "Creating foreign Gateway-2 Switch..."
#python ovxctl.py -n createSwitch 1 00:00:00:00:00:02:10:00



# Create ports for each Switch. 
# Physical and virtual Port mapping is the same here, too - except the Gateway, 
# where Port 1 is the Port that will be connected to the foreign GW:

# Home Gateway Switch (:11:00):
echo "Creating Ports (1,2,3) for GW-1 Switch..."
python ovxctl.py -n createPort 1 00:00:00:00:00:01:10:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:01:10:00 2

# Foreign Gateway Switch (:21:00):
#echo "Creating Ports (1) for foreign GW-2 Switch..."
#python ovxctl.py -n createPort 1 00:00:00:00:00:02:10:00 1


# Switch 2 (:12:00):
echo "Creating Ports (1,2,3,4) for SWITCH-1..."
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 3
python ovxctl.py -n createPort 1 00:00:00:00:00:01:11:00 4

# Switch 3 (:13:00):
echo "Creating Ports (1,2,3) for SWITCH-2..."
python ovxctl.py -n createPort 1 00:00:00:00:00:01:12:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:01:12:00 2
python ovxctl.py -n createPort 1 00:00:00:00:00:01:12:00 3

# Switch 4 (:14:00):
echo "Creating Ports (1,2) for SWITCH-3..."
python ovxctl.py -n createPort 1 00:00:00:00:00:01:13:00 1
python ovxctl.py -n createPort 1 00:00:00:00:00:01:13:00 2



# Connect Switches with each other:
# GW 1 <-> GW2:
#echo "Connecting Link: (GW-1, Port 2) <-> (GW-2, Port 1)..."
#python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 2 00:a4:23:05:00:00:00:05 1 spf 1

# GW 1 <-> Switch 2:
echo "Connecting Link: (GW-1, Port 2) <-> (SWITCH-2, Port 3)..."
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 2 00:a4:23:05:00:00:00:02 3 spf 1

# Switch 2 <-> Switch 3:
echo "Connecting Link: (SWITCH-2, Port 4) <-> (SWITCH-3, Port 2)..."
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:02 4 00:a4:23:05:00:00:00:03 2 spf 1

# Switch 3 <-> Switch 4:
echo "Connecting Link: (SWITCH-3, Port 3) <-> (SWITCH-4, Port 2)..."
python ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:03 3 00:a4:23:05:00:00:00:04 2 spf 1


# Connect Hosts with Switches:
# GW 1 ([Port 1: Host DHCP])
echo "Connecting Host: hdhcp <-> GW..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:01:01

# Switch 1 ([Port 1: Host 1], [Port 2: Host 2]):
echo "Connecting Host: h1_1_1 <-> SWITCH-1..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 1 00:00:00:00:01:11
echo "Connecting Host: h1_1_2 <-> SWITCH-1..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 2 00:00:00:00:01:12

# Switch 2 ([Port 1: Host 3]):
echo "Connecting Host: h1_2_1 <-> SWITCH-2..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 1 00:00:00:00:01:13

# Switch 3 ([Port 1: Host 4]):
echo "Connecting Host: h1_3_1 <-> SWITCH-3..."
python ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:04 1 00:00:00:00:01:14
