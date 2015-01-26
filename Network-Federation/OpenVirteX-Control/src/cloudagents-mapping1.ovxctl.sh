#!/bin/bash

#################################################
## This Script defines a one-to-one mapping    ##
## from the cloudnet-1 mininet topo to the     ##
## tenant t_id OFC controller via an OVX vnet. ##
#################################################

#author: Constantin.A.Gaul@campus.tu-berlin.de
#date: Jan, 8th. 2015


## Global Command Line Variables ##
###################################

# Command line arguments, specifying the OpenFlow-Controller, managing this vNet:
ofc_ip=""
ofc_port=10000

# The internal OVX tenant ID. 
#Pre-set to 1, but only possible if this is the only (or first) vNet to be created:
t_id=1


## Function Definitions: ##
###########################

printHelp(){
	echo "--ofc_ip is a required parameter!"
        echo "possible calls:"
        echo "minimal arguments:"
        echo "bash ./cloudagents-mapping1.ovxctl.sh --ofc_ip 192.168.1.100 (defaults to port 10.000)"
        echo "all arguments:"
        echo "bash ./cloudagents-mapping1.ovxctl.sh --ofc_ip 192.168.1.100 --ofc_port 12345 --tenant 2"
}

## Command argument evaluation: ##
##################################

while [ $# -gt 0 ]; do 	# Until you run out of parameters
	case "$1" in
	#Required Parameters:
        --ofc_ip)         ofc_ip=$2 ;;
    #Optional Parameters:
        --ofc_port)       ofc_port=$2 ;;
        --tenant)         t_id=$2 ;;
	#Printing the Help:
	-[hH]|-help|--help)	printHelp ;;
	esac
	shift	# Check next set of parameters.
done

if [[ ${ofc_ip} == "" ]]; then
	printHelp
	exit 1
fi



## Script Execution: ##
#######################

echo "Resource-Allocation 1"
# 20:19:39.211 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Mapping hosts List(0:00:00:00:01:11) to virtual tenant network.
# 20:19:39.245 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Network for Tenant 1 at OFC: /192.168.1.42:10000. Is Booted: None
echo "Creating Network for Tenant ${t_id} at OFC: tcp:${ofc_ip}:${ofc_port}"
python2 ovxctl.py -n createNetwork tcp:${ofc_ip}:${ofc_port} 10.0.1.0 16

# 20:19:39.274 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Switch (dpids: 00:00:00:00:00:01:11:00 vdpid: List(69888)) in Tenant-Network 1
echo "Creating Switch dpids: 00:00:00:00:00:01:11:00"
python2 ovxctl.py -n createSwitch ${t_id} 00:00:00:00:00:01:11:00

# 20:19:39.295 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Port (phys: 4 virt: 1) at Switch 00:00:00:00:00:01:11:00 for other Switch in Tenant-Network 1
# 20:19:39.312 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Port (phys: 1 virt: 2) at Switch 00:00:00:00:00:01:11:00 for Host 0:00:00:00:01:11 in Tenant-Network 1
echo "Creating Ports (phys: 4 virt: 1), (phys: 1 virt: 2) at Switch 00:00:00:00:00:01:11:00"
python2 ovxctl.py -n createPort ${t_id} 00:00:00:00:00:01:11:00 4 #1
python2 ovxctl.py -n createPort ${t_id} 00:00:00:00:00:01:11:00 1 #2

# 20:19:39.335 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Host 0:00:00:00:01:11 connected to Switch 00:00:00:00:00:01:11:00 at (physPort: 1, vPort 2)
echo "Connecting Host 00:00:00:00:01:11 to Switch 00:00:00:00:00:01:11:00 at (physPort: 1, vPort 2)"
python2 ovxctl.py -n connectHost ${t_id} 00:a4:23:05:00:01:11:00 2 00:00:00:00:01:11

echo "Starting Network for Tenant ${t_id}"
python2 ovxctl.py -n startNetwork ${t_id}


echo "Resource-Allocation 2"
# 20:19:44.184 [cloudAgentSystem-akka.actor.default-dispatcher-4] INFO  agents.NetworkResourceAgent - Mapping hosts List(0:00:00:00:01:11) to virtual tenant network.


echo "Resource-Allocation 3"
# 20:19:49.192 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Mapping hosts List(0:00:00:00:01:12, 0:00:00:00:01:13) to virtual tenant network.

# 20:19:49.204 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Switch (dpids: 00:00:00:00:00:01:12:00 vdpid: List(70144)) in Tenant-Network 1
echo "Creating Switch dpids: 00:00:00:00:00:01:12:00"
python2 ovxctl.py -n createSwitch ${t_id} 00:00:00:00:00:01:12:00

# 20:19:49.215 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Port (phys: 3 virt: 1) at Switch 00:00:00:00:00:01:12:00 for other Switch in Tenant-Network 1
# 20:19:49.227 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Port (phys: 2 virt: 2) at Switch 00:00:00:00:00:01:12:00 for other Switch in Tenant-Network 1
echo "Creating Ports (phys: 3 virt: 1), (phys: 2 virt: 2) at Switch 00:00:00:00:00:01:12:00"
python2 ovxctl.py -n createPort ${t_id} 00:00:00:00:00:01:12:00 3 #1
python2 ovxctl.py -n createPort ${t_id} 00:00:00:00:00:01:12:00 2 #2

# 20:19:49.252 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Link connection between Switches (00:00:00:00:00:01:11:00:4 - 00:00:00:00:00:01:12:00:2) suceeded!
# 20:19:49.268 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Link connection between Switches (00:00:00:00:00:01:12:00:2 - 00:00:00:00:00:01:11:00:4) suceeded!
echo "Connecting Link between Switches (00:00:00:00:00:01:11:00:4 - 00:00:00:00:00:01:12:00:2)"
python2 ovxctl.py -n connectLink ${t_id} 00:a4:23:05:00:01:11:00 1 00:a4:23:05:00:01:12:00 2 spf 1
#echo "Connecting Link between Switches (00:00:00:00:00:01:12:00:2 - 00:00:00:00:00:01:11:00:4)" TODO: No double links via API allowed! 
#python2 ovxctl.py -n connectLink ${t_id} 00:a4:23:05:00:01:12:00 2 00:a4:23:05:00:01:11:00 1 spf 1

# 20:19:49.279 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Port (phys: 2 virt: 3) at Switch 00:00:00:00:00:01:11:00 for Host 0:00:00:00:01:12 in Tenant-Network 1
echo "Creating Port (phys: 2 virt: 3) at Switch 00:00:00:00:00:01:11:00"
python2 ovxctl.py -n createPort ${t_id} 00:00:00:00:00:01:11:00 2 #3

# 20:19:49.292 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Host 0:00:00:00:01:12 connected to Switch 00:00:00:00:00:01:11:00 at (physPort: 2, vPort 3)
echo "Connecting Host 00:00:00:00:01:12 to Switch 00:00:00:00:00:01:11:00 at (physPort: 2, vPort 3)"
python2 ovxctl.py -n connectHost ${t_id} 00:a4:23:05:00:01:11:00 3 00:00:00:00:01:12

# 20:19:49.303 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Created Port (phys: 1 virt: 3) at Switch 00:00:00:00:00:01:12:00 for Host 0:00:00:00:01:13 in Tenant-Network 1
echo "Creating Port (phys: 1 virt: 3) at Switch 00:00:00:00:00:01:12:00"
python2 ovxctl.py -n createPort ${t_id} 00:00:00:00:00:01:12:00 1 #3

# 20:19:49.318 [cloudAgentSystem-akka.actor.default-dispatcher-3] INFO  agents.NetworkResourceAgent - Host 0:00:00:00:01:13 connected to Switch 00:00:00:00:00:01:12:00 at (physPort: 1, vPort 3)
echo "Connecting Host 00:00:00:00:01:13 to Switch 00:00:00:00:00:01:12:00 at (physPort: 1, vPort 3)"
python2 ovxctl.py -n connectHost ${t_id} 00:a4:23:05:00:01:12:00 3 00:00:00:00:01:13